package com.miirphys.bodiala.security;

import static com.miirphys.bodiala.security.Access.BOOKING_TOKEN;
import static com.miirphys.bodiala.security.Access.PUBLIC;
import static com.miirphys.bodiala.security.Access.STAFF;

import java.util.List;
import org.springframework.http.HttpMethod;

/**
 * The explicit list of every route the API serves and who may call it. {@link SecurityConfig} turns
 * it into the filter chain's rules, in this order (the first match wins), and then <b>denies
 * everything else</b>: a route that is not listed here returns 401 (anonymous) or 403 (staff).
 * Adding an endpoint therefore means adding a row here; {@code RouteMatrixTest} fails until you do.
 *
 * <p>Patterns use Spring's path-pattern syntax ({@code {var}} = one segment, {@code /**} = the rest).
 */
public final class RouteMatrix {

    /** One row of the matrix. {@code method == null} matches every HTTP method. */
    public record Route(HttpMethod method, String pattern, Access access) {
    }

    public static final List<Route> ROUTES = List.of(
            // --- Search and content reads -------------------------------------------------
            new Route(HttpMethod.POST, "/api/search/by-destination", PUBLIC),
            new Route(HttpMethod.POST, "/api/search/by-hotel-ids", PUBLIC),
            new Route(HttpMethod.GET, "/api/static-data/countries", PUBLIC),
            new Route(HttpMethod.GET, "/api/static-data/cities", PUBLIC),
            new Route(HttpMethod.GET, "/api/static-data/hotels", PUBLIC),
            new Route(HttpMethod.GET, "/api/static-data/hotels/{hotelCode}", PUBLIC),
            new Route(HttpMethod.GET, "/api/static-data/hotel-covers", PUBLIC),

            // --- Booking chain --------------------------------------------------------------
            // Quote, then create. Create is meant to sit behind payment or an agent (PAY-3, BOOK-10).
            new Route(HttpMethod.POST, "/api/bookings/prebook", PUBLIC),
            new Route(HttpMethod.POST, "/api/bookings", PUBLIC),
            // Read and cancel one booking: the owner's manage token once SEC-2 lands; open until then.
            new Route(HttpMethod.GET, "/api/bookings/{bookingId}", BOOKING_TOKEN),
            new Route(HttpMethod.POST, "/api/bookings/{bookingId}/cancel", BOOKING_TOKEN),

            // --- Staff ----------------------------------------------------------------------
            // Wipes and refills the catalog tables and spends Content-API quota.
            new Route(HttpMethod.POST, "/api/static-data/import", STAFF),
            // The admin booking list (every customer's bookings, paginated).
            new Route(HttpMethod.GET, "/api/bookings", STAFF),
            // Reserved for staff tools (e.g. the BOOK-10 agent screen).
            new Route(null, "/api/admin/**", STAFF),

            // --- API docs (SEC-8 turns springdoc off in prod, so these then 404) -------------
            new Route(HttpMethod.GET, "/v3/api-docs/**", PUBLIC),
            new Route(HttpMethod.GET, "/v3/api-docs.yaml", PUBLIC),
            new Route(HttpMethod.GET, "/swagger-ui.html", PUBLIC),
            new Route(HttpMethod.GET, "/swagger-ui/**", PUBLIC),

            // --- Actuator (OPS-5) -------------------------------------------------------------
            // Liveness/readiness probes are open (they show no details to anonymous callers);
            // every other actuator endpoint (metrics, env, ...) is staff-only.
            new Route(HttpMethod.GET, "/actuator/health/**", PUBLIC),
            new Route(null, "/actuator/**", STAFF));

    private RouteMatrix() {
    }
}
