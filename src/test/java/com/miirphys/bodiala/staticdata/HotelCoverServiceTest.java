package com.miirphys.bodiala.staticdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miirphys.bodiala.provider.ProviderId;
import com.miirphys.bodiala.provider.error.UpstreamApiException;
import com.miirphys.bodiala.provider.hotelbeds.HotelbedsContentService;
import com.miirphys.bodiala.staticdata.domain.Hotel;
import com.miirphys.bodiala.staticdata.domain.HotelImage;
import com.miirphys.bodiala.staticdata.repo.HotelImageRepository;
import com.miirphys.bodiala.staticdata.repo.HotelRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Cache-first cover lookup with one batched live fetch for the misses (mocked repos + content service). */
class HotelCoverServiceTest {

    private final HotelImageRepository images = mock(HotelImageRepository.class);
    private final HotelRepository hotels = mock(HotelRepository.class);
    private final HotelbedsContentService content = mock(HotelbedsContentService.class);
    private final HotelCoverService service = new HotelCoverService(images, hotels, content);

    @BeforeEach
    void configured() {
        when(content.isConfigured()).thenReturn(true);
    }

    private static HotelImage image(long code) {
        return new HotelImage(code, "https://img/" + code + ".jpg");
    }

    private static Hotel hotel(long code) {
        Hotel h = new Hotel();
        h.setHotelCode(code);
        return h;
    }

    private static org.mockito.ArgumentMatcher<Collection<Long>> exactly(Long... codes) {
        return c -> c != null && c.size() == codes.length && c.containsAll(List.of(codes));
    }

    @Test
    void allCachedMeansNoLiveCall() {
        when(images.findCoverImages(any())).thenReturn(List.of(image(1), image(2)));

        Map<Long, String> covers = service.covers(List.of(1L, 2L), ProviderId.HOTELBEDS);

        assertThat(covers).containsOnlyKeys(1L, 2L);
        verify(content, never()).fetchAndCacheAll(any());
    }

    @Test
    void uncachedHotelsAreFetchedInOneBatchAndTheirCoversReturned() {
        when(images.findCoverImages(argThat(exactly(1L, 2L, 3L)))).thenReturn(List.of(image(1)));
        when(hotels.findAllById(any())).thenReturn(List.of());           // 2 and 3 never cached
        when(images.findCoverImages(argThat(exactly(2L, 3L)))).thenReturn(List.of(image(2), image(3)));

        Map<Long, String> covers = service.covers(List.of(1L, 2L, 3L), ProviderId.HOTELBEDS);

        assertThat(covers).containsOnlyKeys(1L, 2L, 3L);
        verify(content).fetchAndCacheAll(List.of(2L, 3L));
    }

    @Test
    void hotelCachedWithoutPhotosIsNotFetchedAgain() {
        when(images.findCoverImages(any())).thenReturn(List.of());
        when(hotels.findAllById(any())).thenReturn(List.of(hotel(5)));   // cached, but has no images

        assertThat(service.covers(List.of(5L), ProviderId.HOTELBEDS)).isEmpty();
        verify(content, never()).fetchAndCacheAll(any());
    }

    @Test
    void liveFailureStillReturnsTheCachedCovers() {
        when(images.findCoverImages(any())).thenReturn(List.of(image(1)));
        when(hotels.findAllById(any())).thenReturn(List.of());
        when(content.fetchAndCacheAll(any())).thenThrow(new UpstreamApiException("Hotelbeds 403: quota exceeded"));

        assertThat(service.covers(List.of(1L, 2L), ProviderId.HOTELBEDS)).containsOnlyKeys(1L);
    }

    @Test
    void withoutProviderOnlyTheCacheIsRead() {
        when(images.findCoverImages(any())).thenReturn(List.of());

        assertThat(service.covers(List.of(1L), null)).isEmpty();
        verify(content, never()).fetchAndCacheAll(any());
    }

    @Test
    void tooManyCodesIsABadRequest() {
        List<Long> codes = LongStream.rangeClosed(1, HotelCoverService.MAX_CODES + 1).boxed().toList();

        assertThatThrownBy(() -> service.covers(codes, ProviderId.HOTELBEDS))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
