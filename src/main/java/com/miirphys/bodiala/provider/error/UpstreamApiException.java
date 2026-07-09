package com.miirphys.bodiala.provider.error;

/**
 * A hotel supplier returned a business error (e.g. invalid credentials, quota exceeded, rate no
 * longer available) in an otherwise well-formed response. Provider-neutral supertype (the Hotelbeds
 * client throws it). Mapped to HTTP 502.
 */
public class UpstreamApiException extends RuntimeException {

    public UpstreamApiException(String message) {
        super(message);
    }

    public UpstreamApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
