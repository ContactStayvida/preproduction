package com.stayvida.backend.exception;

public final class BookingExceptions {

    private BookingExceptions() {
    }

    public static class OtpRequiredException extends RuntimeException {
        public OtpRequiredException(String message) {
            super(message);
        }
    }

    public static class RoomLockException extends RuntimeException {
        public RoomLockException(String message) {
            super(message);
        }
    }
}
