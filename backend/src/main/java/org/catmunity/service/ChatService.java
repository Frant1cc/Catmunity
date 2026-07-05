package org.catmunity.service;

import org.catmunity.model.vo.AiMessage;
import org.catmunity.model.vo.SessionInfo;
import org.catmunity.model.vo.SessionList;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatService {
//    Flux<AiMessage> analyse(String sessionId, String messageId, String content, List<MultipartFile> files);

    void clearHistory(String sessionId);

    List<SessionInfo> getSession(String sessionId);

    List<SessionList> getSessions();

    void stopChat(String sessionId, String messageId);

    Flux<String> chat(String sessionId, String messageId, String content, List<MultipartFile> files);
}