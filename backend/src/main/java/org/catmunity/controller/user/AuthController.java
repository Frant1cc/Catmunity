package org.catmunity.controller.user;

import jakarta.validation.constraints.NotBlank;
import org.catmunity.model.dto.UserLoginDTO;
import org.catmunity.model.dto.UserRegisterDTO;
import org.catmunity.model.dto.UserUpdateDTO;
import org.catmunity.model.vo.UserLoginVO;
import org.catmunity.result.Result;
import org.catmunity.service.UserService;
import org.catmunity.context.LoginContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import static org.catmunity.constants.MessageConstant.*;
import static org.catmunity.constants.RedisConstant.EMAIL_CODE_KEY;

/**
 * 统一登录认证控制器
 */
@Tag(name = "认证管理")
@RestController
@Slf4j
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 统一登录接口（无需修改，保持原有逻辑）
     */
    @PostMapping("/login")
    @Operation(summary = "统一登录接口")
    public Result<UserLoginVO> login(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        UserLoginVO loginVO = userService.login(userLoginDTO);
        return loginVO != null ? Result.success("登录成功",loginVO) : Result.error(LOGIN_FAILED);
    }

    /**
     * 退出登录接口
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录接口")
    public Result<String> logout(
            @RequestHeader(value = "token", required = false) String token
    ) {
        // 1. 校验token是否为空
        if (!StringUtils.hasText(token)) {
            return Result.error(TOKEN_NOT_NULL); // 提示“token不能为空”
        }

        // 2.调用Service清理登录态（删除Redis中的token关联）
        userService.logout(token);

        // 3. 返回成功响应
        return Result.success(LOGOUT_SUCCESS);
    }

    @PutMapping("/update")
    @Operation(summary = "修改用户信息")
    public Result<String> updateUserInfo(
            @RequestHeader(value = "token", required = false) String token,
            @Valid @RequestBody UserUpdateDTO updateDTO
    ) {
        if (!StringUtils.hasText(token)) {
            return Result.error(TOKEN_NOT_NULL);
        }
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            return Result.error(USER_MESSAGE_ACCESS_ERROR);
        }
        userService.updateUserInfo(userId, updateDTO);
        return Result.success(UPDATE_SUCCESS);
    }

    @PostMapping("/register/sendCode")
    @Operation(summary = "发送注册邮箱验证码")
    public Result<String> sendRegisterCode(String email) {
        if (!StringUtils.hasText(email)) {
            return Result.error("邮箱不能为空");
        }
        boolean success = userService.sendRegisterEmailCode(email);
        return success ? Result.success("验证码发送成功") : Result.error(CODE_SEND_FAIL);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<UserLoginVO> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        UserLoginVO loginVO = userService.register(registerDTO);
        return loginVO != null ? Result.success(REGISTER_SUCCESS, loginVO) : Result.error(SYSTEM_ERROR);
    }
}