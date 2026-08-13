package com.semd.backend.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateResourceCapabilitiesRequest(

        @NotNull(message = "Danh sách trang thiết bị không được để trống")
        List<String> capabilities

) {
}