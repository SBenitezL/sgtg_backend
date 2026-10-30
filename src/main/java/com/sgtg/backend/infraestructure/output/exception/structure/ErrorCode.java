package com.sgtg.backend.infraestructure.output.exception.structure;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ErrorCode {
    GENERIC_ERROR("GC-0001", "GENERIC ERROR"),
    ENTITY_EXISTS("GC-0002", "ERROR, ENTITY ALREADY EXISTS"),
    ENTITY_NOT_FOUND("GC-0003", "ERROR, COULDN'T FOUND ENTITY"),
    BUSINESS_RULE_VIOLATION("GC-0004", "ERROR, BUSINESS RULE HAS BEEN VIOLATED"),
    BAD_CREDENTIALS("GC-0005", "ERROR, BAD CREDENTIALS"),
    BAD_FORMAT("GC-0006", "ERROR, BAD FORMAT"),
    NO_DATA("GC-0007", "ERROR, NO DATA"),
    NO_ACCESS("GC-0008", "ERROR, NO ACCESS"),
    TOKEN_EXPIRED("GC-0009", "ERROR, TOKEN HAS EXPIRED"),
    RESOURCE_NOT_FOUND("GC-0010", "ERROR, RESOURCE NOT FOUND"),
    CONFLICT("GC-0011", "ERROR, CONCURRENT MODIFICATION"),
    PASSWORD_CHANGE_REQUIRED("GC-0012", "ERROR, PASSWORD CHANGE REQUIRED");

    private final String code;
    private final String messageKey;
}
