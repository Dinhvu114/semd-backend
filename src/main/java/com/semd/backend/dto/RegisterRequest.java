package com.semd.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(max = 50, message = "Tên đăng nhập không vượt quá 50 ký tự")
    @Schema(example = "reporter01", description = "Tên đăng nhập")
    String username,

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(max = 100, message = "Mật khẩu không vượt quá 100 ký tự")
    @Schema(example = "123456", description = "Mật khẩu của tài khoản")
    String password,

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên không vượt quá 100 ký tự")
    @Schema(example = "Nguyen Van A", description = "Họ và tên")
    String fullName,

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(max = 15, message = "Số điện thoại không vượt quá 15 ký tự")
    @Schema(example = "0987654321", description = "Số điện thoại")
    String phoneNumber,

    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email không vượt quá 100 ký tự")
    @Schema(example = "reporter01@gmail.com", description = "Địa chỉ email")
    String email,

    @NotBlank(message = "Token xác minh số điện thoại không được để trống")
    @Schema(description = "Token một lần nhận từ API /auth/verify-otp")
    String verificationToken
) {}
