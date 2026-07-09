package com.miirphys.bodiala.provider.rezlive.client;

import com.miirphys.bodiala.provider.error.UpstreamApiException;

/**
 * Raised when RezLive returns a 200-status response carrying an {@code <error>} element
 * (e.g. invalid API key or non-whitelisted IP) rather than a normal payload. A RezLive-specific
 * {@link UpstreamApiException}.
 */
public class RezLiveApiException extends UpstreamApiException {

    public RezLiveApiException(String message) {
        super(message);
    }
}
