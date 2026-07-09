# bodiala — project guide (read me first)

Hotelbeds (APItude) hotel integration. **Three sibling repos** under `C:\Users\user\IdeaProjects\`:

| Repo | What it is | Stack | Port |
|---|---|---|---|
| **bodiala** (this) | Backend: Hotelbeds integration API | Spring Boot 4.1 · Java 25 · Gradle 9.5.1 | 8080 |
| **hotelbeds-stub** (`../hotelbeds-stub`) | Fake Hotelbeds APItude API — no creds, avoids the real 50-req/day eval quota | Spring Boot 4.1 · Java 25 | 9091 |
| **bodiala-web** (`../bodiala-web`) | Frontend UI | React 18 · TypeScript · Vite 5 · Node 24 | 5173 |

> **RezLive was the original supplier and has been removed** (commit history has it). The provider
> abstraction (`ProviderId`, the `*Registry` beans, `?provider=`, the aggregated `CombinedSearchResult`)
> is kept so a second supplier can be re-added, but **Hotelbeds is currently the only implementation.**

Hotelbeds reference lives in `docs/hotelbeds/` — [api-contract.md](docs/hotelbeds/api-contract.md) (the build-ready contract) and [multi-provider-plan.md](docs/hotelbeds/multi-provider-plan.md) (design/history). Read on demand.

## How to run everything

```powershell
# 1) Backend + Postgres + fake Hotelbeds (one command; dev.ps1 also runs `docker compose up -d --wait`)
cd C:\Users\user\IdeaProjects\bodiala
.\dev.ps1                 # DEFAULT: postgres :5432 + hotelbeds-stub :9091 + bodiala :8080 (profile: hotelbeds-stub)
#                         #   -> cache auto-seeds from the stub on startup (no manual /import); waits for the stub first
#   .\dev.ps1 -StaticOnly # postgres :5432 + bodiala only, default profile (static works; live endpoints 503)

# 2) Frontend
cd C:\Users\user\IdeaProjects\bodiala-web
.\dev.cmd                 # or `npm run dev`  -> http://localhost:5173  (Vite proxies /api -> :8080)
```

- **Database:** PostgreSQL in Docker (`docker-compose.yml`, db/user/pass all `bodiala` on `:5432`) — the app's only datasource; schema auto-created via `ddl-auto=update`. `docker compose down -v` wipes it for a fresh start. **Start Docker before running the app or the tests.** Tests hit the same container in an isolated `bodiala_test` schema (create-drop), so they don't touch dev data. No H2 anywhere.
- **Swagger:** http://localhost:8080/swagger-ui.html · OpenAPI: `/v3/api-docs`
- **Static data auto-imports on startup (fresh DB):** the `hotelbeds-stub` profile sets `hotelbeds.import-on-startup=true`, so `.\dev.ps1` seeds the cache from the stub (multi-destination catalog — PMI/BCN/AYT/LON/LIS) with no manual step. Auto-import stays **off** for a *real* Hotelbeds run (`hotelbeds.import-on-startup` default false — protects the 50/day quota); call `POST /api/static-data/import?provider=hotelbeds` once there.
- Stop: close the windows, or kill the port (`Get-NetTCPConnection -LocalPort 8080 | %{ Stop-Process -Id $_.OwningProcess -Force }`); `docker compose down` stops Postgres.

## Profiles / credentials

- **default** — `hotel.provider=hotelbeds`, `hotelbeds.base-url` = the real test host, no creds → live endpoints (search/booking) return **503** by design (until creds are set in `.env`).
- **hotelbeds-stub** (`application-hotelbeds-stub.properties`) — **the default `.\dev.ps1` profile.** Sets `hotel.provider=hotelbeds`, hardcodes `hotelbeds.base-url` at `localhost:9091` (the stub — this overrides any `.env` `HOTELBEDS_BASE_URL`), uses placeholder creds, and `hotelbeds.import-on-startup=true` so the cache **auto-seeds from the stub on startup** (no manual `/import`; the stub serves a multi-destination catalog — PMI/BCN/AYT/LON/LIS). Exercises the whole search + booking chain locally without spending the real 50-req/day eval quota. Set `HOTELBEDS_API_KEY=invalid` to exercise the 502 path.
- **Go-live (real Hotelbeds)** — run WITHOUT the stub profile and supply `HOTELBEDS_API_KEY` + `HOTELBEDS_SECRET` (real key+secret) via a git-ignored project-root `.env` (copy `.env.example`; loaded by `DotEnvEnvironmentPostProcessor`) or real env vars. First live call is the signed `GET /status`; prod requires mTLS (`api-mtls.hotelbeds.com`). Mind the tight test-key quota.

## Build & test

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"   # exactly JDK 25 (26 won't satisfy the toolchain)
docker compose up -d       # tests need Postgres too (isolated bodiala_test schema)
.\gradlew.bat test         # bodiala  (same in ../hotelbeds-stub)
# frontend: cd ../bodiala-web ; npm run build   (tsc type-check + bundle)
```

Toolchain: **Temurin JDK 25** at the path above; **Node 24 LTS** at `C:\Program Files\nodejs` (machine PATH — open a fresh terminal). Gradle comes via the wrapper.

## bodiala architecture (`com.miirphys.bodiala`)

Provider-specific code lives under **`provider.<supplier>`** (currently only `provider.hotelbeds`); the API/web layer, the persisted booking, and the static-data cache are shared/neutral.

- **`config`** — `OpenApiConfig` · `DotEnvEnvironmentPostProcessor` (loads a git-ignored project-root `.env` into the environment at startup so `${HOTELBEDS_*}` resolve from it; real env vars still override; see `.env.example`).
- **`provider`** — the supplier abstraction + implementations. **Neutral:** `ProviderId` (currently just `HOTELBEDS`) · `SearchProvider`/`BookingProvider`/`CatalogImporter` (plain `@Component`s, no `@ConditionalOnProperty`). Each interface's impls are indexed by a `*Registry` (`SearchProviderRegistry`/`BookingProviderRegistry`/`CatalogImporterRegistry`); a request **picks the supplier per-call via the `?provider=` enum query param** (`hotelbeds`, case-insensitive via `ProviderIdConverter`), falling back to the default **`hotel.provider`** (default `hotelbeds`). Booking-keyed ops (cancel/stored read) skip the param and route to the supplier that created the row, resolved from `HotelBooking.provider` via `BookingProviderRegistry.forBooking(id)` (null/blank ⇒ `HOTELBEDS`). `search`/`prebook`/`cancel` return neutral `provider.model` records (frontend JSON preserved). **`hotelbeds/`** — the whole impl: `HotelbedsProperties` (`hotelbeds.*`), `HotelbedsSignature` (X-Signature = lowercase-hex SHA-256(apiKey+secret+epochSecs)), `HotelbedsJsonClient` (signed JSON transport, `Accept-Encoding` omitted, 5s/60s timeouts, `/status`), `HotelbedsSearchProvider` (availability `POST /hotel-api/1.0/hotels`), `HotelbedsBookingProvider` (prebook→`POST /checkrates`, book→`POST /bookings`, cancel→`DELETE /bookings/{ref}`, list/getStored via shared repo), `HotelbedsCatalogImporter` (pages `GET /hotel-content-api/1.0/hotels` → shared cache, bounded by `hotelbeds.catalog-max-hotels` default 100) + `HotelbedsStartupImporter` (seeds the cache on startup when `hotelbeds.import-on-startup`), `HotelbedsContentService` (on-demand single-hotel content for the detail view — `GET /hotels?codes=…`, lazily cached), `HotelbedsFacilityCatalog` (caches `/types/facilities`; resolves facility codes→names for amenities), `dto/**` (JSON response records), `HotelbedsSearch/Booking/ContentMapper`. **`provider.model`** = neutral result records (`SearchResult`/`RateCheckResult`/`CancellationResult`, JSON matching the frontend) + **`CombinedSearchResult`** (the aggregated, provider-tagged search response: flat `hotels[]` each carrying `provider`+`searchSessionId`+`currency`, plus `providerStatus[]`). **`error/`** = neutral `Upstream{Api,Transport}Exception` (Hotelbeds client throws them). Contract: `docs/hotelbeds/api-contract.md`.
- **`staticdata`** — the shared static-data **cache** both providers fill: `domain/*` JPA entities (Country/City/Hotel/HotelImage/PropertyAmenity/RoomAmenity) · `repo/*` · `ImportResult` · `web/StaticDataController` (`/api/static-data/**`; `POST /import?provider=` delegates to the chosen `CatalogImporter` via `CatalogImporterRegistry`) + `web/HotelView`.
- **`search`** — shared search API: request records (`DestinationSearchRequest`, `HotelIdsSearchRequest`, `RoomRequest`) · `SearchAggregationService` (fans out to configured suppliers, flattens + tags into `CombinedSearchResult`, partial-failure tolerant) · `web/HotelSearchController` (`/api/search/by-destination`, `/by-hotel-ids`; both take optional `?provider=`, return `CombinedSearchResult`).
- **`booking`** — shared booking API + storage: request records · `HotelBooking` entity (+ `provider` column) + repo · `web/BookingController` (`/api/bookings/**`: prebook/book take optional `?provider=`; get/cancel route by the stored `provider` column) + `BookingView`.
- **`web.ApiExceptionHandler`** — global `@RestControllerAdvice`: IllegalArgument→400, IllegalState→**503** (creds not set), NoSuchElement→404, DataIntegrityViolation→409, UnsupportedOperation→**501**, `UpstreamApiException`/RestClient/`UpstreamTransportException`→**502**.

## How the Hotelbeds API works (the mental model)

- **Content = a live Content API** (paged `GET /hotel-content-api/1.0/hotels`) synced into the cache via `POST /api/static-data/import?provider=hotelbeds` (bounded by `hotelbeds.catalog-max-hotels` for the quota). Cache `cityCode` = Hotelbeds **destinationCode** (e.g. `PMI`), so destination search lines up. Facility **names** come from a separate `/types/facilities` master (a hotel's facilities are code-only) — `HotelbedsFacilityCatalog` caches it. Availability returns hotels beyond the bounded import, so the detail view fetches uncached hotels on demand (`HotelbedsContentService`, `?codes=`).
- **Flow:** availability (`POST /hotel-api/1.0/hotels`) → **stateless** (no session; a `rateKey` per offer) → **checkrates** (mandatory only for `rateType=RECHECK`; we always call it) → **bookings** → a single `reference`. The neutral `bookingKey` carries `rateKey#rateType`; the booking provider strips the `#suffix` before the wire.
- **Transport:** JSON REST; per-request headers `Api-key` + `X-Signature` = lowercase-hex SHA-256(`apiKey+secret+unixSeconds`). Test host `https://api.test.hotelbeds.com`; free test key quota is tight (8 req/4s, 50/day) — hence the fixture-first tests + bounded catalog sync + the local stub. Full contract: `docs/hotelbeds/api-contract.md`.

## Gotchas / conventions (bitten by these — keep them in mind)

- **The API/web layer is provider-neutral** — search/prebook/cancel return `provider.model` records (+ the aggregated `CombinedSearchResult`) that serialize to the exact JSON the frontend reads. Keep it that way so a second supplier can slot in without a frontend change.
- **Provider selection is per-request.** The adapters are plain `@Component`s (no `@ConditionalOnProperty`), indexed by a `*Registry`; a request selects one via the `?provider=` **enum** query param (case-insensitive via `ProviderIdConverter`), and `hotel.provider` (default `hotelbeds`) sets the default when omitted. Booking-keyed ops route by the stored `HotelBooking.provider` column (null/blank ⇒ `HOTELBEDS`). Tests: `ProviderWiringTest` (hotelbeds loaded + default) + `ProviderSelectionApiTest` (end-to-end `?provider=` binding). *(With one supplier, `resolve(HOTELBEDS)` and the default coincide; the registry/aggregation machinery is retained for a future second provider.)*
- **Hotelbeds `X-Signature` uses the host clock (unix seconds)** — clock skew → 401. Regenerate per request. `Accept-Encoding` is omitted so responses come back uncompressed (no manual gzip). Two error shapes: flat `{"error":"…"}` (gateway 401/403) vs nested `{"error":{code,message}}` (app) — both → `UpstreamApiException` → 502.

- **Response DTOs use `hasError()`, not `isError()`** — `is`+`get` on `error` collide as Jackson JSON properties.
- **`book()`/`cancel()` are intentionally NOT `@Transactional`** — the supplier HTTP call must not run inside a DB tx (pins a connection; risks rolling back an already-charged booking). Persistence happens in its own repo tx after the call.
- **Idempotency** is best-effort via a unique `agent_ref_no` + pre-call lookup (retry-safe, not concurrency-safe). Full reservation-before-call + orphan reconciliation are deferred (go-live).
- **`bookingCode` is a legacy vestige** — Hotelbeds returns a single `reference` (stored as `bookingId`); `bookingCode` stays null. Kept on the entity/`BookingView` to avoid a DB+frontend change; drop it if you never re-add a two-identifier supplier.
- **Spring Boot 4 moved test-slice annotation packages** (`@WebMvcTest` etc.) — use plain unit tests or full `@SpringBootTest`. Boot 4 also uses **Jackson 3** and moved **`EnvironmentPostProcessor`** `org.springframework.boot.env` → `org.springframework.boot` (old one deprecated, removal in 4.2); register it in `META-INF/spring.factories` under the **new** FQN, else it silently won't load.
- **Windows PowerShell blocks `npm run dev`** (execution policy on `npm.ps1`). Use `.\dev.cmd` or `npm.cmd`. Only the user's own shell can fix the policy (`Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`); the agent's shell is a different context.
- **Preview screenshots are flaky in this environment** — verify UI via `preview_eval`/`preview_snapshot` (DOM) when screenshots time out.

## Status

- **Hotelbeds — done & verified.** Against the stub: signed client + `/status`, aggregated availability search, checkrates/prebook→book→cancel, the Content-API catalog sync + startup auto-seed, on-demand per-hotel content for the detail view, and facility-code→name amenity resolution. Also verified against the **real** test API: catalog import, availability search returning live hotels, detail fetch, and amenities. Frontend (`bodiala-web`) drives search→detail→book→bookings→cancel with the `CombinedSearchResult` shape. `gradlew test` green in bodiala + hotelbeds-stub.
- **Aggregated search is built** — `SearchAggregationService` fans out to every configured supplier and returns one flat, provider-tagged `CombinedSearchResult` (duplicates shown twice; dedup is a later "merge" step). With one supplier it's a passthrough.
- **Go-live (real Hotelbeds):** real key+secret in `.env`; first live call is the signed `GET /status`; prod requires mTLS (`api-mtls.hotelbeds.com`). Confirm the UNVERIFIED fields in `docs/hotelbeds/api-contract.md` §9 on the first live calls.
- **Deferred / follow-ups (documented, not built):** dedup the aggregated results (same hotel across suppliers); refine Hotelbeds per-status error handling (401/403/429/410) + backoff; concurrent-safe idempotency (reservation-before-call) + orphan reconciliation; static-data hardening (streaming import, N+1); pagination for `/api/static-data/cities`. **RezLive removed** — restore from git history if a second supplier is wanted; the provider abstraction is retained for that.
