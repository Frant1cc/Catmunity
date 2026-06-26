package org.catmunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.catmunity.model.entity.Report;

@Mapper
public interface ReportMapper extends BaseMapper<Report> {
}