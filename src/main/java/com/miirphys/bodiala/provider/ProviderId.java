package com.miirphys.bodiala.provider;

import java.util.Locale;

/** Stable identifier for a hotel-supplier integration. Persisted on each booking. */
public enum ProviderId {
    HOTELBEDS;

    /**
     * Parse a supplier name case-insensitively (e.g. {@code hotelbeds}, {@code HotelBeds}, {@code HOTELBEDS}).
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

    /**
     * Like {@link #from(String)} but returns {@code fallback} for a blank or unrecognised value
     * instead of throwing — so a stale {@code hotel.provider} config (e.g. a removed supplier) can't
     * brick startup. Used for the configurable default, never for explicit request routing.
     */
    public static ProviderId fromOrDefault(String value, ProviderId fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return ProviderId.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
