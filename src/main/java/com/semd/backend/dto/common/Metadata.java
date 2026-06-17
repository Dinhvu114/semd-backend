package com.semd.backend.dto.common;

public record Metadata(
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
