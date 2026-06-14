package com.sgtg.backend.infraestructure.output.exception.own;

import com.sgtg.backend.infraestructure.output.exception.structure.ErrorCode;

public class EntityExistsException extends OwnException {

    public EntityExistsException(String message) {
        super(message, ErrorCode.ENTITY_EXISTS);
    }

}
