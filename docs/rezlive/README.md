# RezLive static-data integration

This module ingests RezLive's **static (master) data** into a local cache and exposes it over
REST, plus a client for RezLive's live per-hotel content action. See
[`api-reference.md`](api-reference.md) for the documented RezLive API contract.

## What "static data" means for RezLive

RezLive delivers static data two ways:

1. **Bulk master files (CSV)** — Country, City, Hotel Details, Hotels Images, Property Amenities,
   Room Amenities. These are **downloaded manually** from RezLive's credential + IP-whitelisted
   test panel. There is **no documented programmatic download URL**, so this app *ingests* CSVs you
   place in a folder — it does not pull them over the wire.
2. **`gethoteldetails`** — a live XML API action returning full per-hotel content by hotel id. This
   one *is* fetched over the API (needs credentials + a whitelisted IP).

## Configuration (`application.properties` / env vars)

| Property | Env var | Purpose |
|---|---|---|
| `rezlive.base-url` | `REZLIVE_BASE_URL` | Action base URL (default: test panel) |
| `rezlive.agent-code` | `REZLIVE_AGENT_CODE` | `<AgentCode>` in the XML body |
| `rezlive.user-name` | `REZLIVE_USER_NAME` | `<UserName>` in the XML body |
| `rezlive.api-key` | `REZLIVE_API_KEY` | `x-api-key` HTTP header |
| `rezlive.static-data-dir` | `REZLIVE_STATIC_DIR` | Folder holding the downloaded CSV master files |

The local cache runs on **PostgreSQL** in Docker (`docker compose up -d`, see `docker-compose.yml`);
`dev.ps1` starts it automatically. The schema is auto-created on first boot
(`spring.jpa.hibernate.ddl-auto=update`). Point at a managed Postgres in production by setting
`SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`. The test suite
hits the same Postgres in an isolated `bodiala_test` schema, so `docker compose up -d` must be running
for `./gradlew test` too. There is no H2 in the project.

## Interactive API docs (Swagger UI)

With the app running, open **http://localhost:8080/swagger-ui.html** to try every endpoint from the
browser. The raw OpenAPI 3.1 spec is at `/v3/api-docs` (JSON) and `/v3/api-docs.yaml`.

## Ingesting the master files

1. Download the six CSV master files from the RezLive test panel.
2. Drop them into `rezlive.static-data-dir` (default `./data/rezlive`). Filenames are matched by
   keyword: `country`, `city`, `detail` (hotel-details), `image`, `propert`, `room`.
3. **They import automatically on startup** — `rezlive.import-on-startup=true` (default) seeds the
   cache when the directory exists and the cache is empty (fresh DB). Restarts with data already
   present are skipped, so there's no wasteful re-import. Set `rezlive.import-on-startup=false` to
   opt out.
4. To (re-)import on demand — e.g. after refreshing the CSVs — call the endpoint:

```bash
curl -X POST http://localhost:8080/api/static-data/import
# or point at a specific folder:
curl -X POST "http://localhost:8080/api/static-data/import?dir=/path/to/csvs"
```

Each master file is treated as a **full snapshot** — its table is fully replaced on every import,
so re-imports are idempotent. (RezLive publishes no delta/incremental feed.)

## Querying the cache

```
GET /api/static-data/countries
GET /api/static-data/cities?country=AE
GET /api/static-data/hotels?city=968&page=0&size=50
GET /api/static-data/hotels?country=AE
GET /api/static-data/hotels/{hotelCode}      # hotel + images + amenities
```

## Live per-hotel content

```
GET /api/hotel-content/{hotelId}             # single, e.g. XHUB18
GET /api/hotel-content?ids=XHUB18,XHUB19     # batch
```

Returns HTTP **503** until `rezlive.agent-code` / `user-name` / `api-key` are set and your IP is
whitelisted with RezLive.

## Known open questions (verify against real data / RezLive support)

- **CSV layout is unverified.** RezLive documents the *fields* of each master file but not column
  order, delimiter, header row, or encoding. The importer is header-aware (maps by column name) and
  falls back to the documented column order — but confirm against a real downloaded file.
- **Join-key ambiguity.** The CSV `HotelCode` is numeric; `findhotel`/`findhotelbyid` use numeric
  ids; but `gethoteldetails` uses an **alphanumeric** `HotelId` (e.g. `XHUB18`). The docs never state
  that CSV `HotelCode` equals the runtime `HotelId`. Confirm the mapping before wiring content lookups
  to cached hotels.
- **Refresh cadence** is undocumented — decide a re-import schedule with RezLive support.
- **JSON** is marketing-only; the client is built for XML.

## Deferred engineering items (known, not yet done)

These were surfaced by an adversarial code review and consciously deferred until we have a real CSV
and known data volumes — they are scale/format-dependent, not correctness bugs on the current path:

- **Bulk-insert performance (N+1 merge).** Master entities use assigned natural-key ids, so Spring
  Data `saveAll` issues a `SELECT`-before-insert per row. Fine at small scale; for the full hotel
  master, switch to `EntityManager.persist` (rows are genuinely new after the delete-all) or a JDBC
  batch insert.
- **Duplicate natural keys collapse silently (last-wins).** If a real file has repeated keys, or if
  amenities turn out to be one-row-per-amenity, rows merge without warning. Revisit the amenity model
  once a real file is seen.
- **Whole file loaded into memory.** `CsvMasterFileReader` returns a full `List`; a multi-million-row
  images master could OOM. Move to a streaming/chunked reader (flush+clear per N rows) for large files.
- **All-or-nothing transaction.** One bad file rolls back the whole import (each file is validated to
  reduce this). If partial refresh is preferred, give each file its own `REQUIRES_NEW` transaction.
- **`GET /api/static-data/cities` (no filter) is unbounded** — returns the full city master in one
  array. Paginate it like `/hotels` if the city master is large.

## Running

```powershell
# Java 25 toolchain required (see repo root notes).
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
.\gradlew.bat bootRun     # starts on :8080
.\gradlew.bat test        # unit + integration tests
```
