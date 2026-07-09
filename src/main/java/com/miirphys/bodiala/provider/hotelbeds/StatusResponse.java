package com.miirphys.bodiala.provider.hotelbeds;

/** Response of {@code GET /hotel-api/1.0/status}. {@code status} is {@code "OK"} when healthy. */
public record StatusResponse(String status) {

    public boolean isOk() {
        return "OK".equalsIgnoreCase(status);
    }
}
