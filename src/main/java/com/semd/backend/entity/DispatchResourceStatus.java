package com.semd.backend.entity;

public enum DispatchResourceStatus {
    AVAILABLE,      // sẵn sàng
    DISPATCHED,     // đã giao nhiệm vụ
    ON_MISSION,     // đang thực hiện nhiệm vụ
    RETURNING,      // đang trở về
    OFFLINE,        // ngoại tuyến
    MAINTENANCE,    // đang bảo trì
    OUT_OF_SERVICE  // không thực hiện
}
