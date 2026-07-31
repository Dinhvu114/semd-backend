package com.semd.backend.entity;

public enum DispatchRequestStatus {
    PENDING,        // chờ duyệt
    CONFIRMED,      // đã duyệt
    RECOMMENDING,   // đề xuất
    DISPATCHING,    // đang giao nhiệm vụ (tài xế chưa duyệt)
    DISPATCHED,     // đã giao nhiệm vụ
    COMPLETED,      // hoàn thành
    REJECTED,       // đã từ chối
    CANCELLED,      // đã hủy
    FAILED          // thất bại
}
