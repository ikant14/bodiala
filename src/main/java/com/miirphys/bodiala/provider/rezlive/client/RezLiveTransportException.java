package com.miirphys.bodiala.provider.rezlive.client;

import com.miirphys.bodiala.provider.error.UpstreamTransportException;

/**
 * Raised for transport/codec failures talking to RezLive — an unparseable/corrupt response body,
 * a gzip decode error, or a marshalling failure. Distinct from {@link RezLiveApiException} (a
 * well-formed {@code <error>} body) and from {@code IllegalStateException} (which we reserve for
 * "credentials not configured"). A RezLive-specific {@link UpstreamTransportException}; mapped to HTTP 502.
 */
public class RezLiveTransportException extends UpstreamTransportException {

    public RezLiveTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
