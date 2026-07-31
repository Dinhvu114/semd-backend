package com.semd.backend.dto.common;

public record Metadata(
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages
) {
    public static Metadata from(org.springframework.data.domain.Page<?> page) {
        return new Metadata(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
