package com.semd.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record EdgeNodeRequest(
    @NotBlank(message = "Tên vùng quản lý không được để trống")
    @Size(max = 100, message = "Tên vùng quản lý không vượt quá 100 ký tự")
    String nodeName,

    List<CoordinateDto> coverageArea,

    Boolean isActive
) {}
