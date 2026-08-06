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
      
# 🚑 Hướng Dẫn Cài Đặt OSRM & Tích Hợp Mô Phỏng Đường Đi

> Tài liệu này dành cho **Frontend/Mobile developer** muốn kết nối vào hệ thống mô phỏng hành trình xe cấp cứu của SEMD Backend.

---

## 1. Yêu Cầu Hệ Thống

| Phần mềm | Phiên bản | Ghi chú |
|---|---|---|
| Docker Desktop | 4.x trở lên | Bắt buộc |
| Java | 21 | Chạy Spring Boot |
| PostgreSQL | 16 + PostGIS | Chạy qua Docker |
| OSRM Backend | latest | Chạy qua Docker |

---

## 2. Cài Đặt OSRM

### 2.1 Tải bản đồ Việt Nam

Vào: https://download.geofabrik.de/asia/vietnam.html

Tải file `vietnam-latest.osm.pbf` → lưu vào thư mục:
```
C:\osrm-data\vietnam-latest.osm.pbf
```

> File khoảng 100–200MB, đợi tải xong mới chạy lệnh tiếp theo.

### 2.2 Xử lý dữ liệu bản đồ (chỉ làm 1 lần)

Mở PowerShell, chạy lần lượt từng lệnh, **đợi xong mới chạy tiếp**:

```powershell
# Bước 1 — Extract (5–10 phút)
docker run -t -v "C:\osrm-data:/data" `
  ghcr.io/project-osrm/osrm-backend `
  osrm-extract -p /opt/car.lua /data/vietnam-latest.osm.pbf

# Bước 2 — Partition (2–5 phút)
docker run -t -v "C:\osrm-data:/data" `
  ghcr.io/project-osrm/osrm-backend `
  osrm-partition /data/vietnam-latest.osrm

# Bước 3 — Customize (2–5 phút)
docker run -t -v "C:\osrm-data:/data" `
  ghcr.io/project-osrm/osrm-backend `
  osrm-customize /data/vietnam-latest.osrm
```

### 2.3 Chạy OSRM Server

```powershell
docker run -t -i -p 5000:5000 -v "C:\osrm-data:/data" `
  ghcr.io/project-osrm/osrm-backend `
  osrm-routed --algorithm mld /data/vietnam-latest.osrm
```

Thấy log `Listening on: 0.0.0.0:5000` là thành công ✅

### 2.4 Kiểm tra OSRM hoạt động

Mở trình duyệt, dán link:
```
http://localhost:5000/route/v1/driving/105.8342,21.0278;105.8501,21.0322?overview=full&geometries=geojson
```

Response phải có `"code": "Ok"` và mảng `routes` ✅

---

## 3. Chạy Toàn Bộ Hệ Thống (docker-compose)

Tại thư mục gốc project, chạy:

```bash
docker-compose up -d
```

Kiểm tra các service đang chạy:

```bash
docker ps
```

Phải thấy:

| Container | Port | Mô tả |
|---|---|---|
| `semd-postgres` | 5432 | PostgreSQL + PostGIS |
| `semd-minio` | 9000 / 9001 | File storage |
| `semd-osrm` | 5000 | Tính toán đường đi |

---

## 4. Luồng Mô Phỏng Đường Đi

```
Dispatcher tạo mission
        ↓
Dispatcher tạo simulation (POST /ambulance-simulations)
        ↓  OSRM tính 2 chặng: xe→hiện trường, hiện trường→bệnh viện
        ↓
Driver start mission → Dispatcher start simulation (POST /ambulance-simulations/{id}/start)
        ↓
Mỗi tick (mặc định 1 giây):
  - Tính vị trí xe trên geometry OSRM
  - Cập nhật DB
  - Phát WebSocket
        ↓
Xe đến hiện trường → event ARRIVED_AT_SCENE → chờ N giây
        ↓
Xe đến bệnh viện → event SIMULATION_COMPLETED
```

---

## 5. REST API

**Base URL:** `http://localhost:8080/api/v1`

### 5.1 Tạo phiên mô phỏng

```http
POST /ambulance-simulations
Content-Type: application/json

{
  "missionId": 1,
  "hospitalId": 2,
  "tickIntervalMs": 1000,
  "speedMultiplier": 10,
  "sceneWaitSeconds": 5
}
```

| Field | Kiểu | Mô tả |
|---|---|---|
| `missionId` | Integer | ID nhiệm vụ (phải ACCEPTED hoặc EN_ROUTE) |
| `hospitalId` | Integer | ID bệnh viện đích |
| `tickIntervalMs` | Integer | Khoảng thời gian mỗi tick (min 250ms) |
| `speedMultiplier` | Double | Hệ số tốc độ (10 = nhanh gấp 10 lần thực tế) |
| `sceneWaitSeconds` | Integer | Giây chờ tại hiện trường |

**Response 201:**
```json
{
  "id": 1,
  "missionId": 1,
  "resourceId": 3,
  "hospitalId": 2,
  "status": "READY",
  "phase": "TO_SCENE",
  "currentLongitude": 105.8342,
  "currentLatitude": 21.0278
}
```

**Lỗi có thể gặp:**

| HTTP | errorCode | Nguyên nhân |
|---|---|---|
| 404 | `MISSION_NOT_FOUND` | Mission không tồn tại |
| 409 | `INVALID_SIMULATION_STATE` | Mission chưa ACCEPTED/EN_ROUTE |
| 409 | `ACTIVE_MISSION_ALREADY_EXISTS` | Mission đã có simulation đang chạy |
| 422 | `RESOURCE_LOCATION_MISSING` | Xe chưa có tọa độ |
| 422 | `TARGET_LOCATION_MISSING` | Hiện trường chưa có tọa độ |
| 502 | `OSRM_UNAVAILABLE` | OSRM chưa chạy hoặc không tìm được đường |

---

### 5.2 Bắt đầu mô phỏng

```http
POST /ambulance-simulations/{id}/start
```

**Response 200:** status chuyển sang `RUNNING`

---

### 5.3 Dừng mô phỏng

```http
POST /ambulance-simulations/{id}/stop
```

**Response 200:** status chuyển sang `STOPPED`

> Có thể start lại — xe sẽ tiếp tục từ vị trí đã dừng, không chạy lại từ đầu.

---

### 5.4 Lấy trạng thái phiên

```http
GET /ambulance-simulations/{id}
```

**Response 200:**
```json
{
  "id": 1,
  "status": "RUNNING",
  "phase": "TO_SCENE",
  "currentLongitude": 105.8401,
  "currentLatitude": 21.0299,
  "startedAt": "2026-06-14T10:00:00Z"
}
```

---

### 5.5 Theo dõi hành trình realtime (REST Snapshot)

> Dùng khi cần lấy vị trí tức thời mà không dùng WebSocket.

**Theo simulation ID** (dành cho DISPATCHER, ADMIN):
```http
GET /ambulance-simulations/{id}/tracking
```

**Theo mission ID** (dành cho REPORTER, DRIVER — không cần biết simulationId):
```http
GET /ambulance-simulations/by-mission/{missionId}/tracking
```

**Response 200:**
```json
{
  "simulationId": 1,
  "missionId": 1,
  "resourceId": 3,
  "status": "RUNNING",
  "phase": "TO_SCENE",
  "sourceType": "SIMULATION",
  "currentLongitude": 105.8401,
  "currentLatitude": 21.0299,
  "progressPercent": 43.2,
  "remainingDistanceMeters": 1240.5,
  "etaSeconds": 74.3,
  "lastUpdatedAt": "2026-06-14T10:01:23Z"
}
```

| Field | Mô tả |
|---|---|
| `phase` | `TO_SCENE` / `AT_SCENE` / `TO_HOSPITAL` / `ARRIVED_HOSPITAL` |
| `progressPercent` | % hoàn thành chặng hiện tại (0–100) |
| `remainingDistanceMeters` | Quãng đường còn lại (mét) |
| `etaSeconds` | Thời gian dự kiến đến (giây) |
| `sourceType` | `SIMULATION` hoặc `REAL_GPS` |

---

## 6. WebSocket Realtime

### 6.1 Kết nối

```
URL: ws://localhost:8080/ws/websocket
Protocol: STOMP over SockJS
```

**JavaScript (SockJS + StompJS):**
```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  onConnect: () => {
    console.log('WebSocket connected');
    subscribeTracking(client);
  }
});

client.activate();
```

---

### 6.2 Subscribe theo vai trò

**DISPATCHER / ADMIN — nhận tất cả xe:**
```javascript
// Nhận update vị trí tất cả xe đang mô phỏng
client.subscribe('/topic/dispatcher/ambulances', (message) => {
  const data = JSON.parse(message.body);
  updateMapMarker(data);
});

// Nhận sự kiện nhiệm vụ (tạo mới, chấp nhận, từ chối...)
client.subscribe('/topic/dispatcher/missions', (message) => {
  const data = JSON.parse(message.body);
  handleMissionEvent(data);
});
```

**REPORTER / DRIVER — theo dõi 1 xe cụ thể:**
```javascript
// Thay {simulationId} bằng ID phiên mô phỏng
client.subscribe('/topic/simulations/1', (message) => {
  const data = JSON.parse(message.body);
  updateTrackingUI(data);
});
```

**DRIVER — nhận thông báo nhiệm vụ:**
```javascript
// Thay {driverId} bằng ID tài xế
client.subscribe('/topic/driver/5', (message) => {
  const data = JSON.parse(message.body);
  handleDriverNotification(data);
});
```

---

### 6.3 Cấu trúc message WebSocket

**Cập nhật vị trí (phát mỗi tick):**
```json
{
  "eventId": "uuid-v4",
  "eventType": "AMBULANCE_POSITION_UPDATED",
  "occurredAt": "2026-06-14T10:01:23Z",
  "simulationId": 1,
  "missionId": 1,
  "resourceId": 3,
  "sourceType": "SIMULATION",
  "status": "RUNNING",
  "phase": "TO_SCENE",
  "position": {
    "longitude": 105.8401,
    "latitude": 21.0299
  },
  "progressPercent": 43.2,
  "remainingDistanceMeters": 1240.5,
  "etaSeconds": 74.3,
  "sequence": 42
}
```

**Sự kiện hành trình:**
```json
{
  "eventId": "uuid-v4",
  "eventType": "ARRIVED_AT_SCENE",
  "occurredAt": "2026-06-14T10:03:00Z",
  "simulationId": 1,
  "missionId": 1,
  "resourceId": 3,
  "sourceType": "SIMULATION",
  "sequence": 120
}
```

**Các eventType quan trọng:**

| eventType | Ý nghĩa |
|---|---|
| `SIMULATION_STARTED` | Bắt đầu mô phỏng |
| `AMBULANCE_POSITION_UPDATED` | Cập nhật vị trí (mỗi tick) |
| `ARRIVED_AT_SCENE` | Xe đến hiện trường |
| `DEPARTED_TO_HOSPITAL` | Xe rời hiện trường, đến bệnh viện |
| `SIMULATION_COMPLETED` | Xe đến bệnh viện, hoàn tất |
| `SIMULATION_STOPPED` | Mô phỏng bị dừng |
| `SIMULATION_FAILED` | Lỗi trong quá trình mô phỏng |
| `NEW_MISSION` | Nhiệm vụ mới được tạo |
| `MISSION_ACCEPTED` | Driver nhận nhiệm vụ |
| `MISSION_REJECTED` | Driver từ chối |
| `MISSION_EN_ROUTE` | Xe bắt đầu di chuyển |
| `MISSION_COMPLETED` | Nhiệm vụ hoàn thành |

---

### 6.4 Ví dụ React Hook

```javascript
import { useEffect, useRef, useState } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

export function useAmbulanceTracking(simulationId) {
  const [position, setPosition] = useState(null);
  const [phase, setPhase] = useState(null);
  const [progress, setProgress] = useState(0);
  const clientRef = useRef(null);

  useEffect(() => {
    if (!simulationId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      onConnect: () => {
        client.subscribe(`/topic/simulations/${simulationId}`, (msg) => {
          const data = JSON.parse(msg.body);

          if (data.eventType === 'AMBULANCE_POSITION_UPDATED') {
            setPosition(data.position);         // { longitude, latitude }
            setPhase(data.phase);
            setProgress(data.progressPercent);
          }
        });
      }
    });

    client.activate();
    clientRef.current = client;

    return () => client.deactivate();
  }, [simulationId]);

  return { position, phase, progress };
}
```

**Dùng trong component:**
```javascript
function TrackingMap({ simulationId }) {
  const { position, phase, progress } = useAmbulanceTracking(simulationId);

  return (
    <div>
      <p>Phase: {phase}</p>
      <p>Progress: {progress?.toFixed(1)}%</p>
      {position && (
        <p>Vị trí: {position.latitude}, {position.longitude}</p>
      )}
    </div>
  );
}
```

---

## 7. Thứ Tự Gọi API Đầy Đủ (End-to-End)

```
1. POST /dispatch-requests          → tạo yêu cầu cấp cứu
2. POST /dispatch-requests/{id}/verify → Dispatcher xác minh
3. GET  /dispatch-requests/{id}/recommend → lấy Top 3 xe gợi ý
4. POST /dispatch-missions          → tạo nhiệm vụ (chọn xe từ Top 3)
5. POST /dispatch-missions/{id}/accept  → Driver nhận nhiệm vụ
6. POST /dispatch-missions/{id}/start   → Driver bắt đầu di chuyển

--- SIMULATION ---
7. POST /ambulance-simulations          → tạo phiên mô phỏng
8. POST /ambulance-simulations/{id}/start → bắt đầu mô phỏng
   → WebSocket /topic/simulations/{id} bắt đầu phát vị trí

9. [Realtime] ARRIVED_AT_SCENE event
10. [Realtime] DEPARTED_TO_HOSPITAL event
11. [Realtime] SIMULATION_COMPLETED event

--- DRIVER XÁC NHẬN ---
12. POST /dispatch-missions/{id}/arrive-scene
13. POST /dispatch-missions/{id}/start-transport
14. POST /dispatch-missions/{id}/arrive-hospital
15. POST /dispatch-missions/{id}/complete
```

---

## 8. Swagger UI

Toàn bộ API có thể test tại:
```
http://localhost:8080/swagger-ui.html
```

---

## 9. Lưu Ý Quan Trọng

- **OSRM phải chạy** trước khi gọi `POST /ambulance-simulations`, nếu không sẽ nhận lỗi `502 OSRM_UNAVAILABLE`
- **Xe phải có tọa độ** (`current_location`) trước khi tạo simulation
- **Hiện trường phải có tọa độ** (`target_location`) trong dispatch_request
- **Bệnh viện phải có tọa độ** (`location`) trong medical_hospitals
- `speedMultiplier: 10` nghĩa là xe di chuyển nhanh gấp 10 lần thực tế — phù hợp để demo
- `tickIntervalMs: 1000` = cập nhật vị trí mỗi 1 giây
- Vị trí xe trong DB được cập nhật **mỗi 5 giây** (không phải mỗi tick) để tránh quá tải
