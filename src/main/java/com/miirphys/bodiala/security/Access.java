package com.miirphys.bodiala.security;

/** Who may call a route in the {@link RouteMatrix}. */
public enum Access {

    /** Anyone, no credentials. */
    PUBLIC,

    /**
     * Whoever holds the booking's manage token (SEC-2). Until SEC-2 adds the token, these routes are
     * open like {@link #PUBLIC}; they are kept as their own level so that change is one switch arm.
     */
    BOOKING_TOKEN,

    /** Only an authenticated staff principal (role {@code STAFF}). */
    STAFF
}
