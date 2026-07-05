package org.catmunity.service.impl;

import org.catmunity.context.LoginContext;
import org.catmunity.mapper.DatasetFilesMapper;
import org.catmunity.model.entity.DatasetFiles;
import org.catmunity.service.VectorKnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
public class VectorKnowledgeServiceImpl implements VectorKnowledgeService {

    @Autowired
    @Qualifier("catmunityVectorStore")
    private VectorStore vectorStore;

    @Autowired
    private DatasetFilesMapper datasetFilesMapper;

    @Value("${file.upload.path:./catmunity_dataset}")
    private String uploadPath;

    private static final int BATCH_SIZE = 10;

    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(
            Arrays.asList(".pdf", ".docx", ".html", ".htm", ".md", ".markdown", ".json", ".txt")
    );

    private static final String TESSDATA_PATH = "src/main/resources/tessdata";

    @Override
    public String processAndStoreDocumentSimple(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        if (!SUPPORTED_EXTENSIONS.contains(fileExtension)) {
            throw new IllegalArgumentException("不支持的文件类型，仅支持：PDF、DOCX、HTML、Markdown、JSON、TXT");
        }

        String storedFilePath = saveFileToDisk(file, originalFilename, fileExtension);

        DatasetFiles datasetFile = DatasetFiles.builder()
                .name(originalFilename)
                .filePath(storedFilePath)
                .fileType(fileExtension)
                .fileSize(file.getSize())
                .userId(LoginContext.getUserId())
                .disabled(1)
                .createdAt(new Date())
                .hitCount(0)
                .build();

        datasetFilesMapper.insert(datasetFile);

        try {
            List<Document> documents = loadAndChunkDocument(storedFilePath, fileExtension, datasetFile.getId().toString());
            if (!documents.isEmpty()) {
                int totalWritten = batchWriteDocuments(documents);
                log.info("成功处理并存储文档: {}, chunk数量: {}, 实际写入: {}",
                        originalFilename, documents.size(), totalWritten);
            }
            return datasetFile.getId().toString();
        } catch (Exception e) {
            log.error("处理文档失败: {}", originalFilename, e);
            throw new RuntimeException("文档处理失败: " + e.getMessage(), e);
        }
    }

    private int batchWriteDocuments(List<Document> documents) {
        int totalWritten = 0;
        int batchCount = (int) Math.ceil((double) documents.size() / BATCH_SIZE);

        log.info("【知识库分批写入】总共 {} 条文档，分为 {} 批，每批最多 {} 条",
                documents.size(), batchCount, BATCH_SIZE);

        for (int i = 0; i < documents.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, documents.size());
            List<Document> batch = documents.subList(i, endIndex);
            int batchNumber = (i / BATCH_SIZE) + 1;

            try {
                vectorStore.accept(batch);
                totalWritten += batch.size();
                log.info("【知识库分批写入】第 {} / {} 批完成，写入 {} 条文档",
                        batchNumber, batchCount, batch.size());
            } catch (Exception e) {
                log.error("【知识库分批写入】第 {} / {} 批失败: {}",
                        batchNumber, batchCount, e.getMessage());
                throw new RuntimeException("批量写入文档失败", e);
            }
        }

        return totalWritten;
    }

    private String saveFileToDisk(MultipartFile file, String originalFilename, String extension) {
        String fileName = originalFilename;
        String dirPath = uploadPath;
        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("创建文件目录失败");
        }

        File targetFile = new File(dirPath + File.separator + fileName);
        int counter = 1;
        while (targetFile.exists()) {
            String nameWithoutExt = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
            fileName = nameWithoutExt + "_(" + counter + ")" + ext;
            targetFile = new File(dirPath + File.separator + fileName);
            counter++;
        }

        String fullPath = dirPath + File.separator + fileName;

        try (InputStream inputStream = file.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(fullPath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } catch (IOException e) {
            log.error("文件保存失败: {}", fullPath, e);
            throw new RuntimeException("文件保存失败", e);
        }

        File savedFile = new File(fullPath);
        if (!savedFile.exists() || savedFile.length() == 0) {
            log.error("文件保存异常: {} 不存在或大小为0", fullPath);
            throw new RuntimeException("文件保存异常");
        }
        log.info("文件保存成功: {}, 大小: {} bytes", fullPath, savedFile.length());
        return fullPath;
    }

    private List<Document> loadAndChunkDocument(String filePath, String extension, String metadataId) {
        Resource resource = new FileSystemResource(filePath);
        List<Document> documents = new ArrayList<>();
        if (!resource.exists()) {
            throw new RuntimeException("文件不存在");
        }

        if (".pdf".equals(extension)) {
            documents = loadPdfDocument(resource);
        } else {
            documents = loadGenericDocument(resource);
        }

        for (Document doc : documents) {
            doc.getMetadata().put("file_id", metadataId);
        }

        TokenTextSplitter textSplitter = new TokenTextSplitter(
                500,
                1000,
                5,
                2000,
                true
        );

        return textSplitter.apply(documents);
    }

    private List<Document> loadPdfDocument(Resource resource) {
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                resource,
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                        .withPagesPerDocument(1)
                        .build()
        );
        List<Document> documents = reader.read();

        return documents;
    }

    private List<Document> loadGenericDocument(Resource resource) {
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        return reader.get();
    }

    @Override
    public List<Document> searchDocuments(String query, int topK) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        return vectorStore.similaritySearch(searchRequest);
    }

    @Override
    public List<Document> searchDocuments(SearchRequest searchRequest) {
        return vectorStore.similaritySearch(searchRequest);
    }

    @Override
    public Resource getFile(String fileId) {
        DatasetFiles datasetFile = datasetFilesMapper.selectById(fileId);
        if (datasetFile == null || datasetFile.getDisabled() == -1) {
            return null;
        }

        File file = new File(datasetFile.getFilePath());
        if (!file.exists()) {
            log.warn("文件不存在: {}", datasetFile.getFilePath());
            return null;
        }

        return new FileSystemResource(file);
    }

    @Override
    public boolean deleteByFileId(String fileId) {
        DatasetFiles datasetFile = datasetFilesMapper.selectById(fileId);
        if (datasetFile == null) {
            return false;
        }

        File file = new File(datasetFile.getFilePath());
        if (file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                log.warn("物理文件删除失败: {}", datasetFile.getFilePath());
            }
        }

        datasetFile.setDisabled(-1);
        datasetFile.setDisabledAt(new Date());
        int result = datasetFilesMapper.updateById(datasetFile);

        return result > 0;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }
}