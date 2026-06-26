package org.catmunity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.jsonwebtoken.lang.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.context.LoginContext;
import org.catmunity.exception.BusinessNotFoundException;
import org.catmunity.mapper.PostMapper;
import org.catmunity.model.dto.PostDTO;
import org.catmunity.model.entity.Post;
import org.catmunity.model.entity.User;
import org.catmunity.model.vo.PostVO;
import org.catmunity.result.PageResult;
import org.catmunity.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private final OSSService ossService;
    private final UserService userService;
    private final PostLikeService postLikeService;
    private final PostFavoriteService postFavoriteService;

    @Override
    public PostVO createPost(PostDTO postDTO) {
        Long userId = LoginContext.getUserId();
        Post post = new Post();
        post.setUserId(userId);
        post.setContent(postDTO.getContent());
        post.setImages(Collections.isEmpty(postDTO.getImages()) ? new ArrayList<>() : postDTO.getImages());
        post.setTags(postDTO.getTags() != null ? postDTO.getTags() : new ArrayList<>());
        post.setStatus(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setFavoriteCount(0);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());

        save(post);
        log.info("创建帖子成功：id={}, userId={}", post.getId(), userId);
        return toVO(post);
    }

    @Override
    public PostVO getPostById(Long id) {
        Post post = getById(id);
        if (post == null || post.getStatus() == 1) {
            throw new BusinessNotFoundException("帖子不存在");
        }
        return toVO(post);
    }

    @Override
    public PageResult<PostVO> getPostPage(Integer pageNum, Integer pageSize, String sortBy) {
        Page<Post> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 0);
        if ("hot".equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(Post::getLikeCount, Post::getCommentCount, Post::getCreatedAt);
        } else {
            wrapper.orderByDesc(Post::getCreatedAt);
        }
        page = page(page, wrapper);
        List<PostVO> voList = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), page.getPages(), pageNum, pageSize, voList);
    }

    @Override
    public List<PostVO> getUserPosts(Long userId) {
        List<Post> posts = list(new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .eq(Post::getStatus, 0)
                .orderByDesc(Post::getCreatedAt));
        return posts.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<PostVO> getFollowingPosts(Integer pageNum, Integer pageSize) {
        Long currentUserId = LoginContext.getUserId();
        List<Long> followingIds = getFollowingUserIds(currentUserId);
        if (followingIds.isEmpty()) {
            return new ArrayList<>();
        }
        Page<Post> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Post::getUserId, followingIds)
                .eq(Post::getStatus, 0)
                .orderByDesc(Post::getCreatedAt);
        page = page(page, wrapper);
        return page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<PostVO> getPostsByTag(String tag, Integer pageNum, Integer pageSize) {
        Page<Post> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Post::getTags, tag)
                .eq(Post::getStatus, 0)
                .orderByDesc(Post::getCreatedAt);
        page = page(page, wrapper);
        return page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public PostVO updatePost(PostDTO postDTO) {
        Post existing = getById(postDTO.getId());
        if (existing == null || existing.getStatus() == 1) {
            throw new BusinessNotFoundException("帖子不存在");
        }
        Long currentUserId = LoginContext.getUserId();
        if (!existing.getUserId().equals(currentUserId)) {
            throw new BusinessNotFoundException("无权修改此帖子");
        }
        existing.setContent(postDTO.getContent());
        existing.setTags(postDTO.getTags());
        updateById(existing);
        log.info("更新帖子成功：id={}", existing.getId());
        return toVO(existing);
    }

    @Override
    public void deletePost(Long id) {
        Post post = getById(id);
        if (post == null) {
            throw new BusinessNotFoundException("帖子不存在");
        }
        Long currentUserId = LoginContext.getUserId();
        if (!post.getUserId().equals(currentUserId)) {
            throw new BusinessNotFoundException("无权删除此帖子");
        }
        post.setStatus(1);
        updateById(post);
        log.info("删除帖子成功：id={}", id);
    }

    @Override
    public List<String> uploadPostImages(MultipartFile[] images) {
        if (images.length > 9) {
            throw new BusinessNotFoundException("图片最多9张");
        }
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : images) {
            if (!file.isEmpty()) {
                String objectName = "post/" + LoginContext.getUserId() + "/" + UUID.randomUUID().toString().replace("-", "") + getExtension(file.getOriginalFilename());
                String url = ossService.uploadFile(file, objectName);
                imageUrls.add(url);
            }
        }
        return imageUrls;
    }

    private List<Long> getFollowingUserIds(Long userId) {
        return new ArrayList<>();
    }

    private PostVO toVO(Post post) {
        PostVO vo = new PostVO();
        BeanUtils.copyProperties(post, vo);
        if (post.getImages() != null) {
            vo.setImages(new ArrayList<>(post.getImages()));
        } else {
            vo.setImages(new ArrayList<>());
        }
        Long currentUserId = LoginContext.getUserId();
        if (currentUserId != null) {
            vo.setIsLiked(postLikeService.isLiked(post.getId()));
            vo.setIsFavorited(postFavoriteService.isFavorited(post.getId()));
        } else {
            vo.setIsLiked(false);
            vo.setIsFavorited(false);
        }
        User user = userService.getById(post.getUserId());
        if (user != null) {
            vo.setUsername(user.getIdentifier());
            vo.setAvatar(user.getAvatar());
        }
        return vo;
    }

    private String getExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return ".jpg";
    }
}