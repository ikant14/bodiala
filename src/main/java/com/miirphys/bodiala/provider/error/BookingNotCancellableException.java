package com.miirphys.bodiala.provider.error;

/**
 * The supplier answered a cancellation, but refused it because of the booking's state on its side
 * (it can't see the booking, or won't cancel it online). Not an outage, so it maps to HTTP 409 with a
 * message the guest can act on, rather than a 502. Provider-neutral.
 */
public class BookingNotCancellableException extends RuntimeException {

    public BookingNotCancellableException(String message, Throwable cause) {
        super(message, cause);
    }
}
