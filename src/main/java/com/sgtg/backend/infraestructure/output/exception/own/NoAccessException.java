package com.sgtg.backend.infraestructure.output.exception.own;

import com.sgtg.backend.infraestructure.output.exception.structure.ErrorCode;

public class NoAccessException extends OwnException {

    public NoAccessException(String message) {
        super(message, ErrorCode.NO_ACCESS);
    }

}
