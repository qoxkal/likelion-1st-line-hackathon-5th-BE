package com.line1.booktalk.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 모든 예외를 명세서의 오류 형식으로 바꿔서 응답한다.
 * 로그에는 토큰, 아이 이름, 이미지 같은 민감한 값을 남기지 않는다 (명세서 14.1).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 서비스 코드에서 직접 던진 오류 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, e.getMessage(), List.of()));
    }

    /** @Valid 검증 실패 (예: age가 3~7 범위 밖) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldDetail> details = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_ERROR, details);
    }

    /** JSON 형식이 깨졌거나 enum 값이 틀린 경우 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return build(ErrorCode.VALIDATION_ERROR, List.of());
    }

    /** 쿼리 파라미터 타입이 틀린 경우 (예: limit=abc) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return build(ErrorCode.VALIDATION_ERROR,
                List.of(new ErrorResponse.FieldDetail(e.getName(), "형식이 올바르지 않아요.")));
    }

    /** 필수 쿼리/폼 값이 빠진 경우 (예: childId 없음) */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e) {
        return build(ErrorCode.VALIDATION_ERROR,
                List.of(new ErrorResponse.FieldDetail(e.getParameterName(), "필수 값이에요.")));
    }

    /** multipart 요청에서 파일 파트가 빠진 경우 */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException e) {
        return build(ErrorCode.VALIDATION_ERROR,
                List.of(new ErrorResponse.FieldDetail(e.getRequestPartName(), "필수 값이에요.")));
    }

    /** 요청 Content-Type이 맞지 않는 경우 (예: 업로드에 JSON으로 보냄) */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException e) {
        return build(ErrorCode.VALIDATION_ERROR, List.of());
    }

    /** 업로드 파일이 application.yml의 최대 크기를 넘은 경우 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadSize(MaxUploadSizeExceededException e) {
        return build(ErrorCode.FILE_TOO_LARGE, List.of());
    }

    /** 없는 주소로 요청한 경우 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return build(ErrorCode.RESOURCE_NOT_FOUND, List.of());
    }

    /** 지원하지 않는 HTTP 메서드 (예: GET만 있는 곳에 POST) */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException e) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, List.of());
    }

    /** 그 밖의 예상하지 못한 오류 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unexpected error: {}", e.getClass().getName(), e);
        return build(ErrorCode.INTERNAL_ERROR, List.of());
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode code, List<ErrorResponse.FieldDetail> details) {
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, code.getMessage(), details));
    }
}
