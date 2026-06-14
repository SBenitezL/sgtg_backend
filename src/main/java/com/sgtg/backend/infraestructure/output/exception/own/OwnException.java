package com.sgtg.backend.infraestructure.output.exception.own;

import com.sgtg.backend.infraestructure.output.exception.structure.ErrorCode;

import lombok.Getter;

@Getter
public class OwnException extends RuntimeException {
    private final String messageKey;
    private final String code;

    public OwnException(ErrorCode errorCode) {
        super(errorCode.getCode());
        this.messageKey = errorCode.getMessageKey();
        this.code = errorCode.getCode();
    }

    public OwnException(final String message, final ErrorCode errorCode) {
        super(message);
        this.messageKey = errorCode.getMessageKey();
        this.code = errorCode.getCode();
    }

}
