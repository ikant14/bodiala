package com.miirphys.bodiala.provider;

import java.util.Locale;

/** Stable identifier for a hotel-supplier integration. Persisted on each booking. */
public enum ProviderId {
    REZLIVE,
    HOTELBEDS;

    /**
     * Parse a supplier name case-insensitively (e.g. {@code rezlive}, {@code RezLive}, {@code REZLIVE}).
     * Used to bind the {@code ?provider=} query param, the {@code hotel.provider} default, and a
     * booking's persisted {@code provider} column. Throws {@link IllegalArgumentException} for an
     * unknown or null value (→ HTTP 400 via the global handler).
     */
    public static ProviderId from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("provider is required");
        }
        return ProviderId.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
