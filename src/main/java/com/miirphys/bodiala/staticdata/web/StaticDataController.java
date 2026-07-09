package com.miirphys.bodiala.staticdata.web;

import com.miirphys.bodiala.provider.CatalogImporterRegistry;
import com.miirphys.bodiala.provider.ProviderId;
import com.miirphys.bodiala.provider.hotelbeds.HotelbedsContentService;
import com.miirphys.bodiala.staticdata.ImportResult;
import com.miirphys.bodiala.staticdata.domain.City;
import com.miirphys.bodiala.staticdata.domain.Country;
import com.miirphys.bodiala.staticdata.domain.Hotel;
import com.miirphys.bodiala.staticdata.domain.HotelImage;
import com.miirphys.bodiala.staticdata.repo.CityRepository;
import com.miirphys.bodiala.staticdata.repo.CountryRepository;
import com.miirphys.bodiala.staticdata.repo.HotelImageRepository;
import com.miirphys.bodiala.staticdata.repo.HotelRepository;
import com.miirphys.bodiala.staticdata.repo.PropertyAmenityRepository;
import com.miirphys.bodiala.staticdata.repo.RoomAmenityRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Syncs the Hotelbeds catalog into the local static-data cache and queries it.
 */
@RestController
@RequestMapping("/api/static-data")
@Tag(name = "Static data", description = "Sync and query the cached Hotelbeds catalog")
public class StaticDataController {

    private final CatalogImporterRegistry catalogImporters;
    private final HotelbedsContentService hotelbedsContentService;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final HotelRepository hotelRepository;
    private final HotelImageRepository hotelImageRepository;
    private final PropertyAmenityRepository propertyAmenityRepository;
    private final RoomAmenityRepository roomAmenityRepository;

    public StaticDataController(CatalogImporterRegistry catalogImporters,
                                HotelbedsContentService hotelbedsContentService,
                                CountryRepository countryRepository,
                                CityRepository cityRepository,
                                HotelRepository hotelRepository,
                                HotelImageRepository hotelImageRepository,
                                PropertyAmenityRepository propertyAmenityRepository,
                                RoomAmenityRepository roomAmenityRepository) {
        this.catalogImporters = catalogImporters;
        this.hotelbedsContentService = hotelbedsContentService;
        this.countryRepository = countryRepository;
        this.cityRepository = cityRepository;
        this.hotelRepository = hotelRepository;
        this.hotelImageRepository = hotelImageRepository;
        this.propertyAmenityRepository = propertyAmenityRepository;
        this.roomAmenityRepository = roomAmenityRepository;
    }

    /** Import the catalog from the supplier named by {@code ?provider=} (default {@code hotel.provider}). */
    @Operation(summary = "Import the catalog from a supplier",
            description = "Fills the local cache from the Hotelbeds Content API, replacing each table (idempotent).")
    @PostMapping("/import")
    public ImportResult importCatalog(
            @Parameter(description = "Supplier to import from: hotelbeds (case-insensitive). "
                    + "Omit to use the configured default.")
            @RequestParam(required = false) ProviderId provider) {
        return catalogImporters.resolve(provider).importCatalog();
    }

    @Operation(summary = "List countries")
    @GetMapping("/countries")
    public List<Country> countries() {
        return countryRepository.findAll();
    }

    @Operation(summary = "List cities", description = "Optionally filter by 2-letter country code, e.g. AE")
    @GetMapping("/cities")
    public List<City> cities(@RequestParam(required = false) String country) {
        return country == null ? cityRepository.findAll() : cityRepository.findByCountryCode(country);
    }

    @Operation(summary = "List hotels (paged)",
            description = "Filter by city (numeric code, e.g. 968) or 2-letter country code.")
    @GetMapping("/hotels")
    public Page<Hotel> hotels(@RequestParam(required = false) String city,
                              @RequestParam(required = false) String country,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 500));
        if (city != null) {
            return hotelRepository.findByCityCode(city, pageable);
        }
        if (country != null) {
            return hotelRepository.findByCountryCode(country, pageable);
        }
        return hotelRepository.findAll(pageable);
    }

    @Operation(summary = "Get one hotel with images + amenities",
            description = "Reads the local cache. On a miss, ?provider=hotelbeds fetches the hotel live from the "
                    + "Hotelbeds Content API and caches it (an availability search returns hotels beyond the "
                    + "bounded catalog import). Unknown code → 404.")
    @GetMapping("/hotels/{hotelCode}")
    public HotelView hotel(@PathVariable Long hotelCode,
                           @Parameter(description = "On a cache miss, fetch this hotel live from the supplier "
                                   + "(currently only hotelbeds).")
                           @RequestParam(required = false) ProviderId provider) {
        Hotel hotel = hotelRepository.findById(hotelCode)
                .orElseGet(() -> fetchLive(hotelCode, provider));
        List<String> images = hotelImageRepository.findByHotelCode(hotelCode).stream()
                .map(HotelImage::getImage)
                .toList();
        String propertyAmenities = propertyAmenityRepository.findById(hotelCode)
                .map(a -> a.getHotelAmenities())
                .orElse(null);
        String roomAmenities = roomAmenityRepository.findById(hotelCode)
                .map(a -> a.getRoomAmenities())
                .orElse(null);
        return new HotelView(hotel, images, propertyAmenities, roomAmenities);
    }

    /** Cache miss: fetch the hotel live from the supplier (Hotelbeds only for now), else 404. */
    private Hotel fetchLive(Long hotelCode, ProviderId provider) {
        if (provider == ProviderId.HOTELBEDS && hotelbedsContentService.isConfigured()) {
            return hotelbedsContentService.fetchAndCache(hotelCode); // NoSuchElement (404) if unknown
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown hotelCode " + hotelCode);
    }
}
