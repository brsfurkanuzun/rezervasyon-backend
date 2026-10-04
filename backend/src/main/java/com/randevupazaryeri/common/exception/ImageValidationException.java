package com.randevupazaryeri.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Rejected upload; the message is safe to show to the user. */
@Getter
public class ImageValidationException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode code;

    private ImageValidationException(HttpStatus status, ErrorCode code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ImageValidationException invalid(String message) {
        return new ImageValidationException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_IMAGE, message);
    }

    public static ImageValidationException invalidParameter(String message) {
        return new ImageValidationException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, message);
    }

    public static ImageValidationException tooLarge(String message) {
        return new ImageValidationException(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCode.FILE_TOO_LARGE, message);
    }

    public static ImageValidationException unsupportedType(String message) {
        return new ImageValidationException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCode.UNSUPPORTED_MEDIA_TYPE, message);
    }
}
