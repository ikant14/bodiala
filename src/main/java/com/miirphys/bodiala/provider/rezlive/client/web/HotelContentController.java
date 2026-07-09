package com.miirphys.bodiala.provider.rezlive.client.web;

import com.miirphys.bodiala.provider.rezlive.client.HotelContentService;
import com.miirphys.bodiala.provider.rezlive.client.dto.content.HotelDetailsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Live per-hotel static content from RezLive's {@code gethoteldetails} action.
 * Requires configured credentials + a whitelisted IP. Error/transport mapping to HTTP status
 * is handled centrally by {@code ApiExceptionHandler}.
 */
@RestController
@Tag(name = "Hotel content (live)", description = "Live RezLive gethoteldetails — needs credentials + whitelisted IP")
public class HotelContentController {

    private final HotelContentService hotelContentService;

    public HotelContentController(HotelContentService hotelContentService) {
        this.hotelContentService = hotelContentService;
    }

    @Operation(summary = "Get live content for one hotel",
            description = "Alphanumeric RezLive hotel id, e.g. XHUB18. Returns 503 if credentials are unset.")
    @GetMapping("/api/hotel-content/{hotelId}")
    public HotelDetailsResponse hotelContent(@PathVariable String hotelId) {
        return hotelContentService.getHotelDetails(List.of(hotelId));
    }

    /** Batch variant: {@code /api/hotel-content?ids=XHUB18,XHUB19}. */
    @Operation(summary = "Get live content for multiple hotels")
    @GetMapping("/api/hotel-content")
    public HotelDetailsResponse hotelContent(@RequestParam List<String> ids) {
        return hotelContentService.getHotelDetails(ids);
    }
}
