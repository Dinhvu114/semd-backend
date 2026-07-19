package com.semd.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceTypeRequest(
    @NotBlank(message = "Mã loại dịch vụ không được để trống")
    @Size(max = 50, message = "Mã loại dịch vụ không vượt quá 50 ký tự")
    String typeCode,

    @NotBlank(message = "Tên hiển thị không được để trống")
    @Size(max = 100, message = "Tên hiển thị không vượt quá 100 ký tự")
    String displayName,

    @NotNull(message = "Trọng số ưu tiên không được để trống")
    Integer priorityWeight
) {}
