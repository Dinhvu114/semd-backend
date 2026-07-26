# Phase 2 - Dispatch request migrations

Tài liệu này ghi lại các thay đổi database nên thực hiện sau khi luồng tiếp nhận
và xác minh yêu cầu ở Phase 1 đã ổn định. Phase 1 không phụ thuộc vào bất kỳ
migration nào trong danh sách này.

## 1. Đổi tên cột review

Hai cột hiện tại được dùng cho cả quyết định xác nhận và từ chối, vì vậy tên
`confirmed_*` không còn phản ánh đúng ý nghĩa:

```sql
ALTER TABLE public.dispatch_requests
    RENAME COLUMN confirmed_by TO reviewed_by;

ALTER TABLE public.dispatch_requests
    RENAME COLUMN confirmed_at TO reviewed_at;
```

Sau migration cần đổi mapping entity, DTO và query liên quan từ
`confirmedBy/confirmedAt` sang `reviewedBy/reviewedAt`.

## 2. Chống tạo nhiều dispatch request cho cùng emergency call

Chỉ áp dụng nếu business rule được chốt là một `emergency_call` chỉ sinh một
`dispatch_request`:

```sql
CREATE UNIQUE INDEX uq_dispatch_requests_call_id
    ON public.dispatch_requests(call_id)
    WHERE call_id IS NOT NULL;
```

Trước khi tạo index phải kiểm tra và xử lý dữ liệu trùng:

```sql
SELECT call_id, COUNT(*)
FROM public.dispatch_requests
WHERE call_id IS NOT NULL
GROUP BY call_id
HAVING COUNT(*) > 1;
```

## 3. Tối ưu hàng đợi điều phối

Endpoint hàng đợi lọc theo `status` và sắp xếp theo `created_at`, `id`:

```sql
CREATE INDEX idx_dispatch_requests_status_created_id
    ON public.dispatch_requests(status, created_at DESC, id DESC);
```

Sau khi thêm index cần dùng `EXPLAIN (ANALYZE, BUFFERS)` với dữ liệu đủ lớn để
xác nhận planner sử dụng index. Index `idx_requests_status` cũ có thể được giữ
đến khi đo đạc xong rồi mới quyết định loại bỏ.

## 4. Bổ sung index cho khóa ngoại

PostgreSQL không tự tạo index cho foreign key:

```sql
CREATE INDEX idx_dispatch_requests_call_id
    ON public.dispatch_requests(call_id);

CREATE INDEX idx_dispatch_requests_service_type_id
    ON public.dispatch_requests(service_type_id);

CREATE INDEX idx_dispatch_requests_edge_node_id
    ON public.dispatch_requests(edge_node_id);
```

Nếu đã tạo unique index cho `call_id` ở mục 2 thì không cần index thường cho
`call_id`.

## 5. Ràng buộc dữ liệu trạng thái và mức khẩn cấp

Chỉ thêm sau khi đã kiểm tra dữ liệu hiện hữu:

```sql
SELECT DISTINCT status FROM public.dispatch_requests;
SELECT DISTINCT urgency_level FROM public.dispatch_requests;
```

Ràng buộc đề xuất:

```sql
ALTER TABLE public.dispatch_requests
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE public.dispatch_requests
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE public.dispatch_requests
    ADD CONSTRAINT chk_dispatch_requests_status
    CHECK (status IN (
        'PENDING', 'CONFIRMED', 'RECOMMENDING', 'DISPATCHING',
        'DISPATCHED', 'COMPLETED', 'REJECTED', 'CANCELLED', 'FAILED'
    ));

ALTER TABLE public.dispatch_requests
    ADD CONSTRAINT chk_dispatch_requests_urgency
    CHECK (urgency_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'));
```

## 6. Cột version cho optimistic locking

Phase 1 dùng pessimistic row lock nên chưa cần thay schema. Nếu sau này cần
optimistic locking:

```sql
ALTER TABLE public.dispatch_requests
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
```

Sau đó thêm `@Version` vào entity. Chỉ chọn một chiến lược khóa chính cho từng
use case sau khi đã đo tải; không thêm cột này chỉ để tồn tại song song mà không
được sử dụng.

## Không nằm trong kế hoạch

- Không thêm bảng lịch sử xác minh.
- Không thêm workflow engine.
- Không thêm bảng outbox trong phạm vi đồ án hiện tại.

