package com.randevupazaryeri.image.storage;

/**
 * A storage provider call failed. The message is for server logs only; API clients
 * always receive a generic error (see {@code GlobalExceptionHandler}).
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
