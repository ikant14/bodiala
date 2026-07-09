# bodiala multi-provider design — RezLive + Hotelbeds

> **SUPERSEDED (2026-07-09): the `@ConditionalOnProperty` "one active supplier per run" model
> described below has been replaced.** Both suppliers' beans now load together; a request selects
> one per call via the `?provider=` enum query param through per-interface `*Registry` beans
> (`SearchProviderRegistry`/`BookingProviderRegistry`/`CatalogImporterRegistry`), with
> `hotel.provider` acting only as the default. Booking-keyed ops route by the stored
> `HotelBooking.provider` column (null/blank ⇒ RezLive). See CLAUDE.md → "How the provider switch
> works" + the Gotchas note. The sections below are kept for historical design rationale only.
>
> **STATUS (2026-07-09): Phases 0–2 IMPLEMENTED & verified.** New `com.miirphys.bodiala.provider`
> package: `ProviderId`, `SearchProvider`/`BookingProvider` interfaces, `rezlive.RezLive{Search,Booking}Provider`
> adapters, `hotel.provider` switch (default `rezlive`, `@ConditionalOnProperty`); controllers inject the
> interfaces; `HotelBooking` gained a nullable `provider` column (defaults `REZLIVE`) surfaced in `BookingView`.
> Verified: full suite green (incl. `ProviderWiringTest`) + live search→prebook→book→cancel through the seam
> against the stub, JSON shape byte-identical, booking row stamped `REZLIVE`.
> **Deviation from the plan below:** response-body neutralisation was **deferred** out of Phase 1 — the
> interfaces still return the RezLive DTOs. Rationale: the neutral shapes should be co-designed against
> Hotelbeds' real JSON (Phases 4–5); doing it now, RezLive-only, is pure risk (frontend contract) with no
> benefit and would bake in RezLive-isms.
> **Package reorg (later request):** all RezLive code was subsequently moved under
> `com.miirphys.bodiala.provider.rezlive.*` (client/transport/DTOs, services, `RezLiveProperties`,
> CSV ingestion under `staticimport/`), overriding §5's "no package moves" line. Shared API/web,
> `HotelBooking`, and the `staticdata` cache stayed neutral. Verified behaviour-preserving.
> Phases 3–7 (Hotelbeds) are pending real creds in `.env`.

## Original design (proposed)

> Generated 2026-07-09 by a multi-agent research + design pass (`hotelbeds-provider-plan`
> workflow: 5 Hotelbeds-API research agents + 4 bodiala code-mapping agents → design →
> 3 adversarial reviewers → synthesis). **No code has been written.** Some Hotelbeds facts
> are marked UNVERIFIED and must be confirmed against live docs/the test API before coding.
> Hotelbeds availability/booking/cancel are HIGH-confidence (sourced from the official
> APItude OpenAPI 3.0 spec).

## Summary
Add Hotelbeds APItude behind three segregated capability interfaces (`SearchProvider` /
`BookingProvider` / `ContentProvider`), wrapping the existing RezLive services as thin adapters
("wrap, don't rewrite"). The reviews converge on **cutting the facade/composite/registry tower**
(single active provider per deployment, one `@ConditionalOnProperty` bean per interface),
**making the schema change additive-only** (nullable `provider` column, no rename, no composite
unique, no Flyway yet), and **introducing neutral response records only where a JAXB DTO currently
leaks to a controller return type**. Two semantic holes must be closed in the model itself before
Hotelbeds can book: `rateType` and `holder` have nowhere to live in the reused records. Hotelbeds
transport needs a custom `ResponseErrorHandler` + gzip handling as prerequisites, not footnotes.

---

## 1. Neutral interfaces (final signatures)

Three capability interfaces + a stable id. **No `HotelProvider` facade, no `CompositeHotelProvider`,
no `activeProvider` chooser bean** (cut per reviews — over-engineered for single-active). Controllers
inject the capability interface directly.

```java
package com.miirphys.bodiala.provider;

public enum ProviderId { REZLIVE, HOTELBEDS }
```

```java
public interface SearchProvider {
    ProviderId id();
    boolean isConfigured();
    SearchResult searchByDestination(DestinationSearchRequest request);
    SearchResult searchByHotelIds(HotelIdsSearchRequest request);
}
```

```java
public interface BookingProvider {
    ProviderId id();
    boolean isConfigured();

    RateCheckResult checkRates(PrebookRequest request);           // RezLive prebook ≡ HB checkrates
    HotelBooking   book(BookRequest request);                     // NOT @Transactional (both impls)
    CancellationResult cancel(String bookingRef);
    BookingDetail  getBooking(String bookingRef);
    CancellationPolicy cancellationPolicyForOffer(CancellationPolicyLookupRequest request);
    CancellationPolicy cancellationPolicyForBooking(String bookingRef);
    default Optional<ConfirmationDetail> confirmation(String bookingRef) { return Optional.empty(); }
}
```

```java
public interface ContentProvider {
    ProviderId id();
    boolean isConfigured();
    ImportResult  refreshCatalog(CatalogRefreshRequest request);  // RezLive CSV dir | HB paged /hotels
    HotelContent  getHotelContent(List<String> hotelIds);         // RezLive gethoteldetails | HB /hotels/{codes}/details
}
```

`listBookings()`/`getStored()` stay **off the interface** — they read the shared `HotelBooking`
table via a provider-neutral repo used by the controller directly.

**Model changes forced by the reviews (correctness, not gold-plating):**
- `PrebookRequest` / `NeutralRoomOffer` gain the offer key such that **`rateType` round-trips**. The
  HB search mapper suffixes `rateType` onto the neutral `bookingKey` (`<rateKey>#BOOKABLE|RECHECK`)
  and the HB `checkRates`/`book` impls strip it before the wire call. This makes "skip checkrates when
  BOOKABLE" implementable in the stateless pass-through flow. (RezLive ignores the suffix.)
- `BookRequest` gains an optional `Holder(name, surname)` **or** the HB adapter synthesizes holder
  from the first non-child guest of the first room — decision required (Open Q1). Without it every HB
  booking fails schema validation.

---

## 2. Concept-mapping table

| Capability | RezLive | Neutral | Hotelbeds APItude |
|---|---|---|---|
| Catalog/masters | CSV file-drop | `ContentProvider.refreshCatalog` → shared cache | `GET /hotel-content-api/1.0/hotels` (paged `from/to` ≤1000) |
| Geography | COUNTRY/CITY CSVs | `Country`/`City` cache | `/locations/countries`, `/locations/destinations` |
| Per-hotel detail | `gethoteldetails` (alphanumeric `HotelId`) | `getHotelContent` | `GET /hotels/{codes}/details` (numeric `code`) |
| Refresh model | full snapshot | `CatalogRefreshRequest` | `lastUpdateTime` incremental / paging |
| Availability | `findhotel` / `findhotelbyid` | `searchBy{Destination,HotelIds}` | `POST /hotel-api/1.0/hotels` |
| Search session | `SearchSessionId` | `SearchResult.sessionToken` | none — stateless (`""`) |
| Per-offer token | `BookingKey` | `NeutralRoomOffer.bookingKey` | `rateKey` (+ our `#rateType` suffix) |
| Rate re-check | `prebook` (always) | `checkRates` | `POST /checkrates` (only when `rateType==RECHECK`) |
| Book | `bookhotel` | `book` | `POST /bookings` (`clientReference`, `holder`, `rooms[].rateKey`, `tolerance`) |
| Idempotency ref | `agentRefNo` | `BookRequest.agentRefNo` | `clientReference` (≤20 chars, not server-dedup'd) |
| Booking id | `BookingId`+`BookingCode` | `bookingRef` (+ nullable `bookingCode`) | single `reference` (`XXX-YYYYYY`) |
| Get booking | `getbookingdetails` | `getBooking` | `GET /bookings/{reference}` |
| Cancel | `cancelhotel` | `cancel` | `DELETE /bookings/{ref}?cancellationFlag=CANCELLATION` |
| Cancel dry-run | none | (Open Q — not on interface) | `cancellationFlag=SIMULATION` |
| Pre-book policy | `getcancellationpolicy` | `cancellationPolicyForOffer` | embedded `cancellationPolicies[]` in avail/checkrates |
| Post-book policy | `getCancellationPolicyAfterBooking` | `cancellationPolicyForBooking` | policy in `GET /bookings/{ref}` |
| Supplier confirm | `getConfirmationDetails` | `confirmation` (Optional) | none — instant confirm |
| Refundable | derived | `CancellationPolicy.refundable` | `rateClass` NOR vs NRF |
| Customer price | rate | `totalRate` = **`sellingRate`** (never `net`) | `rate.sellingRate` (`hotelSellingRate` only for AT_HOTEL) |
| Auth | `x-api-key` + in-body `<Authentication>` | provider-internal | `Api-key` + `X-Signature` = SHA-256hex(`apiKey+secret+epochSec`), lowercase, per-call |
| Wire dates | `dd/MM/yyyy` | ISO in requests; string echo in responses | `yyyy-MM-dd` (HB mapper reformats to `dd/MM/yyyy` for frontend) |

---

## 3. Provider selection & config

**Selector:** `hotel.provider=rezlive|hotelbeds` (default `rezlive`, `matchIfMissing=true`). One
conditional bean per interface — no chooser bean, no ambiguous `List<HotelProvider>`:

```java
@Bean @ConditionalOnProperty(name="hotel.provider", havingValue="rezlive", matchIfMissing=true)
SearchProvider rezSearch(HotelSearchService s){ return new RezLiveSearchProvider(s); }
@Bean @ConditionalOnProperty(name="hotel.provider", havingValue="hotelbeds")
SearchProvider hbSearch(HotelbedsSearchProvider s){ return s; }
// …same pattern for BookingProvider, ContentProvider
```

Exactly one bean per interface at runtime → controllers inject the interface with no
`@Primary`/`@Qualifier`. `ProviderRegistry`/cross-provider routing is **deferred to Phase 5** (there
are zero Hotelbeds rows to route until the first HB `book()` lands).

```java
@ConfigurationProperties(prefix = "hotelbeds")
public record HotelbedsProperties(
        String baseUrl, String apiKey, String secret,
        String language, String sourceMarket) {
    public boolean hasCredentials() {
        return isNotBlank(apiKey) && isNotBlank(secret) && isNotBlank(baseUrl);
    }
}
```

**.env keys** (loaded by existing `DotEnvEnvironmentPostProcessor`):
```
HOTEL_PROVIDER=hotelbeds
HOTELBEDS_BASE_URL=https://api.test.hotelbeds.com
HOTELBEDS_API_KEY=…
HOTELBEDS_SECRET=…
HOTELBEDS_LANGUAGE=ENG
HOTELBEDS_SOURCE_MARKET=ES
```
`IllegalState → 503` stays the "creds not configured" gate. Existing `rezlive.*` namespace unchanged.

---

## 4. Shared vs provider-specific + DB impact

**Shared:** all web controllers (routing unchanged; only injected type + return type move to neutral
where a JAXB DTO leaks), `BookingView`/`HotelView`/`ImportResult` projections, the static-data cache
(Country/City/Hotel/images/amenities + repos — both providers fill the same tables), `HotelBooking`
entity, `ApiExceptionHandler`.

**Provider-specific:** transport/auth (`RezLiveXmlClient`/`Codec` vs new `HotelbedsJsonClient`), wire
DTOs (`client.dto.**` JAXB stay RezLive-only; HB gets `provider.hotelbeds.dto` JSON), catalog
acquisition (`MasterFile`/`CsvMasterFileReader` → RezLive-only; HB `HotelbedsCatalogClient` paginated
HTTP), response mappers (fixture-tested, HTTP-free).

**DB impact — additive only** (the sharpest review finding; the original rename plan corrupts data
under `ddl-auto=update`):
- Add `provider` as **nullable**, app-default `"REZLIVE"`; one-time `UPDATE hotel_booking SET
  provider='REZLIVE' WHERE provider IS NULL`.
- **Do NOT rename** `rez_booking_id`/`rez_booking_code` (Hibernate never renames — it would add
  `booking_ref` and leave the old `NOT NULL UNIQUE` column orphaned, breaking every INSERT).
- **Keep** the single-column unique on `rez_booking_id` for now.
- Add repo finders `findByProviderAndBookingRef` / `findByProviderAndAgentRefNo`; keep old finders
  delegating with `REZLIVE`.
- **Deferred to Phase 5**: column renames, composite uniques `(provider, booking_ref)` /
  `(provider, agent_ref_no)`, introducing Flyway.

**Status normalization:** HB returns `CONFIRMED`/`CANCELLED`/`PRECONFIRMED`; stored convention is
`Confirmed`/`Cancelled`. Normalize to one canonical vocabulary before persisting/returning.

---

## 5. Refactor blast-radius (concrete files)

- `HotelSearchController` — inject `SearchProvider`; return `HotelFindResponse` → `SearchResult`.
  `@SpringBootTest`/MockMvc tests that deserialize into the concrete type break — budget for these.
- `BookingController` — inject `BookingProvider`; booking read DTOs → neutral records **only where
  JAXB leaks** (search + `book`/`prebook`). Defer neutralizing `GetBookingResponse`, confirmation,
  both policies until HB needs them.
- `web/ApiExceptionHandler.java` — hardcoded `"RezLive error: "` / `"RezLive request failed: "` /
  `"RezLive response could not be processed: "` prefixes. Repoint `@ExceptionHandler` to
  `UpstreamApiException`/`UpstreamTransportException`; **keep the message strings verbatim** or every
  `ProblemDetail.detail` assertion breaks.
- `booking/HotelBooking.java` — add nullable `provider` only (see §4).
- `client/RezLiveApiException`, `RezLiveTransportException` — become subclasses of new
  `provider.error.UpstreamApiException` / `UpstreamTransportException`.
- Untouched (delegation keeps their tests green): `HotelSearchService`, `BookingService`
  (non-`@Transactional` book/cancel preserved), `RezLiveXmlClient`/`Codec`, `client.dto.**`,
  `StaticDataImportService`, `HotelContentService`.
- New adapters: `RezLiveSearchProvider`, `RezLiveBookingProvider`, `RezLiveContentProvider`. **No
  package moves in Phase 1.**

**Hotelbeds transport prerequisites (not optional):**
- **Custom `ResponseErrorHandler`** on the HB `RestClient` — otherwise every 4xx collapses to 502 and
  the distinctions are lost: 401 sig/clock → transport; 403 quota → api; 429 throttle → backoff;
  **410 Gone → success `CancellationResult` (already cancelled)**.
- **Two error-body shapes:** gateway/auth = flat `{"error":"…"}`; application = `{"error":{code,
  message}, auditData}`. Parser must handle both.
- **gzip:** either **omit `Accept-Encoding`** (JDK client negotiates + decompresses) or add a
  decompressing interceptor. Setting the header manually disables transparent decompression.
- **`clientReference`:** truncate to ≤20 **once**, and **store exactly what you send** — reuse it for
  both `findByProviderAndAgentRefNo` and post-timeout reconcile (`GET /bookings?clientReference=…`).
- **Price:** customer-visible `totalRate` = `sellingRate` (never `net` = your margin).

---

## 6. Phased plan (each independently testable)

- **Phase 0 — Interfaces + neutral models.** `provider.*` interfaces, `provider.model` records
  (rateType-carrying key + optional holder), `Upstream*` exception supertypes. *Test:* compile; suite green.
- **Phase 1 — RezLive adapters + controller cutover.** Wrap services; switch controllers to neutral
  types where JAXB leaks; `@JsonProperty` to pin frontend field names. *Test:* full suite vs stub;
  JSON-shape lock test; enumerate `BookingDetail`+`HotelContent` fields vs actual `bodiala-web` usage;
  run frontend search→book→cancel vs stub.
- **Phase 2 — Additive `provider` column + backfill.** Nullable column, app-default, one `UPDATE`.
  No rename, no composite unique, no Flyway. *Test:* booking round-trip; existing rows → `REZLIVE`.
- **Phase 3 — HB config + signed client + `/status` smoke.** `HotelbedsProperties`,
  `HotelbedsJsonClient`, `signedHeaders()`, custom `ResponseErrorHandler`, gzip decision. *Test:*
  unit-test signature against doc canonical vector; **one live `GET /hotel-api/1.0/status`** with the
  TEST key (cheapest real auth+clock check).
- **Phase 4 — HB search + rate-check.** Availability mapping, destination→hotel-codes resolution via
  cache (≤2000-code chunking) or `geolocation` fallback, `rateType` suffix, ISO→`dd/MM/yyyy`. *Test:*
  record 1–2 live avail responses as fixtures, unit-test mapper offline; one live avail + one checkrates.
- **Phase 5 — HB book/cancel/get/policy + cross-provider routing.** Holder synthesis,
  `clientReference` store-what-you-send + pre-call lookup, ≥60s read timeout, no blind retry
  (reconcile via `clientReference`), 410/403/429 handling, status normalization. **Now** add
  `ProviderRegistry` + Flyway migration for composite uniques/renames. *Test:* one live
  book→getBooking→cancel(SIMULATION then real) on TEST; fixture mapper tests; RezLive cancel routes by
  stored `provider`.
- **Phase 6 — HB Content catalog sync.** Paged `/hotels` + `/types` + `/locations` filling the shared
  cache; `lastUpdateTime` incremental; scheduled sync (replaces startup one-shot; **cold-start
  prerequisite for HB destination search**). *Test:* fixture mapper tests; small live `from=1&to=5`.
- **Phase 7 — Flip default + docs.** `hotel.provider=hotelbeds`, frontend end-to-end vs TEST, update
  `CLAUDE.md`/`docs`.

Mapper logic is HTTP-free/fixture-tested → the **50 req/day** sandbox quota is spent only on ~6 true
smoke calls, never in CI.

---

## 7. Risks & OPEN QUESTIONS (owner must decide)

**Decisions required:**
1. **Holder source** — add `Holder` to `BookRequest`, or synthesize from first non-child guest?
   (Blocks every HB booking.)
2. **`rateType` carriage** — accept the `#BOOKABLE|RECHECK` suffix on the neutral `bookingKey`, or
   just **always call `/checkrates`** (simpler/safe, but doubles booking-path calls under the quota)?
3. **`AT_WEB` card-required rates** — HB `book` needs `paymentData.paymentCard`; current records have
   no card fields. Add optional `PaymentCard` now, or filter out `AT_WEB` rates until needed?
4. **`confirmation()` empty contract** — controller emits 200-empty or 404 when `Optional.empty()`?
5. **Cold-start HB search** — destination search is dead until first catalog sync populates the cache.
   Gate search behind a sync, or ship `geolocation` fallback?
6. **Destination-code repopulation** — flipping `hotel.provider` against a RezLive-populated geography
   cache sends numeric RezLive codes where HB expects alphanumeric (`PMI`/`DXB`). Re-sync before flip.
7. **429 backoff** — exponential backoff in the client, or surface 429/503 with `Retry-After`?
8. **Account default currency** (EUR/USD/GBP) — affects price display.

**UNVERIFIED Hotelbeds facts — confirm against live docs/API before/at coding:**
- Prod host `api.hotelbeds.com` (only TEST host printed in docs).
- Uppercase-hex signature rejection; presence of `Retry-After`/rate-limit headers on 429.
- Exact `error.{code,message}` + `auditData` co-occurrence and the flat-vs-nested body split.
- `/hotels/{codes}/details` sub-path vs `/hotels?codes=`; `/types/` & `/locations/` prefixes on v1.0.
- Image host `photos.hotelbeds.com/giata/{path}` + size prefixes.
- `paxes` nesting (docs show it in `ApiOccupancy`; 3.0 schema omits it).
- `/status` response body shape; a live NRF token example.
- ≤2000 availability code cap and whether `geolocation` is an acceptable large-destination fallback.

**Operational:** 50 req/day sandbox cap (403 on exceed → fixture-first mandatory); host clock drift →
401 signature failures; Content API must not be polled real-time (credential-block risk — cache +
`lastUpdateTime` only).

**Recommendation:** ship Phases 0–2 first (pure refactor + additive nullable column — RezLive stays
fully working, near-zero risk), then gate Hotelbeds behind `hotel.provider=hotelbeds` and validate
incrementally against the TEST key, spending quota only on the ~6 isolated smoke calls.
