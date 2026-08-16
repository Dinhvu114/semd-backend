package com.semd.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.dto.request.UpdateResourceLocationRequest;
import com.semd.backend.dto.response.DispatchResourceResponse;
import com.semd.backend.security.UserPrincipal;
import com.semd.backend.service.DispatchResourceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/driver-resource")
@Tag(
    name = "DRIVER - Resource",
    description = "API quản lý xe cấp cứu cho tài xế"
)
public class DriverResourceController {
    private final DispatchResourceService resourceService;

    public DriverResourceController(
        DispatchResourceService resourceService
    ) {
        this.resourceService = resourceService;
    }
    @GetMapping
    @PreAuthorize("hasAnyRole('DRIVER')")
    @Operation(summary = "Xem chi tiết xe cứu thương hiện tại")
    public ResponseEntity<BaseResponse<DispatchResourceResponse>> getMyResource(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DispatchResourceResponse result =
                resourceService.getMyResource(principal.getId());

        return ResponseEntity.ok(
                BaseResponse.success(result)
        );
    }

    @PatchMapping("/location")
    @PreAuthorize("hasAnyRole('DRIVER')")
    @Operation(summary = "Cập nhật vị trí xe cứu thương")
    public ResponseEntity<BaseResponse<Void>> updateLocation(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody UpdateResourceLocationRequest request
    ) {

        resourceService.updateMyResourceLocation(principal.getId(), request);

        return ResponseEntity.ok(
            BaseResponse.success(
                200,
                "Cập nhật vị trí phương tiện thành công",            
                null
            )
        );
    }
}
