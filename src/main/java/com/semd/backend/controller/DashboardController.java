package com.semd.backend.controller;

import com.semd.backend.dto.dashboard.DashboardFilter;
import com.semd.backend.dto.dashboard.DashboardResponse;
import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.security.UserPrincipal;
import com.semd.backend.service.DashboardExcelService;
import com.semd.backend.service.DashboardQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(
        name = "Dashboard",
        description = "Dashboard thống kê và xuất Excel theo vai trò"
)
public class DashboardController {

    private final DashboardQueryService query;
    private final DashboardExcelService excel;

    public DashboardController(
            DashboardQueryService query,
            DashboardExcelService excel
    ) {
        this.query = query;
        this.excel = excel;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dashboard Admin")
    public ResponseEntity<BaseResponse<DashboardResponse>> admin(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam(
                    name = "timezone",
                    defaultValue = "Asia/Ho_Chi_Minh"
            )
            String timezone,

            @RequestParam(name = "providerId", required = false)
            Integer providerId,

            @RequestParam(
                    name = "granularity",
                    defaultValue = "AUTO"
            )
            String granularity,

            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DashboardResponse result = query.admin(
                filter(
                        from,
                        to,
                        timezone,
                        providerId,
                        granularity
                ),
                principal
        );

        return ResponseEntity.ok(
                BaseResponse.success(result)
        );
    }

    @GetMapping("/admin/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xuất dashboard Admin")
    public ResponseEntity<byte[]> adminExport(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam(
                    name = "timezone",
                    defaultValue = "Asia/Ho_Chi_Minh"
            )
            String timezone,

            @RequestParam(name = "providerId", required = false)
            Integer providerId,

            @RequestParam(
                    name = "granularity",
                    defaultValue = "AUTO"
            )
            String granularity,

            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DashboardResponse data = query.admin(
                filter(
                        from,
                        to,
                        timezone,
                        providerId,
                        granularity
                ),
                principal
        );

        return file("admin", data);
    }

    @GetMapping("/dispatcher")
    @PreAuthorize("hasRole('DISPATCHER')")
    @Operation(summary = "Dashboard Dispatcher")
    public ResponseEntity<BaseResponse<DashboardResponse>> dispatcher(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam(
                    name = "timezone",
                    defaultValue = "Asia/Ho_Chi_Minh"
            )
            String timezone,

            @RequestParam(
                    name = "granularity",
                    defaultValue = "AUTO"
            )
            String granularity,

            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DashboardResponse result = query.dispatcher(
                filter(
                        from,
                        to,
                        timezone,
                        null,
                        granularity
                ),
                principal
        );

        return ResponseEntity.ok(
                BaseResponse.success(result)
        );
    }

    @GetMapping("/dispatcher/export")
    @PreAuthorize("hasRole('DISPATCHER')")
    @Operation(summary = "Xuất dashboard Dispatcher")
    public ResponseEntity<byte[]> dispatcherExport(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam(
                    name = "timezone",
                    defaultValue = "Asia/Ho_Chi_Minh"
            )
            String timezone,

            @RequestParam(
                    name = "granularity",
                    defaultValue = "AUTO"
            )
            String granularity,

            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DashboardResponse data = query.dispatcher(
                filter(
                        from,
                        to,
                        timezone,
                        null,
                        granularity
                ),
                principal
        );

        return file("dispatcher", data);
    }

    @GetMapping("/driver")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Dashboard Driver")
    public ResponseEntity<BaseResponse<DashboardResponse>> driver(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam(
                    name = "timezone",
                    defaultValue = "Asia/Ho_Chi_Minh"
            )
            String timezone,

            @RequestParam(
                    name = "granularity",
                    defaultValue = "AUTO"
            )
            String granularity,

            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DashboardResponse result = query.driver(
                filter(
                        from,
                        to,
                        timezone,
                        null,
                        granularity
                ),
                principal
        );

        return ResponseEntity.ok(
                BaseResponse.success(result)
        );
    }

    @GetMapping("/driver/export")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Xuất dashboard Driver")
    public ResponseEntity<byte[]> driverExport(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam(
                    name = "timezone",
                    defaultValue = "Asia/Ho_Chi_Minh"
            )
            String timezone,

            @RequestParam(
                    name = "granularity",
                    defaultValue = "AUTO"
            )
            String granularity,

            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DashboardResponse data = query.driver(
                filter(
                        from,
                        to,
                        timezone,
                        null,
                        granularity
                ),
                principal
        );

        return file("driver", data);
    }

    @GetMapping("/provider")
    @PreAuthorize("hasRole('PROVIDER_ADMIN')")
    @Operation(summary = "Dashboard Provider")
    public ResponseEntity<BaseResponse<DashboardResponse>> provider(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam(
                    name = "timezone",
                    defaultValue = "Asia/Ho_Chi_Minh"
            )
            String timezone,

            @RequestParam(
                    name = "granularity",
                    defaultValue = "AUTO"
            )
            String granularity,

            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DashboardResponse result = query.provider(
                filter(
                        from,
                        to,
                        timezone,
                        null,
                        granularity
                ),
                principal
        );

        return ResponseEntity.ok(
                BaseResponse.success(result)
        );
    }

    @GetMapping("/provider/export")
    @PreAuthorize("hasRole('PROVIDER_ADMIN')")
    @Operation(summary = "Xuất dashboard Provider")
    public ResponseEntity<byte[]> providerExport(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam(
                    name = "timezone",
                    defaultValue = "Asia/Ho_Chi_Minh"
            )
            String timezone,

            @RequestParam(
                    name = "granularity",
                    defaultValue = "AUTO"
            )
            String granularity,

            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DashboardResponse data = query.provider(
                filter(
                        from,
                        to,
                        timezone,
                        null,
                        granularity
                ),
                principal
        );

        return file("provider", data);
    }

    private DashboardFilter filter(
            LocalDateTime from,
            LocalDateTime to,
            String timezone,
            Integer providerId,
            String granularity
    ) {
        return new DashboardFilter(
                from,
                to,
                timezone,
                providerId,
                granularity
        );
    }

    private ResponseEntity<byte[]> file(
            String role,
            DashboardResponse data
    ) {
        String name = role
                + "-dashboard_"
                + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")
                )
                + ".xlsx";

        byte[] content = excel.export(role, data);

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument"
                                        + ".spreadsheetml.sheet"
                        )
                )
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition
                                .attachment()
                                .filename(
                                        name,
                                        StandardCharsets.UTF_8
                                )
                                .build()
                                .toString()
                )
                .contentLength(content.length)
                .body(content);
    }
}