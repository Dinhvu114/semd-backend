package com.semd.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(max = 50, message = "Tên đăng nhập không vượt quá 50 ký tự")
    String username,

    @Size(max = 100, message = "Mật khẩu không vượt quá 100 ký tự")
    String password,

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên không vượt quá 100 ký tự")
    String fullName,

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 15, message = "Số điện thoại không vượt quá 15 ký tự")
    String phoneNumber,

    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email không vượt quá 100 ký tự")
    String email,

    @NotBlank(message = "Vai trò không được để trống")
    @Size(max = 20, message = "Vai trò không vượt quá 20 ký tự")
    String role,

    Boolean isActive
) {}
