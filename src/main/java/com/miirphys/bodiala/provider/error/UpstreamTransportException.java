package com.miirphys.bodiala.provider.error;

/**
 * A transport or parse failure talking to a hotel supplier — a timeout, connection error, or an
 * unparseable/corrupt response body. Provider-neutral supertype (distinct from
 * {@link UpstreamApiException}, a well-formed error body). Mapped to HTTP 502.
 */
public class UpstreamTransportException extends RuntimeException {

    public UpstreamTransportException(String message) {
        super(message);
    }

    public UpstreamTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
