package com.sgtg.backend.infraestructure.output.exception.own;

import com.sgtg.backend.infraestructure.output.exception.structure.ErrorCode;

public class BusinessRuleException extends ManageRunTimeException {

    private static final String EXCEPTION_FORMAT = "%s - Business rule exception: %s";
    private final String businessRule;

    public BusinessRuleException(String businessRule) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION);
        this.businessRule = businessRule;
    }

    @Override
    public String formatException() {
        return String.format(EXCEPTION_FORMAT, errorCode.getCode(), businessRule);
    }

}
