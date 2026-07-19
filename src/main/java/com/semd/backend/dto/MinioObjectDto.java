package com.semd.backend.dto;

import java.time.ZonedDateTime;

public record MinioObjectDto(
    String objectKey,
    long size,
    String contentType,
    ZonedDateTime lastModified
) {}
