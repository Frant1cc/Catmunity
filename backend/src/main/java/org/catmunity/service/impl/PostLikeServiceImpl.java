package org.catmunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.context.LoginContext;
import org.catmunity.exception.BusinessNotFoundException;
import org.catmunity.mapper.PostLikeMapper;
import org.catmunity.mapper.PostMapper;
import org.catmunity.model.entity.Post;
import org.catmunity.model.entity.PostLike;
import org.catmunity.model.entity.User;
import org.catmunity.model.vo.PostVO;
import org.catmunity.service.PostLikeService;
import org.catmunity.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostLikeServiceImpl extends ServiceImpl<PostLikeMapper, PostLike> implements PostLikeService {

    private final PostMapper postMapper;
    private final UserService userService;

    @Override
    @Transactional
    public PostVO likePost(Long postId) {
        Long userId = LoginContext.getUserId();
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == 1) {
            throw new BusinessNotFoundException("帖子不存在");
        }
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId);
        if (getOne(wrapper) != null) {
            throw new BusinessNotFoundException("已经点赞过了");
        }
        PostLike like = new PostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        save(like);
        post.setLikeCount(post.getLikeCount() + 1);
        postMapper.updateById(post);
        log.info("点赞成功：postId={}, userId={}", postId, userId);
        return toVO(post);
    }

    @Override
    @Transactional
    public PostVO unlikePost(Long postId) {
        Long userId = LoginContext.getUserId();
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == 1) {
            throw new BusinessNotFoundException("帖子不存在");
        }
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId);
        PostLike like = getOne(wrapper);
        if (like == null) {
            throw new BusinessNotFoundException("未点赞过此帖子");
        }
        removeById(like.getId());
        if (post.getLikeCount() > 0) {
            post.setLikeCount(post.getLikeCount() - 1);
            postMapper.updateById(post);
        }
        log.info("取消点赞成功：postId={}, userId={}", postId, userId);
        return toVO(post);
    }

    @Override
    public Boolean isLiked(Long postId) {
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            return false;
        }
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId);
        return getOne(wrapper) != null;
    }

    private PostVO toVO(Post post) {
        PostVO vo = new PostVO();
        BeanUtils.copyProperties(post, vo);
        if (post.getImages() != null) {
            vo.setImages(new ArrayList<>(post.getImages()));
        } else {
            vo.setImages(new ArrayList<>());
        }
        if (post.getTags() != null) {
            vo.setTags(new ArrayList<>(post.getTags()));
        } else {
            vo.setTags(new ArrayList<>());
        }
        vo.setIsLiked(true);
        vo.setIsFavorited(false);
        User user = userService.getById(post.getUserId());
        if (user != null) {
            vo.setUsername(user.getIdentifier());
            vo.setAvatar(user.getAvatar());
        }
        return vo;
    }
}