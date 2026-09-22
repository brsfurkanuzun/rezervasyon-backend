package com.randevupazaryeri.common.exception;

public class AppointmentConflictException extends BusinessRuleException {
    public AppointmentConflictException() {
        super(ErrorCode.APPOINTMENT_SLOT_UNAVAILABLE, "Selected appointment slot is no longer available.");
    }

    public AppointmentConflictException(String message) {
        super(ErrorCode.APPOINTMENT_SLOT_UNAVAILABLE, message);
    }
}
