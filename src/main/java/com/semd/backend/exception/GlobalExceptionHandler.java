package com.semd.backend.exception;

import com.semd.backend.dto.common.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;
import com.semd.backend.service.DispatchMissionService;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        BaseResponse<Void> response = BaseResponse.fail(ex.getMessage(), 404);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<BaseResponse<Void>> handleAuthException(AuthException ex) {
        BaseResponse<Void> response = BaseResponse.fail(ex.getMessage(), 401);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        BaseResponse<Void> response = BaseResponse.fail("Bạn không có quyền thực hiện chức năng này", 403);
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        BaseResponse<Void> response = BaseResponse.fail(errorMessage, 400);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Void>> handleUnreadableRequest(HttpMessageNotReadableException ex) {
        String detail = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        BaseResponse<Void> response = BaseResponse.fail(
                "JSON request không hợp lệ: " + detail, 400);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        BaseResponse<Void> response = BaseResponse.fail(ex.getMessage(), 400);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BusinessConflictException.class)
    public ResponseEntity<BaseResponse<Void>> handleBusinessConflict(BusinessConflictException ex) {
        BaseResponse<Void> response = BaseResponse.fail(ex.getMessage(), 409);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(OtpDeliveryException.class)
    public ResponseEntity<BaseResponse<Void>> handleOtpDeliveryException(OtpDeliveryException ex) {
        BaseResponse<Void> response = BaseResponse.fail(ex.getMessage(), 503);
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleGeneralException(Exception ex) {
        ex.printStackTrace(); // Log the stack trace for debugging
        BaseResponse<Void> response = BaseResponse.fail("Lỗi hệ thống: " + ex.getMessage(), 500);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @ExceptionHandler(DispatchMissionService.MissionException.class)
    public ResponseEntity<BaseResponse<Void>> handleMissionException(
            DispatchMissionService.MissionException ex) {
        BaseResponse<Void> response = BaseResponse.fail(ex.getMessage(), ex.getHttpStatus());
        return new ResponseEntity<>(response, HttpStatus.valueOf(ex.getHttpStatus()));
    }
}
