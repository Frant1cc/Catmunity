package org.catmunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.catmunity.model.dto.PostReportDTO;
import org.catmunity.model.entity.PostReport;

public interface PostReportService extends IService<PostReport> {

    void createReport(PostReportDTO reportDTO);
}