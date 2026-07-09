package com.miirphys.bodiala.provider.hotelbeds;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit-tests the error-message extraction for both Hotelbeds error-body shapes. */
class HotelbedsJsonClientTest {

    @Test
    void extractsNestedApplicationErrorMessage() {
        String body = "{\"auditData\":{\"token\":\"X\"},\"error\":{\"code\":\"INVALID_DATA-X\","
                + "\"message\":\"The booking has already been cancelled\"}}";
        assertThat(HotelbedsJsonClient.extractMessage(body))
                .isEqualTo("The booking has already been cancelled");
    }

    @Test
    void extractsFlatGatewayError() {
        assertThat(HotelbedsJsonClient.extractMessage("{\"error\":\"Request signature verification failed\"}"))
                .isEqualTo("Request signature verification failed");
    }

    @Test
    void fallsBackToRawBodyWhenNoRecognisedShape() {
        assertThat(HotelbedsJsonClient.extractMessage("Service Unavailable")).isEqualTo("Service Unavailable");
        assertThat(HotelbedsJsonClient.extractMessage("")).isEqualTo("no error body");
    }
}
