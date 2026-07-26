# Phase 1 - Dispatch request review without database migration

## Phạm vi

Phase 1 hoàn thiện bước điều phối viên xác minh yêu cầu bằng các cột hiện có,
không tạo hoặc chỉnh sửa Flyway migration.

Quy ước dữ liệu tạm thời:

- `confirmed_by`: người đưa ra quyết định review, dùng cho cả confirm và reject.
- `confirmed_at`: thời điểm đưa ra quyết định review.
- `review_note`: ghi chú xác nhận hoặc lý do từ chối.
- `status`: kết quả `CONFIRMED` hoặc `REJECTED`.

## Thay đổi API

### Xác nhận yêu cầu

```http
POST /api/v1/dispatch-requests/{id}/confirm
Authorization: Bearer <dispatcher-or-admin-token>
Content-Type: application/json

{
  "note": "Đã gọi lại và xác nhận sự cố"
}
```

`dispatcherId` đã bị loại khỏi body. Backend lấy user ID từ JWT.

### Từ chối yêu cầu

```http
POST /api/v1/dispatch-requests/{id}/reject
Authorization: Bearer <dispatcher-or-admin-token>
Content-Type: application/json

{
  "reason": "Người báo xác nhận bấm nhầm nút SOS"
}
```

`reason` là bắt buộc và tối đa 1000 ký tự.

### Cập nhật severity

Chỉ chấp nhận `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` và chỉ cho phép khi request
đang `PENDING` hoặc `CONFIRMED`.

## Business rules

1. Chỉ request `PENDING` được confirm hoặc reject.
2. Confirm và reject đều lưu người review, thời điểm review và ghi chú.
3. Các thao tác review khóa row `dispatch_requests` bằng
   `PESSIMISTIC_WRITE`; request đồng thời đến sau sẽ đọc trạng thái mới và nhận
   `409 Conflict`.
4. Chỉ request `CONFIRMED` mới được tạo mission.
5. Chỉ resource `AVAILABLE` mới được gán mission.
6. Tạo mission khóa cả request và resource để tránh hai transaction đồng thời
   cùng sử dụng dữ liệu cũ.
7. Tạo mission chỉ dành cho `ADMIN` hoặc `DISPATCHER`; cập nhật trạng thái
   mission dành cho `ADMIN` hoặc `DRIVER`.

## HTTP errors

- `400 Bad Request`: body không hợp lệ, thiếu reason hoặc severity sai.
- `404 Not Found`: request, resource hoặc dispatcher không tồn tại.
- `409 Conflict`: state transition không hợp lệ hoặc resource không sẵn sàng.
- `403 Forbidden`: role không được phép gọi endpoint.

## Kiểm thử tự động

Chạy toàn bộ test:

```powershell
mvn test
```

Chạy riêng test Phase 1:

```powershell
mvn '-Dtest=DispatchRequestServiceTest,DispatchMissionServiceTest' test
```

Các test mới xác minh:

- Confirm request `PENDING` lấy dispatcher từ user ID đáng tin cậy và lưu dữ
  liệu review.
- Reject request `PENDING` lưu người review, thời điểm và lý do.
- Không được review request đã xử lý.
- Không được đổi severity sau khi dispatch.
- Không được tạo mission từ request chưa confirm.
- Không được dùng resource không available.
- Tạo mission hợp lệ cập nhật đồng bộ request, resource và mission.

## Kiểm thử API thủ công

1. Đăng nhập bằng dispatcher và lấy JWT.
2. Lấy một request `PENDING`.
3. Gọi confirm không có `dispatcherId`; kiểm tra response `200` và database có
   `status=CONFIRMED`, `confirmed_by` bằng user trong JWT.
4. Gọi reject lại cùng request; kỳ vọng `409`.
5. Tạo mission cho request `PENDING`; kỳ vọng `409`.
6. Tạo mission cho request `CONFIRMED` với resource `AVAILABLE`; kỳ vọng `201`,
   request thành `DISPATCHED`, resource thành `DISPATCHED`.
7. Gửi reject với reason rỗng; kỳ vọng `400`.
8. Dùng token role `REPORTER/USER` để confirm hoặc tạo mission; kỳ vọng `403`.
