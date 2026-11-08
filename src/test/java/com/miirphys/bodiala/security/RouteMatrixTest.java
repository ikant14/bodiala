package com.miirphys.bodiala.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.miirphys.bodiala.booking.HotelBooking;
import com.miirphys.bodiala.booking.HotelBookingRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The route-authorisation matrix, end to end over a real port (SEC-1; QA-4 extends it with the
 * booking-token and cross-owner cases once SEC-2 exists). Uses the test-only staff login from
 * {@code src/test/resources/application.properties}. No Hotelbeds credentials are set, so routes that
 * would call the supplier stop at 503 before any upstream request.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RouteMatrixTest {

    private static final String STAFF = basic("staff", "test-staff-password");
    private static final String WRONG_PASSWORD = basic("staff", "not-the-password");

    @LocalServerPort
    int port;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    @Autowired
    HotelBookingRepository bookings;

    private final HttpClient http = HttpClient.newHttpClient();

    // --- the matrix covers every route the app serves ------------------------------------

    @Test
    void everyControllerRouteHasAMatrixEntry() {
        List<String> missing = new ArrayList<>();
        mappings.getHandlerMethods().forEach((info, handler) -> {
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            for (String pattern : info.getPatternValues()) {
                if (pattern.equals("/error")) {
                    continue; // Boot's error page: reached only by the (permitted) error dispatch
                }
                for (RequestMethod method : methods.isEmpty() ? Set.of(RequestMethod.GET) : methods) {
                    if (matrixEntry(HttpMethod.valueOf(method.name()), pattern).isEmpty()) {
                        missing.add(method + " " + pattern);
                    }
                }
            }
        });
        assertThat(missing).as("routes with no RouteMatrix entry (they are denied)").isEmpty();
    }

    // --- deny by default ---------------------------------------------------------------

    @Test
    void routesNotInTheMatrixAreDenied() throws Exception {
        assertThat(call("GET", "/api/not-a-route", null).statusCode()).isEqualTo(401);
        assertThat(call("DELETE", "/api/bookings/1-123", null).statusCode()).isEqualTo(401);
        assertThat(call("PUT", "/api/static-data/countries", null).statusCode()).isEqualTo(401);
        assertThat(call("GET", "/api/static-data/import", null).statusCode()).isEqualTo(401);
        assertThat(call("GET", "/error", null).statusCode()).isEqualTo(401);
        // An authenticated staff user is still refused a route nobody listed.
        assertThat(call("GET", "/api/not-a-route", STAFF).statusCode()).isEqualTo(403);
        assertThat(call("DELETE", "/api/bookings/1-123", STAFF).statusCode()).isEqualTo(403);
    }

    @Test
    void a401IsProblemJsonWithNoBasicChallenge() throws Exception {
        HttpResponse<String> response = call("GET", "/api/bookings", null);

        assertThat(response.statusCode()).isEqualTo(401);
        // No WWW-Authenticate: Basic, so browsers never prompt for (and cache) staff credentials.
        assertThat(response.headers().firstValue("WWW-Authenticate")).isEmpty();
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                type -> assertThat(type).startsWith("application/problem+json"));
        assertThat(json(response).path("status").asInt()).isEqualTo(401);
        // Stateless: no session cookie, ever.
        assertThat(response.headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    void securityErrorsHaveTheSameShapeAsApiExceptionHandlerErrors() throws Exception {
        JsonNode notFound = json(call("GET", "/api/bookings/no-such-booking", null)); // ApiExceptionHandler
        JsonNode unauthorized = json(call("GET", "/api/bookings", null));
        JsonNode forbidden = json(call("GET", "/api/not-a-route", STAFF));

        assertThat(unauthorized.propertyNames()).containsExactlyInAnyOrderElementsOf(notFound.propertyNames());
        assertThat(forbidden.propertyNames()).containsExactlyInAnyOrderElementsOf(notFound.propertyNames());
        assertThat(unauthorized.path("instance").asString()).isEqualTo("/api/bookings");
        assertThat(forbidden.path("status").asInt()).isEqualTo(403);
    }

    @Test
    void aBadBasicHeaderGets401EvenOnAPublicRoute() throws Exception {
        // Documented in SecurityConfig: the Basic filter rejects bad credentials before the matrix runs.
        assertThat(call("GET", "/api/static-data/countries", WRONG_PASSWORD).statusCode()).isEqualTo(401);
        assertThat(call("GET", "/api/static-data/countries", null).statusCode()).isEqualTo(200);
    }

    // --- staff routes ----------------------------------------------------------------

    @Test
    void staffRoutesRejectAnonymousAndWrongCredentials() throws Exception {
        for (String auth : new String[] {null, WRONG_PASSWORD, basic("someone", "test-staff-password")}) {
            assertThat(call("POST", "/api/static-data/import", auth).statusCode()).isEqualTo(401);
            assertThat(call("GET", "/api/bookings", auth).statusCode()).isEqualTo(401);
            assertThat(call("GET", "/api/admin/anything", auth).statusCode()).isEqualTo(401);
            assertThat(call("GET", "/actuator/env", auth).statusCode()).isEqualTo(401);
        }
    }

    @Test
    void staffCanReachTheStaffRoutes() throws Exception {
        // Past security; the importer then refuses for lack of Hotelbeds credentials (503), before any call.
        assertThat(call("POST", "/api/static-data/import", STAFF).statusCode()).isEqualTo(503);
        assertThat(call("GET", "/api/bookings", STAFF).statusCode()).isEqualTo(200);
    }

    @Test
    void theAdminBookingListIsPagedNewestFirstWithAMaximumPageSize() throws Exception {
        List<HotelBooking> saved = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                saved.add(bookings.save(booking("route-matrix-" + UUID.randomUUID())));
            }

            JsonNode page = json(call("GET", "/api/bookings?page=0&size=2", STAFF));
            assertThat(page.path("content").size()).isEqualTo(2);
            assertThat(page.path("content").get(0).path("bookingId").asString())
                    .isEqualTo(saved.get(2).getBookingId());
            assertThat(page.path("page").path("size").asInt()).isEqualTo(2);
            assertThat(page.path("page").path("totalElements").asLong()).isGreaterThanOrEqualTo(3);

            assertThat(call("GET", "/api/bookings?size=100", STAFF).statusCode()).isEqualTo(200);
            assertThat(call("GET", "/api/bookings?size=101", STAFF).statusCode()).isEqualTo(400);
            assertThat(call("GET", "/api/bookings?size=0", STAFF).statusCode()).isEqualTo(400);
            assertThat(call("GET", "/api/bookings?page=-1", STAFF).statusCode()).isEqualTo(400);
        } finally {
            bookings.deleteAll(saved);
        }
    }

    // --- public and booking-token routes ------------------------------------------------

    @Test
    void publicRoutesNeedNoCredentials() throws Exception {
        assertThat(call("GET", "/api/static-data/countries", null).statusCode()).isEqualTo(200);
        assertThat(call("GET", "/api/static-data/cities?country=ES", null).statusCode()).isEqualTo(200);
        assertThat(call("GET", "/api/static-data/hotels", null).statusCode()).isEqualTo(200);
        assertThat(call("GET", "/api/static-data/hotel-covers?codes=1,2", null).statusCode()).isEqualTo(200);
        // Supplier-backed routes get past security and stop at 503 (no Hotelbeds credentials in tests).
        assertThat(call("POST", "/api/search/by-destination", null, "{}").statusCode()).isEqualTo(503);
        assertThat(call("GET", "/v3/api-docs", null).statusCode()).isEqualTo(200);
        assertThat(call("GET", "/swagger-ui.html", null).statusCode()).isIn(200, 302);
    }

    @Test
    void bookingReadAndCancelStayOpenUntilSec2() throws Exception {
        // BOOKING_TOKEN routes are open for now: an unknown booking is a 404, not a 401.
        assertThat(call("GET", "/api/bookings/no-such-booking", null).statusCode()).isEqualTo(404);
        assertThat(call("POST", "/api/bookings/no-such-booking/cancel", null).statusCode()).isEqualTo(404);
    }

    @Test
    void errorsOnPublicRoutesKeepTheirStatus() throws Exception {
        // These go through the container's error dispatch, which must not turn into a 401.
        assertThat(call("GET", "/api/static-data/hotels/999999999", null).statusCode()).isEqualTo(404);
        assertThat(call("GET", "/api/static-data/hotels/not-a-number", null).statusCode()).isEqualTo(400);
        assertThat(call("POST", "/api/search/by-destination?provider=expedia", null, "{}").statusCode())
                .isEqualTo(400);
    }

    // --- helpers -------------------------------------------------------------------

    private static Optional<RouteMatrix.Route> matrixEntry(HttpMethod method, String path) {
        PathContainer container = PathContainer.parsePath(path);
        return RouteMatrix.ROUTES.stream()
                .filter(route -> route.method() == null || route.method().equals(method))
                .filter(route -> PathPatternParser.defaultInstance.parse(route.pattern()).matches(container))
                .findFirst();
    }

    private HttpResponse<String> call(String method, String pathAndQuery, String authorization) throws Exception {
        return call(method, pathAndQuery, authorization, null);
    }

    private HttpResponse<String> call(String method, String pathAndQuery, String authorization, String jsonBody)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + pathAndQuery))
                .method(method, jsonBody == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(jsonBody));
        if (jsonBody != null) {
            request.header("Content-Type", "application/json");
        }
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonNode json(HttpResponse<String> response) {
        return JsonMapper.builder().build().readTree(response.body());
    }

    private static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private static HotelBooking booking(String bookingId) {
        HotelBooking b = new HotelBooking();
        b.setBookingId(bookingId);
        b.setStatus("CONFIRMED");
        b.setProvider("HOTELBEDS");
        b.setHotelName("Route Matrix Test Hotel");
        b.setArrivalDate(LocalDate.now().plusDays(30));
        b.setDepartureDate(LocalDate.now().plusDays(32));
        b.setCurrency("EUR");
        b.setPrice("100.00");
        return b;
    }
}
