package com.semd.backend.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Tham số phân trang dùng chung cho mọi API danh sách. */
public class PageRequestDto {

    @Schema(description = "Số trang, bắt đầu từ 0", example = "0", minimum = "0")
    private int pageNumber = 0;

    @Schema(description = "Số phần tử mỗi trang, từ 1 đến 100", example = "10", minimum = "1", maximum = "100")
    private int pageSize = 10;

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = Math.max(pageNumber, 0);
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = Math.min(Math.max(pageSize, 1), 100);
    }

    public Pageable toPageable(Sort sort) {
        return PageRequest.of(pageNumber, pageSize, sort);
    }
}
