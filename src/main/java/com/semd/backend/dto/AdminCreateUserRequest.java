package com.semd.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.semd.backend.entity.RoleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminCreateUserRequest(
        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Size(max = 50, message = "Tên đăng nhập không vượt quá 50 ký tự")
        String username,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, max = 100, message = "Mật khẩu phải có từ 6 đến 100 ký tự")
        String password,

        @NotBlank(message = "Họ và tên không được để trống")
        @Size(max = 100, message = "Họ và tên không vượt quá 100 ký tự")
        String fullName,

        @JsonAlias("phoneNumber")
        @NotBlank(message = "Số điện thoại không được để trống")
        @Size(max = 15, message = "Số điện thoại không vượt quá 15 ký tự")
        String phone,

        @Email(message = "Email không đúng định dạng")
        @Size(max = 100, message = "Email không vượt quá 100 ký tự")
        String email,

        @NotNull(message = "Vai trò không được để trống")
        RoleCode role,

        Integer providerId
) {
}
