package org.catmunity.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.model.dto.PostDTO;
import org.catmunity.model.vo.PostVO;
import org.catmunity.result.PageResult;
import org.catmunity.result.Result;
import org.catmunity.service.PostService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "用户端-社区帖子管理")
@RestController
@Slf4j
@RequestMapping("/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "发布帖子")
    @PostMapping
    public Result<PostVO> createPost(@RequestBody PostDTO postDTO) {
        PostVO vo = postService.createPost(postDTO);
        return Result.success(vo);
    }

    @Operation(summary = "获取帖子详情")
    @GetMapping("/{id}")
    public Result<PostVO> getPostById(@PathVariable Long id) {
        PostVO vo = postService.getPostById(id);
        return Result.success(vo);
    }

    @Operation(summary = "首页信息流")
    @GetMapping("/feed")
    public Result<PageResult<PostVO>> getPostFeed(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "time") String sortBy) {
        PageResult<PostVO> pageResult = postService.getPostPage(pageNum, pageSize, sortBy);
        return Result.success(pageResult);
    }

    @Operation(summary = "获取用户发布的帖子")
    @GetMapping("/user/{userId}")
    public Result<List<PostVO>> getUserPosts(@PathVariable Long userId) {
        List<PostVO> list = postService.getUserPosts(userId);
        return Result.success(list);
    }

    @Operation(summary = "获取关注用户的动态")
    @GetMapping("/following")
    public Result<List<PostVO>> getFollowingPosts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        List<PostVO> list = postService.getFollowingPosts(pageNum, pageSize);
        return Result.success(list);
    }

    @Operation(summary = "根据话题标签获取帖子")
    @GetMapping("/tag/{tag}")
    public Result<List<PostVO>> getPostsByTag(
            @PathVariable String tag,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        List<PostVO> list = postService.getPostsByTag(tag, pageNum, pageSize);
        return Result.success(list);
    }

    @Operation(summary = "更新帖子")
    @PutMapping
    public Result<PostVO> updatePost(@RequestBody PostDTO postDTO) {
        PostVO vo = postService.updatePost(postDTO);
        return Result.success(vo);
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/{id}")
    public Result<?> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "给帖子上传图片")
    @PostMapping("/images")
    public Result<List<String>> uploadPostImages(@RequestParam("files") MultipartFile[] files) {
        List<String> list = postService.uploadPostImages(files);
        return Result.success(list);
    }
}