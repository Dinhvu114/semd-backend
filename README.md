# SEMD Backend — Conventions & Workflow

## 1. Cấu trúc thư mục

```
src/main/java/com/semd/backend/
├── config/         ← SecurityConfig, OpenApiConfig
├── controller/     ← Nhận request, trả response (không chứa logic)
├── service/
│   └── impl/       ← Logic nghiệp vụ
├── repository/     ← JPA Repositories
├── entity/         ← JPA Entities
├── dto/
│   ├── request/    ← CreateXxxRequest
│   └── response/   ← XxxResponse
└── exception/      ← GlobalExceptionHandler

src/main/resources/
├── application.yml
├── application-local.yml   ← KHÔNG push Git
└── db/migration/
    └── V1__init_schema.sql
```

---

## 2. Git Branch

```
main       ← chỉ merge khi sprint done, KHÔNG push thẳng
develop    ← tích hợp hàng ngày, KHÔNG push thẳng
feature/xxx  ← tính năng mới
fix/xxx      ← sửa bug
chore/xxx    ← cấu hình, setup
```

**Quy trình mỗi task:**
```bash
git checkout develop && git pull origin develop
git checkout -b feature/ten-tinh-nang
# ... code ...
git add . && git commit -m "feat: mô tả"
git push origin feature/ten-tinh-nang
# → Tạo Pull Request vào develop trên GitHub
```

---

## 3. Commit Message

Format: `<type>(<scope>): <mô tả>`

| Type | Dùng khi |
|---|---|
| `feat` | Thêm tính năng mới |
| `fix` | Sửa bug |
| `chore` | Cấu hình, setup, build |
| `docs` | Tài liệu, comment |
| `refactor` | Tái cấu trúc, không đổi logic |
| `test` | Viết test |

Ví dụ:
```
feat(dispatch): thêm API tạo yêu cầu cứu hộ
fix(flyway): sửa lỗi migration V1 thiếu PostGIS
chore(config): thêm dependency hibernate-spatial
```

---

## 4. Naming Conventions

| Thành phần | Quy tắc | Ví dụ |
|---|---|---|
| Entity | PascalCase | `DispatchRequest` |
| Repository | PascalCase + Repository | `DispatchRequestRepository` |
| Service (interface) | PascalCase + Service | `DispatchRequestService` |
| Service (impl) | PascalCase + ServiceImpl | `DispatchRequestServiceImpl` |
| Controller | PascalCase + Controller | `DispatchRequestController` |
| DTO Request | PascalCase + Request | `CreateDispatchRequest` |
| DTO Response | PascalCase + Response | `DispatchRequestResponse` |
| DB table | snake_case | `dispatch_requests` |
| DB column | snake_case | `created_at`, `is_active` |
| API endpoint | kebab-case | `/api/v1/dispatch-requests` |
| Hằng số | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Variable/Method | camelCase | `findByStatus()` |

---

## 5. Quy tắc code

- **Controller** chỉ nhận request và gọi service, không chứa logic nghiệp vụ
- **Không** để `System.out.println`, `TODO`, magic number trong code được push
- **Boolean** đặt tên bắt đầu bằng `is`, `has`, `can`: `isActive`, `hasPermission`
- **Method** bắt đầu bằng động từ: `get`, `create`, `find`, `validate`, `send`
- Mọi endpoint phải có `@Tag` và `@Operation` của Springdoc

---

## 6. Quy tắc Database Migration (Flyway)

- **Thư mục chứa file migration**: `src/main/resources/db/migration/` (Chú ý: Đây phải là cấu trúc thư mục lồng nhau `db/migration`, **không** được tạo thư mục chứa dấu chấm dạng `db.migration`).
- **Quy tắc đặt tên file**: `V<Version>__<Mô_tả_ngắn>.sql` (Bắt buộc phải có **2 dấu gạch dưới `__`** sau số phiên bản). Ví dụ: `V9__add_status_column.sql`.
- **Tuyệt đối không chỉnh sửa các file SQL migration cũ** đã được merge vào nhánh chung (`develop` hoặc `main`). Việc chỉnh sửa file cũ sẽ làm thay đổi mã băm (checksum) và làm ứng dụng của toàn bộ các thành viên bị sập lỗi khởi động.
- **Nếu cần thay đổi cấu trúc DB**: Luôn viết thêm file SQL mới với số phiên bản tăng dần (ví dụ `V9`, `V10`...) chứa các câu lệnh chỉnh sửa (`ALTER TABLE`, `CREATE TABLE`...) chứ không sửa file cũ.
- **Khắc phục lỗi lệch Checksum (nếu lỡ tay sửa file cũ ở local)**:
  - *Cách 1 (Khuyên dùng):* Chạy lệnh `git checkout -- <đường_dẫn_file>` để khôi phục trạng thái ban đầu của file SQL.
  - *Cách 2:* Drop database trống và chạy lại ứng dụng để Flyway dựng lại DB từ đầu.
  - *Cách 3:* Cấu hình tạm thời `spring.flyway.repair-on-migrate: true` trong `application.yaml` để sửa lại mã băm dưới DB, chạy xong ứng dụng cần xóa dòng này đi.

---

## 7. File không được push lên Git

```gitignore
target/
.idea/
*.iml
application-local.yml
.env
```

---

## 8. Checklist trước khi tạo Pull Request

- [ ] `mvn spring-boot:run` chạy không lỗi
- [ ] Không commit password, file bí mật
- [ ] Commit message đúng format
- [ ] Không có `System.out.println` thừa
- [ ] Đã test API trên Swagger UI

