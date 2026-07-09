package com.miirphys.bodiala.provider.rezlive.staticimport;

import com.miirphys.bodiala.staticdata.ImportResult;
import com.miirphys.bodiala.provider.rezlive.RezLiveProperties;
import com.miirphys.bodiala.provider.rezlive.staticimport.CsvMasterFileReader;
import com.miirphys.bodiala.provider.rezlive.staticimport.CsvMasterFileReader.Row;
import com.miirphys.bodiala.provider.rezlive.staticimport.MasterFile;
import com.miirphys.bodiala.staticdata.domain.City;
import com.miirphys.bodiala.staticdata.domain.Country;
import com.miirphys.bodiala.staticdata.domain.Hotel;
import com.miirphys.bodiala.staticdata.domain.HotelImage;
import com.miirphys.bodiala.staticdata.domain.PropertyAmenity;
import com.miirphys.bodiala.staticdata.domain.RoomAmenity;
import com.miirphys.bodiala.staticdata.repo.CityRepository;
import com.miirphys.bodiala.staticdata.repo.CountryRepository;
import com.miirphys.bodiala.staticdata.repo.HotelImageRepository;
import com.miirphys.bodiala.staticdata.repo.HotelRepository;
import com.miirphys.bodiala.staticdata.repo.PropertyAmenityRepository;
import com.miirphys.bodiala.staticdata.repo.RoomAmenityRepository;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ingests the RezLive CSV master files from the static-data directory into the local cache.
 *
 * <p>RezLive provides no delta/incremental feed, so each master file is treated as a full
 * snapshot: the corresponding table is fully replaced on every import (delete-all then insert),
 * which makes re-imports idempotent. Files are imported in dependency order
 * (country -> city -> hotel -> images/amenities).
 */
@Service
public class StaticDataImportService {

    private static final Logger log = LoggerFactory.getLogger(StaticDataImportService.class);

    private final RezLiveProperties properties;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final HotelRepository hotelRepository;
    private final HotelImageRepository hotelImageRepository;
    private final PropertyAmenityRepository propertyAmenityRepository;
    private final RoomAmenityRepository roomAmenityRepository;

    public StaticDataImportService(RezLiveProperties properties,
                                   CountryRepository countryRepository,
                                   CityRepository cityRepository,
                                   HotelRepository hotelRepository,
                                   HotelImageRepository hotelImageRepository,
                                   PropertyAmenityRepository propertyAmenityRepository,
                                   RoomAmenityRepository roomAmenityRepository) {
        this.properties = properties;
        this.countryRepository = countryRepository;
        this.cityRepository = cityRepository;
        this.hotelRepository = hotelRepository;
        this.hotelImageRepository = hotelImageRepository;
        this.propertyAmenityRepository = propertyAmenityRepository;
        this.roomAmenityRepository = roomAmenityRepository;
    }

    /** Import all master files from the configured {@code rezlive.static-data-dir}. */
    @Transactional
    public ImportResult importAll() {
        return importAll(Path.of(properties.getStaticDataDir()));
    }

    /** Import all master files found in the given directory. */
    @Transactional
    public ImportResult importAll(Path dir) {
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Static-data directory does not exist: " + dir.toAbsolutePath());
        }
        log.info("Importing RezLive static data from {}", dir.toAbsolutePath());
        ImportResult result = ImportResult.empty();

        importFile(dir, MasterFile.COUNTRY, countryRepository, this::toCountry, result);
        importFile(dir, MasterFile.CITY, cityRepository, this::toCity, result);
        importFile(dir, MasterFile.HOTEL_DETAILS, hotelRepository, this::toHotel, result);
        importFile(dir, MasterFile.HOTEL_IMAGES, hotelImageRepository, this::toHotelImage, result);
        importFile(dir, MasterFile.PROPERTY_AMENITIES, propertyAmenityRepository, this::toPropertyAmenity, result);
        importFile(dir, MasterFile.ROOM_AMENITIES, roomAmenityRepository, this::toRoomAmenity, result);

        log.info("Static-data import complete: {} rows across {} files ({} skipped)",
                result.totalRows(), result.imported().size(), result.skipped().size());
        return result;
    }

    private <T> void importFile(Path dir,
                                MasterFile masterFile,
                                JpaRepository<T, ?> repository,
                                Function<Row, T> mapper,
                                ImportResult result) {
        Optional<Path> file = resolveFile(dir, masterFile);
        if (file.isEmpty()) {
            result.skipped().put(masterFile.name(), "no file matching '*" + masterFile.filenameKeyword() + "*.csv'");
            log.warn("Skipping {} — no matching CSV in {}", masterFile, dir);
            return;
        }

        List<T> entities = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(file.get(), StandardCharsets.UTF_8)) {
            for (Row row : CsvMasterFileReader.read(reader, masterFile)) {
                T entity = mapper.apply(row);
                if (entity != null) {
                    entities.add(entity);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading " + file.get(), e);
        }

        // Full-snapshot semantics: replace the whole table.
        repository.deleteAllInBatch();
        repository.saveAll(entities);
        result.imported().put(masterFile.name(), entities.size());
        log.info("Imported {} rows for {} from {}", entities.size(), masterFile, file.get().getFileName());
    }

    private Optional<Path> resolveFile(Path dir, MasterFile masterFile) {
        String keyword = masterFile.filenameKeyword();
        try (var paths = Files.list(dir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.endsWith(".csv") && name.contains(keyword);
                    })
                    .sorted()
                    .findFirst();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed listing " + dir, e);
        }
    }

    // --- Row -> entity mappers (return null to skip an unusable row) --------------------

    private Country toCountry(Row row) {
        String code = row.getString("CountryCode");
        // CountryCode is a 2-letter ISO code (stored in a length-2 column). Skip anything that
        // isn't 2 chars — this also drops a stray non-canonical header row that slipped through
        // as data, instead of letting it raise a DB error that aborts the whole import.
        if (code == null || code.length() != 2) {
            return null;
        }
        return new Country(code, row.getString("Name"));
    }

    private City toCity(Row row) {
        String cityCode = row.getString("City");
        if (cityCode == null) {
            return null;
        }
        return new City(cityCode, row.getString("Name"), row.getString("CountryCode"));
    }

    private Hotel toHotel(Row row) {
        Long hotelCode = row.getLong("HotelCode");
        if (hotelCode == null) {
            return null;
        }
        Hotel hotel = new Hotel();
        hotel.setHotelCode(hotelCode);
        hotel.setName(row.getString("Name"));
        hotel.setCityCode(row.getString("City"));
        hotel.setCountryCode(row.getString("CountryCode"));
        hotel.setRating(row.getInt("Rating"));
        hotel.setHotelAddress(row.getString("HotelAddress"));
        hotel.setHotelPostalCode(row.getString("HotelPostelCode")); // misspelled in RezLive CSV header
        hotel.setLatitude(row.getDouble("Latitude"));
        hotel.setLongitude(row.getDouble("Longitude"));
        hotel.setDescription(row.getString("Desc"));
        return hotel;
    }

    private HotelImage toHotelImage(Row row) {
        Long hotelCode = row.getLong("HotelCode");
        String image = row.getString("Image");
        if (hotelCode == null || image == null) {
            return null;
        }
        return new HotelImage(hotelCode, image);
    }

    private PropertyAmenity toPropertyAmenity(Row row) {
        Long hotelCode = row.getLong("HotelCode");
        if (hotelCode == null) {
            return null;
        }
        return new PropertyAmenity(hotelCode, row.getString("HotelAmenities"));
    }

    private RoomAmenity toRoomAmenity(Row row) {
        Long hotelCode = row.getLong("HotelCode");
        if (hotelCode == null) {
            return null;
        }
        return new RoomAmenity(hotelCode, row.getString("RoomAmenities"));
    }
}
