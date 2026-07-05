package org.catmunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.catmunity.model.dto.ModerationActionDTO;
import org.catmunity.model.dto.ModerationQueryDTO;
import org.catmunity.model.entity.ModerationRecord;
import org.catmunity.model.vo.ModerationRecordVO;
import org.catmunity.result.PageResult;

import java.util.List;

public interface ModerationService extends IService<ModerationRecord> {

    PageResult<ModerationRecordVO> getPendingList(ModerationQueryDTO queryDTO, Integer pageNum, Integer pageSize);

    PageResult<ModerationRecordVO> getModerationLogs(ModerationQueryDTO queryDTO, Integer pageNum, Integer pageSize);

    ModerationRecordVO getModerationDetail(Long id);

    void approveContent(Long moderationId, String comment);

    void rejectContent(Long moderationId, String reason);

    void batchApprove(List<Long> moderationIds);

    void batchReject(List<Long> moderationIds, String reason);

    void autoModerate(Long contentType, Long contentId, String content, Long authorId);
}