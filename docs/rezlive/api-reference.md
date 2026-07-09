# RezLive.com (XMLHUB / RezTez) B2B Travel API — Reference

## Provenance & confidence

- **Single source.** Everything in this file was extracted from the public RezLive GitBook at
  `https://rezlive.gitbook.io/rezlive-api-doc/` (raw markdown obtained by appending `.md` to each
  page URL). There is no second corroborating source; this is a single-source reference.
- **Confidence.** XML request/response examples, endpoint URLs, and the `<Authentication>` block were
  captured with high fidelity (reproduced from the pages' fenced code blocks). Field/column tables were
  reproduced as documented, preserving exact spellings including known misspellings
  (e.g. `HotelPostelCode` in the Hotel Details CSV, vs `HotelPostalCode` in the gethoteldetails XML).
- **CSV layout caveat.** The GitBook documents the *fields* of each master file but does NOT show real
  sample rows, delimiters, header row, quoting, or encoding for the CSV downloads. **The actual
  downloaded CSV file layout may still differ from what is documented here** — verify against a real
  downloaded file before relying on column order/format.
- **"not documented"** appears wherever the GitBook is silent on a point; those are genuine gaps, not
  omissions in this transcription.
- Captured 2026-07-07. Docs reference test-environment (`test.xmlhub.com`) endpoints throughout.

---

## 1. HTTP request contract

Source: `https://rezlive.gitbook.io/rezlive-api-doc/request-urls.md`
and `https://rezlive.gitbook.io/rezlive-api-doc/sample-scripts.md`

**Transport (from the sample scripts):**

- **Method:** `POST`
- **Base URL pattern (test):** `http://test.xmlhub.com/testpanel.php/action/{action}`
  (plain HTTP in the docs; production/live URLs are **not documented** — per "Where to start", live
  credentials/URLs are issued only after certification).
- **Header — API key:** `x-api-key: XXXXXX` (required on every request).
- **Header — Content-Type:**
  - PHP sample sets the *response* header `Content-type: text/xml`; the request is sent as an HTTP POST
    with cURL `CURLOPT_POST` and body field `XML=`.
  - Java sample sets request property `Content-Type: text/html; charset=utf-8` and also sets
    `enctype: application/x-www-form-urlencoded`.
- **Additional headers seen in the Java sample:** `Connection: Keep-Alive`,
  `Accept-Language: en-US,en;q=0.5`.
- **Request body form-field name:** `XML=` followed by the URL-encoded XML payload
  (PHP: `"XML=" . urlencode($str)`).
- **URL-encoding:** the XML string is URL-encoded before being placed in the `XML=` field (PHP
  `urlencode()`). (In the Java sample the payload is written directly via `DataOutputStream.writeBytes`;
  the exact `urlParameters` construction — whether it prefixes `XML=` — is not shown verbatim on the page.)
- **Response compression:** responses are **gzip-compressed**; the PHP sample decodes with
  `gzdecode($result)`.

**Endpoint / action URLs (Request URLs page):**

| API Function | Endpoint |
|---|---|
| Hotel Search By ID | `http://test.xmlhub.com/testpanel.php/action/findhotelbyid` |
| Hotel Search By Geo (destination) | `http://test.xmlhub.com/testpanel.php/action/findhotel` |
| Hotel Pre-Book | `http://test.xmlhub.com/testpanel.php/action/prebook` |
| Hotel Booking | `http://test.xmlhub.com/testpanel.php/action/bookhotel` |
| Hotel Booking Cancellation | `http://test.xmlhub.com/testpanel.php/action/cancelhotel` |
| Get Cancellation Policy | `http://test.xmlhub.com/testpanel.php/action/getcancellationpolicy` |
| Get Booked Hotel Details | `http://test.xmlhub.com/testpanel.php/action/getbookingdetails` |
| Get Hotel Detail | `http://test.xmlhub.com/testpanel.php/action/gethoteldetails` |
| Get Cancellation Policy After Booking | `http://test.xmlhub.com/testpanel.php/action/getCancellationPolicyAfterBooking` |
| Get Hotel Confirmation Detail | `http://test.xmlhub.com/testpanel.php/action/getConfirmationDetails` |

The Request URLs page does not itself state HTTP methods; the method (POST) comes from the sample scripts.

---

## 2. Authentication (in-body block)

Source: `sample-scripts.md`, `get-hotel-detail.md`, `search-by-destination.md`, `search-by-hotel-id.md`,
`where-to-start.md`

Every request XML carries an in-body `<Authentication>` element with **exactly two child fields**:

```xml
<Authentication>
  <AgentCode>XXXXX</AgentCode>
  <UserName>XXXXX</UserName>
</Authentication>
```

- `AgentCode` — unique agent/user code assigned by XMLHUB (the "Where to start" page refers to this as
  the "User Code": a distinctive identifier assigned for quick user recognition).
- `UserName` — alphanumeric username (self-generated at registration), required in all server queries.
- **The API key / password is NOT placed in the body.** It is passed only via the `x-api-key` HTTP
  header. The "Where to start" page explicitly notes: *"Password should not be added in the request body."*

Credentials required for API access (per "Where to start"):
1. **User Code** — assigned identifier.
2. **User name** — alphanumeric string required in all server queries.
3. **x-api-key** — passed in the request header.

Test-environment registration URL:
`https://alpha.rezlive.com/common/index/action/agentregistration`

---

## 3. Sample Scripts (transport + code)

Source: `https://rezlive.gitbook.io/rezlive-api-doc/sample-scripts.md`

The page provides PHP, Java, and .NET (C#) examples that POST to `.../action/findhotel`.

> Note: the GitBook's raw markdown could only be extracted through a summarizing fetch layer that would
> not emit the whole page byte-for-byte. The code below is reconstructed line-by-line from that layer and
> is faithful to the documented calls/strings, but is **not guaranteed byte-perfect verbatim**. Treat the
> transport facts (header names, `XML=` field, `urlencode`, `gzdecode`, Content-Type strings) as
> high-confidence; treat exact whitespace/ordering as indicative.

**PHP sample:**

```php
set_time_limit(0);
$str = "<HotelFindRequest>...</HotelFindRequest>";
file_put_contents("xml/resquest".time().".xml", $str);
$url = "http://test.xmlhub.com/testpanel.php/action/findhotel";
$headers = array('x-api-key: XXXXXX');
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_POST, 1);
curl_setopt($ch, CURLOPT_POSTFIELDS, "XML=" . urlencode($str));
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
$result = curl_exec($ch);
curl_close($ch);
header("Content-type: text/xml");
print_R(gzdecode($result));
```

- PHP POST body field: `XML=` (with `urlencode($str)`).
- Response decoded via `gzdecode()` (gzip).

**Java sample:**

```java
String urlParameters = "<HotelFindRequest>...</HotelFindRequest>";
String url = "http://test.xmlhub.com/testpanel.php/action/findhotel";
URL obj = new URL(url);
HttpURLConnection con = (HttpURLConnection) obj.openConnection();
con.setRequestMethod("POST");
con.setDoInput(true);
con.setDoOutput(true);
con.setUseCaches(false);
con.setRequestProperty("Content-Type", "text/html; charset=utf-8");
con.setRequestProperty("Connection", "Keep-Alive");
con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
con.setRequestProperty("enctype", "application/x-www-form-urlencoded");
DataOutputStream wr = new DataOutputStream(con.getOutputStream());
wr.writeBytes(urlParameters);
wr.flush();
wr.close();
int responseCode = con.getResponseCode();
BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
// ... read response ...
```

- The class in the sample is named `HttpURLConnectionExample` with `main()` / `sendPost()` methods and
  sets the `x-api-key` header via `con.setRequestProperty("x-api-key", apiKey)`.
- Java request Content-Type: `text/html; charset=utf-8`; `enctype: application/x-www-form-urlencoded`.

**.NET (C#)** is listed as available and uses `HttpContext.Current.Server.UrlEncode()` for URL-encoding
the XML; the full C# body was not reproduced on the page beyond that detail.

---

## 4. Static Data — Master Files (overview)

Source: `https://rezlive.gitbook.io/rezlive-api-doc/static-data-master-files.md`

- Master records are provided as **CSV files** ("Master records (CSV files)").
- Files cover: **Countries, Cities, Hotels, Rooms information** (plus hotel images and amenities — see
  the per-file pages below).
- **Access method:** obtained through a **test-panel URL** using assigned credentials. The page states
  **"IP Authentication is required"** — you must supply your server/dev-site IP address for access
  (IP whitelisting).
- **Scriptable download URL:** **not documented.** The page describes accessing files "from the following
  test panel URL" but does NOT publish a direct, stable, programmatic download link. As documented, access
  is via the web test panel gated by credentials + IP whitelisting; there is no documented stable
  authenticated file URL for automated/scripted pulls.
- **Refresh cadence / delta / incremental updates:** **not documented.** The page says nothing about
  update frequency, delta files, incremental sync, or versioning/changelog for the master data.

### 4.1 Country
Source: `https://rezlive.gitbook.io/rezlive-api-doc/static-data-master-files/country.md`

| Attributes  | Data Type | Description |
| ----------- | --------- | ----------- |
| Name        | String    | Name of the country |
| CountryCode | String    | Country Code for that particular country. It's a unique two-letter ISO code of each country. "AE" for United Arab Emirates |

### 4.2 City
Source: `https://rezlive.gitbook.io/rezlive-api-doc/static-data-master-files/city.md`

| Attributes  | Data Type | Description |
| ----------- | --------- | ----------- |
| City        | String    | `<City>` tag is a unique 3-5 letter (Numeric) code for each city |
| Name        | String    | Name of the city |
| CountryCode | String    | Country Code for that particular Country. It's a unique two-letter ISO code of each country. "AE" for United Arab Emirates |

> Documented type is **String**, described as a *"unique 3-5 letter (Numeric) code"*. Observed example
> values in the request XML are numeric (e.g. `<City>968</City>`).

### 4.3 Hotel Details
Source: `https://rezlive.gitbook.io/rezlive-api-doc/static-data-master-files/hotel-details.md`

| Attributes      | Data Type | Description |
| --------------- | --------- | ----------- |
| HotelCode       | Int       | A unique hotel code for each individual hotel |
| Name            | String    | Name of the hotel |
| City            | String    | City in which that hotel is located. `<City>` tag is a unique 3-5 letter (Numeric) code for each city |
| CountryCode     | String    | Country Code for that particular country. It's a unique two-letter ISO code of each country. "AE" for United Arab Emirates |
| Rating          | Int       | Star rating for that particular hotel. Ranges from 1 to 5. |
| HotelAddress    | String    | Address of that hotel |
| HotelPostelCode | String    | Postal Code as per hotel location |
| Latitude        | Double    | Latitude Coordinates of that hotel location |
| Longitude       | Double    | Longitude Coordinates of that hotel location |
| Desc            | Text      | Description tag include information about hotel |

> Note the documented misspelling **`HotelPostelCode`** (not "HotelPostalCode"). The runtime
> gethoteldetails response uses the correctly-spelled `HotelPostalCode` — they are different spellings in
> the two documents.

### 4.4 Hotels Images
Source: `https://rezlive.gitbook.io/rezlive-api-doc/static-data-master-files/hotels-images.md`

| Attributes | Data Type | Description |
| ---------- | --------- | ----------- |
| HotelCode  | Int       | A unique hotel code for each individual hotel |
| Image      | String    | File path for the image to be uploaded for that particular hotel |

### 4.5 Property Amenities
Source: `https://rezlive.gitbook.io/rezlive-api-doc/static-data-master-files/property-amenities.md`

| Attributes     | Data Type | Description |
| -------------- | --------- | ----------- |
| HotelCode      | Int       | A unique hotel code for each individual hotel |
| HotelAmenities | String    | Consist a list of all the hotel amenities available |

### 4.6 Room Amenities
Source: `https://rezlive.gitbook.io/rezlive-api-doc/static-data-master-files/room-amenities.md`

| Attributes    | Data Type | Description |
| ------------- | --------- | ----------- |
| HotelCode     | Int       | A unique hotel code for each individual hotel |
| RoomAmenities | String    | Consist a list of all the room amenities available |

---

## 5. Search by Destination (findhotel)

Source: `https://rezlive.gitbook.io/rezlive-api-doc/search-by-destination.md`
Endpoint: `http://test.xmlhub.com/testpanel.php/action/findhotel`

**Request (0 children):**

```xml
<?xml version="1.0"?>
<HotelFindRequest>
  <Authentication>
    <AgentCode>XXXXX</AgentCode>
    <UserName>XXXXX</UserName>
  </Authentication>
  <Booking>
    <ArrivalDate>25/09/2023</ArrivalDate>
    <DepartureDate>26/09/2023</DepartureDate>
    <CountryCode>AE</CountryCode>
    <City>968</City>
    <GuestNationality>AE</GuestNationality>
    <HotelRatings>
      <HotelRating>1</HotelRating>
      <HotelRating>2</HotelRating>
      <HotelRating>3</HotelRating>
      <HotelRating>4</HotelRating>
      <HotelRating>5</HotelRating>
    </HotelRatings>
    <Rooms>
      <Room>
        <Type>Room-1</Type>
        <NoOfAdults>1</NoOfAdults>
        <NoOfChilds>0</NoOfChilds>
      </Room>
    </Rooms>
  </Booking>
</HotelFindRequest>
```

**Request (2 children)** — same shape, with:

```xml
<NoOfChilds>2</NoOfChilds>
<ChildrenAges>
  <ChildAge>4</ChildAge>
  <ChildAge>10</ChildAge>
</ChildrenAges>
```

**Success response:**

```xml
<?xml version="1.0"?>
<HotelFindResponse>
  <SearchSessionId>YWRhc2Rhc2Fkc2FkczEyMjMyc2ZzZA==</SearchSessionId>
  <ArrivalDate>25/10/2023</ArrivalDate>
  <DepartureDate>26/10/2023</DepartureDate>
  <Currency>AED</Currency>
  <GuestNationality>BE</GuestNationality>
  <Hotels>
    <Hotel>
      <Id>151641</Id>
      <Name>Shangri-la hotel dubai</Name>
      <Rating>5.00</Rating>
      <Price>1249.4664</Price>
      <RoomDetails>
        <RoomDetail>
          <Type>Deluxe Room, Sea View</Type>
          <BookingKey>RZHNiuMwEIRfRWeBjf4sOzoFhgkztz3ksFfZascsdf335ds3</BookingKey>
          <Adults>1</Adults>
          <Children>0</Children>
          <ChildrenAges>0</ChildrenAges>
          <TotalRooms>1</TotalRooms>
          <TotalRate>1251.07066583</TotalRate>
          <RoomDescription>Free High-speed Internet</RoomDescription>
          <BoardBasis>Room Only</BoardBasis>
          <TermsAndConditions/>
        </RoomDetail>
        <!-- ... additional RoomDetail entries ... -->
      </RoomDetails>
    </Hotel>
  </Hotels>
</HotelFindResponse>
```

**Error response:**

```xml
<?xml version="1.0"?>
<HotelFindResponse>
  <error>We're sorry, but it appears that the API key you provided in your XML request is invalid or incorrect. Please double-check the API key you're using to ensure it is accurate and IP's are whiteliste.</error>
</HotelFindResponse>
```

**Request fields:**

| Field | Data Type | Required |
|-------|-----------|----------|
| x-api-key (HTTP header) | String | Yes |
| AgentCode | String | Yes |
| UserName | String | Yes |
| ArrivalDate | Date (dd/MM/yyyy) | Yes |
| DepartureDate | Date (dd/MM/yyyy) | Yes |
| CountryCode | String | Yes |
| City | String (numeric city code) | Yes |
| GuestNationality | String | Yes |
| HotelRating (repeatable) | Integer | Yes |
| Type (Room) | String | Yes |
| NoOfAdults | Integer | Yes |
| NoOfChilds | Integer | Yes |
| ChildAge (repeatable) | Integer | Conditional — required if NoOfChilds > 0 |

**Response fields:**

| Field | Data Type |
|-------|-----------|
| SearchSessionId | String |
| ArrivalDate | Date |
| DepartureDate | Date |
| Currency | String |
| GuestNationality | String |
| Id (Hotel) | (numeric in examples, e.g. 151641) |
| Name | String |
| Rating | Decimal (e.g. 5.00) |
| Price | Double |
| Type (Room) | String |
| BookingKey | String |
| Adults | Integer |
| Children | Integer |
| ChildrenAges | String |
| TotalRooms | Integer |
| TotalRate | Double |
| RoomDescription | String |
| BoardBasis | String |
| TermsAndConditions | String |
| error | String (on failure) |

---

## 6. Search by Hotel Id (findhotelbyid)

Source: `https://rezlive.gitbook.io/rezlive-api-doc/search-by-hotel-id.md`
Endpoint: `http://test.xmlhub.com/testpanel.php/action/findhotelbyid`

**Request (0 children):**

```xml
<?xml version="1.0"?>
<HotelFindRequest>
  <Authentication>
    <AgentCode>XXXXX</AgentCode>
    <UserName>XXXXX</UserName>
  </Authentication>
  <Booking>
    <ArrivalDate>11/10/2023</ArrivalDate>
    <DepartureDate>12/10/2023</DepartureDate>
    <CountryCode>AE</CountryCode>
    <City>968</City>
    <HotelIDs>
      <Int>150884</Int>
      <Int>171888</Int>
      <Int>245325</Int>
      <Int>151754</Int>
      <Int>248860</Int>
    </HotelIDs>
    <GuestNationality>IN</GuestNationality>
    <Rooms>
      <Room>
        <Type>Room-1</Type>
        <NoOfAdults>2</NoOfAdults>
        <NoOfChilds>0</NoOfChilds>
      </Room>
    </Rooms>
  </Booking>
</HotelFindRequest>
```

> The 2-children example on the page spells the container `<HotelIds>` (mixed case) instead of
> `<HotelIDs>` — the docs use both casings. The hotel identifiers are numeric integers inside `<Int>`
> elements (e.g. `150884`).

**Success response:**

```xml
<?xml version="1.0"?>
<HotelFindResponse>
  <SearchSessionId>YWRhc2Rhc2FkczEyMjMyc2ZzZA==</SearchSessionId>
  <ArrivalDate>11/10/2023</ArrivalDate>
  <DepartureDate>12/10/2023</DepartureDate>
  <Currency>AED</Currency>
  <GuestNationality>IN</GuestNationality>
  <Hotels>
    <Hotel>
      <Id>150884</Id>
      <Name>Dusit thani dubai</Name>
      <Rating>5.00</Rating>
      <Price>825.931588664</Price>
      <RoomDetails>
        <RoomDetail>
          <Type>Deluxe Room, 1 King Bed</Type>
          <BookingKey>RZHLjuMgEEV_hTWSLco2fq0ijSbq3vUii9liU8QohEJA1PHfD-xsadd23fsdrw</BookingKey>
          <Adults>2</Adults>
          <Children>0</Children>
          <ChildrenAges>0</ChildrenAges>
          <TotalRooms>1</TotalRooms>
          <TotalRate>825.931588664</TotalRate>
          <RoomDescription>Free High-speed Internet</RoomDescription>
          <BoardBasis>Room Only No Breakfast</BoardBasis>
          <TermsAndConditions/>
        </RoomDetail>
      </RoomDetails>
    </Hotel>
  </Hotels>
</HotelFindResponse>
```

**Request fields (as documented):**

| Attribute | Data Type | Required | Notes |
|-----------|-----------|----------|-------|
| x-api-key (HTTP header) | String | Yes | Unique code assigned by XMLHUB |
| Authentication | Array | Yes | Contains AgentCode and UserName |
| AgentCode | String | Yes | Unique identifier by XMLHUB |
| UserName | String | Yes | Self-generated at registration |
| Booking | Array | Yes | Search criteria container |
| ArrivalDate | Date (dd/MM/yyyy) | Yes | |
| DepartureDate | Date (dd/MM/yyyy) | Yes | |
| HotelIDs | Array | Yes | Container for hotel identifiers |
| Int | Integer | Yes | Individual hotel ID (e.g. 150884) |
| CountryCode | String | Yes | Two-letter ISO (e.g. "AE") |
| City | String | Yes | numeric city code (e.g. 968) |
| GuestNationality | String | Yes | 2-letter nationality code |
| Rooms / Room | Array | Yes | Room container / entry |
| Type | String | Yes | Room type description |
| NoOfAdults | Integer | Yes | |
| NoOfChilds | Integer | Yes | |
| ChildrenAges / ChildAge | Array / Integer | Conditional | Required if NoOfChilds > 0 |

**Response fields:** identical set to Search by Destination
(`SearchSessionId, ArrivalDate, DepartureDate, Currency, GuestNationality, Hotels/Hotel, Id, Name,
Rating, Price, RoomDetails/RoomDetail, Type, BookingKey, Adults, Children, ChildrenAges, TotalRooms,
TotalRate, RoomDescription, BoardBasis, TermsAndConditions, error`).
Documented notes on the page: `TotalRooms` max 8; `TotalRate` for multiple rooms is separated by `|`;
`BookingKey` is the unique key used for booking/cancellation.

---

## 7. Get Hotel Detail (gethoteldetails)

Source: `https://rezlive.gitbook.io/rezlive-api-doc/get-hotel-detail.md`
Endpoint: `http://test.xmlhub.com/testpanel.php/action/gethoteldetails`

**Request:**

```xml
<?xml version="1.0"?>
<HotelDetailsRequest>
  <Authentication>
    <AgentCode>XXXXX</AgentCode>
    <UserName>XXXXX</UserName>
  </Authentication>
  <Hotels>
    <HotelId>XHUB18</HotelId>
  </Hotels>
</HotelDetailsRequest>
```

**Response:**

```xml
<?xml version="1.0"?>
<HotelDetailsResponse>
  <Hotels>
    <HotelId>XHBE9179</HotelId>
    <HotelName>Minotel Prince de Liege</HotelName>
    <Rating>3</Rating>
    <City>Brussels</City>
    <Country>Belgium</Country>
    <Location/>
    <Phone/>
    <Telephone/>
    <Fax/>
    <Email/>
    <Website/>
    <Description>The ideally located and recently renovated hotel includes facilities as 24-hour reception desk, shuttle service, tourist information office, business center, private garage, restaurant, airconditioned conferences/banquets facilities, free WiFi internet co</Description>
    <HotelAddress>CHAUSSEE DE NINOVE 664 Brussels</HotelAddress>
    <Latitude>50.8468</Latitude>
    <Longitude>4.29978</Longitude>
    <HotelPostalCode>1070</HotelPostalCode>
    <HotelAmenities>Bathroom,Shower,Hairdryer,Direct dial telephone,Satellite / cable TV,Internet access,Double bed,Individual heating,Reception area,24h check-in,24h. Reception,Lift-s,Bar-s,Conference room,Breakfast room,Restaurant -s,Restaurant - non-smoking area,Mobile ph</HotelAmenities>
    <RoomAmenities/>
    <Images>
      <Image>http://test.xmlhub.com/.../images?img=aHR0cDovL2ltYWdlLm1ldGdsb2JhbC5jb20v...</Image>
      <Image>http://test.xmlhub.com/.../images?img=aHR0cDovL2ltYWdlLm1ldGdsb2JhbC5jb20v...</Image>
    </Images>
  </Hotels>
</HotelDetailsResponse>
```

> **Identifier format here is alphanumeric**, unlike the numeric IDs used in findhotel/findhotelbyid:
> the request uses `<HotelId>XHUB18</HotelId>` and the response uses `<HotelId>XHBE9179</HotelId>`.

**Request fields:**

| Field | Data Type | Required |
|-------|-----------|----------|
| x-api-key (HTTP header) | String | Yes |
| Authentication (AgentCode, UserName) | — | Yes |
| Hotels / HotelId | String | Yes |

**Response fields:**

| Field | Data Type |
|-------|-----------|
| Hotels / HotelId | String (alphanumeric, e.g. XHBE9179) |
| HotelName | String |
| Rating | Integer |
| City | String (city NAME here, e.g. "Brussels") |
| Country | String (country NAME here, e.g. "Belgium") |
| Location | String |
| Phone | String |
| Telephone | String |
| Fax | String |
| Email | String |
| Website | String |
| Description | Text |
| HotelAddress | String |
| Latitude | String/Double |
| Longitude | String/Double |
| HotelPostalCode | String |
| HotelAmenities | String (comma-separated) |
| RoomAmenities | String (comma-separated) |
| Images / Image | Array of String (image proxy URLs) |

---

## 8. Cross-page notes & documented contradictions

### 8.1 City code — numeric or alphanumeric?
- **Documented type:** `String` (City master file page). Description: *"`<City>` tag is a unique
  **3-5 letter (Numeric)** code for each city."*
- **Example values:** numeric — `<City>968</City>` in both findhotel and findhotelbyid requests.
- **Conclusion:** Documented as a String field whose value is a 3–5 character numeric code. Treat it as a
  string carrying digits (e.g. `968`), NOT a free alphanumeric code. (One request-field annotation on the
  search-by-hotel-id page loosely calls it a "4-letter alphanumeric code," but the authoritative
  master-file description and all examples are numeric.)

### 8.2 CSV `HotelCode` vs runtime `HotelId`
- **CSV master files** (Hotel Details, Hotels Images, Property/Room Amenities): identifier is `HotelCode`,
  type **`Int`**.
- **findhotel / findhotelbyid** (search): use **numeric** hotel IDs — response `<Id>150884</Id>`,
  `<Id>151641</Id>`; request `<HotelIDs><Int>150884</Int>...`. These match the numeric/`Int` shape of the
  CSV `HotelCode`.
- **gethoteldetails** (Get Hotel Detail): uses an **alphanumeric** `<HotelId>` — request `XHUB18`,
  response `XHBE9179` (an internal "XHUB…"/"XH…" prefixed code).
- **What the docs actually say:** The GitBook **never explicitly states that the CSV `HotelCode` equals
  the runtime search `HotelId`.** It is left implicit. The numeric IDs used in search line up with the
  `Int` CSV `HotelCode` by type, but the gethoteldetails example uses a *different, alphanumeric*
  identifier format (`XHUB18` / `XHBE9179`) that does **not** match the numeric search IDs. So the docs
  are internally inconsistent: **there is no documented statement equating the CSV HotelCode to the
  gethoteldetails HotelId**, and the two search-family endpoints (numeric) and gethoteldetails
  (alphanumeric) use visibly different identifier schemes. **Relationship = not documented; verify against
  live data.**

### 8.3 Master file downloads — scriptable URL or manual panel?
- Documented as accessed **via the test-panel URL** with **assigned credentials + IP whitelisting**
  ("IP Authentication is required").
- **No stable, published, scriptable download endpoint is documented.** As written, retrieval is a
  credential/IP-gated web test panel, not a documented authenticated file API. **A stable scriptable
  download URL = not documented.**

### 8.4 Refresh cadence / delta / incremental updates
- **Not documented anywhere.** The Static Data page and per-file pages say nothing about update frequency,
  delta/incremental files, versioning, or changelogs for the master data.

### 8.5 Field-spelling watch-list (verbatim as documented)
- Hotel Details CSV: **`HotelPostelCode`** (misspelled) — but gethoteldetails XML uses **`HotelPostalCode`**.
- Search-by-hotel-id request container appears as both **`HotelIDs`** and **`HotelIds`** across the two
  examples on that page; hotel IDs are wrapped in **`<Int>`** elements.
- Children count element is **`NoOfChilds`** (not "NoOfChildren").
- Search response hotel identifier tag is **`<Id>`** (not `HotelId`); gethoteldetails uses **`<HotelId>`**.
