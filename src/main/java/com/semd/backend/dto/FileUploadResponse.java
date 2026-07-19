package com.semd.backend.dto;

public record FileUploadResponse(
    String objectKey,
    String contentType,
    long size
) {}
