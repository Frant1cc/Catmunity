package org.catmunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.catmunity.model.entity.CatHealthEvent;

@Mapper
public interface CatHealthEventMapper extends BaseMapper<CatHealthEvent> {
}