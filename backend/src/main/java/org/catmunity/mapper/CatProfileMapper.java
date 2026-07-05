package org.catmunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.catmunity.model.entity.CatProfile;
import org.catmunity.model.entity.DatasetFiles;

@Mapper
public interface CatProfileMapper extends BaseMapper<CatProfile> {
}