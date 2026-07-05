package org.catmunity.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.model.dto.PostReportDTO;
import org.catmunity.model.vo.PostVO;
import org.catmunity.result.PageResult;
import org.catmunity.result.Result;
import org.catmunity.service.PostFavoriteService;
import org.catmunity.service.PostLikeService;
import org.catmunity.service.PostReportService;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户端-帖子互动管理")
@RestController
@Slf4j
@RequestMapping("/post/interaction")
@RequiredArgsConstructor
public class PostInteractionController {

    private final PostLikeService postLikeService;
    private final PostFavoriteService postFavoriteService;
    private final PostReportService postReportService;

    @Operation(summary = "点赞帖子")
    @PostMapping("/{postId}/like")
    public Result<PostVO> likePost(@PathVariable Long postId) {
        PostVO vo = postLikeService.likePost(postId);
        return Result.success(vo);
    }

    @Operation(summary = "取消点赞")
    @DeleteMapping("/{postId}/like")
    public Result<PostVO> unlikePost(@PathVariable Long postId) {
        PostVO vo = postLikeService.unlikePost(postId);
        return Result.success(vo);
    }

    @Operation(summary = "收藏帖子")
    @PostMapping("/{postId}/favorite")
    public Result<PostVO> favoritePost(@PathVariable Long postId) {
        PostVO vo = postFavoriteService.favoritePost(postId);
        return Result.success(vo);
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{postId}/favorite")
    public Result<PostVO> unfavoritePost(@PathVariable Long postId) {
        PostVO vo = postFavoriteService.unfavoritePost(postId);
        return Result.success(vo);
    }

    @Operation(summary = "获取我的收藏列表")
    @GetMapping("/favorites")
    public Result<PageResult<PostVO>> getMyFavorites(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResult<PostVO> pageResult = postFavoriteService.getMyFavorites(pageNum, pageSize);
        return Result.success(pageResult);
    }

    @Operation(summary = "举报帖子")
    @PostMapping("/report")
    public Result<?> reportPost(@RequestBody PostReportDTO reportDTO) {
        postReportService.createReport(reportDTO);
        return Result.success("举报成功");
    }
}