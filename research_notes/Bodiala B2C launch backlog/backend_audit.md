# Bodiala backend + Hotelbeds stub: gaps, risks and tech debt blocking a public B2C booking site

Read-only audit of the code as of 2026-11-08 (branch `hotel-card-photos`, HEAD `985d946`). Nothing was built, run or committed, and Hotelbeds was not called. Citations use reference-style links (definitions at the bottom) and point to absolute local paths with line numbers. Severity scale: **BLOCKER** (must be fixed before public traffic or real money) / **HIGH** / **MEDIUM** / **LOW**. Each finding ends with a *Fix:* direction.

**Blockers at a glance:**
1. No auth on any endpoint. Anyone can list every booking, cancel any booking by its (guessable) supplier reference, or wipe and re-import the catalog.
2. No payments. An anonymous `POST /api/bookings` creates a real Hotelbeds booking, which the agency is invoiced for, with nothing collected from the guest. That includes non-refundable rates, and the guest is never told a rate is non-refundable.
3. When the booking outcome is unknown (60 s timeout or persist failure), the code does nothing: no reconciliation and no orphan recovery. In practice idempotency is not active, because the frontend never sends `agentRefNo`.
4. The price shown, checked and stored is inconsistent (net vs selling). The code never compares price with the quote and silently accepts a 2% tolerance.
5. There is no production mTLS client.
6. The schema is managed by `ddl-auto=update`, with no migrations.
7. Anonymous traffic can trigger live Content-API calls without limit, with a credential-block risk.
8. Hotelbeds certification requirements are not met: rate comments, voucher (supplier name/VAT plus guest e-mail), children ages, and lead-passenger holder.

## 1. Security: authentication, authorisation, CORS, input validation, secrets, injection, abuse protection, docs exposure

### Takeaway
The API has no security layer at all: no Spring Security dependency and no authn/authz on reads, writes, cancels or the admin import. Booking ids are supplier references, which are low-entropy and sequential-looking. Several endpoints let an anonymous caller spend the Hotelbeds quota or trigger real-money bookings and cancellations. Error bodies forward raw upstream messages.

### Cited Findings
- **[BLOCKER] No security framework is present.** `build.gradle` dependencies are only starter, web, data-jpa, validation, springdoc, commons-csv, JAXB and postgresql. There is no `spring-boot-starter-security`, no actuator and no rate limiter. — [build.gradle:21-42][BG]. *Fix:* add Spring Security with a stateless setup. Public routes: search, content, prebook, create-booking (behind payment). Booking reads and cancels need a guest token (an e-mail magic link, or booking ref + last name + a high-entropy secret) or a customer account. Admin routes (`/import`, list-all) need staff auth.
- **[BLOCKER] `GET /api/bookings` returns every booking in the table, unauthenticated and unpaged.** The controller calls `listBookings()`, which is `bookings.findAll()`. — [BookingController.java:62-66][BC]; [HotelbedsBookingProvider.java:199-202][BP]. The frontend calls it as "my bookings". — [bodiala-web api.ts:106][WAPI]. Today the row holds no guest names (see §3), but it does expose hotel, dates, price and agentRefNo for every customer. Once PII is added this becomes a GDPR breach. *Fix:* remove the public list. Scope reads to the authenticated customer or guest token. Paginate the admin list.
- **[BLOCKER] Anyone can cancel any booking by id.** `POST /api/bookings/{bookingId}/cancel` takes the Hotelbeds reference from the path, with no ownership check. — [BookingController.java:74-79][BC]; [BookingProviderRegistry.java:36-40][BPR]. Hotelbeds references look like `"1-4075658"`. — [api-contract.md:38][AC]. The stub issues them sequentially. — [StubBookingStore.java:30-37][SBS]. So the ids are enumerable. `GET /api/bookings/{bookingId}` is equally open. — [BookingController.java:68-73][BC]. *Fix:* never accept the supplier reference as the public key. Expose an opaque, random public booking id plus a per-booking secret or an owner check. Require re-auth or confirmation for cancels. Add an audit log.
- **[BLOCKER] The admin catalog import is public.** `POST /api/static-data/import` has no auth. — [StaticDataController.java:73-82][SDC]. It runs `deleteAllInBatch()` on country, city, hotel, image and amenity tables, then re-imports. — [HotelbedsCatalogImporter.java:108-114][CI]. Each call spends Content-API quota by paging `/hotels`. — [HotelbedsCatalogImporter.java:128-151][CI]. One anonymous request can therefore wipe the catalog or burn quota. *Fix:* move it to an admin-only route or a scheduled job (see §5). Never expose it on the public API.
- **[BLOCKER] Anonymous callers can drive live Content-API calls without limit.**
  - `GET /api/static-data/hotel-covers?codes=…&provider=hotelbeds` fetches every code not already cached, live, on each call (up to 100 codes). — [HotelCoverService.java:53-76][HCS].
  - Codes Hotelbeds doesn't know are never cached as misses: only hotels already in the cache are excluded. So bogus codes cause a live call on *every* request. — [HotelCoverService.java:60-67][HCS]; [HotelbedsContentService.java:85-98][CS].
  - The same applies to `GET /hotels/{code}?provider=hotelbeds`. — [StaticDataController.java:117-123][SDC], [StaticDataController.java:150-155][SDC]; [HotelbedsContentService.java:59-64][CS].
  - The team's own plan warns: "Content API must not be polled real-time (credential-block risk — cache + `lastUpdateTime` only)". — [multi-provider-plan.md:301-302][MP].

  *Fix:* serve content only from a pre-synced cache (full sync plus incremental `lastUpdateTime`, see §7). Add negative caching. Put a per-IP or global rate limit and a quota budget on any live content fallback.
- **[HIGH] There is no rate limiting or bot protection on search, prebook or book.** Every `POST /api/search/by-destination` causes at least one Hotelbeds availability call. — [SearchAggregationService.java:63-106][SAS]; [HotelbedsSearchProvider.java:87-91][SP]. Hotelbeds throttles with 403 (quota) and 429 (QPS). — [api-contract.md:16][AC]. *Fix:* add a gateway or app rate limit (for example per-IP token buckets), a short-TTL availability cache keyed by the normalised request, CAPTCHA or bot scoring on book, and alerting on quota.
- **[HIGH] Bean validation is unused, so request records are unconstrained.**
  - `spring-boot-starter-validation` is on the classpath — [build.gradle:25][BG] — but no `@Valid` or constraint exists anywhere in `src/main` (grep found no `@Valid`, `@NotNull` or `@Size`).
  - The records accept free-form strings for numeric fields: `adults`, `children`, `totalRooms` and `totalRate` are `String`. — [BookRoom.java:6-15][BRM]; [BookRequest.java:10-21][BKR]; [GuestModel.java:4-10][GM]; [RoomRequest.java:8-12][RR].
  - There is no max on rooms, nights, guests or name length, and no check that arrival is not in the past. — [HotelbedsBookingProvider.java:289-296][BP]; [HotelbedsSearchProvider.java:118-135][SP].
  - `by-hotel-ids` passes an arbitrarily long `hotelIds` list straight upstream. The 2000-code cap applies only to the multi-city path. — [SearchAggregationService.java:43-44][SAS], [SearchAggregationService.java:89-94][SAS] vs [SearchAggregationService.java:108-112][SAS]; [HotelbedsSearchProvider.java:76-85][SP].

  *Fix:* add `@Valid` plus Jakarta constraints on all request DTOs. Use typed numerics (and money as decimal). Bound list sizes and stay length. Reject past dates. Validate ISO country codes.
- **[MEDIUM] Upstream and internal error text reaches anonymous clients.**
  - `ApiExceptionHandler` echoes `e.getMessage()` for 502 upstream, transport and codec errors. — [ApiExceptionHandler.java:60-76][AEH].
  - The Hotelbeds client puts the parsed upstream message, or the *raw response body* as a fallback, into that message. — [HotelbedsJsonClient.java:76-96][JC].
  - `RestClientException` messages typically include the upstream URL.
  - Aggregated search copies `e.getMessage()` into `providerStatus[].error` in a 200 response. — [SearchAggregationService.java:147-150][SAS].
  - `IllegalStateException` → 503 with its message is a blanket mapping, so unrelated internal state errors also surface as "503 + message". — [ApiExceptionHandler.java:30-34][AEH].

  *Fix:* return stable error codes and generic text to the public. Log details server-side with a correlation id. Map internal errors to 500.
- **[MEDIUM] CORS is not configured, and the frontend relies on the Vite dev proxy.** The vite config says "no CORS config needed on the backend" because `/api` is proxied to :8080. — [bodiala-web vite.config.ts:5-13][VITE]. Nothing in the backend configures CORS (grep found no `CrossOrigin` or `cors`). *Fix:* serve the SPA and API same-origin behind a production reverse proxy, or add an explicit CORS allow-list. If cookie-based auth is added, add CSRF protection too.
- **[MEDIUM] Swagger UI and the OpenAPI spec are exposed in every profile.** springdoc is enabled with no profile gating. — [application.properties:31-35][AP]; [OpenApiConfig.java:13-31][OAC]. It documents the admin import and the cancel-any-booking routes. *Fix:* set `springdoc.api-docs.enabled=false` and `springdoc.swagger-ui.enabled=false` in prod, or put them behind admin auth.
- **[MEDIUM] Secrets and config handling fail open in places.**
  - The datasource defaults to `bodiala/bodiala` when env vars are missing, so prod would silently try default creds. — [application.properties:22-24][AP].
  - Docker Postgres publishes `5432:5432` on all interfaces with password `bodiala`. — [docker-compose.yml:17-21][DC].
  - The `.env` loader reads a CWD-relative `.env` by default. — [DotEnvEnvironmentPostProcessor.java:45][DOT]. It logs only key names, which is good. — [DotEnvEnvironmentPostProcessor.java:54-57][DOT].
  - The git-ignored local `.env` still carries `REZLIVE_*` entries. Key names were observed; values were deliberately not read. `.gitignore` still says it "holds RezLive credentials". — [.gitignore:5-6][GI].

  *Fix:* in prod, take no defaults for DB or supplier secrets (fail startup). Use a secret manager or platform env vars, not `.env`. Rotate and remove the stale RezLive creds. Bind Postgres to localhost or a private network.
- **[MEDIUM] Activating the stub profile by mistake fakes bookings.** The `hotelbeds-stub` profile hard-codes `hotelbeds.base-url=http://localhost:9091` with placeholder creds and enables startup import. — [application-hotelbeds-stub.properties:7-13][APS]. The stub accepts any non-blank key, never verifies the signature, and always answers `CONFIRMED`. — [StubController.java:27-29][STC], [StubController.java:489-495][STC]; [StubBookingStore.java:32-37][SBS]. If that profile were active in prod, customers would receive fake confirmations. *Fix:* fail startup when a prod profile is combined with a localhost or stub base URL, and keep the stub profile out of prod artifacts.
- **[LOW] Injection surface is small.** Queries are derived or parameterised JPQL. — [HotelImageRepository.java:19-21][HIR]; [HotelRepository.java:19-20][HR]. URLs are built from config values and `Long` codes. — [HotelbedsContentService.java:90-92][CS]; [HotelbedsCatalogImporter.java:135-136][CI]. The cancel URL concatenates the stored supplier reference into a `RestClient` URI template. — [HotelbedsBookingProvider.java:159-161][BP]. *Fix:* use `uri("/…/bookings/{ref}", ref)` so values are encoded.

### Inferences
- Combined with §2, the open `POST /api/bookings` plus the absence of payment means an anonymous attacker can create real, possibly non-refundable, Hotelbeds bookings billed to the agency. This is the single most dangerous issue for a public launch.
- Making the public booking key unguessable matters even after auth: guest-checkout flows usually rely on a "manage booking" link, and the supplier reference must not be enough to act on a booking.

### Gaps
- I did not see the actual Hotelbeds production reference format or how sequential it is. The only evidence is the example `"1-4075658"` ([api-contract.md:38][AC]) and the stub's generator.
- I did not inspect the tracked H2 file `data/bodiala.mv.db` (see §8) for leftover personal data.

## 2. Booking correctness and money safety: idempotency, races, orphans, reconciliation, price changes, rate expiry, timeouts and retries, transactions, what is persisted

### Takeaway
The booking chain is a thin happy path. Idempotency only works when the client supplies `agentRefNo`, and the frontend never does. A timeout or a persist failure after Hotelbeds confirms leaves an orphan that only appears in a log line, with no reconciliation call anywhere. Prices are never compared between quote and confirmation. Net cost and selling price are mixed across search, prebook and storage. Cancellation handles only a narrow set of supplier responses.

### Cited Findings
- **[BLOCKER] Idempotency is inactive in practice.**
  - The pre-call lookup runs only `if (clientSuppliedRef)`. — [HotelbedsBookingProvider.java:92-101][BP]. Otherwise a random `BODIALA-xxxxxxxx` reference is generated per request. — [HotelbedsBookingProvider.java:270-275][BP].
  - The frontend's `confirmBooking()` sends no `agentRefNo`. — [bodiala-web BookHotelDialog.tsx:115-131][WBD].
  - So a double-click, a browser retry or a proxy retry produces a second real booking.

  *Fix:* generate an idempotency key server-side at checkout or prebook (bound to the cart or payment intent). Persist a `PENDING` booking row with the exact `clientReference` *before* calling Hotelbeds. Make `POST /bookings` require that key.
- **[BLOCKER] Unknown-outcome bookings (timeouts) have no handling.**
  - The read timeout is 60 s for all calls. — [HotelbedsJsonClient.java:42-45][JC]. That meets Hotelbeds' "at least 60 seconds" booking-confirmation minimum — [Hotelbeds best practices][HBBP], [Hotelbeds certification 3.11][HBCERT] — but leaves no margin.
  - A timeout on `POST /hotel-api/1.0/bookings` ([HotelbedsBookingProvider.java:112][BP]) surfaces as a 502. — [ApiExceptionHandler.java:66-70][AEH]. The booking may already be confirmed at Hotelbeds.
  - No code ever reads bookings back from Hotelbeds: there is no `GET /bookings/{ref}` or booking-list call. `getStored` reads only the local table. — [HotelbedsBookingProvider.java:204-208][BP].
  - The plan called for "post-timeout reconcile (`GET /bookings?clientReference=…`)". — [multi-provider-plan.md:233-235][MP]. CLAUDE.md lists reconciliation as deferred. — [CLAUDE.md:78][CMD], [CLAUDE.md:89][CMD].

  *Fix:* on timeout or 5xx, mark the row `UNKNOWN`. Run a reconciler, synchronously with a short retry and then as a scheduled job, that queries Hotelbeds by reference or clientReference and date range. Then either adopt the booking or confirm its absence. Never let the client blindly retry with a new reference.
- **[BLOCKER] An orphan is created when persistence fails after a confirmed supplier booking.** The catch block only logs "BOOKING PERSIST FAILED … recover/cancel manually" and rethrows. — [HotelbedsBookingProvider.java:136-142][BP]. Ways this happens:
  - A unique violation on `agent_ref_no` or `rez_booking_id` — [HotelBooking.java:19-22][HBK] — in the concurrent-retry race. — [CLAUDE.md:78][CMD].
  - A collision of the 32-bit random suffix (8 hex chars of a UUID). — [HotelbedsBookingProvider.java:271-274][BP].
  - A DB outage.

  *Fix:* use reservation-before-call (insert `PENDING` first, inside its own short transaction) so the post-call step is an UPDATE. Add an orphan queue or table, alerting, and auto-cancel or adoption by the reconciler.
- **[HIGH] The concurrent double-submit race is not handled.** Two simultaneous requests with the same `agentRefNo` both miss `findByAgentRefNo`, both call Hotelbeds, and the second insert fails on the unique index. That is a double charge plus an orphan. Acknowledged as "retry-safe, not concurrency-safe". — [HotelbedsBookingProvider.java:94-101][BP]; [CLAUDE.md:78][CMD]. Hotelbeds does not dedupe on `clientReference`. — [api-contract.md:40][AC]. *Fix:* use the `PENDING` row with a unique constraint as a lock (insert-first wins), or an advisory lock keyed on the idempotency key.
- **[HIGH] Truncating `agentRefNo` to 20 chars can return someone else's booking.** `clientReference()` truncates silently. — [HotelbedsBookingProvider.java:270-275][BP]. Two different client refs with the same first 20 characters map to one key, and the second request gets back the *first* customer's booking as an "idempotent" hit. — [HotelbedsBookingProvider.java:95-100][BP]. Because `agentRefNo` is client-controlled, anyone who knows or guesses a ref can read that booking's view through `POST /api/bookings`. *Fix:* make the server own the reference. Reject over-length refs instead of truncating. Bind idempotent replays to the same customer and payment.
- **[BLOCKER] Price integrity: the code never compares the quote with the confirmation.**
  - `BookRoom.totalRate` is accepted but never used. — [BookRoom.java:13][BRM]; [HotelbedsBookingProvider.java:212-225][BP].
  - Every booking is sent with `"tolerance": 2`. — [HotelbedsBookingProvider.java:110][BP]. Tolerance is a percentage. — [api-contract.md:36][AC]. So Hotelbeds may confirm up to 2% above the checked price with no signal to bodiala.
  - Prebook returns `bookingAfterPrice = hotel.totalNet` (net cost) with `bookingBeforePrice` and `difference` as `null`, so the UI cannot detect a price change. — [HotelbedsBookingMapper.java:32-36][BM].
  - The per-room `totalRate` shown to the guest is `sellingRate`, falling back to `net`. — [HotelbedsBookingMapper.java:39-44][BM]; [HotelbedsSearchMapper.java:57-59][SM]. This contradicts the plan's rule "customer-visible `totalRate` = `sellingRate` (never `net`)". — [multi-provider-plan.md:130][MP], [multi-provider-plan.md:236][MP].
  - The stored `price` is `b.hotel().totalNet()`, the net cost, not what the guest pays. — [HotelbedsBookingProvider.java:133][BP].
  - There is no markup or pricing engine anywhere.

  *Fix:* carry a server-side quote (prebook id → price, currency, rateKey, expiry) and charge exactly that. Compare the supplier's confirmed net with the quoted net. Decide on tolerance explicitly (0 or a small, budgeted margin). Store net cost, sell price, currency and margin as `BigDecimal`. Never show net to consumers.
- **[HIGH] Booked data is taken from the client, not the supplier.** `hotelId`, `hotelName`, `city`, `countryCode`, `arrivalDate` and `departureDate` are persisted verbatim from the request. — [HotelbedsBookingProvider.java:126-131][BP]. The supplier response carries the authoritative hotel code and name. — [BookingResponse.java:21-26][BR]. The actual stay is encoded in the rateKey, which must never be parsed. — [api-contract.md:26][AC]. A buggy or tampered client can therefore store a booking whose hotel and dates differ from what was actually booked. *Fix:* persist from the Hotelbeds booking response (hotel, checkIn/checkOut, rooms, paxes, totals) and reject on mismatch with the quote.
- **[MEDIUM] Non-confirmed outcomes are thrown away.**
  - A missing reference or a status other than CONFIRMED/PRECONFIRMED throws `UpstreamApiException` with nothing persisted, even though Hotelbeds may hold the booking. — [HotelbedsBookingProvider.java:113-119][BP].
  - `PRECONFIRMED` is treated as success with no follow-up. — [HotelbedsBookingProvider.java:277-280][BP].

  *Fix:* persist every supplier response with a reference. Model the PRECONFIRMED and UNKNOWN states and reconcile them.
- **[MEDIUM] Rate and quote expiry are not modelled.** No timestamp or TTL is kept for search results or prebook. Hotelbeds' best practices state the order (one Availability, then one CheckRates if RECHECK, then one Booking) and say that on a 500, "the product is no longer available … restart the booking". — [Hotelbeds best practices][HBBP]. All upstream errors become a generic 502, so the UI cannot tell "sold out / restart" apart from an outage. — [HotelbedsJsonClient.java:76-80][JC]; [ApiExceptionHandler.java:60-64][AEH]. *Fix:* store the quote with an expiry. Map Hotelbeds 500/PRODUCT_ERROR on book or checkrates to a distinct "rate no longer available" (409/410) so the UI restarts.
- **[HIGH] Cancellation handles a narrow set of outcomes, with no fee quote.**
  - Only 404 and 410 are mapped to 409; everything else is passed through as 502. — [HotelbedsBookingProvider.java:186-196][BP].
  - The contract says Hotelbeds `409` = already cancelled ("treat idempotently on cancel"). — [api-contract.md:54][AC]. That case currently becomes a 502 and the local row stays `Confirmed`.
  - A non-`CANCELLED` status is returned to the client without persisting anything or raising an alert. — [HotelbedsBookingProvider.java:165-177][BP].
  - No `cancellationFlag=SIMULATION` fee quote is shown before the real cancel, though the contract documents it. — [api-contract.md:45-47][AC].
  - No refund is triggered.
  - The sources disagree on 410: "replay of a request that failed in last 15 min" — [api-contract.md:54][AC]; "410 Gone → success (already cancelled)" — [multi-provider-plan.md:229][MP]; "call CRC; temporary, try again" in code — [HotelbedsBookingProvider.java:191-194][BP].

  *Fix:* run a two-step cancel (SIMULATION quote, then confirm). Treat 409 as idempotent success after verifying with a GET. Persist every attempt. Hook into refund processing. Settle the 410 meaning with Hotelbeds.
- **[MEDIUM] Transaction boundaries are inconsistent.**
  - `book`/`cancel` correctly run the HTTP call outside a DB transaction. — [HotelbedsBookingProvider.java:79-84][BP], [HotelbedsBookingProvider.java:145-147][BP]; rule in [BookingProvider.java:18-19][BPI].
  - But `HotelbedsContentService.fetchAndCache` and `fetchAndCacheAll` are `@Transactional` and make the HTTP call inside. — [HotelbedsContentService.java:59-68][CS], [HotelbedsContentService.java:74-82][CS].
  - So does the whole catalog import. — [HotelbedsCatalogImporter.java:75-83][CI].
  - These pin a pooled DB connection for up to the 60 s read timeout, and they are reachable anonymously (§1). That is a connection-pool exhaustion vector.

  *Fix:* fetch first, then upsert in a short transaction.
- **[MEDIUM] Cancel has no concurrency control.** `HotelBooking` has no `@Version` (grep found no `@Version` in `src/main`). — [HotelBooking.java:18-83][HBK]. Two concurrent cancels both call Hotelbeds, and the last write wins. *Fix:* use optimistic locking plus a state-machine guard (`CONFIRMED → CANCELLING → CANCELLED`).

### Inferences
- Because the default `clientReference` is random and never shown to the user, a support agent cannot match an orphaned Hotelbeds booking to a customer: the row doesn't exist and no guest contact is stored (§3). Today, recovering an orphan depends on reading application logs.
- The 2% tolerance becomes a direct margin loss once a payment is captured for the quoted amount before booking.

### Gaps
- I found no authoritative statement of rateKey validity duration. The Hotelbeds best-practices page does not cover it. — [Hotelbeds best practices][HBBP].
- It is UNVERIFIED whether Hotelbeds' booking-list endpoint supports filtering by `clientReference`. The plan assumes it ([multi-provider-plan.md:235][MP]); the contract lists `clientReference dedup` as unverified ([api-contract.md:76-79][AC]).

## 3. Data model gaps for B2C: customers and contact details, multiple rooms and guests, children ages, special requests, payments and refunds, audit trail, status lifecycle

### Takeaway
`HotelBooking` is a supplier-reference record, not a consumer booking. It stores no guest names, no contact details, no rooms or occupants, no rate or cancellation terms, no payment data and no history. The frontend sends one guest, one room and no children ages. The certification test booking requires 2 adults and 2 children.

### Cited Findings
- **[BLOCKER] No customer or contact data is stored.** The entity has no guest names, e-mail, phone, address, language or market. — [HotelBooking.java:28-82][HBK]. Guest names are sent to Hotelbeds but not persisted. — [HotelbedsBookingProvider.java:103-109][BP], [HotelbedsBookingProvider.java:227-241][BP]. Hotelbeds certification requires a voucher sent to the end user for every confirmed booking. — [Hotelbeds certification §4.1][HBCERT]. *Fix:* add `Customer`/`Contact` (e-mail, phone, locale, marketing consent) plus `BookingGuest` (per room, lead flag, child age). Add GDPR retention and erasure.
- **[BLOCKER] No payments or refunds model or integration exists.** No payment entity, PSP client or refund flow exists. The Hotelbeds `paymentData` (card + contact) is not supported, even though the contract says it is required for AT_HOTEL / credit-card accounts. — [api-contract.md:36-37][AC]. The plan left this as an open decision. — [multi-provider-plan.md:281-282][MP]. *Fix:* add `Payment` (intent id, amount, currency, status, captured/refunded amounts) tied to booking states. Authorise before `POST /bookings`, capture on CONFIRMED, void or refund on failure or cancel. Keep card data at a PSP (tokenised) to minimise PCI scope.
- **[HIGH] There is no status lifecycle.** `status` is a free-text `String`, title-cased from whatever Hotelbeds returns (`Confirmed`, `Preconfirmed`, `Cancelled`). — [HotelBooking.java:38-39][HBK]; [HotelbedsBookingProvider.java:124][BP], [HotelbedsBookingProvider.java:282-287][BP]. There are no PENDING_PAYMENT, PENDING_SUPPLIER, UNKNOWN, FAILED, CANCELLING or REFUNDED states, and no `updatedAt`. *Fix:* use an enum state machine with timestamps and an append-only `booking_event` audit table (who, what, when, supplier auditData token).
- **[HIGH] The rate and contract terms in force at booking time are not stored.** Missing: room type, board, rateKey, rateClass (NRF), cancellation policy, rate comments, taxes, supplier name/VAT, selling price, modification policies. — [HotelBooking.java:28-82][HBK]. The Hotelbeds booking DTO binds only `reference, clientReference, status, cancellationReference, currency, hotel{code,name,totalNet,cancellationAmount}`. — [BookingResponse.java:10-26][BR]. The contract response also has `holder`, `hotel.rooms[]`, `supplier`, `pendingAmount` and `modificationPolicies`. — [api-contract.md:37-39][AC]. *Fix:* persist a JSON snapshot of the confirmed supplier response plus normalised columns for the voucher, support and disputes.
- **[HIGH] Children ages are not supported end to end.**
  - The frontend search sends `noOfChilds` without `childrenAges`. — [bodiala-web searchState.ts:97-106][WSS].
  - The backend sends `children > 0` without `paxes` when ages are absent. — [HotelbedsSearchProvider.java:104-116][SP].
  - Booking adds a child age only if present. — [HotelbedsBookingProvider.java:237-239][BP].
  - Nothing validates that ages are present or that their count matches `children`. — [HotelbedsSearchProvider.java:118-135][SP].
  - Hotelbeds certification: children are optional, but "if you support children, the child's age is mandatory". The LIVE certification test booking must be 2 adults/2 children. — [Hotelbeds certification §3.3, §6.1][HBCERT].

  *Fix:* require ages per child at search and book. Carry occupancy on the quote. Validate the count against the rate.
- **[HIGH] Multi-room and multi-guest are only partly supported.**
  - The backend accepts several rooms, and each search occupancy is sent as `rooms: 1`. — [HotelbedsSearchProvider.java:104-110][SP].
  - Prebook keeps only the *first* rate's cancellation policy for all rooms. — [HotelbedsBookingMapper.java:46-62][BM].
  - The frontend books exactly one room with one adult guest, whatever the adult count. — [bodiala-web BookHotelDialog.tsx:130][WBD]; [bodiala-web searchState.ts:104][WSS].
  - If the guest list is empty, the holder falls back to `"Guest"/"Guest"`. — [HotelbedsBookingProvider.java:103-107][BP].
  - Certification requires the holder to be the lead passenger, with at least one passenger name per room. — [Hotelbeds certification §4.3][HBCERT].
  - Certification recommends identical occupancies in one node with `rooms=N` and split nodes otherwise. — [Hotelbeds certification §3.4][HBCERT].

  *Fix:* use a proper occupancy model (rooms → paxes). Require a lead-guest name and at least one name per room. Make the holder an explicit request field. Keep cancellation policies per room.
- **[MEDIUM] Special requests and remarks are not supported.** No `remark` field exists on the request or the entity. — [BookRequest.java:10-21][BKR]. The Hotelbeds booking request supports `remark?`. — [api-contract.md:36][AC]. *Fix:* add a bounded free-text field, passed through and persisted.
- **[MEDIUM] Money and currency types are weak.** Price, cancellation charges and rates are `String`. — [HotelBooking.java:66-82][HBK]; [SearchResult.java:31-41][SR]. The search currency is taken from the *first* hotel only. — [HotelbedsSearchMapper.java:23][SM]. There is no display-currency conversion. The plan flagged the account default currency as an open decision. — [multi-provider-plan.md:289][MP]. *Fix:* use `BigDecimal` plus an ISO-4217 currency on every amount, and define a currency strategy (supplier currency vs display currency and FX source).
- **[LOW] Legacy naming remains.** Columns are `rez_booking_id`/`rez_booking_code` (RezLive era), and `bookingCode` is always null. — [HotelBooking.java:29-33][HBK]; [CLAUDE.md:79][CMD]. *Fix:* rename in the first Flyway migration (§5).

### Inferences
- Without stored contact details, the business cannot send confirmations, handle hotel-side issues, or comply with the voucher requirement. That makes the contact model a hard prerequisite of certification, not just of B2C UX.

### Gaps
- It is not confirmed whether Hotelbeds requires a pax entry for every occupant or just one name per room. The certification page says "at least one passenger name per room". — [Hotelbeds certification §4.3][HBCERT].

## 4. Hotelbeds integration robustness: per-status errors, backoff, quota, mTLS, signature clock skew, timeouts, dispute logging, certification, UNVERIFIED fields

### Takeaway
The transport is a bare `RestClient` over `HttpURLConnection`. It has no mTLS, no status-specific handling (apart from cancel 404/410), no retry or backoff, no circuit breaker and no request/response audit logging. Several Hotelbeds certification items are missing: rate comments, voucher supplier data, promotions, children, sourceMarket rules, and CheckRate usage. Several contract fields are still UNVERIFIED.

### Cited Findings
- **[BLOCKER] There is no mTLS client for production.** Go-live requires mTLS at `api-mtls.hotelbeds.com` with a client certificate. — [api-contract.md:10-11][AC]. The client uses `SimpleClientHttpRequestFactory` with no `SSLContext`, keystore or SSL bundle. — [HotelbedsJsonClient.java:40-46][JC]. The docs still say production is `https://api.hotelbeds.com`. — [HotelbedsProperties.java:16][HP]; [.env.example:19-20][ENV]. *Fix:* use Spring Boot SSL bundles (PKCS12 keystore from a secret store) on a pooled HTTP client (JDK HttpClient or Apache HC5). Point the prod profile at the mTLS host. Monitor certificate expiry.
- **[HIGH] There is no per-status error handling, retry, backoff or circuit breaker.** Every non-2xx becomes `UpstreamApiException(status, message)`. — [HotelbedsJsonClient.java:76-80][JC]. Callers treat it as a 502, except cancel 404/410. — [HotelbedsBookingProvider.java:186-196][BP]. The contract distinguishes:
  - 401 (signature/clock)
  - 403 (quota)
  - 429 (QPS)
  - 409 (already cancelled)
  - 410 (replay)

  — [api-contract.md:16][AC], [api-contract.md:49-54][AC]. The plan called a custom handler for these "not optional". — [multi-provider-plan.md:226-229][MP]. It is still listed as deferred. — [CLAUDE.md:89][CMD]. *Fix:* map 401 → alert (clock or creds), 403 → quota circuit-open, 429 → bounded backoff with jitter on idempotent reads only (search, content, never `POST /bookings`), 500/PRODUCT_ERROR → "rate unavailable". Add a circuit breaker (for example Resilience4j) and expose upstream status in metrics.
- **[HIGH] There is no request/response audit trail for disputes.** No HTTP logging interceptor exists. No DTO binds `auditData`, although Hotelbeds returns it on every response (the stub mimics `auditData.token`). — [AvailabilityResponse.java:10][AR]; [CheckRateResponse.java:10][CR]; [BookingResponse.java:10][BR]; [StubController.java:451-462][STC]. The only booking trace is two log lines. — [HotelbedsBookingProvider.java:97-98][BP], [HotelbedsBookingProvider.java:139-141][BP]. *Fix:* persist the request and response of checkrates, bookings and cancel (redacting PII and card data), plus the auditData token, keyed by booking id. Ship structured logs.
- **[BLOCKER] Rate comments are not mapped or shown.** Certification §3.9 says "Show them to the customer before confirmation". They come from CheckRate (required for RECHECK) or from Content API RateComments. — [Hotelbeds certification][HBCERT]. The `CheckRateResponse.Rate` DTO has no `rateComments` — [CheckRateResponse.java:20-23][CR] — although the contract lists it. — [api-contract.md:30-31][AC]. *Fix:* bind `rateComments`, return them from prebook, require the UI to display and acknowledge them, and persist them on the booking.
- **[BLOCKER] Voucher data (supplier, VAT, reference) is missing.** Certification §4.5 mandates "Payable through XXX, acting as agent for the service operating company…" with the supplier name and VAT number from the supplier tag. §4.4 requires reference, dates, room type, board type and rate comments. — [Hotelbeds certification][HBCERT]. `BookingResponse` does not bind `supplier`, rooms or board. — [BookingResponse.java:12-26][BR]. *Fix:* bind `hotel.supplier{name,vatNumber}`, rooms, board, paxes and comments. Generate and e-mail the voucher (PDF or HTML).
- **[HIGH] `paymentType`, taxes, NRF and promotions are not surfaced.**
  - `AvailabilityResponse.Rate` drops `paymentType`, `cancellationPolicies`, `taxes` and offers. — [AvailabilityResponse.java:30-42][AR]. All are in the contract's response. — [api-contract.md:23-26][AC].
  - `rateClass` (NOR/NRF) is bound but never mapped to the client. — [AvailabilityResponse.java:32][AR]; [HotelbedsSearchMapper.java:60-62][SM]. Consumers cannot see "non-refundable".
  - AT_HOTEL rates are neither filtered nor supported. The request has no `filter.paymentType`. — [HotelbedsSearchProvider.java:93-102][SP]. The contract says `paymentData` is required for them. — [api-contract.md:36-37][AC].
  - Certification requires displaying promotions. — [Hotelbeds certification §2.7][HBCERT].

  *Fix:* filter to the payment model you support, map and display NRF, taxes and fees, cancellation deadlines (in destination time — [Hotelbeds certification §3.8][HBCERT]) and promotions.
- **[MEDIUM] CheckRates usage does not match Hotelbeds guidance.**
  - Prebook always calls `/checkrates`, even for BOOKABLE rates, and batches every room's rateKey into one call. — [HotelbedsBookingProvider.java:65-77][BP]; [CLAUDE.md:67][CMD].
  - Best practices say to send CheckRates only for RECHECK (or for BOOKABLE "only if you need extra data such as RateComments") and to send "one rateKey per operation". — [Hotelbeds best practices][HBBP].
  - The certification page instead says "Up to 10 rates per call. Group them where possible", which conflicts with the one-rateKey guidance. — [Hotelbeds certification §2.5-2.6][HBCERT].

  *Fix:* call checkrates when the key is RECHECK, or for BOOKABLE only to fetch rate comments. Confirm with Hotelbeds whether to group keys or send one at a time.
- **[MEDIUM] sourceMarket is global, and guestNationality is ignored.** `sourceMarket` is a single global config value. — [HotelbedsSearchProvider.java:98-100][SP]; [application.properties:12][AP]. The request's `guestNationality` is never sent upstream. — [HotelbedsSearchProvider.java:70-72][SP]. The frontend fills `guestNationality` with the *destination* country. — [bodiala-web searchState.ts:103][WSS]. Certification: "Prices from one market cannot be shown for another". — [Hotelbeds certification §3.6][HBCERT]. *Fix:* derive the source market per visitor (billing country or geo-IP) and send it per request.
- **[MEDIUM] All calls share one 60 s read timeout and an unpooled client.** One factory serves search, content and booking with a 5 s connect and 60 s read timeout. — [HotelbedsJsonClient.java:40-46][JC]. A slow availability call holds a servlet thread for 60 s. `SimpleClientHttpRequestFactory` has no connection pooling. *Fix:* use per-operation timeouts (for example availability ~10-15 s, content ~10 s, booking ≥60 s per Hotelbeds), a pooled client, and bulkheads.
- **[LOW] Signature and clock skew are handled correctly, but nothing monitors the clock.** The signature is regenerated per request from `Instant.now()`. — [HotelbedsJsonClient.java:69-74][JC]; [HotelbedsSignature.java:9-27][SIG]. Clock drift causes 401s, and nothing checks NTP or alerts on 401 bursts. *Fix:* ensure host NTP, alert on 401 rate, and run a startup or readiness `/status` probe ([HotelbedsJsonClient.java:48-51][JC] exists but is never called).
- **[MEDIUM] Quota awareness is missing.** The catalog default of 100 hotels — [application.properties:15-16][AP] — and the stub exist to protect the 50 requests/day evaluation quota. — [CLAUDE.md:68][CMD]. There is no usage counter or budget anywhere. *Fix:* add metrics on upstream calls per endpoint, alerts near the quota, and degrade gracefully (for example disable the live content fallback).
- **[MEDIUM] Several contract fields are still UNVERIFIED.** Listed as needing live confirmation:
  - CheckRates `hotel` vs `hotels` wrapper
  - clientReference dedup
  - 429 body and Retry-After
  - 410 body
  - `cancellationPolicies[].percent`
  - prod host (mTLS)
  - `/status` envelope
  - `hotel.code` identity across the Booking and Content APIs

  — [api-contract.md:76-79][AC]; also noted in [CheckRateResponse.java:6-8][CR]. The plan adds more: the paxes nesting, the `/hotels/{codes}/details` vs `?codes=` sub-path, and the image host and sizes. — [multi-provider-plan.md:291-299][MP]. *Fix:* confirm each on the first live calls and with the account manager. Record real responses as test fixtures.
- **[LOW] The facility master's failure mode is sticky.** A failed load caches an empty map until restart. — [HotelbedsFacilityCatalog.java:41-53][FC], [HotelbedsFacilityCatalog.java:75-79][FC]. Paging stops at `to=1000`. — [HotelbedsFacilityCatalog.java:61-62][FC]. *Fix:* use a TTL with retry, page until the total, and persist to the DB.

### Inferences
- The Hotelbeds certification steps (send a voucher, a live test booking with children, cancel it) cannot pass with the current backend. Certification is therefore on the critical path for go-live, independent of payments.

### Gaps
- The Hotelbeds pages I fetched don't cover QPS limits for production keys, Retry-After semantics, or the booking-list reconciliation API. — [Hotelbeds best practices][HBBP]. These need the API reference or the account manager.

## 5. Operability: logging, metrics, health checks, per-environment config, DB migrations, backups, Docker and deploy, CI

### Takeaway
The repo is set up for one developer's laptop: `bootRun` processes launched by a PowerShell script, a Postgres container with weak credentials, and the schema managed by Hibernate `ddl-auto=update`. There is no health endpoint, no metrics, no structured logging, no scheduled jobs, no production profile, no app container image and no CI.

### Cited Findings
- **[BLOCKER] The schema has no migrations.** `spring.jpa.hibernate.ddl-auto=update` is set in the *default* profile. — [application.properties:25][AP]. There is no Flyway or Liquibase dependency. — [build.gradle:21-42][BG]. The plan's "Flyway migration for composite uniques/renames" was not built. — [multi-provider-plan.md:259-260][MP]. Spring Boot lists `update` among Hibernate's options and supports Flyway/Liquibase as "higher-level database migration tool[s]" that "should [be used] alone to create and initialize the schema". It does not explicitly recommend `update` for or against production. — [Spring Boot data-initialization how-to][SBDI]. *Fix:* adopt Flyway with a baseline from the current schema (renaming the `rez_*` columns), set `ddl-auto=validate` in all non-test profiles, and review migrations in CI.
- **[HIGH] There are no health checks or metrics.** No `spring-boot-starter-actuator` or Micrometer. — [build.gradle:21-42][BG]. `dev.ps1` waits only for the *stub's* `/health`. — [dev.ps1:149-158][DEV], [dev.ps1:180][DEV]. *Fix:* add actuator with liveness and readiness (DB + Hotelbeds `/status` as a non-fatal indicator), and Prometheus/OTel metrics for request latency, upstream calls by status, booking state counts, orphan count and quota usage. Lock actuator down to an internal port.
- **[HIGH] Logging is not production-ready.** No logging config exists in `application.properties`. — [application.properties:1-39][AP]. There are no correlation or request ids. Log lines include booking references and clientReferences. — [HotelbedsBookingProvider.java:97-98][BP], [HotelbedsBookingProvider.java:139-141][BP]. *Fix:* use structured JSON logs with MDC request/booking ids, a PII redaction policy, and log shipping and retention.
- **[HIGH] There is no deploy story.** No Dockerfile, CI workflow or IaC is tracked (the tracked-file list shows only `docker-compose.yml` for Postgres). — [docker-compose.yml:1-31][DC]. Apps are started with `gradlew bootRun` in console windows. — [dev.ps1:144-147][DEV]. The Postgres image is pinned only to the major version `postgres:17`. — [docker-compose.yml:14][DC]. *Fix:* build an OCI image with the Spring Boot buildpack or a Dockerfile, run CI (build, test with a Postgres service or Testcontainers, dependency and secret scanning), use environment promotion, and deploy blue/green.
- **[HIGH] There are no backups or DR.** Data lives in a local named Docker volume. — [docker-compose.yml:22-31][DC]. No backup or PITR is configured. *Fix:* use managed Postgres with PITR and tested restores. Booking and payment tables are the system of record and must be protected.
- **[MEDIUM] There is no per-environment config.** Only the default profile (test host, blank creds → 503) and `hotelbeds-stub` exist. — [application.properties:5-16][AP]; [application-hotelbeds-stub.properties:1-13][APS]. There is no `prod`/`staging` profile and no fail-fast validation of required prod settings. *Fix:* add `staging`/`prod` profiles with `@Validated` `@ConfigurationProperties` (non-blank creds, mTLS bundle, https base URL) and fail startup when invalid.
- **[HIGH] There are no scheduled jobs.** No `@Scheduled` or `@EnableScheduling` exists in `src/main`. — [BodialaApplication.java:7-15][APP]. So there is no reconciliation (§2) and no content refresh. Certification asks you to "Store the data locally and update at least weekly". — [Hotelbeds certification §5][HBCERT]. The only seed is a startup import that runs when the cache is empty, and only in the stub profile. — [HotelbedsStartupImporter.java:38-61][SI]; [application-hotelbeds-stub.properties:13][APS]. *Fix:* add a scheduler with a distributed lock (for example ShedLock) for the incremental content sync, booking reconciliation, PRECONFIRMED follow-up and orphan alerts.

### Inferences
- With `ddl-auto=update` and no migration history, the first schema change to the booking table (adding guests or payments) could silently diverge between environments. That is why migrations belong ahead of the data-model work in §3.

### Gaps
- The intended hosting target (cloud, NAS, VPS) is unknown. `DotEnvEnvironmentPostProcessor` mentions "the NAS". — [DotEnvEnvironmentPostProcessor.java:26][DOT].

## 6. Tests and stub fidelity

### Takeaway
There are 82 `@Test` methods in bodiala and 10 in the stub. They are mostly mapper, registry and aggregation unit tests. The money path (`book()`, idempotency, persist failure, timeouts) has zero tests. No HTTP-level test covers the booking chain or the error mapping. The stub's happy-path-only, partly hard-coded behaviour hides exactly the failure modes listed above.

### Cited Findings
- **[HIGH] The money path is untested.**
  - No test calls `HotelbedsBookingProvider.book()`: a grep for `.book(`/`prebook(` in `src/test` finds none. The only booking-provider test covers cancel (5 tests). — [HotelbedsBookingProviderCancelTest.java:53-111][BCT].
  - Prebook is covered only by a pure mapper test. — [HotelbedsBookingMapperTest.java:13-44][BMT].
  - There is no test for `HotelbedsCatalogImporter`, `BookingController` HTTP behaviour, or `ApiExceptionHandler` mapping.
  - `HotelbedsJsonClientTest` checks only message regex extraction (3 tests), not status, timeout or header behaviour. — [HotelbedsJsonClientTest.java:8-29][JCT].

  *Fix:* unit-test `book()` for the idempotent hit, truncation, non-confirmed status, persist failure and timeout. Write client tests with MockRestServiceServer or WireMock covering 401/403/409/410/429/500 and timeouts. Add an end-to-end booking-chain test against the stub or WireMock.
- **[MEDIUM] Fixtures are synthetic, not recorded.** Mapper tests use hand-made keys such as `RK-RECHECK`/`RK-BOOKABLE`. — [HotelbedsSearchMapperTest.java:45-51][SMT]. There are no `src/test/resources` JSON fixtures; the only test resource is `application.properties`. — [src/test/resources/application.properties:1-14][TAP]. The plan intended to "record 1–2 live avail responses as fixtures". — [multi-provider-plan.md:254-256][MP]. *Fix:* record sanitised real responses (availability, checkrates, booking, cancel, error shapes) and use them as contract fixtures.
- **[MEDIUM] Test infrastructure is coupled to the dev DB.** Tests share the dev Postgres container and only switch schema (`bodiala_test`, create-drop). — [src/test/resources/application.properties:1-10][TAP]. Parallel runs or CI agents would collide. The context-load test needs Docker running. — [BodialaApplicationTests.java:7-12][BAT]. *Fix:* use Testcontainers Postgres.
- **[MEDIUM] Concurrency and security tests are missing.** There are no tests for double-submit, concurrent cancel, or authz (there is no auth yet). *Fix:* add them along with the fixes in §1 and §2.
- **[HIGH] The stub's fidelity gaps hide real failure modes:**
  - The X-Signature is not verified, and auth failures are only a 403 for a blank or `invalid` key. There is no 401, 429 or 5xx, no slow responses and no timeouts. — [StubController.java:27-29][STC], [StubController.java:489-495][STC].
  - Availability honours only the *first* occupancy. — [StubController.java:114-116][STC], [StubController.java:466-472][STC]. It returns no `paymentType` variants (always AT_WEB), no taxes, no cancellation policies, no rateComments and no promotions. — [StubController.java:432-449][STC].
  - Checkrates hard-codes stay dates `2026-10-25/26` and a cancellation deadline of `2026-10-20` (already in the past on 2026-11-08). It always prices at the RECHECK net, whichever key was sent. — [StubController.java:181-221][STC].
  - Booking does no validation of rateKey, holder, paxes, tolerance or clientReference. Its `totalNet` (= net × 1.2) differs from the checkrates `totalNet` (= net × 0.9). — [StubController.java:240-257][STC], [StubController.java:191][STC], [StubController.java:216][STC]. A price-change check would therefore fire on every stub booking.
  - Cancel always returns a 0 charge and never 409 or 410. — [StubController.java:279-298][STC].
  - Content `?codes=` returns only the *first* code — [StubController.java:319-325][STC] — so the batched cover fetch is never exercised realistically. Unknown codes are synthesised as "Stub Hotel N" instead of absent — [StubController.java:65-67][STC] — which hides the negative-cache/quota-drain bug in §1. There is no real paging. — [StubController.java:326-334][STC].
  - Bookings are in memory and lost on restart. — [StubBookingStore.java:27-38][SBS]. There is no booking-list endpoint, so there is nothing to reconcile against.
  - The stub tests cover only the happy chain and a few errors. — [StubControllerTest.java:15-132][STT].

  *Fix:* add fault-injection switches (headers or config) for 401/429/500/timeout/price-change/PRECONFIRMED, multi-room and child handling, a booking-list endpoint, persistent or seeded bookings, and signature verification. Alternatively, replace the stub with WireMock mappings recorded from the real API.

### Inferences
- Because every risky branch (timeout, persist failure, price change, 409/429/500) is untested and the stub cannot trigger it, "verified against the stub" in [CLAUDE.md:86-87][CMD] says nothing about money safety.

### Gaps
- Line coverage could not be measured, because the read-only constraint meant the build/JaCoCo could not be run.

## 7. Performance and scalability: N+1, static-data import, caching, payload sizes, pagination

### Takeaway
The catalog import is an in-memory, single-transaction, delete-all/re-insert job capped at 100 hotels. That doesn't scale to the ~250k-hotel catalog a B2C site needs, and with IDENTITY ids the image inserts can't be batched. Several public endpoints are unpaged or return large entity payloads. Availability has no caching.

### Cited Findings
- **[HIGH] The catalog import cannot scale to a full sync.**
  - All pages are accumulated in memory, then one `@Transactional` method performs `deleteAllInBatch` and `saveAll` per table, with the HTTP paging inside the transaction. — [HotelbedsCatalogImporter.java:75-126][CI], [HotelbedsCatalogImporter.java:128-151][CI].
  - The javadoc puts the full catalog at ~250k hotels. — [HotelbedsCatalogImporter.java:33-36][CI]. At ≤1000 per page that is roughly 250+ requests held in one transaction.
  - The delete-all also wipes hotels cached on demand. — [HotelbedsCatalogImporter.java:111-112][CI].
  - There is no incremental `lastUpdateTime` sync, although the contract documents it. — [api-contract.md:57][AC].
  - The default cap of 100 hotels — [application.properties:16][AP] — means multi-city search only covers cached hotels. — [DestinationSearchRequest.java:9-13][DSR]; [SearchAggregationService.java:88-94][SAS].
  - Country "names" are just the ISO code. — [HotelbedsContentMapper.java:111-113][CM].
  - The destinations master is not imported, so the city list contains only destinations that have imported hotels. — [HotelbedsCatalogImporter.java:100-105][CI].

  *Fix:* use a streaming, page-by-page upsert (each page in its own transaction), staging tables plus a swap or a soft "seen in sync" marker, an incremental `lastUpdateTime` job, and an import of `/locations/countries,destinations`.
- **[MEDIUM] The IDENTITY id strategy defeats JDBC batching.** `HotelImage` and `HotelBooking` use `GenerationType.IDENTITY`. — [HotelImage.java:20-23][HI]; [HotelBooking.java:25-27][HBK]. `hibernate.jdbc.batch_size=200` is set. — [application.properties:27-29][AP]. Hibernate's reference guide says insert batching is disabled transparently for an identity identifier generator. — [Hibernate batch processing guide][HIB]. A Hibernate forum reply says this applies per entity type and that a sequence generator restores batching. — [Hibernate discourse][HIBD]. Note that the guide cited is an older JBoss EAP 5 edition. *Fix:* switch `HotelImage` to a pooled sequence, or use bulk `COPY`/JDBC batch for imports.
- **[MEDIUM] Several endpoints are unpaged or return large payloads.**
  - `/countries` and `/cities` are unpaged `findAll()`. — [StaticDataController.java:84-94][SDC]. This is a documented follow-up. — [CLAUDE.md:89][CMD].
  - `/hotels` allows up to 500 per page and returns the JPA entity, including the `@Lob` description. — [StaticDataController.java:96-111][SDC]; [Hotel.java:52-54][HOT]. The frontend asks for `size=200`. — [bodiala-web api.ts:67][WAPI].
  - The hotel detail view returns the entity directly. — [HotelView.java:10-15][HV].

  *Fix:* use DTO projections, pagination or type-ahead for cities, HTTP caching headers or a CDN for static data, and text search on destinations.
- **[MEDIUM] Availability has no caching or result paging.** Every search goes upstream. — [SearchAggregationService.java:125-156][SAS]. All rooms and rates for every hotel are returned in one response. — [HotelbedsSearchMapper.java:35-45][SM]. Hotelbeds certification checks correct display "including … pagination". — [Hotelbeds certification §3.1][HBCERT]. *Fix:* use a short-TTL cache (minutes) keyed by the normalised request, server-side result paging and sorting, and trimmed per-hotel payloads (min price plus lazy room list).
- **[LOW] There is a race in the on-demand content cache.** `deleteByHotelCode` followed by `saveAll` without a lock duplicates images under concurrent first views. — [HotelbedsContentService.java:101-111][CS]. *Fix:* use an upsert keyed on (hotel_code, position) or a per-hotel lock.
- **[LOW] The multi-city cap keeps an arbitrary subset.** Unordered codes are truncated to 2000. — [SearchAggregationService.java:89-94][SAS]; [HotelRepository.java:19-20][HR]. *Fix:* order by relevance or rating and chunk into multiple calls.
- **[LOW] Amenities are stored as one comma-joined string.** — [HotelbedsContentMapper.java:84-90][CM]; [PropertyAmenity.java:21-23][PA]. A name that contains a comma would be split wrongly. *Fix:* use a child table.
- **[LOW] Supplier calls run sequentially.** The provider loop is sequential. — [SearchAggregationService.java:135-151][SAS]. That doesn't matter with one supplier, but a second supplier would add latency. *Fix:* fan out in parallel with timeouts when a second supplier is added.

### Inferences
- The connection-pool exhaustion vector (HTTP inside a transaction, reachable anonymously, §2) is likely the first scalability failure under real traffic, before any query-level N+1.

### Gaps
- No load or performance measurements were possible under the read-only constraint.

## 8. Other issues a senior engineer would flag before go-live

### Takeaway
Repo hygiene and legacy debt are minor individually but add up: unused RezLive-era dependencies, a committed H2 database file, stale comments and docs, a provider abstraction carrying RezLive-shaped response models, and missing legal and compliance scaffolding for consumer bookings.

### Cited Findings
- **[LOW] Unused dependencies remain.** `commons-csv` and JAXB ("for the RezLive XML API") are still declared. — [build.gradle:30-35][BG]. A grep of `src/` for `org.apache.commons.csv` and `jakarta.xml.bind` found no usages. *Fix:* remove them to reduce attack surface and image size.
- **[LOW] A binary database is committed to git.** `data/bodiala.mv.db` and `data/bodiala.lock.db` (H2 files) are tracked since the first commit, and `.gitignore` does not exclude `data/`. — [.gitignore:1-42][GI]. CLAUDE.md says "No H2 anywhere". — [CLAUDE.md:31][CMD]. *Fix:* remove them from the repo (and from history if they contain real data) and ignore `data/`.
- **[LOW] Comments and docs are stale.** docker-compose still references "the RezLive stub". — [docker-compose.yml:11][DC]. `.gitignore` references RezLive creds and CSVs. — [.gitignore:5-8][GI]. `HotelbedsProperties` calls Hotelbeds "the second supplier … Active only when `hotel.provider=hotelbeds`". — [HotelbedsProperties.java:5-11][HP]. The `ImportResult` javadoc talks about master files. — [ImportResult.java:6-9][IR]. `AGENTS.md` is untracked and has diverged from `CLAUDE.md` (missing the 409 cancel mapping). *Fix:* clean these up in one docs PR.
- **[LOW] The neutral models still have a RezLive shape.** `RateCheckResult.PriceDetails` has `bookingBeforePrice/agentBalance/agentCurrency` and `preBookingRequest.preBooking` nesting. — [RateCheckResult.java:10-37][RCR]. `bookingCode` is always null. — [CLAUDE.md:79][CMD]. *Fix:* when adding the quote model (§2), introduce a clean v2 API contract for B2C, versioned under `/api/v2`, and retire the RezLive-shaped records.
- **[MEDIUM] Consumer and legal scaffolding is absent from the backend.** There is no acceptance record for terms or cancellation policy at booking time, no consent fields, no data retention or erasure, no tax and fee disclosure (taxes are unmapped, §4), and no voucher or e-mail sending capability (no mail dependency in [build.gradle:21-42][BG]). *Fix:* store the terms version accepted, the policy snapshot and the rate comments acknowledged. Add a transactional e-mail service. Define GDPR retention.
- **[LOW] The single-provider abstraction adds weight.** Registries, `?provider=` and aggregation are retained for a hypothetical second supplier. — [CLAUDE.md:11][CMD]; [ProviderRegistry.java:17-53][PR]. The surface is harmless but larger. Note that `?provider=` is a public parameter, and `ProviderId.from` returns "No enum constant …" text through the 400 handler. — [ProviderId.java:15-20][PID]; [ApiExceptionHandler.java:25-28][AEH]. *Fix:* keep it, but hide `provider` from the public API (choose server-side).

### Inferences
- Suggested sequencing for a B2C launch backlog, based on the dependencies above:
  1. Security baseline and Flyway.
  2. Quote, booking state machine and contact/guest model.
  3. Payments with reservation-before-call, plus reconciliation.
  4. Hotelbeds certification items (rate comments, voucher, children, sourceMarket, CheckRate usage) and mTLS.
  5. Content sync job and caching.
  6. Observability, CI/CD and backups.
  7. Stub fault injection and booking-path tests (run in parallel throughout).

### Gaps
- The contents of the tracked H2 file and the values in the local `.env` were deliberately not inspected. Whether they hold real or personal data is unknown.

[BG]: file:///C:/Users/user/IdeaProjects/bodiala/build.gradle
[AP]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/resources/application.properties
[APS]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/resources/application-hotelbeds-stub.properties
[TAP]: file:///C:/Users/user/IdeaProjects/bodiala/src/test/resources/application.properties
[DC]: file:///C:/Users/user/IdeaProjects/bodiala/docker-compose.yml
[DEV]: file:///C:/Users/user/IdeaProjects/bodiala/dev.ps1
[GI]: file:///C:/Users/user/IdeaProjects/bodiala/.gitignore
[ENV]: file:///C:/Users/user/IdeaProjects/bodiala/.env.example
[CMD]: file:///C:/Users/user/IdeaProjects/bodiala/CLAUDE.md
[AC]: file:///C:/Users/user/IdeaProjects/bodiala/docs/hotelbeds/api-contract.md
[MP]: file:///C:/Users/user/IdeaProjects/bodiala/docs/hotelbeds/multi-provider-plan.md
[APP]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/BodialaApplication.java
[DOT]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/config/DotEnvEnvironmentPostProcessor.java
[OAC]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/config/OpenApiConfig.java
[AEH]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/web/ApiExceptionHandler.java
[BC]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/booking/web/BookingController.java
[HBK]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/booking/HotelBooking.java
[BKR]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/booking/BookRequest.java
[BRM]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/booking/BookRoom.java
[GM]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/booking/GuestModel.java
[BPI]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/BookingProvider.java
[BPR]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/BookingProviderRegistry.java
[PR]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/ProviderRegistry.java
[PID]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/ProviderId.java
[BP]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/hotelbeds/HotelbedsBookingProvider.java
[JC]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/hotelbeds/HotelbedsJsonClient.java
[SP]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/hotelbeds/HotelbedsSearchProvider.java
[SM]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/hotelbeds/HotelbedsSearchMapper.java
[BM]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/hotelbeds/HotelbedsBookingMapper.java
[CI]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/hotelbeds/HotelbedsCatalogImporter.java
[CS]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/hotelbeds/HotelbedsContentService.java
[FC]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/hotelbeds/HotelbedsFacilityCatalog.java
[CM]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/hotelbeds/HotelbedsContentMapper.java
[SI]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/hotelbeds/HotelbedsStartupImporter.java
[HP]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/hotelbeds/HotelbedsProperties.java
[SIG]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/hotelbeds/HotelbedsSignature.java
[AR]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/hotelbeds/dto/AvailabilityResponse.java
[CR]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/hotelbeds/dto/CheckRateResponse.java
[BR]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/hotelbeds/dto/BookingResponse.java
[SR]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/model/SearchResult.java
[RCR]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/provider/model/RateCheckResult.java
[SAS]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/search/SearchAggregationService.java
[DSR]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/search/DestinationSearchRequest.java
[RR]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/search/RoomRequest.java
[SDC]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/staticdata/web/StaticDataController.java
[HV]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/staticdata/web/HotelView.java
[HCS]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/staticdata/HotelCoverService.java
[IR]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/staticdata/ImportResult.java
[HI]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/staticdata/domain/HotelImage.java
[HOT]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/staticdata/domain/Hotel.java
[PA]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/staticdata/domain/PropertyAmenity.java
[HIR]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/staticdata/repo/HotelImageRepository.java
[HR]: file:///C:/Users/user/IdeaProjects/bodiala/src/main/java/com/miirphys/bodiala/staticdata/repo/HotelRepository.java
[BCT]: file:///C:/Users/user/IdeaProjects/bodiala/src/test/java/com/miirphys/bodiala/provider/hotelbeds/HotelbedsBookingProviderCancelTest.java
[BMT]: file:///C:/Users/user/IdeaProjects/bodiala/src/test/java/com/miirphys/bodiala/provider/hotelbeds/HotelbedsBookingMapperTest.java
[JCT]: file:///C:/Users/user/IdeaProjects/bodiala/src/test/java/com/miirphys/bodiala/provider/hotelbeds/HotelbedsJsonClientTest.java
[SMT]: file:///C:/Users/user/IdeaProjects/bodiala/src/test/java/com/miirphys/bodiala/provider/hotelbeds/HotelbedsSearchMapperTest.java
[BAT]: file:///C:/Users/user/IdeaProjects/bodiala/src/test/java/com/miirphys/bodiala/BodialaApplicationTests.java
[STC]: file:///C:/Users/user/IdeaProjects/hotelbeds-stub/src/main/java/com/miirphys/hotelbedsstub/StubController.java
[SBS]: file:///C:/Users/user/IdeaProjects/hotelbeds-stub/src/main/java/com/miirphys/hotelbedsstub/StubBookingStore.java
[STT]: file:///C:/Users/user/IdeaProjects/hotelbeds-stub/src/test/java/com/miirphys/hotelbedsstub/StubControllerTest.java
[WSS]: file:///C:/Users/user/IdeaProjects/bodiala-web/src/searchState.ts
[WBD]: file:///C:/Users/user/IdeaProjects/bodiala-web/src/components/BookHotelDialog.tsx
[WAPI]: file:///C:/Users/user/IdeaProjects/bodiala-web/src/api.ts
[VITE]: file:///C:/Users/user/IdeaProjects/bodiala-web/vite.config.ts
[HBCERT]: https://developer.hotelbeds.com/documentation/hotels/knowledge-base/
[HBBP]: https://developer.hotelbeds.com/documentation/hotels/knowledge-base/best-practices/
[SBDI]: https://docs.spring.io/spring-boot/how-to/data-initialization.html
[HIB]: https://docs.redhat.com/en/documentation/JBoss_Enterprise_Application_Platform_Common_Criteria_Certification/5/html/hibernate_core_reference_guide/batch
[HIBD]: https://discourse.hibernate.org/t/why-cant-hibernate-do-batch-inserts-when-i-using-identity-generators-mysql8/10200
