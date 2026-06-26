package org.catmunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.context.LoginContext;
import org.catmunity.exception.BusinessNotFoundException;
import org.catmunity.mapper.PostFavoriteMapper;
import org.catmunity.mapper.PostMapper;
import org.catmunity.model.entity.Post;
import org.catmunity.model.entity.PostFavorite;
import org.catmunity.model.entity.User;
import org.catmunity.model.vo.PostVO;
import org.catmunity.result.PageResult;
import org.catmunity.service.PostFavoriteService;
import org.catmunity.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostFavoriteServiceImpl extends ServiceImpl<PostFavoriteMapper, PostFavorite> implements PostFavoriteService {

    private final PostMapper postMapper;
    private final UserService userService;

    @Override
    @Transactional
    public PostVO favoritePost(Long postId) {
        Long userId = LoginContext.getUserId();
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == 1) {
            throw new BusinessNotFoundException("帖子不存在");
        }
        LambdaQueryWrapper<PostFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostFavorite::getPostId, postId).eq(PostFavorite::getUserId, userId);
        if (getOne(wrapper) != null) {
            throw new BusinessNotFoundException("已经收藏过了");
        }
        PostFavorite favorite = new PostFavorite();
        favorite.setPostId(postId);
        favorite.setUserId(userId);
        save(favorite);
        post.setFavoriteCount(post.getFavoriteCount() + 1);
        postMapper.updateById(post);
        log.info("收藏成功：postId={}, userId={}", postId, userId);
        return toVO(post);
    }

    @Override
    @Transactional
    public PostVO unfavoritePost(Long postId) {
        Long userId = LoginContext.getUserId();
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == 1) {
            throw new BusinessNotFoundException("帖子不存在");
        }
        LambdaQueryWrapper<PostFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostFavorite::getPostId, postId).eq(PostFavorite::getUserId, userId);
        PostFavorite favorite = getOne(wrapper);
        if (favorite == null) {
            throw new BusinessNotFoundException("未收藏过此帖子");
        }
        removeById(favorite.getId());
        if (post.getFavoriteCount() > 0) {
            post.setFavoriteCount(post.getFavoriteCount() - 1);
            postMapper.updateById(post);
        }
        log.info("取消收藏成功：postId={}, userId={}", postId, userId);
        return toVO(post);
    }

    @Override
    public Boolean isFavorited(Long postId) {
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            return false;
        }
        LambdaQueryWrapper<PostFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostFavorite::getPostId, postId).eq(PostFavorite::getUserId, userId);
        return getOne(wrapper) != null;
    }

    @Override
    public PageResult<PostVO> getMyFavorites(Integer pageNum, Integer pageSize) {
        Long userId = LoginContext.getUserId();
        Page<PostFavorite> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PostFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostFavorite::getUserId, userId).orderByDesc(PostFavorite::getCreatedAt);
        page = page(page, wrapper);
        List<Long> postIds = page.getRecords().stream().map(PostFavorite::getPostId).collect(Collectors.toList());
        List<Post> posts = postIds.isEmpty() ? new ArrayList<>() : postMapper.selectBatchIds(postIds);
        List<PostVO> voList = posts.stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), page.getPages(), pageNum, pageSize, voList);
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
        vo.setIsLiked(false);
        vo.setIsFavorited(true);
        User user = userService.getById(post.getUserId());
        if (user != null) {
            vo.setUsername(user.getIdentifier());
            vo.setAvatar(user.getAvatar());
        }
        return vo;
    }
}