package com.miirphys.bodiala.provider.hotelbeds;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Computes the Hotelbeds APItude {@code X-Signature}: the lowercase hex SHA-256 of
 * {@code apiKey + secret + <unix-time-in-seconds>}, regenerated on every request. The timestamp is
 * the current time in seconds since the epoch (UTC); Hotelbeds tolerates a small clock skew, so the
 * host clock must be roughly correct or calls fail with a 401.
 */
public final class HotelbedsSignature {

    private HotelbedsSignature() {
    }

    public static String compute(String apiKey, String secret, long epochSeconds) {
        String raw = apiKey + secret + epochSeconds;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash); // lowercase hex
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e); // never on a standard JRE
        }
    }
}
