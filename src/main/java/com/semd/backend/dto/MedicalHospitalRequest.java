package com.semd.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record MedicalHospitalRequest(
        @NotBlank(message = "Tên bệnh viện không được để trống")
        @Size(max = 150, message = "Tên bệnh viện không vượt quá 150 ký tự")
        String hospitalName,

        @Size(max = 255, message = "Địa chỉ bệnh viện không vượt quá 255 ký tự")
        String hospitalAddress,

        @NotNull(message = "Kinh độ không được để trống")
        @DecimalMin(value = "-180.0", message = "Kinh độ phải từ -180 đến 180")
        @DecimalMax(value = "180.0", message = "Kinh độ phải từ -180 đến 180")
        Double longitude,

        @NotNull(message = "Vĩ độ không được để trống")
        @DecimalMin(value = "-90.0", message = "Vĩ độ phải từ -90 đến 90")
        @DecimalMax(value = "90.0", message = "Vĩ độ phải từ -90 đến 90")
        Double latitude,

        Map<String, Object> capabilities,

        @Size(max = 15, message = "Số điện thoại liên hệ không vượt quá 15 ký tự")
        String contactPhone,

        Boolean isActive
) {}
