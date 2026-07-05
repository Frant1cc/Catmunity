package org.catmunity.controller.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.catmunity.constants.MessageConstant.UPDATE_SUCCESS;
import static org.catmunity.constants.MessageConstant.USER_MESSAGE_ACCESS_ERROR;

@Tag(name = "用户端-用户管理")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @Autowired
    private MailUtils mailUtils;

    @Operation(summary = "获取当前用户详情")
    @GetMapping("/current")
    public Result<UserAdminVO> getUserAdminDetail() {
        UserAdminVO userDetail = userService.getUserAdminDetail(LoginContext.getUserId());
        return Result.success(userDetail);
    }

    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/email/code")
    public Result<String> sendEmailCode(
            String email) {
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