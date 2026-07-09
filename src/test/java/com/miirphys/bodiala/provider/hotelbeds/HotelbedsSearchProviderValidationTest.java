package com.miirphys.bodiala.provider.hotelbeds;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miirphys.bodiala.search.DestinationSearchRequest;
import com.miirphys.bodiala.search.RoomRequest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pre-flight validation of the Hotelbeds availability request: bad input must fail fast with an
 * IllegalArgumentException (→ HTTP 400) rather than being sent to Hotelbeds and bounced back as a
 * 502 (which also wastes the tight test-key quota). The client is never reached, so it's null.
 */
class HotelbedsSearchProviderValidationTest {

    private final HotelbedsSearchProvider provider =
            new HotelbedsSearchProvider(null, credentialedProperties());

    private static HotelbedsProperties credentialedProperties() {
        HotelbedsProperties p = new HotelbedsProperties();
        p.setApiKey("k");
        p.setSecret("s");
        return p; // baseUrl already defaults to the test host, so hasCredentials() is true
    }

    private static DestinationSearchRequest request(String city, Integer adults) {
        return new DestinationSearchRequest(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3), "AE", city, "AE",
                List.of(), List.of(new RoomRequest("DBL", adults, 0, List.of())));
    }

    @Test
    void rejectsDestinationCodeLongerThanThreeChars() {
        assertThatThrownBy(() -> provider.searchByDestination(request("115936", 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1-3 characters");
    }

    @Test
    void rejectsBlankDestinationCode() {
        assertThatThrownBy(() -> provider.searchByDestination(request("   ", 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void rejectsRoomWithZeroAdults() {
        assertThatThrownBy(() -> provider.searchByDestination(request("DXB", 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 1 adult");
    }

    @Test
    void acceptsAValidThreeCharCodeAndAtLeastOneAdult() {
        // Passes validation, then fails only at the (null) client call — proving validation let it through.
        assertThatThrownBy(() -> provider.searchByDestination(request("DXB", 2)))
                .isInstanceOf(NullPointerException.class);
    }
}
