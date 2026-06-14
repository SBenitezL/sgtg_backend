package com.sgtg.backend.infraestructure.output.exception.own;

import com.sgtg.backend.infraestructure.output.exception.structure.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public abstract class ManageRunTimeException extends RuntimeException {

    protected ErrorCode errorCode;

    public abstract String formatException();
}
