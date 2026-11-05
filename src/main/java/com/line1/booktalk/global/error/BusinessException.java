package com.line1.booktalk.global.error;

import lombok.Getter;

/**
 * 서비스 코드에서 정해진 오류를 낼 때 사용한다.
 * 예: throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
