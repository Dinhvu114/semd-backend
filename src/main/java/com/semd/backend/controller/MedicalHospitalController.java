package com.semd.backend.controller;

import com.semd.backend.dto.MedicalHospitalDto;
import com.semd.backend.dto.common.BaseResponse;
import com.semd.backend.dto.common.Metadata;
import com.semd.backend.dto.common.PageRequestDto;
import com.semd.backend.service.MedicalHospitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/medical-hospitals")
@Tag(name = "Medical Hospitals", description = "Danh sách trung tâm cấp cứu/bệnh viện")
public class MedicalHospitalController {

    private final MedicalHospitalService service;

    public MedicalHospitalController(MedicalHospitalService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Lấy danh sách trung tâm cấp cứu có lọc và phân trang")
    public ResponseEntity<BaseResponse<List<MedicalHospitalDto>>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @ParameterObject @ModelAttribute PageRequestDto pagination) {
        Page<MedicalHospitalDto> result = service.search(
                keyword, isActive, pagination.toPageable(Sort.by("hospitalName").ascending()));
        return ResponseEntity.ok(BaseResponse.success(result.getContent(), Metadata.from(result)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<BaseResponse<MedicalHospitalDto>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(BaseResponse.success(service.getById(id)));
    }

}
