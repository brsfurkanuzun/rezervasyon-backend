package com.randevupazaryeri.common.exception;

public class InvalidAppointmentException extends BusinessRuleException {
    public InvalidAppointmentException(String message) {
        super(ErrorCode.INVALID_APPOINTMENT, message);
    }
}
