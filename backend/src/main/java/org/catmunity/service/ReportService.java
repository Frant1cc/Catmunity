package org.catmunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.catmunity.model.entity.Report;
import org.catmunity.model.vo.ReportVO;
import org.catmunity.result.PageResult;

import java.util.List;

public interface ReportService extends IService<Report> {

    void createReport(Integer contentType, Long contentId, Long reporterId, Integer reasonType, String reasonDescription);

    PageResult<ReportVO> getPendingReports(Integer pageNum, Integer pageSize);

    PageResult<ReportVO> getAllReports(Integer pageNum, Integer pageSize);

    void handleReport(Long reportId, Long handlerId, Integer action, String result);

    void batchHandleReports(List<Long> reportIds, Long handlerId, Integer action, String result);
}