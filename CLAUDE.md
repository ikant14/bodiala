# bodiala — project guide (read me first)

RezLive.com hotel integration (Hotelbeds APItude is the second supplier). **Four sibling repos** under `C:\Users\user\IdeaProjects\`:

| Repo | What it is | Stack | Port |
|---|---|---|---|
| **bodiala** (this) | Backend: RezLive + Hotelbeds integration API | Spring Boot 4.1 · Java 25 · Gradle 9.5.1 | 8080 |
| **rezlive-stub** (`../rezlive-stub`) | Fake RezLive API for local testing (no creds) | Spring Boot 4.1 · Java 25 | 9090 |
| **hotelbeds-stub** (`../hotelbeds-stub`) | Fake Hotelbeds APItude API — no creds, avoids the real 50-req/day eval quota | Spring Boot 4.1 · Java 25 | 9091 |
| **bodiala-web** (`../bodiala-web`) | Frontend UI | React 18 · TypeScript · Vite 5 · Node 24 | 5173 |

Deeper reference lives in `docs/rezlive/` — [api-reference.md](docs/rezlive/api-reference.md) (search/content/static contract), [booking-flow.md](docs/rezlive/booking-flow.md) (prebook→book→cancel), [README.md](docs/rezlive/README.md) (this module's usage). Read those on demand; don't inline them here.

## How to run everything

```powershell
# 1) Backend + Postgres + fake Hotelbeds (one command; dev.ps1 also runs `docker compose up -d --wait`)
cd C:\Users\user\IdeaProjects\bodiala
.\dev.ps1                 # DEFAULT: postgres :5432 + hotelbeds-stub :9091 + bodiala :8080 (profile: hotelbeds-stub)
#                         #   -> cache auto-seeds from the stub on startup (no manual /import); waits for the stub first
#   .\dev.ps1 -RezLive    # postgres :5432 + rezlive-stub :9090 + bodiala :8080 (profile: stub) — the kept RezLive path
#   .\dev.ps1 -StaticOnly # postgres :5432 + bodiala only, default profile (static works; live endpoints 503)

# 2) Frontend
cd C:\Users\user\IdeaProjects\bodiala-web
.\dev.cmd                 # or `npm run dev`  -> http://localhost:5173  (Vite proxies /api -> :8080)
```

- **Database:** PostgreSQL in Docker (`docker-compose.yml`, db/user/pass all `bodiala` on `:5432`) — the app's only datasource; schema auto-created via `ddl-auto=update`. `docker compose down -v` wipes it for a fresh start. **Start Docker before running the app or the tests.** Tests hit the same container in an isolated `bodiala_test` schema (create-drop), so they don't touch dev data. No H2 anywhere.
- **Swagger:** http://localhost:8080/swagger-ui.html · OpenAPI: `/v3/api-docs`
- **Static data auto-imports on startup (fresh DB):** under **Hotelbeds** the `hotelbeds-stub` profile sets `hotelbeds.import-on-startup=true`, so `.\dev.ps1` seeds the cache from the stub (multi-destination catalog) with no manual step. Auto-import stays **off** for a *real* Hotelbeds run (`hotelbeds.import-on-startup` default false — protects the 50/day quota); call `POST /api/static-data/import?provider=hotelbeds` once there. Under **RezLive** (`-RezLive`) the CSV master files in `data/rezlive/` auto-import (`rezlive.import-on-startup`).
- Stop: close the windows, or kill the port (`Get-NetTCPConnection -LocalPort 8080 | %{ Stop-Process -Id $_.OwningProcess -Force }`); `docker compose down` stops Postgres.

## Profiles / credentials

- **default** — no RezLive creds; live endpoints (content/search/booking) return **503** by design.
- **stub** (`application-stub.properties`) — points `rezlive.base-url` at `localhost:9090` with placeholder creds, so everything works locally against the stub.
- **hotelbeds-stub** (`application-hotelbeds-stub.properties`) — **the default `.\dev.ps1` profile.** Sets `hotel.provider=hotelbeds`, points `hotelbeds.base-url` at `localhost:9091` (the hotelbeds-stub) with placeholder creds, and `hotelbeds.import-on-startup=true` so the cache **auto-seeds from the stub on startup** (no manual `/import`; the stub serves a multi-destination catalog — PMI/BCN/AYT/LON/LIS). Lets you exercise the whole Hotelbeds search + booking chain locally without spending the real 50-req/day eval quota. Set `HOTELBEDS_API_KEY=invalid` to exercise the 502 path.
- **Go-live** — run WITHOUT the stub profile and supply `REZLIVE_BASE_URL`, `REZLIVE_AGENT_CODE`, `REZLIVE_USER_NAME`, `REZLIVE_API_KEY` (real sandbox/prod values) — either as real env vars or via a git-ignored project-root `.env` file (copy `.env.example`; loaded by `DotEnvEnvironmentPostProcessor`). All three of agent-code/user-name/api-key must be set or the live endpoints 503. Requires IP whitelisting on RezLive's side.

## Build & test

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"   # exactly JDK 25 (26 won't satisfy the toolchain)
docker compose up -d       # tests need Postgres too (isolated bodiala_test schema)
.\gradlew.bat test         # bodiala  (same in ../rezlive-stub)
# frontend: cd ../bodiala-web ; npm run build   (tsc type-check + bundle)
```

Toolchain: **Temurin JDK 25** at the path above; **Node 24 LTS** at `C:\Program Files\nodejs` (machine PATH — open a fresh terminal). Gradle comes via the wrapper.

## bodiala architecture (`com.miirphys.bodiala`)

Provider-specific code lives under **`provider.<supplier>`**; the API/web layer, the persisted booking, and the static-data cache are shared/neutral.

- **`config`** — `OpenApiConfig` · `DotEnvEnvironmentPostProcessor` (loads a git-ignored project-root `.env` into the environment at startup so `${REZLIVE_*}`/`${HOTELBEDS_*}` resolve from it; real env vars still override; see `.env.example`). (`RezLiveProperties` now lives under `provider.rezlive`.)
- **`provider`** — the supplier abstraction + per-supplier implementations (see `docs/hotelbeds/multi-provider-plan.md`). **Neutral:** `ProviderId` (REZLIVE|HOTELBEDS) · `SearchProvider`/`BookingProvider`/`CatalogImporter` — **both suppliers are loaded together now** (no more `@ConditionalOnProperty`). Each interface's impls are indexed by a `*Registry` (`SearchProviderRegistry`/`BookingProviderRegistry`/`CatalogImporterRegistry`); a request **picks the supplier per-call via the `?provider=` query param** (`rezlive`|`hotelbeds`, case-insensitive via `ProviderIdConverter`), falling back to the default **`hotel.provider`** (default `rezlive`). Booking-keyed ops (cancel/details/confirmation/post-booking-policy/stored read) skip the param and route to the supplier that created the row, resolved from `HotelBooking.provider` via `BookingProviderRegistry.forBooking(id)`. `search`/`prebook`/`cancel` return neutral `provider.model` records (frontend JSON preserved); the 4 secondary booking reads still return RezLive DTOs (Hotelbeds → **501**). **`rezlive/`** = the whole RezLive impl: `RezLive{Search,Booking}Provider` adapters · `RezLiveProperties` (`rezlive.*`) · `HotelSearchService` (findhotel/findhotelbyid; ISO→`dd/MM/yyyy`) · `BookingService` (prebook→book→cancel, details, policies, confirmation) · `client/**` (XML transport `RezLiveXmlClient`/`RezLiveXmlCodec` + exceptions `RezLiveApiException`/`RezLiveTransportException` + JAXB `dto/**` grouped `common`/`content`/`search`/`booking/{book,cancellation,confirmation}` + `HotelContentService` + `web/HotelContentController` `/api/hotel-content/**`) · `staticimport/**` (CSV ingestion: `MasterFile`, `CsvMasterFileReader`, `StaticDataImportService`, `StaticDataStartupImporter`). **`hotelbeds/`** = **Phases 3–6 done, verified end-to-end vs `hotelbeds-stub`**: `HotelbedsProperties` (`hotelbeds.*`), `HotelbedsSignature` (X-Signature = lowercase-hex SHA-256(apiKey+secret+epochSecs)), `HotelbedsJsonClient` (signed JSON transport, `Accept-Encoding` omitted, 5s/60s timeouts, `/status`), `HotelbedsSearchProvider` (availability `POST /hotel-api/1.0/hotels` → `SearchResult`), `HotelbedsBookingProvider` (prebook→`POST /checkrates`, book→`POST /bookings`, cancel→`DELETE /bookings/{ref}`, list/getStored via shared repo; 4 secondary reads → **501**), `HotelbedsCatalogImporter` (pages `GET /hotel-content-api/1.0/hotels` → shared cache, bounded by `hotelbeds.catalog-max-hotels` default 100 for the quota), `dto/**` (JSON response records), `HotelbedsSearch/Booking/ContentMapper`. **`hotel.provider` now only sets the *default* supplier for requests that omit `?provider=` — both stacks are always in the context, so you can hit either supplier in the same run (creds permitting); live Hotelbeds calls need creds in `.env` (real key+secret set there).** **`provider.model`** = neutral result records (`SearchResult`/`RateCheckResult`/`CancellationResult`, JSON matching the frontend; both providers map into them — search + prebook + cancel neutralised). **`error/`** = neutral `Upstream{Api,Transport}Exception` (RezLive exceptions extend them). Contract: `docs/hotelbeds/api-contract.md`.
- **`staticdata`** — the shared static-data **cache** both providers fill: `domain/*` JPA entities (Country/City/Hotel/HotelImage/PropertyAmenity/RoomAmenity) · `repo/*` · `ImportResult` · `web/StaticDataController` (`/api/static-data/**`; `POST /import?provider=` delegates to the chosen `CatalogImporter` via `CatalogImporterRegistry`) + `web/HotelView`.
- **`search`** — shared search API: request records (`DestinationSearchRequest`, `HotelIdsSearchRequest`, `RoomRequest`) · `web/HotelSearchController` (`/api/search/by-destination`, `/by-hotel-ids`; both take optional `?provider=`, inject `SearchProviderRegistry`, return neutral `provider.model.SearchResult`).
- **`booking`** — shared booking API + storage: request records · `HotelBooking` entity (+ `provider` column) + repo · `web/BookingController` (`/api/bookings/**`; injects `BookingProviderRegistry` — new requests take optional `?provider=`, booking-keyed ops route by the stored `provider` column) + `BookingView`.
- **`web.ApiExceptionHandler`** — global `@RestControllerAdvice`: IllegalArgument→400, IllegalState→**503** (creds not set), NoSuchElement→404, DataIntegrityViolation→409, UnsupportedOperation→**501** (provider doesn't implement it yet), `UpstreamApiException`/RestClient/`UpstreamTransportException`→**502** (provider-neutral: RezLive + Hotelbeds).

## How the RezLive API actually works (the mental model)

- **Static/master data = CSV files** downloaded manually from RezLive's credentialed panel (NOT an API). We ingest them into the cache. `gethoteldetails` is the one live API for per-hotel content.
- **Flow:** search (`findhotel`/`findhotelbyid`) → `SearchSessionId` + per-room `BookingKey` → **prebook** (returns a refreshed BookingKey + price delta) → **bookhotel** → `BookingId` + `BookingCode` → those two identifiers feed cancel / getbookingdetails / confirmation / after-booking-policy. `getcancellationpolicy` is a pre-booking check keyed on BookingKey.
- **Transport:** `POST {base}/action/{action}`, header `x-api-key`, body `XML=`+URL-encoded XML, gzip response.

## How Hotelbeds (the second supplier) differs

- **Content = a live Content API** (paged `GET /hotel-content-api/1.0/hotels`), not CSVs — synced into the **same** cache via `POST /api/static-data/import` (bounded by `hotelbeds.catalog-max-hotels` for the quota). Cache `cityCode` = Hotelbeds **destinationCode** (e.g. `PMI`), so destination search lines up.
- **Flow:** availability (`POST /hotel-api/1.0/hotels`) → **stateless** (no session; a `rateKey` per offer) → **checkrates** (mandatory only for `rateType=RECHECK`; we always call it) → **bookings** → a single `reference` (no BookingCode). The neutral `bookingKey` carries `rateKey#rateType`; the booking provider strips the suffix before the wire.
- **Transport:** JSON REST; per-request headers `Api-key` + `X-Signature` = lowercase-hex SHA-256(`apiKey+secret+unixSeconds`). Test host `https://api.test.hotelbeds.com`; free test key quota is tight (8 req/4s, 50/day) — hence the fixture-first tests + bounded catalog sync. Full contract: `docs/hotelbeds/api-contract.md`; design + phases: `docs/hotelbeds/multi-provider-plan.md`.

## Gotchas / conventions (bitten by these — keep them in mind)

- **RezLive JAXB response DTOs are unmarshal-only** (private fields, no setters) — Hotelbeds can't construct them, which forced the neutral `provider.model` records (`SearchResult`/`RateCheckResult`/`CancellationResult`) so both providers can build the response. Design them to serialize to the SAME JSON the frontend already read.
- **Both suppliers' provider beans are always loaded** (the adapters are plain `@Component` — no `@ConditionalOnProperty` anymore). Each interface's impls are indexed by a `*Registry`; a request selects one via the `?provider=` **enum** query param (case-insensitive via `ProviderIdConverter`), and `hotel.provider` only sets the default when it's omitted. Booking-keyed ops route by the stored `HotelBooking.provider` column (null/blank ⇒ RezLive, the pre-split default — NOT the configurable default). The **only** remaining `@ConditionalOnProperty("hotel.provider")` is `StaticDataStartupImporter` (rezlive-only CSV auto-seed on startup), which is not a provider adapter. Tests: `ProviderWiringTest` (both load, default rezlive) + `HotelbedsProviderWiringTest` (boots `hotel.provider=hotelbeds` → default flips, rezlive still reachable) + `ProviderSelectionApiTest` (end-to-end `?provider=` binding).
- **Hotelbeds `X-Signature` uses the host clock (unix seconds)** — clock skew → 401. Regenerate per request. `Accept-Encoding` is omitted so responses come back uncompressed (no manual gzip). Two error shapes: flat `{"error":"…"}` (gateway 401/403) vs nested `{"error":{code,message}}` (app) — both → `UpstreamApiException` → 502.

- **Response DTOs use `hasError()`, not `isError()`** — `is`+`get` on `error` collide as Jackson JSON properties.
- **`book()`/`cancel()` are intentionally NOT `@Transactional`** — the RezLive HTTP call must not run inside a DB tx (pins a connection; risks rolling back an already-charged booking). Persistence happens in its own repo tx after the call.
- **Idempotency** is best-effort via a unique `agent_ref_no` + pre-call lookup (retry-safe, not concurrency-safe). Full reservation-before-call + orphan reconciliation are deferred (go-live).
- **City** is a numeric *string* code (e.g. `968`=Dubai). **CSV `HotelCode` (Int) vs `gethoteldetails` `HotelId` (alphanumeric, e.g. `XHUB18`) relationship is NOT documented — verify against real data.** Dates on the wire are `dd/MM/yyyy`.
- **Spring Boot 4 moved test-slice annotation packages** (`@WebMvcTest` etc.) — use plain unit tests or full `@SpringBootTest`. Spring Boot 4 also uses **Jackson 3** (JSON) — XML binding is JAXB, not jackson-dataformat-xml. Boot 4 also moved **`EnvironmentPostProcessor`** `org.springframework.boot.env` → `org.springframework.boot` (old one deprecated, removal in 4.2); register it in `META-INF/spring.factories` under the **new** FQN, else it silently won't load.
- **Windows PowerShell blocks `npm run dev`** (execution policy on `npm.ps1`). Use `.\dev.cmd` or `npm.cmd`. Only the user's own shell can fix the policy (`Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`); the agent's shell is a different context.
- **Preview screenshots are flaky in this environment** — verify UI via `preview_eval`/`preview_snapshot` (DOM) when screenshots time out.

## Status

- **RezLive — done & verified end-to-end vs `rezlive-stub`:** static-data cache + query, `gethoteldetails`, search, the full booking chain (all 7 actions), and the React frontend (search→detail→book→bookings→cancel).
- **Hotelbeds — done & verified end-to-end vs `hotelbeds-stub` (Phases 3–6):** signed client + `/status`, availability search, checkrates/book/cancel, and the Content-API catalog sync. Drove search→prebook→book→cancel *and* the catalog import + cache-backed browsing against the stub; `gradlew test` green in bodiala + both stubs. **Both suppliers work behind one interface with the frontend JSON unchanged**, selectable by `hotel.provider` (one supplier per run, not both).
- **Go-live (external):** RezLive — real sandbox creds + IP whitelisting; verify real CSV layout + `HotelCode`↔`HotelId`. Hotelbeds — real key+secret in `.env` + flip `HOTEL_PROVIDER=hotelbeds`; first live call is the signed `GET /status`; prod requires mTLS (`api-mtls.hotelbeds.com`). Confirm the UNVERIFIED fields in `docs/hotelbeds/api-contract.md` §9 on the first live calls.
- **Deferred (documented, not built):** neutralise the 4 secondary Hotelbeds booking reads (currently **501**; not used by the frontend); refine Hotelbeds per-status error handling (401/403/429/410) + backoff; concurrent-safe idempotency (reservation-before-call) + orphan reconciliation; static-data hardening (streaming import, N+1); pagination for `/api/static-data/cities`. A cross-provider **aggregating** search (query both suppliers + merge) was intentionally NOT built.
