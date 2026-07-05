package org.catmunity.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.model.dto.CommentDTO;
import org.catmunity.model.vo.CommentVO;
import org.catmunity.result.Result;
import org.catmunity.service.CommentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户端-帖子评论管理")
@RestController
@Slf4j
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "发表评论/回复")
    @PostMapping
    public Result<CommentVO> createComment(@RequestBody CommentDTO commentDTO) {
        CommentVO vo = commentService.createComment(commentDTO);
        return Result.success(vo);
    }

    @Operation(summary = "获取帖子的评论列表")
    @GetMapping("/post/{postId}")
    public Result<List<CommentVO>> getCommentsByPostId(@PathVariable Long postId) {
        List<CommentVO> list = commentService.getCommentsByPostId(postId);
        return Result.success(list);
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    public Result<?> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success("删除成功");
    }
}