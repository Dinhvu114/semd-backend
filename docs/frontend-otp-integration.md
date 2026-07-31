# Hướng dẫn frontend tích hợp OTP đăng ký

## 1. Tổng quan

Frontend dùng cùng một API cho cả hai chế độ:

- `mock`: backend trả mã OTP trong `data` để phát triển/demo.
- `sms`: backend gửi SMS qua nhà cung cấp và luôn trả `data: null`.

Frontend không tự quyết định chế độ và không được mặc định mã `123456`.

## 2. Gửi OTP

```http
POST /api/v1/auth/send-otp
Content-Type: application/json

{
  "phoneNumber": "0987654321"
}
```

Response chế độ mock:

```json
{
  "code": 200,
  "success": true,
  "message": "Mã OTP thử nghiệm đã được tạo",
  "data": "748291",
  "metadata": null
}
```

Response chế độ SMS:

```json
{
  "code": 200,
  "success": true,
  "message": "Mã OTP đã được gửi",
  "data": null,
  "metadata": null
}
```

Ví dụ gọi service:

```javascript
const handleSendOtp = async () => {
  const phoneNumber = formData.phoneNumber.trim();
  if (!phoneNumber) {
    setError('Vui lòng nhập số điện thoại.');
    return;
  }

  setIsSendingOtp(true);
  setError('');

  try {
    const response = await authService.sendOtp(phoneNumber);
    setOtpRequestedFor(phoneNumber);

    if (response.data) {
      // Chỉ xuất hiện trong môi trường mock/demo.
      setDevOtp(response.data);
    } else {
      setDevOtp('');
    }

    setSuccess(response.message);
    startOtpCountdown(300);
    startResendCountdown(60);
  } catch (error) {
    setError(error.response?.data?.message || 'Không thể gửi mã OTP.');
  } finally {
    setIsSendingOtp(false);
  }
};
```

Nút gửi OTP phải có `type="button"` để không submit form:

```jsx
<button
  type="button"
  onClick={handleSendOtp}
  disabled={isSendingOtp || resendSeconds > 0}
>
  {resendSeconds > 0 ? `Gửi lại sau ${resendSeconds}s` : 'Gửi OTP'}
</button>
```

Trong mock, có thể hiển thị:

```jsx
{devOtp && (
  <p>Mã OTP thử nghiệm: <strong>{devOtp}</strong></p>
)}
```

Không tự động điền mã vào ô OTP; người dùng nên nhập lại để trình diễn đúng luồng.

## 3. Xác minh OTP

Không gửi OTP cùng toàn bộ thông tin đăng ký. Sau khi người dùng nhập mã, gọi API riêng:

```http
POST /api/v1/auth/verify-otp
Content-Type: application/json

{
  "phoneNumber": "0987654321",
  "otpCode": "748291"
}
```

Response:

```json
{
  "code": 200,
  "success": true,
  "message": "Thành công",
  "data": {
    "verificationToken": "short-lived-one-time-token",
    "expiresInSeconds": 600
  },
  "metadata": null
}
```

FE chỉ giữ `verificationToken` trong state của màn hình đăng ký, không lưu localStorage:

```javascript
const response = await authService.verifyOtp(
  formData.phoneNumber.trim(),
  formData.otpCode.trim()
);
setVerificationToken(response.data.verificationToken);
```

Token có hiệu lực 10 phút, ràng buộc với đúng số điện thoại và chỉ dùng một lần.

## 4. Đăng ký

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "reporter02",
  "password": "12345678",
  "fullName": "Nguyễn Văn A",
  "phoneNumber": "0987654321",
  "email": "reporter02@example.com",
  "verificationToken": "short-lived-one-time-token"
}
```

Frontend phải gửi cùng số điện thoại đã xác minh. Trước khi submit:

```javascript
if (otpRequestedFor !== formData.phoneNumber.trim()) {
  setError('Vui lòng gửi OTP cho số điện thoại hiện tại.');
  return;
}

if (!verificationToken) {
  setError('Vui lòng xác minh OTP trước khi đăng ký.');
  return;
}
```

Nếu người dùng sửa số điện thoại sau khi gửi OTP:

```javascript
setOtpRequestedFor('');
setDevOtp('');
setVerificationToken('');
setFormData(previous => ({ ...previous, phoneNumber: newPhone, otpCode: '' }));
```

## 5. Lỗi cần xử lý

| HTTP | Ý nghĩa | Xử lý FE |
|---|---|---|
| `400` | Sai định dạng, OTP sai/hết hạn hoặc gửi lại quá sớm | Hiển thị `response.data.message` |
| `503` | SMS provider chưa cấu hình hoặc tạm thời lỗi | Cho phép thử lại sau, không chuyển bước |
| `500` | Lỗi hệ thống/Redis | Hiển thị thông báo chung |

Backend hiện cấu hình:

- OTP hết hạn sau 5 phút.
- Chỉ được yêu cầu lại sau 60 giây.
- Tối đa 5 lần xác minh sai.
- OTP dùng một lần.

## 6. Service frontend

```javascript
sendOtp: async (phoneNumber) => {
  const response = await api.post('/v1/auth/send-otp', { phoneNumber });
  return response.data;
},

verifyOtp: async (phoneNumber, otpCode) => {
  const response = await api.post('/v1/auth/verify-otp', { phoneNumber, otpCode });
  return response.data;
},

register: async (data) => {
  const response = await api.post('/v1/auth/register', data);
  return response.data;
}
```

Không cần gửi JWT cho hai endpoint vì chúng là API công khai.

## 7. Chuyển backend sang SMS thật

Thiết lập biến môi trường:

```env
OTP_DELIVERY_MODE=sms
SMS_ENDPOINT=https://provider.example/api/messages
SMS_API_TOKEN=replace-with-real-secret
SMS_SENDER=SEMD
OTP_HASH_SECRET=replace-with-a-long-random-secret
```

Adapter mặc định gửi:

```json
{
  "to": "+84987654321",
  "sender": "SEMD",
  "message": "Ma xac thuc SEMD cua ban la 748291. Ma co hieu luc trong 5 phut."
}
```

với header:

```http
Authorization: Bearer <SMS_API_TOKEN>
```

Mỗi nhà cung cấp có payload/header khác nhau. Khi có tài liệu API chính thức, backend cần sửa ánh xạ trong `HttpSmsOtpDeliveryService`; frontend không thay đổi.
