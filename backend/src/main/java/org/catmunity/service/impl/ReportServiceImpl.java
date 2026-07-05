package org.catmunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.context.LoginContext;
import org.catmunity.exception.BusinessException;
import org.catmunity.mapper.ReportMapper;
import org.catmunity.model.entity.Report;
import org.catmunity.Enum.ContentType;
import org.catmunity.Enum.ViolationType;
import org.catmunity.model.vo.ReportVO;
import org.catmunity.result.PageResult;
import org.catmunity.service.ReportService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements ReportService {

    private final ReportMapper reportMapper;

    @Override
    @Transactional
    public void createReport(Integer contentType, Long contentId, Long reporterId, Integer reasonType, String reasonDescription) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getContentType, contentType)
                .eq(Report::getContentId, contentId)
                .eq(Report::getReporterId, reporterId)
                .eq(Report::getStatus, 0);

        Report existingReport = reportMapper.selectOne(wrapper);
        if (existingReport != null) {
            throw new BusinessException("您已举报过该内容，请勿重复提交");
        }

        Report report = Report.builder()
                .contentType(contentType)
                .contentId(contentId)
                .reporterId(reporterId)
                .reasonType(reasonType)
                .reasonDescription(reasonDescription)
                .status(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        reportMapper.insert(report);
        log.info("用户 {} 举报内容 {}:{}，原因: {}", reporterId, contentType, contentId, reasonDescription);
    }

    @Override
    public PageResult<ReportVO> getPendingReports(Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getStatus, 0)
                .orderByAsc(Report::getCreatedAt);

        Page<Report> page = new Page<>(pageNum, pageSize);
        page = page(page, wrapper);

        List<ReportVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(page.getTotal(), page.getPages(), pageNum, pageSize, voList);
    }

    @Override
    public PageResult<ReportVO> getAllReports(Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Report::getCreatedAt);

        Page<Report> page = new Page<>(pageNum, pageSize);
        page = page(page, wrapper);

        List<ReportVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(page.getTotal(), page.getPages(), pageNum, pageSize, voList);
    }

    @Override
    @Transactional
    public void handleReport(Long reportId, Long handlerId, Integer action, String result) {
        Report report = this.getById(reportId);
        if (report == null) {
            throw new BusinessException("举报记录不存在");
        }
        if (report.getStatus() != 0) {
            throw new BusinessException("该举报已处理");
        }

        report.setHandlerId(handlerId);
        report.setHandlerResult(result);
        report.setHandledAt(LocalDateTime.now());
        report.setStatus(action == 1 ? 1 : 2);
        report.setUpdatedAt(LocalDateTime.now());

        this.updateById(report);
        log.info("管理员 {} 处理举报 {}，处理结果: {}", handlerId, reportId, result);
    }

    @Override
    @Transactional
    public void batchHandleReports(List<Long> reportIds, Long handlerId, Integer action, String result) {
        for (Long reportId : reportIds) {
            try {
                handleReport(reportId, handlerId, action, result);
            } catch (Exception e) {
                log.error("批量处理举报 {} 失败", reportId, e);
            }
        }
    }

    private ReportVO toVO(Report report) {
        ReportVO vo = new ReportVO();
        BeanUtils.copyProperties(report, vo);

        vo.setContentTypeStr(ContentType.fromCode(report.getContentType()) != null ?
                ContentType.fromCode(report.getContentType()).getDescription() : "未知");

        vo.setReasonDescription(ViolationType.fromCode(report.getReasonType()) != null ?
                ViolationType.fromCode(report.getReasonType()).getDescription() :
                (report.getReasonDescription() != null ? report.getReasonDescription() : "未知"));

        vo.setStatusStr(switch (report.getStatus()) {
            case 0 -> "待处理";
            case 1 -> "已处理";
            case 2 -> "已忽略";
            default -> "未知";
        });

        return vo;
    }
}