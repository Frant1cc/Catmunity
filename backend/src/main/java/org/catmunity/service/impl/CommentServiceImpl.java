package org.catmunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.context.LoginContext;
import org.catmunity.exception.BusinessNotFoundException;
import org.catmunity.mapper.CommentMapper;
import org.catmunity.mapper.PostMapper;
import org.catmunity.model.dto.CommentDTO;
import org.catmunity.model.entity.Comment;
import org.catmunity.model.entity.Post;
import org.catmunity.model.entity.User;
import org.catmunity.model.vo.CommentVO;
import org.catmunity.service.CommentService;
import org.catmunity.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final PostMapper postMapper;
    private final UserService userService;

    @Override
    @Transactional
    public CommentVO createComment(CommentDTO commentDTO) {
        Long userId = LoginContext.getUserId();
        Post post = postMapper.selectById(commentDTO.getPostId());
        if (post == null || post.getStatus() == 1) {
            throw new BusinessNotFoundException("帖子不存在");
        }
        Comment comment = new Comment();
        comment.setPostId(commentDTO.getPostId());
        comment.setUserId(userId);
        comment.setParentId(commentDTO.getParentId());
        comment.setContent(commentDTO.getContent());
        comment.setStatus(0);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        save(comment);
        post.setCommentCount(post.getCommentCount() + 1);
        postMapper.updateById(post);
        log.info("创建评论成功：id={}, postId={}", comment.getId(), comment.getPostId());
        return toVO(comment);
    }

    @Override
    public List<CommentVO> getCommentsByPostId(Long postId) {
        List<Comment> allComments = list(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .eq(Comment::getStatus, 0)
                .orderByAsc(Comment::getCreatedAt));
        List<CommentVO> allVO = allComments.stream().map(this::toVO).collect(Collectors.toList());
        return buildCommentTree(allVO);
    }

    @Override
    @Transactional
    public void deleteComment(Long id) {
        Comment comment = getById(id);
        if (comment == null) {
            throw new BusinessNotFoundException("评论不存在");
        }
        Long currentUserId = LoginContext.getUserId();
        if (!comment.getUserId().equals(currentUserId)) {
            throw new BusinessNotFoundException("无权删除此评论");
        }
        comment.setStatus(1);
        updateById(comment);
        Post post = postMapper.selectById(comment.getPostId());
        if (post != null && post.getCommentCount() > 0) {
            post.setCommentCount(post.getCommentCount() - 1);
            postMapper.updateById(post);
        }
        log.info("删除评论成功：id={}", id);
    }

    private List<CommentVO> buildCommentTree(List<CommentVO> comments) {
        List<CommentVO> rootComments = new ArrayList<>();
        List<CommentVO> childComments = new ArrayList<>();
        for (CommentVO comment : comments) {
            if (comment.getParentId() == null) {
                rootComments.add(comment);
            } else {
                childComments.add(comment);
            }
        }
        for (CommentVO parent : rootComments) {
            parent.setChildren(findChildren(parent.getId(), childComments));
        }
        return rootComments;
    }

    private List<CommentVO> findChildren(Long parentId, List<CommentVO> allChildren) {
        List<CommentVO> children = new ArrayList<>();
        for (CommentVO child : allChildren) {
            if (child.getParentId().equals(parentId)) {
                child.setChildren(findChildren(child.getId(), allChildren));
                children.add(child);
            }
        }
        return children;
    }

    private CommentVO toVO(Comment comment) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);
        User user = userService.getById(comment.getUserId());
        if (user != null) {
            vo.setUsername(user.getIdentifier());
            vo.setAvatar(user.getAvatar());
        }
        vo.setChildren(new ArrayList<>());
        return vo;
    }
}