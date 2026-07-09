package com.miirphys.bodiala.staticdata;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.miirphys.bodiala.provider.rezlive.RezLiveProperties;
import com.miirphys.bodiala.provider.rezlive.staticimport.StaticDataImportService;
import com.miirphys.bodiala.provider.rezlive.staticimport.StaticDataStartupImporter;
import com.miirphys.bodiala.staticdata.repo.CountryRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StaticDataStartupImporterTest {

    private final StaticDataImportService importService = mock(StaticDataImportService.class);
    private final CountryRepository countryRepository = mock(CountryRepository.class);

    @TempDir
    Path dir;

    private RezLiveProperties props() {
        RezLiveProperties p = new RezLiveProperties();
        p.setStaticDataDir(dir.toString());
        return p;
    }

    private StaticDataStartupImporter importer(RezLiveProperties p) {
        return new StaticDataStartupImporter(p, importService, countryRepository);
    }

    @Test
    void disabledDoesNothing() {
        RezLiveProperties p = props();
        p.setImportOnStartup(false);

        importer(p).run(null);

        verify(importService, never()).importAll(any(Path.class));
        verify(countryRepository, never()).count();
    }

    @Test
    void skipsWhenCacheAlreadyPopulated() {
        when(countryRepository.count()).thenReturn(5L);

        importer(props()).run(null);

        verify(importService, never()).importAll(any(Path.class));
    }

    @Test
    void importsWhenEnabledDirExistsAndCacheEmpty() throws IOException {
        Files.writeString(dir.resolve("country.csv"), "Name,CountryCode\nBelgium,BE\n");
        when(countryRepository.count()).thenReturn(0L);
        when(importService.importAll(any(Path.class))).thenReturn(ImportResult.empty());

        importer(props()).run(null);

        verify(importService).importAll(eq(dir));
    }

    @Test
    void neverFailsStartupWhenImportThrows() {
        when(countryRepository.count()).thenReturn(0L);
        when(importService.importAll(any(Path.class))).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> importer(props()).run(null)).doesNotThrowAnyException();
    }
}
