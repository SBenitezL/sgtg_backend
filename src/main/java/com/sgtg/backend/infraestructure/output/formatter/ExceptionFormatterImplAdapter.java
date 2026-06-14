package com.sgtg.backend.infraestructure.output.formatter;

import com.sgtg.backend.application.output.ExceptionFormatterIntPort;

public class ExceptionFormatterImplAdapter implements ExceptionFormatterIntPort {

    @Override
    public void returnResponseErrorEntityExists(String message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'returnResponseErrorEntityExists'");
    }

    @Override
    public void returnResponseErrorEntityNotFound(String message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'returnResponseErrorEntityNotFound'");
    }

    @Override
    public void returnResponseBusinessRuleViolated(String message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'returnResponseBusinessRuleViolated'");
    }

    @Override
    public void returnResponseBadCredentials(String message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'returnResponseBadCredentials'");
    }

    @Override
    public void returnResponseBadFormat(String message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'returnResponseBadFormat'");
    }

    @Override
    public void returNoData(String message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'returNoData'");
    }

    @Override
    public void returNoAccess(String message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'returNoAccess'");
    }

}
