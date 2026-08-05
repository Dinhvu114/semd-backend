package com.semd.backend.entity;

public enum DispatchMissionStatus {
    CREATED,        // giữ lại tương thích DB cũ
    DISPATCHED,     // vừa tạo, chờ driver phản hồi
    ACCEPTED,       // driver nhận
    REJECTED,       // driver từ chối
    EN_ROUTE,       // đang di chuyển đến hiện trường
    ARRIVED_SCENE,  // đến hiện trường
    TRANSPORTING,   // đang chở bệnh nhân
    ARRIVED_HOSPITAL, // đến bệnh viện
    COMPLETED,      // hoàn thành
    CANCELLED,      // bị huỷ (redispatch)
    FAILED,         // lỗi hệ thống
    TIMEOUT         // hết thời gian chờ
}