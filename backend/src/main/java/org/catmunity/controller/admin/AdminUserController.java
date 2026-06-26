package org.catmunity.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.catmunity.context.LoginContext;
import org.catmunity.model.dto.BanUserDTO;
import org.catmunity.model.dto.UserQueryDTO;
import org.catmunity.model.dto.UserUpdateDTO;
import org.catmunity.model.vo.UserAdminVO;
import org.catmunity.result.PageResult;
import org.catmunity.result.Result;
import org.catmunity.service.UserService;
import org.catmunity.utils.MailUtils;
import org.catmunity.utils.VerCodeGenerateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.catmunity.constants.MessageConstant.*;

@Tag(name = "管理端-用户管理")
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private final UserService userService;

    @Autowired
    private MailUtils mailUtils;

    @Operation(summary = "获取用户列表")
    @GetMapping("/list")
    public Result<PageResult<UserAdminVO>> getUserList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAtStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAtEnd,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        UserQueryDTO queryDTO = UserQueryDTO.builder()
                .keyword(keyword)
                .type(type)
                .status(status)
                .createdAtStart(createdAtStart)
                .createdAtEnd(createdAtEnd)
                .build();

        PageResult<UserAdminVO> result = userService.getAdminUserPage(queryDTO, pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{userId}")
    public Result<UserAdminVO> getUserDetail(@PathVariable Long userId) {
        UserAdminVO userDetail = userService.getUserAdminDetail(userId);
        return Result.success(userDetail);
    }

    @Operation(summary = "封禁用户")
    @PostMapping("/ban")
    public Result<?> banUser(@RequestBody BanUserDTO banUserDTO) {
        userService.banUser(banUserDTO.getUserId(), banUserDTO.getReason());
        return Result.success("用户已封禁");
    }

    @Operation(summary = "解封用户")
    @PostMapping("/unban/{userId}")
    public Result<?> unbanUser(@PathVariable Long userId) {
        userService.unbanUser(userId);
        return Result.success("用户已解封");
    }

    @Operation(summary = "分配用户角色")
    @PostMapping("/role")
    public Result<?> assignRole(
            @RequestParam Long userId,
            @RequestParam Integer userType) {
        userService.assignUserRole(userId, userType);
        return Result.success("角色分配成功");
    }

    @Operation(summary = "批量封禁用户")
    @PostMapping("/ban/batch")
    public Result<?> batchBanUsers(@RequestBody List<Long> userIds) {
        userService.batchBanUsers(userIds);
        return Result.success("批量封禁成功");
    }

    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/email/code")
    public Result<String> sendEmailCode(String email) {
        boolean success = mailUtils.sendEmailCode(email, VerCodeGenerateUtil.getVerCode());
        if (success) {
            return Result.success("验证码已发送，请注意查收（5分钟内有效）");
        } else {
            return Result.error("验证码发送失败，请稍后重试");
        }
    }

    @PutMapping("/update")
    @Operation(summary = "修改当前用户信息")
    public Result<String> updateUserInfo(
            @Valid @RequestBody UserUpdateDTO updateDTO
    ) {
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            return Result.error(USER_MESSAGE_ACCESS_ERROR);
        }
        userService.updateUserInfo(userId, updateDTO);
        return Result.success(UPDATE_SUCCESS);
    }
}