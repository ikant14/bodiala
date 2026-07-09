# Hotelbeds APItude — build-ready contract (research 2026-07-09)

> Source of truth for `com.miirphys.bodiala.provider.hotelbeds`, extracted from Hotelbeds' official
> APItude OpenAPI 3.0 specs + developer docs. Money fields are **strings** unless noted; wire dates
> are ISO `yyyy-MM-dd` (NOT RezLive's `dd/MM/yyyy`). Items marked UNVERIFIED need confirming against
> the live test key.

## 0. Transport / auth / status
- **Base (test):** `https://api.test.hotelbeds.com/hotel-api/1.0` (booking) · `.../hotel-content-api/1.0` (content).
- **Base (prod):** legacy `https://api.hotelbeds.com/...`; current go-live requires **mTLS** at
  `https://api-mtls.hotelbeds.com/...` (client cert). Confirm host + cert with the account manager.
- **X-Signature (CONFIRMED):** lowercase-hex `SHA-256(apiKey + secret + unixSeconds)`, regenerated per request.
  Vector: `sha256("apiKey"+"secret"+"1500000000") = 737588110177fbb75b52721ce2ff7995d53cd195cc454353ba98511048a7be91`.
- **Headers:** `Api-key`, `X-Signature`, `Accept: application/json`, `Accept-Encoding: gzip` (responses gzipped),
  `Content-Type: application/json` (POST). We omit `Accept-Encoding` so responses come back uncompressed.
- **Rate limits:** daily eval quota exceeded → **403** (flat error); burst/QPS → **429**. `Retry-After` header UNVERIFIED.
- **`GET /status`** → `{"auditData":{...},"status":"OK"}`. Use to smoke-test auth + clock.

## 1. Availability — `POST /hotels`
Request: `stay{checkIn,checkOut}` (ISO), `occupancies[]{rooms,adults,children,paxes[]{type:AD|CH,age}}`,
one-of `hotels{hotel:[int codes ≤2000]}` / `destination{code,zones[]}` / `geolocation{lat,lon,radius,unit}`,
`language`, `sourceMarket`, optional `filter{maxRatesPerRoom,paymentType,...}`, `boards{included,board[]}`.
Response: `hotels.hotels[]{code(int),name,categoryCode,destinationCode,currency,minRate,maxRate,
rooms[]{code,name,rates[]{rateKey,rateClass(NOR|NRF),rateType(BOOKABLE|RECHECK),net,sellingRate?,
allotment?,paymentType(AT_WEB|AT_HOTEL),boardCode,boardName,rooms,adults,children,childrenAges?,
cancellationPolicies[]{amount,from(ISO tz)},taxes?,dailyRates?}}}}`. `rateKey` is opaque — never parse.

## 2. CheckRates — `POST /checkrates`
Mandatory only when a rate's `rateType == RECHECK`; `BOOKABLE` rates book directly. Up to 10 rateKeys.
Request: `{rooms:[{rateKey}],upselling?,language?}`. Response: `hotel{...,rooms[]{rates[]{rateKey(refreshed),
net,sellingRate,rateComments,cancellationPolicies[],...}}],totalNet,currency}`. ⚠ VERIFY singular `hotel`
wrapper (schema says `hotels`).

## 3. Booking — `POST /bookings`
Request: `{holder{name,surname},rooms[]{rateKey,paxes[]{roomId,type:AD|CH,name,surname,age(CH)}}},
clientReference(≤20, REQUIRED),remark?,tolerance(%, default 2),paymentData?{paymentCard,contactData}}`.
`paymentData` required for AT_HOTEL / credit-card accounts. Response: `booking{reference("1-4075658"),
clientReference,status(CONFIRMED|PRECONFIRMED|CANCELLED),holder,hotel{code,name,rooms[],totalNet(str),
currency,supplier},totalNet(number),pendingAmount(number),currency,modificationPolicies}`. Instant confirm.
**No server dedup on clientReference** — keep bodiala's `agent_ref_no` pre-call lookup.

## 4. Get booking — `GET /bookings/{reference}`
Same `booking` shape as create. Unknown/foreign → 404 `INVALID_DATA`.

## 5. Cancel — `DELETE /bookings/{reference}?cancellationFlag=CANCELLATION`
`SIMULATION` = dry-run fee quote. Response: `booking{status:CANCELLED,cancellationReference,
hotel{cancellationAmount(number, 0=free),rooms[]{rates[]{net}}}}`.

## 6. Errors (custom handler)
Two shapes — branch on `error` being string vs object:
- **Flat (gateway):** `{"error":"Request signature verification failed"}` — 401 (sig/key), 403 (forbidden/quota), 429.
- **Nested (app):** `{"auditData":{token},"error":{"code":"CATEGORY-TOKEN","message":"..."}}` — 400/404/409/500.
Categories: INVALID_REQUEST, INVALID_DATA, SYSTEM_ERROR, PRODUCT_ERROR, CONFIGURATION_ERROR.
409 = already-cancelled (treat idempotently on cancel). 410 = replay of a request that failed in last 15 min.

## 7. Content API (later) — `/hotel-content-api/1.0`
`GET /hotels?fields=all&language=ENG&from=1&to=1000&lastUpdateTime=…` (≤1000/page). Fields `name/description/
address/city` are `{content,languageCode}`; `coordinates{longitude,latitude}`; `code`/`giataCode` ints.
Detail: `GET /hotels/{codes}/details` (codes in path). Master data: `/locations/{countries,destinations}`,
`/types/{facilities,categories,boards,chains}`. Images: `https://photos.hotelbeds.com/giata/{size}/{path}`
(sizes: bigger/xl/xxl/original/small/medium; `path` from `images[].path`).

## 8. Mapping → existing RezLive-shaped result (until neutralised)
| Neutral/RezLive target | Hotelbeds source |
|---|---|
| session id | `""` (Hotelbeds is stateless) |
| per-room `bookingKey` | `rate.rateKey` + `"#" + rate.rateType` (strip suffix before wire; decides checkrates) |
| displayed `totalRate` | `rate.sellingRate` else `rate.net` |
| hotel id | `hotel.code` (int → string) |
| dates | ISO both ways — NO `dd/MM/yyyy` conversion |
| prebook | `POST /checkrates` only when `#RECHECK`; else echo |
| BookingId | `booking.reference` (single id; no BookingCode) |
| agentRefNo | request `clientReference` (≤20; not server-deduped) |
| cancel result | `booking.cancellationReference` + `hotel.cancellationAmount` |

## 9. UNVERIFIED (confirm on live test key)
CheckRates `hotel` vs `hotels` wrapper; clientReference dedup; 429 body + Retry-After; 410 body;
`cancellationPolicies[].percent` ever populated; prod host (mTLS); `/status` exact envelope; numeric
`hotel.code` identity across Booking vs Content APIs.
