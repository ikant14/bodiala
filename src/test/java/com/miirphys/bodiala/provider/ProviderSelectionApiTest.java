package com.miirphys.bodiala.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * End-to-end proof that the {@code ?provider=} query param binds through the whole MVC stack: the
 * {@link ProviderIdConverter} is auto-registered (a lowercase value is accepted), the request routes
 * to the selected supplier, and the outcomes map to the right HTTP status. Runs against a real port
 * (driven with the JDK HttpClient) with no credentials configured (default profile), so no live
 * supplier call is made — each assertion short-circuits before any upstream request.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProviderSelectionApiTest {

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private int post(String pathAndQuery, String jsonBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + pathAndQuery))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private int get(String pathAndQuery) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + pathAndQuery))
                .GET().build();
        return http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    @Test
    void lowercaseProviderBindsAndRoutesToHotelbeds() throws Exception {
        // ?provider=hotelbeds (lowercase) only reaches HotelbedsSearchProvider — and gets 503 for
        // missing creds — if the case-insensitive converter is registered and routing works.
        assertThat(post("/api/search/by-destination?provider=hotelbeds", "{}")).isEqualTo(503);
    }

    @Test
    void unknownProviderIsRejectedWith400() throws Exception {
        assertThat(post("/api/search/by-destination?provider=expedia", "{}")).isEqualTo(400);
    }

    @Test
    void bookingKeyedReadRoutesByStoredProviderAnd404sWhenUnknown() throws Exception {
        // No ?provider= — forBooking looks up the row, which doesn't exist → 404.
        assertThat(get("/api/bookings/does-not-exist")).isEqualTo(404);
    }
}
