package org.catmunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.catmunity.model.entity.Comment;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}