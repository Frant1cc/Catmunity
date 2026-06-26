package org.catmunity.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VectorKnowledgeService {
    String processAndStoreDocumentSimple(MultipartFile file);

    List<Document> searchDocuments(String query, int topK);

    List<Document> searchDocuments(SearchRequest searchRequest);

    Resource getFile(String fileId);

    boolean deleteByFileId(String fileId);
}