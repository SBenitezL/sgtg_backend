package com.sgtg.backend.infraestructure.output.formatter;

import org.springframework.stereotype.Service;

import com.sgtg.backend.application.output.ExceptionFormatterIntPort;
import com.sgtg.backend.infraestructure.output.exception.own.BadCredentialException;
import com.sgtg.backend.infraestructure.output.exception.own.BadFormatException;
import com.sgtg.backend.infraestructure.output.exception.own.BusinessRuleException;
import com.sgtg.backend.infraestructure.output.exception.own.EntityExistsException;
import com.sgtg.backend.infraestructure.output.exception.own.EntityNotFoundException;
import com.sgtg.backend.infraestructure.output.exception.own.NoAccessException;
import com.sgtg.backend.infraestructure.output.exception.own.NoDataException;

@Service
public class ExceptionFormatterImplAdapter implements ExceptionFormatterIntPort {

    @Override
    public void returnResponseErrorEntityExists(String message) {
        EntityExistsException objException = new EntityExistsException(message);
        throw objException;
    }

    @Override
    public void returnResponseErrorEntityNotFound(String message) {
        EntityNotFoundException objException = new EntityNotFoundException(message);
        throw objException;
    }

    @Override
    public void returnResponseBusinessRuleViolated(String message) {
        BusinessRuleException objException = new BusinessRuleException(message);
        throw objException;
    }

    @Override
    public void returnResponseBadCredentials(String message) {
        BadCredentialException objException = new BadCredentialException(message);
        throw objException;
    }

    @Override
    public void returnResponseBadFormat(String message) {
        BadFormatException objException = new BadFormatException(message);
        throw objException;
    }

    @Override
    public void returnNoData(String message) {
        NoDataException objException = new NoDataException(message);
        throw objException;
    }

    @Override
    public void returnNoAccess(String message) {
        NoAccessException objException = new NoAccessException(message);
        throw objException;
    }
}
