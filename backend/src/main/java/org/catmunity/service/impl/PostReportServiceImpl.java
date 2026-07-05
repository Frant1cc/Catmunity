package org.catmunity.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.context.LoginContext;
import org.catmunity.exception.BusinessNotFoundException;
import org.catmunity.mapper.PostMapper;
import org.catmunity.mapper.PostReportMapper;
import org.catmunity.model.dto.PostReportDTO;
import org.catmunity.model.entity.Post;
import org.catmunity.model.entity.PostReport;
import org.catmunity.service.PostReportService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostReportServiceImpl extends ServiceImpl<PostReportMapper, PostReport> implements PostReportService {

    private final PostMapper postMapper;

    @Override
    public void createReport(PostReportDTO reportDTO) {
        Long reporterId = LoginContext.getUserId();
        Post post = postMapper.selectById(reportDTO.getPostId());
        if (post == null || post.getStatus() == 1) {
            throw new BusinessNotFoundException("帖子不存在");
        }
        if (post.getUserId().equals(reporterId)) {
            throw new BusinessNotFoundException("不能举报自己的帖子");
        }
        PostReport report = new PostReport();
        report.setPostId(reportDTO.getPostId());
        report.setReporterId(reporterId);
        report.setReportedUserId(reportDTO.getReportedUserId());
        report.setReportType(reportDTO.getReportType());
        report.setDescription(reportDTO.getDescription());
        report.setStatus(0);
        save(report);
        log.info("创建举报成功：postId={}, reporterId={}, type={}", reportDTO.getPostId(), reporterId, reportDTO.getReportType());
    }
}