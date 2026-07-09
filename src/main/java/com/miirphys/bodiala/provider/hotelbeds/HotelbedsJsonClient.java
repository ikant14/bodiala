package com.miirphys.bodiala.provider.hotelbeds;

import com.miirphys.bodiala.provider.error.UpstreamApiException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Signed JSON transport for the Hotelbeds APItude API. Every call carries {@code Api-key} +
 * {@code X-Signature} (see {@link HotelbedsSignature}); error responses are turned into an
 * {@link UpstreamApiException} (→ HTTP 502 via the global handler).
 *
 * <p>{@code Accept-Encoding} is intentionally omitted so Hotelbeds returns uncompressed JSON that
 * the message converters deserialize directly — no manual gzip handling. Timeouts are 5s connect /
 * 60s read (bookings can be slow). Refinement of the per-status handling (401/403/429/410) is
 * tracked in {@code docs/hotelbeds/api-contract.md} §6.
 */
@Component
public class HotelbedsJsonClient {

    private static final Pattern NESTED_MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern FLAT_ERROR = Pattern.compile("\"error\"\\s*:\\s*\"([^\"]*)\"");

    private final HotelbedsProperties properties;
    private final RestClient restClient;

    public HotelbedsJsonClient(HotelbedsProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).requestFactory(factory).build();
    }

    /** Signed {@code GET /hotel-api/1.0/status} — succeeds only when auth + host clock are good. */
    public StatusResponse status() {
        return get("/hotel-api/1.0/status", StatusResponse.class);
    }

    public <T> T get(String path, Class<T> type) {
        return restClient.get().uri(path).headers(this::sign)
                .retrieve().onStatus(HttpStatusCode::isError, this::fail).body(type);
    }

    public <T> T post(String path, Object body, Class<T> type) {
        return restClient.post().uri(path).headers(this::sign)
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().onStatus(HttpStatusCode::isError, this::fail).body(type);
    }

    public <T> T delete(String path, Class<T> type) {
        return restClient.method(HttpMethod.DELETE).uri(path).headers(this::sign)
                .retrieve().onStatus(HttpStatusCode::isError, this::fail).body(type);
    }

    private void sign(HttpHeaders headers) {
        long ts = Instant.now().getEpochSecond();
        headers.set("Api-key", properties.getApiKey());
        headers.set("X-Signature", HotelbedsSignature.compute(properties.getApiKey(), properties.getSecret(), ts));
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    }

    private void fail(HttpRequest request, ClientHttpResponse response) throws IOException {
        int code = response.getStatusCode().value();
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        throw new UpstreamApiException("Hotelbeds " + code + ": " + extractMessage(body));
    }

    /** Pull a human message out of either Hotelbeds error shape (nested {@code error.message} or flat {@code error}). */
    static String extractMessage(String body) {
        if (body == null || body.isBlank()) {
            return "no error body";
        }
        Matcher nested = NESTED_MESSAGE.matcher(body);
        if (nested.find()) {
            return nested.group(1);
        }
        Matcher flat = FLAT_ERROR.matcher(body);
        if (flat.find()) {
            return flat.group(1);
        }
        return body.strip();
    }
}
