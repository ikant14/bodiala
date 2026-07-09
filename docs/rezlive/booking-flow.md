# RezLive.com (XMLHUB) — Booking-Flow API Reference

## Provenance & confidence

- **Source:** Single public GitBook — `https://rezlive.gitbook.io/rezlive-api-doc/` (raw markdown obtained by appending `.md` to each page URL). Extracted 2026-07-07.
- **Confidence:** Medium. This is the only source. XML examples below are reproduced as faithfully as the docs render them (element names, casing, and misspellings preserved). The public docs are example-heavy; some field-level "required/optional" detail comes from the docs' parameter tables (summarized where a verbatim table was not retrievable). **Verify every field, endpoint host, and status vocabulary against a real sandbox before relying on it.**
- **Endpoint host caveat:** All documented endpoints use the test host `http://test.xmlhub.com/testpanel.php/action/...`. The `/action/<name>` path segment is the load-bearing part; the host/base URL will differ in production (see the docs' "Request URLs" page).
- **Common to every request:** an `<Authentication><AgentCode/><UserName/></Authentication>` block, plus an `x-api-key: XXXXXX` HTTP header (required).

---

## Flow overview (how identifiers chain)

```
Search (search-by-hotel-id / search-by-destination)
   -> returns  SearchSessionId  (one per search)
   -> returns  BookingKey       (one per room offer)

getcancellationpolicy   uses  BookingKey (+ search params)      [optional pre-check]
prebook                 uses  SearchSessionId + BookingKey      -> returns a (possibly) refreshed BookingKey + price delta
bookhotel               uses  SearchSessionId + BookingKey + Guests
                        -> returns  BookingId + BookingCode + BookingStatus
cancelhotel             uses  BookingId + BookingCode
getbookingdetails       uses  BookingId + BookingCode
getCancellationPolicyAfterBooking  uses  BookingId + BookingCode
getConfirmationDetails  uses  BookingId + BookingCode
```

- **`SearchSessionId`** — "Each search is assigned with a SearchSessionId which is used to fetch details from that particular session." Threaded into `prebook` and `bookhotel`.
- **`BookingKey`** — "A BookingKey is a unique key for each room which is required to be sent in Cancellation Policy, Prebook and Booking API request." The search-result `BookingKey` feeds `getcancellationpolicy`/`prebook`/`bookhotel`. Note the prebook RESPONSE returns a (longer, refreshed) `BookingKey` per room.
- **`BookingId` + `BookingCode`** — produced by the `bookhotel` RESPONSE; both are required together as the booking identifier for `cancelhotel`, `getbookingdetails`, `getCancellationPolicyAfterBooking`, and `getConfirmationDetails`.

---

## 1. Hotel Pre-Booking — `prebook`

Source: https://rezlive.gitbook.io/rezlive-api-doc/hotel-pre-booking

**Endpoint**

```
http://test.xmlhub.com/testpanel.php/action/prebook
```

**Purpose (verbatim):** "This API provide you with the latest updated price and cancellation policy of a particular hotel's particular room(s) just before booking confirmation. In case if there is any change happened to the previous details, an intimation will be provided to you about the same."

**Request** — root element `<PreBookingRequest>`. Takes BOTH `SearchSessionId` AND per-room `BookingKey`.

```xml
Headers
x-api-key: XXXXXX
 
<PreBookingRequest>
<Authentication>
<AgentCode>XXXXX</AgentCode>
<UserName>XXXXX</UserName>
</Authentication>
<PreBooking>
<SearchSessionId>XXXXXXXXXXXXXXXXXXXXXX</SearchSessionId>
<ArrivalDate>23/10/2023</ArrivalDate>
<DepartureDate>01/11/2023</DepartureDate>
<GuestNationality>IN</GuestNationality>
<CountryCode>TR</CountryCode>
<City>GTR15</City>
<HotelId>150090</HotelId>
<Currency>USD</Currency>
<RoomDetails>
<RoomDetail>
<Type>Double Room</Type>
<BookingKey>VY5PS8QwFMS_Ss85LMkmfW1zVFwUFpVVwZukL2gdfg43dfg</BookingKey>
<Adults>2</Adults>
<Children>0</Children>
<ChildrenAges>0</ChildrenAges>
<TotalRooms>1</TotalRooms>
<TotalRate>1079.88</TotalRate>
</RoomDetail>
</RoomDetails>
</PreBooking>
</PreBookingRequest>
```

**Request fields** (per docs parameter table; all marked Required = Yes unless noted):
`AgentCode`, `UserName` (Authentication); `SearchSessionId`; `ArrivalDate` (dd/mm/yyyy); `DepartureDate` (dd/mm/yyyy); `GuestNationality` (2-letter); `CountryCode` (2-letter); `City` (city code); `HotelId`; `Currency`; and per `RoomDetail`: `Type`, `BookingKey`, `Adults`, `Children`, `ChildrenAges`, `TotalRooms`, `TotalRate`.

**Response** — root element `<PreBookingResponse>`. It echoes the request inside `<PreBookingRequest>` (note: the echoed block carries a refreshed, much longer `<BookingKey>` per room, plus `<TermsAndConditions/>` and a `<CancellationInformations>` list), then appends `<PreBookingDetails>` with the price delta.

```xml
<PreBookingResponse>
<PreBookingRequest>
<Authentication>
<AgentCode>XXXXX</AgentCode>
<UserName>XXXXX</UserName>
</Authentication>
<PreBooking>
<ArrivalDate>23/10/2023</ArrivalDate>
<DepartureDate>01/11/2023</DepartureDate>
<GuestNationality>IN</GuestNationality>
<CountryCode>TR</CountryCode>
<City>Istanbul</City>
<HotelId>150090</HotelId>
<Currency>USD</Currency>
<RoomDetails>
<RoomDetail>
<Type>Double Room</Type>
<BookingKey>7VfJDqrYFv2VGmtepFdM7oBGOhEFpZ0dGuEgnXSCX18HvbdSNbmpwcsbvQEJOWc3a--99gp0Q9MUMGn3nsLSq6aFUbInMWyV1X1SRHWc7HGMogh8tZJgkVzqrpfgtL-T6bZjmZztC4Jut3XyCLHkkaY9uWrruoyTLtqLsEr-4Np6qOI_Lm1d1j2sq_9Y6PqPc1XMq2ho26SK5j13EFddAtooeyQzjPckDQAZEndyh0XbLU4TGLPdMji74uv6Aav0mMz7p61J3G6rlZt4sIvKu1_fPch8rrqLepJ3tnR_nKjxMuBFRfW9JL2Ocycqx6cnrCPXIdaeOeTHtmbNrsboEvSYpQAi_fFj5YB-j61g17RJiNLtcfTOjQAWIIQF7Of9rR2S1c9bBAbhZSl61Sagq6v9amhi0Cfx35DeTrVmt8I8PoTByKXN7v0E_</BookingKey>
<Adults>2</Adults>
<Children>0</Children>
<ChildrenAges>0</ChildrenAges>
<TotalRooms>1</TotalRooms>
<TotalRate>1079.88</TotalRate>
<TermsAndConditions/>
</RoomDetail>
</RoomDetails>
<CancellationInformations>
<CancellationInformation>
<StartDate>11 Apr 2023 00:00:00</StartDate>
<EndDate>07 Jul 2023 23:59:00</EndDate>
<ChargeType>Amount</ChargeType>
<ChargeAmount>0</ChargeAmount>
<Currency>AED</Currency>
</CancellationInformation>
<CancellationInformation>
<StartDate>08 Jul 2023 00:00:00</StartDate>
<EndDate>05 Aug 2023 23:59:00</EndDate>
<ChargeType>Amount</ChargeType>
<ChargeAmount>1079.88</ChargeAmount>
<Currency>AED</Currency>
</CancellationInformation>
<Info>Early checkout, No Show and Late amendments will result in 100% cancellation charges. In case of date change or reduction/Increase in number of nights or change in occupancy, rates are subject to change.</Info>
</CancellationInformations>
</PreBooking>
</PreBookingRequest>
<PreBookingDetails>
<BookingBeforePrice>1079.88</BookingBeforePrice>
<BookingAfterPrice>1079.88484464</BookingAfterPrice>
<Difference>0.00484499999993</Difference>
<AgentBalance>240669.45</AgentBalance>
<AgentCurrency>USD</AgentCurrency>
</PreBookingDetails>
</PreBookingResponse>
```

**Response fields:**
- `<PreBookingRequest>` — echo of the request; the per-room `<BookingKey>` here is refreshed (use it for the subsequent `bookhotel`). Adds `<TermsAndConditions/>` and a `<CancellationInformations>` list (each `<CancellationInformation>` = `StartDate`, `EndDate`, `ChargeType` [Amount|Percentage], `ChargeAmount`, `Currency`) plus a free-text `<Info>`.
- `<PreBookingDetails>`:
  - `BookingBeforePrice` — "Price which was sent in the pre-booking request".
  - `BookingAfterPrice` — "Updated/Most Recent price for that booking".
  - `Difference` — variance between the two prices.
  - `AgentBalance`, `AgentCurrency` — agent's current wallet balance/currency.

---

## 2. Hotel Booking — `bookhotel`

Source: https://rezlive.gitbook.io/rezlive-api-doc/hotel-booking

**Endpoint**

```
http://test.xmlhub.com/testpanel.php/action/bookhotel
```

**Request** — root element `<BookingRequest>`. Takes `SearchSessionId` + per-room `BookingKey`, adds an agent reference and the `<Guests>` list. Note: multi-room fields are pipe-`|`-delimited within a single `<RoomDetail>` (e.g. `<Adults>2|2</Adults>`, `<TotalRate>332.75424|332.75424</TotalRate>`), and each room gets its own repeated `<Guests>` block. Docs state up to 8 rooms can be booked in one request.

```xml
<?xml version="1.0"?>
<BookingRequest>
<Authentication>
<AgentCode>XXXX</AgentCode>
<UserName>XXXX</UserName>
</Authentication>
<Booking>
<SearchSessionId>XXXXXXXXXXXXXXXXXXXXXX</SearchSessionId>
<AgentRefNo>123456</AgentRefNo>
<ArrivalDate>25/10/2023</ArrivalDate>
<DepartureDate>26/10/2023</DepartureDate>
<GuestNationality>BE</GuestNationality>
<CountryCode>AE</CountryCode>
<City>968</City>
<HotelId>151754</HotelId>
<Name>Eureka hotel</Name>
<Currency>AED</Currency>
<RoomDetails>
<RoomDetail>
<Type><![CDATA[Standard|Standard]]></Type>
<BookingKey>7VfJDqrYFv2VGmtepFdM7oBGOhEFpZ0dGuEgnXSCX18HvbdSNbmpwcsbvQEJOWc3a--
99gp0Q9MUMGn3nsLSq6aFUbInMWyV1X1SRHWc7HGMogh8tZJgkVzqrpfgtL- T6bZjmZztC4Jut3XyCLHkkaY9uWrruoyTLtqLsEr-
4Np6qOI_Lm1d1j2sq_9Y6PqPc1XMq2ho26SK5j13EFddAtooeyQzjPckDQAZEndyh0XbLU4TGLPdMji74uv6Aav0mMz7p61J3G
6rlZt4sIvKu1_fPch8rrqLepJ3tnR_nKjxMuBFRfW9JL2Ocycqx6cnrCPXIdaeOeTHtmbNrsboEvSYpQAi_fFj5YB- j61g17RJiNLtcfTOjQAWIIQF7Of9rR2S1c9bBAbhZSl61Sagq6v9amhi0Cfx35DeTrVmt8I8PoTByKXN7v0E_ </BookingKey> <Adults>2|2</Adults>
<Children>1|1</Children>
<ChildrenAges>7|10</ChildrenAges>
<TotalRooms>2</TotalRooms>
<TotalRate>332.75424|332.75424</TotalRate>
<Guests>
<Guest>
<Salutation>Mr</Salutation>
<FirstName>Aakash</FirstName>
<LastName>Patel</LastName>
</Guest>
<Guest>
<Salutation>Mr</Salutation>
<FirstName>Alia</FirstName>
<LastName>Patel</LastName>
</Guest>
<Guest>
<Salutation>Child</Salutation> <FirstName>Dolly</FirstName>
<LastName>Patel</LastName>
<IsChild>1</IsChild>
<Age>7</Age>
</Guest> </Guests>
<Guests>
<Guest>
<Salutation>Mr</Salutation>
<FirstName>Ramesh</FirstName>
<LastName>Patel</LastName>
</Guest>
<Guest>
<Salutation>Mr</Salutation>
<FirstName>Veena</FirstName>
<LastName>Patel</LastName>
</Guest>
<Guest>
<Salutation>Child</Salutation>
<FirstName>Dev</FirstName>
<LastName>Patel</LastName> <IsChild>1</IsChild>
<Age>10</Age>
</Guest>
</Guests>
</RoomDetail>
</RoomDetails>
</Booking>
</BookingRequest>
```

**Request fields:**
- Authentication: `AgentCode`, `UserName` (required).
- `<Booking>`: `SearchSessionId` (required), `AgentRefNo` (required per docs — "a reference which agent/client can provide and use to track bookings as per his system"), `ArrivalDate`, `DepartureDate`, `GuestNationality`, `CountryCode`, `City`, `HotelId`, `Name` (hotel name), `Currency` — all required.
- `<RoomDetail>`: `Type` (CDATA; pipe-delimited per room), `BookingKey`, `Adults`, `Children`, `ChildrenAges`, `TotalRooms`, `TotalRate` — all required. Multi-room values are separated by `|`.
- `<Guests>` (one block per room) / `<Guest>`: `Salutation` (required), `FirstName` (required), `LastName` (required), `IsChild` (required — 1 for a child), `Age` (optional — "Age of the child, should be passed if isChild is 1").
- **Contact fields:** No separate lead/contact block (email/phone/address) appears in the documented request. **Not documented** — only per-guest name + salutation are shown.

**Response** — root element `<BookingResponse>`. Echoes the request under `<BookingRequest>` (with enriched hotel info: `Name`, `Address`, `Description`, `Latitude`, `Longitude`, `ThumbImages`, `HotelImages`, `CancellationPolicy`, and the booked `Guests`), then the key `<BookingDetails>` block carrying the identifiers and status.

```xml
<?xml version="1.0" ?>
<BookingResponse>
<BookingRequest>
<Authentication> <AgentCode>XXXXX</AgentCode>
<UserName>XXXXX</UserName>
</Authentication>
<Booking>
<ArrivalDate>26/09/2023</ArrivalDate>
<DepartureDate>27/09/2023</DepartureDate>
<GuestNationality>AE</GuestNationality>
<CountryCode>AE</CountryCode>
<City>968</City>
<HotelId>265841</HotelId>
<Name>Holiday inn bur dubai - embassy district</Name>
<Address>Embassy District Plot No. 313-425 Al Hamriyah Dubai City P O Box 11454</Address> <Description/>
<Latitude>25.193953</Latitude>
<Longitude>55.279097</Longitude>
<ThumbImages>
http://test.xmlhub.com/../images?img=aHR0cDovZw==

</ThumbImages>
<HotelImages/>
<Currency>INR</Currency>
<RoomDetails>
<RoomDetail>
<Type>Standard Room</Type>
<BookingKey>7VfJDqrYFv2VGmtepFdM7oBGOhEFpZ0dGuEgnXSCX18HvbdSNbmpwcsbvQEJOWc3a--
99gp0Q9MUMGn3nsLSq6aFUbInMWyV1X1SRHWc7HGMogh8tZJgkVzqrpfgtL- T6bZjmZztC4Jut3XyCLHkkaY9uWrruoyTLtqLsEr-
4Np6qOI_Lm1d1j2sq_9Y6PqPc1XMq2ho26SK5j13EFddAtooeyQzjPckDQAZEndyh0XbLU4TGLPdMji74uv6Aav0mMz7p61J3G
6rlZt4sIvKu1_fPch8rrqLepJ3tnR_nKjxMuBFRfW9JL2Ocycqx6cnrCPXIdaeOeTHtmbNrsboEvSYpQAi_fFj5YB- j61g17RJiNLtcfTOjQAWIIQF7Of9rR2S1c9bBAbhZSl61Sagq6v9amhi0Cfx35DeTrVmt8I8PoTByKXN7v0E_ </BookingKey> <Adults>2</Adults>
<Children>2</Children>
<ChildrenAges>7*6</ChildrenAges>
<TotalRooms>1</TotalRooms>
<TotalRate>7191.31200984</TotalRate>
<CancellationPolicy/>
<Guests>
<Guest>
<Salutation>Mr</Salutation>
<FirstName>mahesh</FirstName>
<LastName>gohil</LastName>
</Guest>
<Guest>
<Salutation>Mr</Salutation>
<FirstName>amit</FirstName> <LastName>patel</LastName>
</Guest>
<Guest>
<Salutation>Mr</Salutation>
<FirstName>kapil</FirstName>
<LastName>sibal</LastName>
<IsChild>1</IsChild>
<Age>7</Age>
</Guest>
<Guest>
<Salutation>Mr</Salutation>
<FirstName>milap</FirstName>
<LastName>patel</LastName>
<IsChild>1</IsChild>
<Age>6</Age>
</Guest>
</Guests>
</RoomDetail>
</RoomDetails>
</Booking>
</BookingRequest>
<BookingDetails>
<BookingId>XHUB560</BookingId>
<BookingCode>XHUBXI-HL-14424443</BookingCode>
<BookingStatus>Confirmed</BookingStatus>
<BookingPrice>16.51</BookingPrice> <BookingCurrency>USD</BookingCurrency>
</BookingDetails>
</BookingResponse>
```

**Response fields — `<BookingDetails>` (the identifiers you keep):**
- `BookingId` — unique booking identifier (e.g. `XHUB560`).
- `BookingCode` — booking tracking code (e.g. `XHUBXI-HL-14424443`).
- `BookingStatus` — booking status (example shows `Confirmed`; docs mention other statuses such as Rejected/Failed — full vocabulary **not fully documented**, verify in sandbox).
- `BookingPrice` — final booked amount.
- `BookingCurrency` — currency of the transaction.

> Both `BookingId` AND `BookingCode` are required together for all downstream calls (cancel / get-details / policy-after-booking / confirmation).

---

## 3. Hotel Booking Cancellation — `cancelhotel`

Source: https://rezlive.gitbook.io/rezlive-api-doc/hotel-booking-cancellation

**Endpoint**

```
http://test.xmlhub.com/testpanel.php/action/cancelhotel
```

**Request** — root element `<CancellationRequest>`. Identifier = `BookingId` + `BookingCode` (both from the `bookhotel` response).

```xml
<?xml version="1.0" ?>
<CancellationRequest>
<Authentication>
<AgentCode>XXXXXX</AgentCode>
<UserName>XXXXXX</UserName>
</Authentication>
<Cancellation>
<BookingId>XHUB2424</BookingId>
<BookingCode>XHUB2145429</BookingCode>
</Cancellation>
</CancellationRequest>
```

**Request fields:** `AgentCode`, `UserName`; `<Cancellation>` → `BookingId` (required, from booking response), `BookingCode` (required, from booking response).

**Response** — root element `<CancellationResponse>`.

```xml
<?xml version="1.0" ?>
<CancellationResponse>
<BookingId>XHUB560</BookingId>
<BookingCode>XHUBXI-HL-14424443</BookingCode>
<Status>true</Status>
<CancellationCharges>16.51</CancellationCharges>
<Currency>USD</Currency>
</CancellationResponse>
```

**Response fields:**
- `BookingId`, `BookingCode` — echo of the cancelled booking.
- `Status` — `true`/`false` (whether cancellation succeeded). NOTE: this is a boolean `Status`, distinct from the `BookingStatus` string elsewhere.
- `CancellationCharges` — fee applied per the cancellation policy.
- `Currency` — currency of the charge (agent's configured currency).

---

## 4. Hotel Booking Cancellation Policy — `getcancellationpolicy`

Source: https://rezlive.gitbook.io/rezlive-api-doc/hotel-booking-cancellation-policy

Pre-booking policy check (before a booking exists). Keyed on `BookingKey` (from search), NOT on BookingId.

**Endpoint**

```
http://test.xmlhub.com/testpanel.php/action/getcancellationpolicy
```

**Request** — root element `<CancellationPolicyRequest>`. (Authentication + search-style params + per-room `BookingKey`. No `<Booking>`/`<PreBooking>` wrapper — the fields sit directly under the root.)

```xml
<?xml version="1.0"?>
<CancellationPolicyRequest>
<Authentication>
<AgentCode>XXXXX</AgentCode>
<UserName>XXXXX</UserName>
</Authentication>
<ArrivalDate>26/12/2023</ArrivalDate>
<DepartureDate>29/12/2023</DepartureDate>
<HotelId>730202</HotelId>
<CountryCode>AE</CountryCode>
<City>968</City>
<GuestNationality>IN</GuestNationality>
<Currency>THB</Currency>
<RoomDetails>
<RoomDetail>
<BookingKey>FcmxDYAwDATAiV5622_HbMAItERRGvbvUa69pi7vEjNXTcW7suY2iRZyOp47CoLTEmy4YQRPfD8</BookingKey>
<Adults>4</Adults>
<Children>0</Children>
<ChildrenAges>0</ChildrenAges>
<Type>Bed and Breakfast-Minimum Stay Offer-No Show 100% will be charged</Type>
</RoomDetail>
</RoomDetails>
</CancellationPolicyRequest>
```

**Request fields:** `AgentCode`, `UserName`; `ArrivalDate`, `DepartureDate`, `HotelId`, `CountryCode`, `City`, `GuestNationality`, `Currency`; per `RoomDetail`: `BookingKey`, `Adults`, `Children`, `ChildrenAges`, `Type`.

**Response** — root element `<CancellationPolicyResponse>`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<CancellationPolicyResponse>
<CancellationInformations>
<CancellationInformation>
<StartDate>13 Oct 2023 00:00:00</StartDate>
<EndDate>14 Oct 2023 23:59:00</EndDate>
<ChargeType>Percentage</ChargeType>
<ChargeAmount>50</ChargeAmount>
<Currency>THB</Currency>
</CancellationInformation>
<CancellationInformation>
<StartDate>15 Oct 2023 00:00:00</StartDate>
<EndDate>17 Oct 2023 23:59:00</EndDate>
<ChargeType>Percentage</ChargeType>
<ChargeAmount>100</ChargeAmount>
<Currency>THB</Currency>
</CancellationInformation>
<Info>Early checkout, No Show and Late amendments will result in 100% cancellation charges. In case of date change or reduction/Increase in number of nights or change in occupancy, rates are subject to change.</Info>
</CancellationInformations>
</CancellationPolicyResponse>
```

**Response fields:** `<CancellationInformations>` → repeated `<CancellationInformation>` (`StartDate`, `EndDate`, `ChargeType` [Percentage|Amount], `ChargeAmount`, `Currency`) + free-text `<Info>`.

---

## 5. Get Booked Hotel Detail — `getbookingdetails`

Source: https://rezlive.gitbook.io/rezlive-api-doc/get-booked-hotel-detail

**Endpoint**

```
http://test.xmlhub.com/testpanel.php/action/getbookingdetails
```

**Request** — root element `<GetBookingRequest>`. Identifier = `BookingId` + `BookingCode` (from the `bookhotel` response). Here the identifiers sit directly under the root (no `<Cancellation>` wrapper).

```xml
<?xml version="1.0"?>
<GetBookingRequest>
<Authentication>
<AgentCode>XXXXX</AgentCode>
<UserName>XXXXX</UserName>
</Authentication>
<BookingId>XHUB2A34C0</BookingId>
<BookingCode>XHUB2ABDC0</BookingCode>
</GetBookingRequest>
```

**Request fields:** `AgentCode`, `UserName`; `BookingId` (required), `BookingCode` (required).

**Response** — root element `<GetBookingResponse>`.

```xml
<GetBookingResponse> 
<Booking> <BookingId>XHUB2A34C0</BookingId> 
<CheckIn>2023-01-28</CheckIn> 
<CheckOut>2023-01-30</CheckOut> 
<Bookingdate/> 
<BookingStatus>Invoiced</BookingStatus> 
<HotelInfo> 
<HotelId>163102</HotelId> 
<HotelName/> 
<HotelAddress/> 
<HotelCity>Pattaya</HotelCity> 
<HotelCountryCode>TH</HotelCountryCode> 
<Hoteldescription><![CDATA[]]></Hoteldescription> 
<Nationality>TH</Nationality> 
<LeaderFirstName>ADULT</LeaderFirstName> 
<LeaderLastName>ONE</LeaderLastName> 
<CancellationPolicy> 
<CancellationPolicyInfo>50% hrs.</CancellationPolicyInfo> 	Charges 	applicable 	if 	cancelled 	After 	26 	Sep 	2014 	00:00:00 
<CancellationPolicyInfo>100% hrs.</CancellationPolicyInfo> 	Charges 	applicable 	if 	cancelled 	After 	28 	Sep 	2014 	00:00:00 
<CancellationPolicyInfoExtra> 
<Deadline_date>2023-09-26 00:00:00</Deadline_date> 
<Price>4943.24</Price> 
<Currency>THB</Currency> 
</CancellationPolicyInfoExtra> 
</CancellationPolicy> 
</HotelInfo> 
<RateInfo> 
<CurrencyCode>THB</CurrencyCode> 
<TotalRate>4943.24</TotalRate> 
</RateInfo> 
<RoomInfo> 
<RoomType>Superior Double Room</RoomType> 
<NoOfAdult>1</NoOfAdult> 
<NoOfChild>0</NoOfChild> 
<GuestInfos> 
<GuestInfo> 
<PaxType>Adult</PaxType> 
<Salutation>Mrs</Salutation> 
<FirstName>ADULT</FirstName> 
<LastName>ONE</LastName> 
</GuestInfo> 
</GuestInfos> 
</RoomInfo> 
</Booking> 
</GetBookingResponse>
```

**Response fields — `<Booking>`:**
- `BookingId`, `CheckIn` (yyyy-mm-dd), `CheckOut`, `Bookingdate`, `BookingStatus` (example shows `Invoiced` — distinct from the `Confirmed` seen on the booking response; status vocabulary **not fully documented**).
- `<HotelInfo>`: `HotelId`, `HotelName`, `HotelAddress`, `HotelCity`, `HotelCountryCode`, `Hoteldescription` (CDATA), `Nationality`, `LeaderFirstName`, `LeaderLastName`, and `<CancellationPolicy>` (repeated `CancellationPolicyInfo` free text + `<CancellationPolicyInfoExtra>` with `Deadline_date`, `Price`, `Currency`).
- `<RateInfo>`: `CurrencyCode`, `TotalRate`.
- `<RoomInfo>`: `RoomType`, `NoOfAdult`, `NoOfChild`, `<GuestInfos>` → `<GuestInfo>` (`PaxType`, `Salutation`, `FirstName`, `LastName`).

> The docs' prose mentions `BookingConfirmationNo` as a key field, but it does NOT appear in the response example above — treat its presence/placement as **not documented / verify in sandbox**. (A supplier-side confirmation number is exposed instead via `getConfirmationDetails` as `HotelConfirmationNo`, section 7.)

---

## 6. Hotel Booking Cancellation Policy after Booking — `getCancellationPolicyAfterBooking`

Source: https://rezlive.gitbook.io/rezlive-api-doc/hotel-booking-cancellation-policy-after-booking

Post-booking policy lookup. Keyed on `BookingId` + `BookingCode` (NOT BookingKey).

**Endpoint** (note the camelCase action name)

```
http://test.xmlhub.com/testpanel.php/action/getCancellationPolicyAfterBooking
```

**Request** — root element `<CancellationPolicyAfterBookingRequest>`.

```xml
<CancellationPolicyAfterBookingRequest>
  <Authentication>
    <AgentCode>XXXXX</AgentCode>
    <UserName>XXXXX</UserName>
  </Authentication>
  <BookingId>XHUB8546EF</BookingId>
  <BookingCode>XHUB46EFFC</BookingCode>
</CancellationPolicyAfterBookingRequest>
```

**Request fields:** `AgentCode`, `UserName`; `BookingId`, `BookingCode`.

**Response** — root element `<CancellationPolicyAfterBookingResponse>`, wrapping an inner `<CancellationPolicyResponse>` identical in shape to section 4.

```xml
<CancellationPolicyAfterBookingResponse>
  <CancellationPolicyResponse>
    <CancellationInformations>
      <CancellationInformation>
        <StartDate>17 Nov 2023 00:00:00</StartDate>
        <EndDate>18 Nov 2023 23:59:00</EndDate>
        <ChargeType>Percentage</ChargeType>
        <ChargeAmount>50</ChargeAmount>
        <Currency>USD</Currency>
      </CancellationInformation>
      <CancellationInformation>
        <StartDate>19 Nov 2023 00:00:00</StartDate>
        <EndDate>21 Nov 2023 23:59:00</EndDate>
        <ChargeType>Percentage</ChargeType>
        <ChargeAmount>100</ChargeAmount>
        <Currency>USD</Currency>
      </CancellationInformation>
      <Info>Early checkout, No Show and Late amendments will result in 100% cancellation charges. In case of date change or reduction/Increase in number of nights or change in occupancy, rates are subject to change.</Info>
    </CancellationInformations>
  </CancellationPolicyResponse>
</CancellationPolicyAfterBookingResponse>
```

**Response fields:** as section 4 (`StartDate`, `EndDate`, `ChargeType`, `ChargeAmount`, `Currency`, `Info`), nested one level deeper.

---

## 7. Hotel Booking Confirmation — `getConfirmationDetails`

Source: https://rezlive.gitbook.io/rezlive-api-doc/hotel-booking-confirmation

Returns supplier/hotel-side confirmation info for a booking. Keyed on `BookingId` + `BookingCode`.

**Endpoint** (camelCase action name)

```
http://test.xmlhub.com/testpanel.php/action/getConfirmationDetails
```

**Request** — root element `<HotelConfirmationRequest>`.

```xml
<HotelConfirmationRequest>
<Authentication>
<AgentCode>XXXXX</AgentCode>
<UserName>XXXXX</UserName>
</Authentication>
<Confirmation>
<BookingId>XHUB535527</BookingId>
<BookingCode>XHUB2145429</BookingCode>
</Confirmation>
</HotelConfirmationRequest>
```

**Request fields:** `AgentCode`, `UserName`; `<Confirmation>` → `BookingId`, `BookingCode`.

**Response** — root element `<HotelConfirmationResponse>`.

```xml
<HotelConfirmationResponse>
<ConfirmationDetails>
<HotelTelephoneNo>787****757</HotelTelephoneNo>
<HotelStaffName>Terds Ress</HotelStaffName>
<HotelConfirmationNo>123456</HotelConfirmationNo>
<ConfirmationStatus>Confirmed</ConfirmationStatus>
<ConfirmationNote>Booking Confirmed</ConfirmationNote>
</ConfirmationDetails>
</HotelConfirmationResponse>
```

**Response fields — `<ConfirmationDetails>`:**
- `HotelTelephoneNo` — hotel phone (masked in example).
- `HotelStaffName` — hotel staff contact name.
- `HotelConfirmationNo` — the supplier/hotel's confirmation number for the booking.
- `ConfirmationStatus` — e.g. `Confirmed`.
- `ConfirmationNote` — free-text note.

---

## Quick field-name cross-reference (identifiers)

| Concept | Field name | Produced by | Consumed by |
|---|---|---|---|
| Search session | `SearchSessionId` | search response | prebook, bookhotel |
| Per-room offer key | `BookingKey` | search response (refreshed by prebook) | getcancellationpolicy, prebook, bookhotel |
| Agent's own ref | `AgentRefNo` | you supply | bookhotel (echoed) |
| Booking id | `BookingId` | bookhotel response | cancelhotel, getbookingdetails, getCancellationPolicyAfterBooking, getConfirmationDetails |
| Booking code | `BookingCode` | bookhotel response | (same four as BookingId — always paired) |
| Booking status (string) | `BookingStatus` | bookhotel / getbookingdetails | — |
| Cancel success (bool) | `Status` | cancelhotel response | — |
| Supplier confirmation no. | `HotelConfirmationNo` | getConfirmationDetails | — |

**Not documented / verify in sandbox:** full `BookingStatus` vocabulary; whether `bookhotel` accepts any lead-guest contact fields (email/phone/address) — none shown; exact presence/location of `BookingConfirmationNo` in `getbookingdetails`; production endpoint host.
