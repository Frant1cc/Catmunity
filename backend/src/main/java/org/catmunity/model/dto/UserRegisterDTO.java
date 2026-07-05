package org.catmunity.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterDTO {

    @Schema(description = "用户账号", required = true)
    @NotBlank(message = "用户账号不能为空")
    private String identifier;

    @Schema(description = "用户密码（8-16位，包含字母和数字）", required = true)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,16}$", message = "密码需8-16位，包含字母和数字")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "用户联系邮箱", required = true)
    @NotBlank(message = "联系邮箱不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z0-9]{2,6}$", message = "邮箱格式不正确")
    private String email;

    @Schema(description = "验证码", required = true)
    @NotBlank(message = "验证码不能为空")
    private String code;
}