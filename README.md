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

## 6. File không được push lên Git

```gitignore
target/
.idea/
*.iml
application-local.yml
.env
```

---

## 7. Checklist trước khi tạo Pull Request

- [ ] `mvn spring-boot:run` chạy không lỗi
- [ ] Không commit password, file bí mật
- [ ] Commit message đúng format
- [ ] Không có `System.out.println` thừa
- [ ] Đã test API trên Swagger UI
