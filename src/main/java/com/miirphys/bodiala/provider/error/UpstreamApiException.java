package com.miirphys.bodiala.provider.error;

/**
 * A hotel supplier returned a business error (e.g. invalid credentials, quota exceeded, rate no
 * longer available) in an otherwise well-formed response. Provider-neutral supertype (the Hotelbeds
 * client throws it). Mapped to HTTP 502 unless a provider translates it into something more specific
 * (see {@link BookingNotCancellableException}).
 */
public class UpstreamApiException extends RuntimeException {

    /** The supplier's HTTP status, or 0 when the error didn't come from an HTTP error response. */
    private final int upstreamStatus;

    public UpstreamApiException(String message) {
        this(0, message);
    }

    public UpstreamApiException(int upstreamStatus, String message) {
        super(message);
        this.upstreamStatus = upstreamStatus;
    }

    public UpstreamApiException(String message, Throwable cause) {
        super(message, cause);
        this.upstreamStatus = 0;
    }

    public int upstreamStatus() {
        return upstreamStatus;
    }
}
