package com.semd.backend.dto.request;

public record ConfirmDispatchRequest(
        Integer dispatcherId,
        String note
) {}
