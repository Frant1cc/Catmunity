package org.catmunity.controller.ai;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import org.catmunity.mapper.DatasetFilesMapper;
import org.catmunity.model.dto.DatasetFilesPageDTO;
import org.catmunity.model.entity.DatasetFiles;
import org.catmunity.result.Result;
import org.catmunity.service.DataSetFilesService;
import org.catmunity.service.VectorKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Tag(name = "管理端-知识库管理")
@Slf4j
@RestController
@RequestMapping("/ai/dataset")
public class DataSetController {
    
    @Autowired
    private VectorKnowledgeService vectorKnowledgeService;

    @Autowired
    private DataSetFilesService dataSetFilesService;

    
    @Operation(summary = "上传文件到知识库")
    @PostMapping("/upload")
    public Result uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileId = vectorKnowledgeService.processAndStoreDocumentSimple(file);
            return Result.success(fileId);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "下载文件")
    @GetMapping("/file/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable String fileId) {
        Resource resource = vectorKnowledgeService.getFile(fileId);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        
        String fileName = URLEncoder.encode(
                Objects.requireNonNull(resource.getFilename()),
                StandardCharsets.UTF_8);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @Operation(summary = "分页获取知识库文件列表")
    @GetMapping("/page")
    public Result<IPage<DatasetFiles>> listFiles( Integer current,  Integer size) {
        return Result.success(dataSetFilesService.getDatasetFilesPage(current, size));
    }
}