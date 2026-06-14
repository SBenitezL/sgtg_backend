package com.sgtg.backend.infraestructure.output.exception.own;

import com.sgtg.backend.infraestructure.output.exception.structure.ErrorCode;

public class BadCredentialException extends OwnException {

    public BadCredentialException(final String message) {
        super(message, ErrorCode.BAD_CREDENTIALS);
    }
}
