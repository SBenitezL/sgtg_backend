package com.sgtg.backend.infraestructure.output.exception.own;

import com.sgtg.backend.infraestructure.output.exception.structure.ErrorCode;

public class NoDataException extends OwnException {

    public NoDataException(String message) {
        super(message, ErrorCode.NO_DATA);
    }
}
