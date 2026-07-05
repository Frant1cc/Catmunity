package org.catmunity.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.catmunity.model.dto.EvaluateDTO;
import org.catmunity.model.dto.EvaluatePageDTO;
import org.catmunity.model.entity.Evaluate;

public interface EvaluateService extends IService<Evaluate> {
    
    Evaluate addEvaluate(EvaluateDTO evaluateDTO);
    
    Evaluate getEvaluateByMessageId(String messageId);
    
    Evaluate getEvaluateBySessionId(String sessionId);
    
    Evaluate updateEvaluate(EvaluateDTO evaluateDTO);
    
    boolean deleteEvaluate(Long id);
    
    boolean deleteEvaluateByMessageId(String messageId);

    Page<Evaluate> getEvaluatePage(EvaluatePageDTO evaluatePageDTO);
}