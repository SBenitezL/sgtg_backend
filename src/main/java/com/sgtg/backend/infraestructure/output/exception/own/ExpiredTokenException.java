package com.sgtg.backend.infraestructure.output.exception.own;

import com.sgtg.backend.infraestructure.output.exception.structure.ErrorCode;

public class ExpiredTokenException extends OwnException {

    public ExpiredTokenException(String message) {
        super(message, ErrorCode.TOKEN_EXPIRED);
    }

}
