package com.shiftly.global.exception;

import lombok.Getter;

/** 도메인 규칙 위반 등 "예상된" 실패. ErrorCode 로 HTTP 상태와 메시지가 결정된다. */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object details;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public BusinessException(ErrorCode errorCode, Object details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details;
    }
}
