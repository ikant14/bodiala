package com.miirphys.bodiala.provider.hotelbeds;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class HotelbedsSignatureTest {

    @Test
    void isLowercaseSha256HexOfApiKeyPlusSecretPlusSeconds() throws Exception {
        String apiKey = "myApiKey";
        String secret = "mySecret";
        long seconds = 1500984199L;

        String actual = HotelbedsSignature.compute(apiKey, secret, seconds);

        // Independently recompute: SHA-256 over the exact concatenation apiKey + secret + seconds.
        byte[] expected = MessageDigest.getInstance("SHA-256")
                .digest((apiKey + secret + seconds).getBytes(StandardCharsets.UTF_8));
        assertThat(actual).isEqualTo(HexFormat.of().formatHex(expected));
        assertThat(actual).matches("[0-9a-f]{64}");
    }

    @Test
    void matchesHotelbedsWorkedVector() {
        // sha256("apiKey" + "secret" + "1500000000"), computed independently (see api-contract.md §0).
        assertThat(HotelbedsSignature.compute("apiKey", "secret", 1500000000L))
                .isEqualTo("737588110177fbb75b52721ce2ff7995d53cd195cc454353ba98511048a7be91");
    }

    @Test
    void regeneratesWhenTimestampChanges() {
        assertThat(HotelbedsSignature.compute("k", "s", 1L))
                .isNotEqualTo(HotelbedsSignature.compute("k", "s", 2L));
    }
}
