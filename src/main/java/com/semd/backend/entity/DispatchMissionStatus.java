package com.semd.backend.entity;

public enum DispatchMissionStatus {
    DISPATCHED,         // đã giao nhiệm vụ
    ACCEPTED,           // đã chấp nhận nhiệm vụ
    REJECTED,           // đã từ chối nhiệm vụ
    EN_ROUTE,           // đang trên đường
    ARRIVED_SCENE,      // đã đến hiện trường
    TRANSPORTING,       // đang vận chuyển đến viện
    ARRIVED_HOSPITAL,   // đã đến viện
    COMPLETED,          // đã hoàn thành nhiệm vụ
    CANCELLED           // đã hủy nhiệm vụ
}