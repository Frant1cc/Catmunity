package org.catmunity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.catmunity.model.dto.CommentDTO;
import org.catmunity.model.entity.Comment;
import org.catmunity.model.vo.CommentVO;

import java.util.List;

public interface CommentService extends IService<Comment> {

    CommentVO createComment(CommentDTO commentDTO);

    List<CommentVO> getCommentsByPostId(Long postId);

    void deleteComment(Long id);
}