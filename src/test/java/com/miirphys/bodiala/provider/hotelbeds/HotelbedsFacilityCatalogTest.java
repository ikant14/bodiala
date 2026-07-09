package com.miirphys.bodiala.provider.hotelbeds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.miirphys.bodiala.provider.hotelbeds.dto.FacilityTypesResponse;
import com.miirphys.bodiala.provider.hotelbeds.dto.HotelContentResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for the lazily-cached facilities-master lookup (mocked client). */
class HotelbedsFacilityCatalogTest {

    private final HotelbedsJsonClient client = mock(HotelbedsJsonClient.class);

    private static HotelbedsProperties configured() {
        HotelbedsProperties p = new HotelbedsProperties();
        p.setApiKey("k");
        p.setSecret("s");
        return p;
    }

    private static FacilityTypesResponse master() {
        return new FacilityTypesResponse(List.of(
                new FacilityTypesResponse.FacilityType(260, 70, new HotelContentResponse.Content("Bar", "ENG")),
                new FacilityTypesResponse.FacilityType(261, 70, new HotelContentResponse.Content("Wi-Fi", "ENG"))), 2);
    }

    @Test
    void resolvesNamesAndFetchesTheMasterOnce() {
        when(client.get(anyString(), eq(FacilityTypesResponse.class))).thenReturn(master());
        HotelbedsFacilityCatalog catalog = new HotelbedsFacilityCatalog(client, configured());

        assertThat(catalog.nameFor(70, 260)).isEqualTo("Bar");
        assertThat(catalog.nameFor(70, 261)).isEqualTo("Wi-Fi");
        assertThat(catalog.nameFor(99, 99)).isNull();       // unknown pair
        assertThat(catalog.nameFor(null, 260)).isNull();    // null-safe

        verify(client, times(1)).get(anyString(), eq(FacilityTypesResponse.class)); // cached after first load
    }

    @Test
    void missingCredentialsYieldsNoNamesAndNoCall() {
        HotelbedsFacilityCatalog catalog = new HotelbedsFacilityCatalog(client, new HotelbedsProperties());

        assertThat(catalog.nameFor(70, 260)).isNull();
        verifyNoInteractions(client);
    }

    @Test
    void upstreamFailureDegradesToEmptyRatherThanThrowing() {
        when(client.get(anyString(), eq(FacilityTypesResponse.class))).thenThrow(new RuntimeException("boom"));
        HotelbedsFacilityCatalog catalog = new HotelbedsFacilityCatalog(client, configured());

        assertThat(catalog.nameFor(70, 260)).isNull();      // no exception propagated
    }
}
