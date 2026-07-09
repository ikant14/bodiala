package com.miirphys.bodiala.provider.hotelbeds;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miirphys.bodiala.staticdata.ImportResult;
import com.miirphys.bodiala.staticdata.repo.HotelRepository;
import org.junit.jupiter.api.Test;

/** Unit tests for the guarded Hotelbeds startup auto-import (mocked importer + repo). */
class HotelbedsStartupImporterTest {

    private final HotelbedsCatalogImporter catalog = mock(HotelbedsCatalogImporter.class);
    private final HotelRepository hotels = mock(HotelRepository.class);

    private static HotelbedsProperties props(boolean onStartup, boolean creds) {
        HotelbedsProperties p = new HotelbedsProperties();
        p.setImportOnStartup(onStartup);
        if (creds) {
            p.setApiKey("k");
            p.setSecret("s");
        }
        return p;
    }

    private HotelbedsStartupImporter runner(HotelbedsProperties p) {
        return new HotelbedsStartupImporter(p, catalog, hotels);
    }

    @Test
    void seedsWhenEnabledConfiguredAndCacheEmpty() {
        when(hotels.count()).thenReturn(0L);
        when(catalog.importCatalog()).thenReturn(ImportResult.empty());

        runner(props(true, true)).run(null);

        verify(catalog).importCatalog();
    }

    @Test
    void disabledByDefault() {
        runner(props(false, true)).run(null);
        verify(catalog, never()).importCatalog();
    }

    @Test
    void skipsWhenCacheAlreadyPopulated() {
        when(hotels.count()).thenReturn(42L);
        runner(props(true, true)).run(null);
        verify(catalog, never()).importCatalog();
    }

    @Test
    void skipsWhenCredentialsMissing() {
        runner(props(true, false)).run(null);
        verify(catalog, never()).importCatalog();
    }

    @Test
    void importFailureNeverBreaksStartup() {
        when(hotels.count()).thenReturn(0L);
        when(catalog.importCatalog()).thenThrow(new RuntimeException("stub not up"));

        assertThatCode(() -> runner(props(true, true)).run(null)).doesNotThrowAnyException();
    }
}
