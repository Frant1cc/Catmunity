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
public class UserUpdateDTO {

    @Schema(description = "用户新密码（8-16位，包含字母和数字）", required = false)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,16}$", message = "密码需8-16位，包含字母和数字")
    private String newPassword;

    @Schema(description = "用户新账号", required = false)
    private String newIdentifier;
}