package com.sgtg.backend.infraestructure.output.exception.own;

import com.sgtg.backend.infraestructure.output.exception.structure.ErrorCode;

public class BadFormatException extends OwnException {

    public BadFormatException(final String message) {
        super(message, ErrorCode.BAD_FORMAT);
    }

}
