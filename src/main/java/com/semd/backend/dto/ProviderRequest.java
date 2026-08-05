package com.semd.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProviderRequest(
    @NotNull(message = "ID chủ sở hữu (ownerUserId) không được để trống")
    Integer ownerUserId,

    @NotBlank(message = "Tên đơn vị không được để trống")
    @Size(max = 150, message = "Tên đơn vị không vượt quá 150 ký tự")
    String providerName,

    @NotBlank(message = "Loại đơn vị không được để trống")
    @Size(max = 20, message = "Loại đơn vị không vượt quá 20 ký tự")
    String providerType,

    @Size(max = 100, message = "Giấy phép kinh doanh không vượt quá 100 ký tự")
    String businessLicense,

    @Size(max = 15, message = "Số điện thoại liên hệ không vượt quá 15 ký tự")
    String contactPhone,

    @Size(max = 255, message = "Địa chỉ liên hệ không vượt quá 255 ký tự")
    String contactAddress,

    BigDecimal commissionRate,
    Boolean isVerified,
    Boolean isActive
) {}
