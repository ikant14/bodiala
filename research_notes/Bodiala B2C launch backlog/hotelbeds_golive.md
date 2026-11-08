# Taking a Hotelbeds APItude (Hotel API + Content API) integration live for a B2C site — certification, display/booking rules, production requirements, and features to build

> Research date: 2026-11-08. Sources are mostly developer.hotelbeds.com pages (now branded "HBX Group"). Those pages carry no visible "last updated" dates, and their code samples use 2020–2021 dates, so treat them as current-but-undated. The Postman collection is from 2018 and is likely outdated. Third-party write-ups are flagged as such. Tags: **[MANDATORY]** = Hotelbeds certification says must/required. **[RECOMMENDED]** = suggested or optional. **[UNVERIFIED]** = only from a search snippet or a third party.
> Local-gap notes compare against `C:\Users\user\IdeaProjects\bodiala\docs\hotelbeds\api-contract.md` and the `provider.hotelbeds` source (read-only check, nothing modified).

## 1. What is Hotelbeds' certification / go-live process for APItude?

### Takeaway
Certification is a self-declared, email-driven review by the APItude team (apitude@hotelbeds.com). It covers six areas: Technical, Workflow, Availability/CheckRate/Confirmation, Voucher, Content, Live environment. It runs on the **test** environment against a URL you provide. After it passes, live keys are issued and you must make one real refundable live booking and cancel it. No official timeline is published. A third-party estimate is 8–12 weeks end to end.

### Cited Findings
- **How to request (MANDATORY steps):** finish development, read the Hotels best practices first ("will speed up the process and avoid problems afterwards"), then email apitude@hotelbeds.com with: your workflow (and each distribution channel's workflow if they differ); commercial decisions such as excluded destinations, room types, board types or hotels; a **certification URL, plus user, password and payment information if needed**; a language guide if there is no common language; how you isolate HBX Group products if you use several suppliers; and any other relevant aspects — [Hotelbeds Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- The six certification areas are Technical, Workflow, Availability/CheckRate/Confirmation, Voucher, Content and Live environment. **Only the filters you actually implement are tested** — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **Technical area:** Hotelbeds reviews how requests are configured and how well you use **GZIP** compression — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **Live-environment area (MANDATORY):**
  - 6.1: after receiving live API keys, book about **6 months ahead for 2 adults + 2 children** and send Hotelbeds the voucher and price. Do **not** use NRF or other rates with cancellation fees. Live bookings are real and hotels will charge cancellation fees.
  - 6.2: cancel the test booking to avoid charges.
  - 6.3: confirm the integration tolerates non-breaking API changes (new response fields, new endpoints, new optional parameters, reordered properties, changed string lengths or formats).
  - [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- Certification itself runs on the test environment, so the live check is what confirms readiness for production — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- The page only says live keys come "when HBX Group give you live access" after certification. It names no reviewers and gives no SLA — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- Third-party write-up (last updated 26 Jan 2026): you register and get evaluation keys (Hotel, Activities, Transfers, each with a secret). The sandbox is limited to 50 requests/day. The live booking "must be canceled promptly; otherwise, the reservation will be charged" — [AltexSoft](https://www.altexsoft.com/blog/hotelbeds-api-integration/)
- Third-party sources say production credentials are issued only after certification passes, and that a commercial agreement is signed before production access — [Vervotech](https://www.vervotech.com/hub/integrations/apitude-api/); [instaGDS](https://www.instagds.com/hotelbeds.html)
- Timeline estimate: **8–12 weeks** end to end including certification and go-live. This is an integration-vendor blog, **[UNVERIFIED]** — [TravelTekPro](https://traveltekpro.com/how-to-integrate-your-hotel-into-hotelbeds-api-traveltekpro/)
- Pricing model, currency (EUR/USD/GBP depending on destination), net vs commissionable: the docs say to contact your HBX Group sales manager, so this is a commercial step outside certification — [Pricing models](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/pricing-models/)
- **Test-environment rule:** "DO NOT make hotel mappings in the test environment" (evaluation/pre-production plan) — [Best practices](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/best-practices/)

### Inferences
- Engineering tasks:
  - Stand up a **publicly reachable staging/certification URL** with test login and test payment, so Hotelbeds reviewers can walk search → detail → book → voucher → cancel.
  - Write a **workflow document**: call sequence, when CheckRate is used, excluded products, and how Hotelbeds results are isolated if a second supplier is added.
  - Plan a **live smoke booking** (about 6 months out, 2AD+2CH ages, refundable NOR rate) and a cancellation runbook.
- The 50/day evaluation quota means certification test runs need careful budgeting, or a temporary quota raise requested from Hotelbeds. The local stub (`hotelbeds-stub`) helps for regression but is not what reviewers test.
- Because commercial terms (net vs commissionable, `hotelMandatory`, payment model) are set per contract, the price-display logic should be config-driven until the contract is signed.

### Gaps
- No official certification timeline, reviewer roles, or formal test-case list (only the numbered checklist). Not published publicly.
- How production keys and the production quota are provisioned is not documented publicly.
- Section numbering on the certification page is inconsistent: 2.2 references "section 1.1", 6.1 references "point 5.2", and item 3.10 is missing. Confirm with apitude@hotelbeds.com.

## 2. Mandatory display rules (rate comments, cancellation, taxes, rate class, boards, promotions, occupancy, category, voucher text, content/images)

### Takeaway
Before confirmation you must display and apply:
- cancellation policies, in destination time
- rate comments, every time they exist
- the full product: price, rooms, dates, hotel, board, room type, category
- `hotelMandatory` selling prices, where flagged

Every confirmed booking needs a voucher with these mandatory fields:
- hotel name and address
- holder name and one pax name per room
- children's ages
- the Hotelbeds reference, dates, room type, board and rate comments
- the "Payable through … acting as agent …" supplier and VAT line

Tax breakdown (included vs payable-at-hotel) is an opt-in feature. It is not a certification item but matters for B2C transparency.

### Cited Findings
**Availability / CheckRate / Confirmation display**
- **3.1 [MANDATORY]:** the "entire product" must display correctly — prices, rooms, dates, hotels, board and room types, **hotel category**, pagination. Disclose any commercial reasons for excluding products — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **3.8 Cancellation policies [MANDATORY]:** display and apply them, or tell HBX Group you do not use them. They come in the rate's `cancellationPolicies` (`amount`, `from`). **Dates/times are based on the booking's destination, not the customer's local time** — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/); fields — [Booking workflow](https://developer.hotelbeds.com/documentation/hotels/booking-api/workflow/)
- Availability can return cancellation fees with amounts and dates at the first step of the booking — [Booking API overview](https://developer.hotelbeds.com/documentation/hotels/booking-api/)
- **Rate class:** `rateClass` NOR/NRF is a rate field in availability — [Postman collection (2018)](https://documenter.gw.postman.com/api/collections/3967924/RW1YqLg1?segregateAuth=true&versionTag=latest). The Hotelbeds Activities docs say NRF is always non-refundable, while NOR requires reading the policy array. That page covers the Activities product; assume the Hotels semantics match and verify — [Activities cancellation policies](https://developer.hotelbeds.com/documentation/activities/knowledge-base/cancellation-policies/)
- **3.9 Rate comments [MANDATORY]:**
  - Show them to the client **before confirmation**.
  - Look up `rateCommentsId` in Content API RateComments, or get them from CheckRate for direct and external products.
  - **RECHECK rates require CheckRate to retrieve comments.**
  - [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **Rate comments mechanics:**
  - Availability returns only `rateCommentsId` (e.g. `"256|24524|3"` = `incoming|code|rateCodes`).
  - CheckRates, Booking and BookingDetail return the full concatenated text in `rateComments`.
  - To build the text at availability time, concatenate:
    1. the Content API comment
    2. the taxes from the availability response
    3. the hotel `issues` from the Content API
    4. the mandatory `facilities` where `voucher` = true
  - Comments have `dateStart`/`dateEnd` matched to the check-in date. If several apply, "concatenate each of them and show it to the final passenger".
  - Offline bulk `rateComments` download is recommended. The online `rateCommentDetails` operation takes the code and check-in date.
  - [Use of rate comments](https://developer.hotelbeds.com/documentation/hotels/content-api/use-rate-comments)
- **3.12 Selling rates [MANDATORY where applicable]:** respect the `hotelMandatory` attribute. HBX "can only recommend this for legal reasons", but non-compliant clients may face exclusions — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **Pricing models:**
  - **Net model:** if `hotelMandatory` = true, "you must respect the value … of the sellingRate attribute". Otherwise you may add your own markup.
  - **Commissionable model:** `sellingRate` is final ("you need to use this field regardless of the value on the hotelMandatory attribute").
  - Fields: `net`; `commission` (commissionable only); `sellingRate` (always in commissionable, net only when `hotelMandatory`); `dailyNet` / `dailySellingRate`; CheckRate/booking `totalNet`, `pendingAmount`, `totalSellingRate`.
  - Example rate: net 171.72, sellingRate 190.80, commissionPCT 10, paymentType AT_HOTEL.
  - [Pricing models](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/pricing-models/)
- "BookingAPI prices are final … Our rates include supplements and discounts and no additional price calculations are required" — [Booking API overview](https://developer.hotelbeds.com/documentation/hotels/booking-api/)
- **3.6 Source market [MANDATORY]:** use prices only for the source market requested. For example, do not show Spanish-market prices obtained with a UK `sourceMarket` — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **3.5 Opaque rates [MANDATORY if used]:** rates with `packaging` = true may only be sold together with flights, transfers or car rentals — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **2.7 Promotions [RECOMMENDED]:** display rate promotions, such as an included transfer or Disney ticket — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/). Rates carry `promotions` and `offers` (`code`, `name`, `amount`), plus `shiftRates` for flexible dates — [Postman collection (2018)](https://documenter.gw.postman.com/api/collections/3967924/RW1YqLg1?segregateAuth=true&versionTag=latest)
- **3.2 Pax selection** is recommended, not mandatory. **3.3 [MANDATORY]:** if you support children, the child's age is mandatory. **3.4:** use `occupancies[].rooms` for identical rooms, and split occupancy nodes when occupancies or child ages differ — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)

**Taxes and fees (tax breakdown)**
- Tax breakdown is **disabled by default**. Enable it via your HBX Technical Account Manager or apitude@hotelbeds.com, and note that activation changes API responses.
- The structure sits in the rate node of Availability, CheckRates and Booking: `taxes{allIncluded, taxes[]{included, amount, currency, clientAmount, clientCurrency, type, subType}}`.
  - Included taxes are summed as type `TAXESANDFEES`.
  - Not-included taxes are either grouped (`TAX` / `FEE` / `TAXANDFEES`) or broken down by concept, with a `subType` such as Resort Fee, City Tax, VAT or Service Charge.
- Not-included taxes and fees are "usually payable by the end customer directly at the hotel".
- Included taxes are informative only and **must not be added to the final price**.
- [Tax breakdown](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/tax-breakdown/)
- Rate comments built at availability time should include the taxes from the availability response — [Use of rate comments](https://developer.hotelbeds.com/documentation/hotels/content-api/use-rate-comments)

**Voucher (section 4)**
- **4.1 [MANDATORY]:** every confirmed booking requires a voucher, delivered so B2B/B2C clients can access it easily — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **4.2 Hotel information:** name and address are mandatory. Category, destination name and phone are recommended — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **4.3 Passenger information [MANDATORY]:**
  - holder name matching the lead pax in the booking request
  - at least one pax name per room (the holder suffices for single-room bookings)
  - children's ages
  - [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **4.4 Booking information:** mandatory: Hotelbeds booking reference, check-in/out dates, room type, board type, and **rate comments (if applicable)**. Agency reference is recommended — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **4.5 Payment information:**
  - **Recommended** not to show the reservation price.
  - **Required text:** "Payable through XXX, acting as agent for the service operating company" plus "VAT: YYY Reference: ZZZ", where XXX = supplier name, YYY = supplier VAT number, ZZZ = booking reference.
  - [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)

**Content (section 5)** — Content API use is encouraged, not mandatory. If used:
- Store it locally and **update at least once a week**. Use hotel detail for hotels missing from your database — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **5.1:** declare whether you use only HBX content or mix providers.
- **5.2:** show the category type (stars, keys, apartments) and its value.
- **5.3:** images must match the hotel; confirm how many you display.
- **5.4:** show hotel and payment facilities. `indFee` = true means a paid facility. "Mandatory" facilities (`indYesOrNo`) are always returned, and false means the hotel lacks them.
- **5.5:** implement per the Crawler specification (PDF linked from the page).
- **5.6:** show descriptions, contract remarks, room types, board types and points of interest.
- [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **Images:**
  - Base URL is `https://photos.hotelbeds.com/giata/` plus the returned path.
  - Sizes: standard 320px (no segment), `small` 74, `medium` 117, `bigger` 800, `xl` 1024, `xxl` 2048, `original`.
  - "some hotels may not have available all images in all sizes". A missing size returns **403 Forbidden `AccessDenied`**.
  - [Use of images](https://developer.hotelbeds.com/documentation/hotels/content-api/use-images)
  - An older "photos/images" page lists different sizes, so the two pages conflict per the search summary — [Photos/images (older)](https://developer.hotelbeds.com/documentation/hotels/content-api/photos-images)

### Inferences
- **Local gaps (bodiala), all likely certification blockers:**
  - **No `rateComments` / `rateCommentsId` handling:** a grep of `provider.hotelbeds` finds no such field ([local: `C:\Users\user\IdeaProjects\bodiala\src\main\java\com\miirphys\bodiala\provider\hotelbeds`]). Parse `rateComments` from the CheckRate and booking responses, show them on the pre-confirm page, and print them on the voucher.
  - **No `taxes` parsing and no `hotelMandatory` handling:** the mapper shows `sellingRate` else `net` ([local: `HotelbedsSearchMapper.java:57-59`, `HotelbedsBookingMapper.java:41-42`]). Implement net+markup vs `sellingRate` logic driven by `hotelMandatory` and the contract model, and show not-included taxes as "payable at hotel".
  - **No voucher generator** (PDF or email) with the "Payable through …" supplier/VAT line. The booking response `hotel.supplier` (listed in `api-contract.md` §3) is the likely data source for supplier name and VAT. **[UNVERIFIED field names]**
  - **Cancellation deadlines** must be rendered in the destination's timezone, using the offset in the ISO `from`. Never convert to the browser's local time.
  - **Image fallback:** request a size, and on 403 fall back to the standard 320px.
  - **Source market:** `hotelbeds.sourceMarket` defaults to empty ([local: `HotelbedsProperties.java:29`]). For B2C, set it to the visitor's market and don't reuse cached prices across markets.
  - **Content refresh:** the catalog import is bounded at 100 hotels and has no weekly refresh job ([local: `HotelbedsProperties.java:35`]). A weekly (ideally daily incremental) refresh is required if Content API is used.

### Gaps
- No official rule found for displaying minimum stay / release, beyond the generic "entire product must display correctly". These restrictions likely arrive inside rate comments. **[UNVERIFIED]**
- Image usage rights, hotlinking vs self-hosting, and caching rules for photos.hotelbeds.com images: not found in public docs.
- The Crawler specification PDF (certification 5.5) was not read. Its purpose (likely price/rate-parity crawling of the partner site) is unverified.
- Exact field names for supplier name/VAT in the booking response were not verified from public docs in this session.

## 3. Booking flow rules (CheckRate, rateKey lifetime, holder/pax, clientReference, remarks, tolerance, voucher)

### Takeaway
The call sequence is strict:
- one Availability (requesting all rooms and as many hotels as possible, ≤2000)
- then CheckRate only for RECHECK rates (or for BOOKABLE when you need rate comments)
- then one Booking, with a ≥60s timeout

Never re-run availability mid-flow and never parse the `rateKey`. Two Hotelbeds pages contradict each other on CheckRate batching: up to 10 rates per call vs one rateKey per call.

### Cited Findings
- **2.1 / 2.2 [MANDATORY]:** the sequence is Availability, then CheckRate (if applicable), then Booking. "Calling Availability again before CheckRate or Booking is not allowed", and do not repeat Availability during CheckRate or Booking — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- "It is never needed to send more than one availability request (or CheckRates) to make one booking." If your workflow seems to need more, contact HBX Group — [Best practices](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/best-practices/)
- **2.3 [MANDATORY]:** request as many hotels as possible per call, **up to 2000**. **2.4:** request all rooms for a reservation in the same Availability call — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **2.5 CheckRate rule:**
  - Certification says call CheckRate **only when `rateType` = RECHECK** and skip it for BOOKABLE — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
  - Best practices softens this: for BOOKABLE, call CheckRate only "if you need extra information such as RateComments". Do not CheckRate every rateKey from availability — [Best practices](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/best-practices/)
  - A BOOKABLE rate can be booked immediately because "the rate information is already updated". After a successful CheckRate, the rate's type changes RECHECK → BOOKABLE — [Booking workflow](https://developer.hotelbeds.com/documentation/hotels/booking-api/workflow/)
- **Batching conflict:**
  - Certification 2.6 says send **up to 10 rates per CheckRate call** — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
  - Best practices says send **one rateKey per CheckRates**, because "some of HBX Group's external suppliers do not allow valuation of multiple rates at the same time". Never mix RECHECK and BOOKABLE keys in one call — [Best practices](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/best-practices/)
- Don't send CheckRates together with booking. Proceed to confirmation only if the CheckRates completion code is 200 — [Best practices](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/best-practices/)
- **rateKey is opaque [MANDATORY]:** don't parse or process it; copy it from availability into later operations. Its format may change at any time — [Best practices](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/best-practices/)
- **3.11 Timeout [MANDATORY]:** the booking confirmation response timeout must be **≥ 60 seconds** — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/); [Best practices](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/best-practices/)
- **Booking request fields:**
  - `holder{name,surname}`
  - `rooms[]{rateKey, paxes[]{roomId,type(AD/CH),name,surname}}`
  - `clientReference`
  - `remark` (free text)
  - `tolerance` (example 2.00)
  - The workflow example names both adults.
  - [Booking workflow](https://developer.hotelbeds.com/documentation/hotels/booking-api/workflow/); [Postman collection (2018)](https://documenter.gw.postman.com/api/collections/3967924/RW1YqLg1?segregateAuth=true&versionTag=latest)
- **Holder and pax requirements:**
  - Holder name must match the lead pax in the booking request.
  - At least one pax name per room.
  - Children's ages required, so collect child ages at search time (3.3) — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **Tolerance:** a community MCP server describes it as "Allowed % price increase before re-confirmation is required", default 2%. A price drop never blocks the booking. This is a community project, not official docs **[UNVERIFIED]** — [Glama hotelbeds-mcp-server](https://glama.ai/mcp/servers/karanrao/hotelbeds-mcp-server)
- **Error handling:**
  - 400 = request problem; fix it before retrying.
  - **500 = "the product is no longer available" → "you must restart your booking process from the beginning"**.
  - Never resend an identical request after a 400/500.
  - Always check completion codes.
  - [Best practices](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/best-practices/)
- Booking operations cover confirm, list, detail, modify, and cancel or **simulate a cancellation** — [Booking API overview](https://developer.hotelbeds.com/documentation/hotels/booking-api/)

### Inferences
- **Local gaps:**
  - `prebook` **always** calls `/checkrates` and sends **all rooms' rateKeys in one call** ([local: `HotelbedsBookingProvider.java:32,72-75`]). That is defensible only for BOOKABLE rates whose comments you need. It also conflicts with best practice (one rateKey per call; don't mix RECHECK and BOOKABLE).
    - Recommended change: CheckRate each RECHECK rateKey individually.
    - For BOOKABLE rates, either CheckRate (one key per call) solely to fetch `rateComments`, or build comments offline from Content API `rateComments`.
    - Document the chosen approach in the certification workflow email.
  - **The holder falls back to "Guest Guest"** when no lead guest is given ([local: `HotelbedsBookingProvider.java:105-107`]). This would violate 4.3 (holder must match the lead pax). Make lead-guest name/surname required in the API validation.
  - **Read timeout is 60s** ([CLAUDE.md](C:\Users\user\IdeaProjects\bodiala\CLAUDE.md): "5s/60s timeouts"). This meets the ≥60s minimum exactly. Consider ~90s for `/bookings` only, plus an "unknown outcome" path (see §6 reconciliation).
  - **Detail-page re-search:** if the frontend detail view re-runs availability (`/by-hotel-ids`) after a destination search, reviewers may treat it as a repeated availability call (2.2). Pass the selected rate through instead. **[Needs check in bodiala-web]**
  - **On a 500 from CheckRate or Booking,** surface "no longer available, search again". No automatic retry.
- `tolerance` is hardcoded to 2 ([local: `HotelbedsBookingProvider.java:110`]). Make it configurable and show the customer the confirmed price if it differs.

### Gaps
- **rateKey lifetime / validity window:** not stated in any public Hotelbeds page found. Ask apitude@hotelbeds.com. As a workaround, re-search if the user idles.
- Official definitions of `tolerance` and of the `remark` length/usage (and whether remarks reach the hotel) were not found in public docs. The JS-rendered API reference could not be read.
- Whether every guest (not just one per room) must be named at booking time is not stated. Certification requires only one per room.

## 4. Payment models offered by Hotelbeds for B2C partners, and what the partner must collect

### Takeaway
Public docs expose only fragments:
- The rate-level `paymentType` (`AT_WEB` in examples; `AT_HOTEL` appears in a pricing example).
- Net vs commissionable pricing models.
- A separate payment host (`api-secure.hotelbeds.com`, non-mTLS) for "Hotelbeds payment solutions".

The actual payment model (prepaid credit line, card charged by Hotelbeds, pay-at-hotel) is a commercial contract decision made with the HBX sales manager. It is not publicly documented.

### Cited Findings
- **Pricing models (net or commissionable)** are set by contract. "contact your HBX Group sales manager, who will recommend the best solution according to your business needs". Currencies may be up to three depending on destination (EUR, USD, GBP) — [Pricing models](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/pricing-models/)
- Rates carry `paymentType`. The workflow example shows `"AT_WEB"` — [Booking workflow](https://developer.hotelbeds.com/documentation/hotels/booking-api/workflow/). The pricing-model example shows `paymentType AT_HOTEL` on a commissionable-looking rate (net 171.72 / sellingRate 190.80 / commission 10%) — [Pricing models](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/pricing-models/)
- "Payment endpoints don't use mTLS and stay on api-secure.hotelbeds.com". The search summary of the same page says "if you're using Hotelbeds' payment solutions, you can continue using the existing endpoint, api-secure.hotelbeds.com" — [Mutual authentication](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/mutual-authentication/)
- Booking/checkrate responses include `totalNet` and `pendingAmount` in both models, and `totalSellingRate` in commissionable — [Pricing models](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/pricing-models/)
- Hotel-side (supplier) settlement by Hotelbeds is via VCC or bank transfer: "a Virtual Credit Card (VCC) is issued for each booking" for hotels. This is Hotelbeds paying hotels, not the partner–customer flow — [HBX hotel help centre: VCC payments](https://hotelhelpcentre.hbxgroup.com/vcc-payments/); [NightsBridge guide](https://nightsbridge.screenstepslive.com/a/1614932-hotelbeds-payments)
- The certification request asks for "payment information if needed" for the certification URL. Reviewers must be able to complete a test payment on your site — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- `api-contract.md` (project's own earlier extraction from the OpenAPI spec) lists `paymentData{paymentCard, contactData}` as "required for AT_HOTEL / credit-card accounts". This is not re-verified from a public page in this session **[UNVERIFIED]** — [local: `C:\Users\user\IdeaProjects\bodiala\docs\hotelbeds\api-contract.md` §3]

### Inferences
- **For a B2C site the realistic models are:**
  1. **Merchant / prepaid "AT_WEB":** the partner charges the customer with its own PSP (PCI scope is the partner's), and Hotelbeds invoices the partner on a credit line or deposit.
  2. **Card-data model:** card details are passed in `paymentData` via `api-secure.hotelbeds.com`, meaning PCI DSS scope for handling raw PANs. Avoid unless contracted.
  3. **AT_HOTEL rates:** the customer pays the hotel. Must be clearly labelled, with the guarantee card possibly required. **[UNVERIFIED mechanics]**
- **Engineering tasks:**
  - Filter availability by `paymentType` to match the contracted model (the filter exists per the project contract `filter{paymentType}`).
  - Integrate a PSP (authorize before `/bookings`, capture after `CONFIRMED`, void on failure).
  - Collect holder name, email and phone (for the voucher and customer service), plus child ages.
  - Store `totalNet`, `pendingAmount` and currency per booking for reconciliation.
- **PSP ordering risk:** charging the customer before the supplier call risks a paid-but-unbooked state, and charging after risks a booked-but-unpaid state. Use authorize→book→capture/void. This ties into the existing "book() not @Transactional" convention.

### Gaps
- No public Hotelbeds documentation found that defines AT_WEB vs AT_HOTEL commercially, credit-line terms, the `paymentData` schema (card fields, 3DS, `webPartner`, device), or VCC issuance to partners. The API reference is JS-rendered and unreadable via fetch, and the Postman collection's visible sections lack `paymentData`. Ask the HBX sales/technical account manager.
- Whether `AT_HOTEL` rates are available to B2C partners at all is unknown.

## 5. Production technical requirements (mTLS, IP whitelisting, quotas, timeouts, gzip, content caching, Cache API, availability best practices)

### Takeaway
Production booking traffic must use **mTLS at `api-mtls.hotelbeds.com`**, with the test equivalent at `api-mtls.test.hotelbeds.com`. Use a portal-generated client cert, still with `Api-key` + `X-Signature`, and renew it before expiry (an email warning comes 1 month ahead). Other requirements:
- **GZIP is explicitly reviewed at certification.**
- Batch up to 2000 hotels per availability call.
- Booking timeout ≥60s.
- Content stored locally and refreshed at least weekly (daily incremental via `lastUpdateTime` recommended).

Production quotas are not public. They are set per partner.

### Cited Findings
- **mTLS [MANDATORY for production booking]:**
  - Production `api-mtls.hotelbeds.com`, test `api-mtls.test.hotelbeds.com`.
  - Required for availability, CheckRate, booking confirmation, booking list, booking detail, booking change, booking cancellation and booking reconfirmation.
  - Payment endpoints stay on `api-secure.hotelbeds.com` (non-mTLS).
  - [Mutual authentication](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/mutual-authentication/)
- **Certificate handling:**
  - Create it in the developer portal (Dashboard → My API Keys → My API Certificates), alias it, and associate it with API keys.
  - Keep `client.key` private ("HBX Group will never request your private key under any circumstances").
  - Example: `curl --cert client.crt --key client.key --cacert ca.crt -H "Api-key: …" -H "X-Signature: …"`.
  - Certificates must be renewed before expiry; an expired cert cannot connect. HBX emails the portal user **one month before expiration**.
  - [Mutual authentication](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/mutual-authentication/)
- **[UNVERIFIED — search snippet only, not found in the fetched page text]:** max **two certificates per API key**. Once a key is associated with a certificate, it **loses access to non-mTLS endpoints after 14 days** by default — [Mutual authentication (search summary)](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/mutual-authentication/)
- **GZIP [MANDATORY review item]:** the Technical certification area reviews "how well you use GZIP compression" — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **Timeouts:** booking confirmation ≥ 60s — [Best practices](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/best-practices/). No other official timeout values found.
- **Quotas:**
  - Evaluation/sandbox is 50 requests/day — [AltexSoft (Jan 2026)](https://www.altexsoft.com/blog/hotelbeds-api-integration/)
  - An API catalog says exceeding the eval quota returns 403, and that production limits are raised at certification and set per partner under contract **[UNVERIFIED, secondary]** — [API Evangelist plans/pricing](https://raw.githubusercontent.com/api-evangelist/hotelbeds/refs/heads/main/plans/hotelbeds-plans-pricing.yml)
  - The "8 req/4s" burst limit in the project CLAUDE.md was not found in any public source this session.
- **IP whitelisting:** the only mention found is the Cache API configuration parameter "credentials/apiKey with a valid IP" — [Cache API](https://developer.hotelbeds.com/documentation/hotels/cache-api/). No IP whitelisting found for the Booking API.
- **Availability best practices:**
  - Up to 2000 hotels per request.
  - All rooms in one request.
  - Filters optional (only implemented ones are tested).
  - [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
  - Search modes: by `hotels.hotel[]` codes, by `destination{code, zone}` (with `stay.shiftDays` for flexible dates), or by `geolocation{latitude, longitude, radius, unit km|mi}`. Top-level `dailyRate: true` returns per-night prices, and top-level `sourceMarket` (e.g. "UK") sets the market — [Postman collection (2018, may be outdated)](https://documenter.gw.postman.com/api/collections/3967924/RW1YqLg1?segregateAuth=true&versionTag=latest)
  - A flexible-date search returns rates for a set number of days before and after check-in — [Booking API overview](https://developer.hotelbeds.com/documentation/hotels/booking-api/)
- **Content caching:**
  - Store Content API data and **update at least once a week**; use hotel detail for hotels missing from your database — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
  - Do a full initial load (≤1000 hotels per page), then "one request (we recommend on a daily basis) using the parameter lastUpdateTime" to get differential changes — [How to use Content API](https://developer.hotelbeds.com/documentation/hotels/content-api/how-use-content-api/)
  - Each hotel has a `lastUpdate` field — [Last update date](https://developer.hotelbeds.com/documentation/hotels/content-api/last-update-date/)
  - Download rate comments offline in bulk and match them locally — [Use of rate comments](https://developer.hotelbeds.com/documentation/hotels/content-api/use-rate-comments)
- **Cache API (`hotel-cache-api`):**
  - "massive access to all our prices and availabilities providing a snapshot", updated **hourly**, delivered as a single CSV in a .zip.
  - Aimed at price-comparison/metasearch, heavy-traffic sites, third-party price databases, and offers/mailings/packages.
  - Configured per apiKey with a valid IP, destination lists, included/excluded hotels, and classification codes.
  - [Cache API](https://developer.hotelbeds.com/documentation/hotels/cache-api/)
  - The endpoint is `GET /hotel-cache-api/1.0/availabilities` **[secondary]** — [API Evangelist store](https://apis.apievangelist.com/store/hotelbeds-cache-api/)
  - Cache prices are approximate, so live availability gives the real price — [AltexSoft](https://www.altexsoft.com/blog/hotelbeds-api-integration/)
- **Compatibility [MANDATORY]:** "Your code must be prepared to accept non-breaking changes in the API" — [Best practices](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/best-practices/)

### Inferences
- **Local gaps:**
  - **GZIP:** the project deliberately **omits `Accept-Encoding`** so responses come back uncompressed ([CLAUDE.md](C:\Users\user\IdeaProjects\bodiala\CLAUDE.md); [local: `HotelbedsJsonClient.java:27`]). Certification explicitly reviews GZIP usage, so this is a likely **certification failure**. Task: send `Accept-Encoding: gzip` and decompress (e.g. a JDK HttpClient/Apache client with transparent gzip, or a manual `GZIPInputStream`).
  - **Batch size:** `/by-hotel-ids` documents "~50 per request recommended" ([local: `HotelSearchController.java:50`]). Certification wants as many as possible, up to 2000. Change the batching to chunks ≤2000.
  - **mTLS client:** add keystore/truststore config (PKCS12 from `client.crt` + `client.key`), switch the prod base URL to `api-mtls.hotelbeds.com`, add a cert-expiry alert, and support two certs for rotation **[UNVERIFIED limit]**.
  - **Jackson tolerance:** deserialization must ignore unknown properties and enum values. Verify that the DTOs and enums (e.g. `rateType`, `paymentType`, `status`) don't fail on new values (certification 6.3).
- Use the Cache API only if building metasearch or SEO price pages. For a standard B2C search flow, live availability plus a local Content API cache is the documented path.

### Gaps
- Production QPS/daily quotas, 429 behaviour and `Retry-After`: not public.
- Certificate validity period, key types, and whether the CA bundle (`ca.crt`) is downloadable: not found.
- Official per-request timeouts for availability/CheckRate: not found.
- Whether the Cache API requires separate commercial approval: not stated.
- The 14-day non-mTLS cutoff and the 2-certs-per-key limit could not be confirmed from page text (page is ~660k chars, mostly images).

## 6. API features worth implementing (booking list/detail for reconciliation, modification, cancellation simulation, upselling, reviews, filters, geolocation, multi-room, pay-at-hotel)

### Takeaway
The Booking API supports confirm, **list**, **detail**, **modify**, **cancel**, and **cancel-simulation**. It also has a **Reconfirmation** service (push or email) for hotel confirmation numbers, plus upselling in CheckRates, flexible dates (`shiftDays`), `dailyRate`, geolocation and multi-room occupancies. Filters, including TripAdvisor reviews, are optional, but if you implement them they are certified.

### Cited Findings
- **Booking operations:** confirm a booking, get a list of bookings, get booking details, **modify a booking**, and **cancel or simulate a cancellation** to see its conditions — [Booking API overview](https://developer.hotelbeds.com/documentation/hotels/booking-api/)
- **Cancellation simulation:** `DELETE /bookings/{ref}?cancellationFlag=SIMULATION` returns the charge without cancelling; `CANCELLATION` cancels — [Glama community MCP server](https://glama.ai/mcp/servers/karanrao/hotelbeds-mcp-server); consistent with [local: `api-contract.md` §5]
- **Reconfirmation Service** with Push Service and Email Service sub-pages (details not public on the overview page). Booking reconfirmation is listed among mTLS operations — [Booking API overview](https://developer.hotelbeds.com/documentation/hotels/booking-api/); [Mutual authentication](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/mutual-authentication/)
- **Upselling:** CheckRates "returns upselling options and rates when available", with a tag showing the difference from the requested rate — [Booking API overview](https://developer.hotelbeds.com/documentation/hotels/booking-api/)
- **Filters:** recommended, not mandatory; only implemented filters are tested — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/). The search summary of the certification page references a `reviews` block of type `TRIPADVISOR` alongside a filter with max hotel count and min rate **[UNVERIFIED, snippet only]** — [Certification process (search summary)](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **Geolocation and destination+zone search, `shiftDays` flexible dates, `dailyRate`, `sourceMarket`** — [Postman collection (2018)](https://documenter.gw.postman.com/api/collections/3967924/RW1YqLg1?segregateAuth=true&versionTag=latest)
- **Multi-room:** use `rooms` count for identical occupancies; split nodes when they differ — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **Tax breakdown** (opt-in) gives the B2C "payable at hotel" amounts — [Tax breakdown](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/tax-breakdown/)
- **Promotions/offers** display recommended — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **Hotel detail on demand** for hotels not in your DB — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)

### Inferences
- **Backlog, by priority for B2C launch:**
  1. **[MANDATORY-adjacent]** Rate comments plus voucher generation and email delivery (see §2).
  2. **[RECOMMENDED, high value]** Reconciliation: a scheduled `GET /bookings` list (by creation/check-in window) plus `GET /bookings/{ref}`, to catch orphan bookings when `/bookings` times out or the local persist fails. bodiala already logs "BOOKING PERSIST FAILED … recover/cancel manually" ([local: `HotelbedsBookingProvider.java:139-140`]).
  3. **[RECOMMENDED]** A cancellation-fee quote via `cancellationFlag=SIMULATION`, shown before the user confirms cancel. grep found no `SIMULATION` in `provider.hotelbeds`, so it's not implemented.
  4. **[RECOMMENDED]** Tax breakdown enabled, with "payable at hotel" display.
  5. **[OPTIONAL]** Filters (price, board, category, reviews), geolocation/map search, flexible dates, upselling, booking modification (`PUT /bookings/{ref}`, schema not verified), and reconfirmation push to surface hotel confirmation numbers.
- Each optional filter adds certification test surface, so ship a small set first.

### Gaps
- Booking list query params (`from`/`to`/`start`/`end`/`filterType`/`status`), the booking-change body and modes, and the reconfirmation push payload were not verified. The official API reference is JS-rendered, and the Postman sections fetched didn't include them.
- The exact TripAdvisor review response fields and the filter schema (`maxHotels`, `maxRatesPerRoom`, `minCategory`, etc.) were not verified from a readable official page.

## 7. Known pitfalls and common certification failures

### Takeaway
Hotelbeds publishes no list of "common failures". The documented rules imply these failure points:
- extra availability/CheckRate calls
- CheckRate on BOOKABLE rates
- small hotel batches
- no GZIP
- missing rate comments or cancellation policies before confirmation
- ignoring `hotelMandatory` or the source market
- voucher lacking the "Payable through" line or rate comments
- live test booking left uncancelled or made on an NRF rate

### Cited Findings
- **Workflow:** "never needed to send more than one availability request (or CheckRates) to make one booking" — [Best practices](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/best-practices/); re-calling availability mid-flow is "not allowed" — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **Retry policy:** blind retries of 400/500 are wrong; a 500 means restart from search — [Best practices](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/best-practices/)
- **rateKey parsing** is prohibited because the format may change — [Best practices](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/best-practices/)
- **Live test:** use a refundable rate and cancel promptly or be charged — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/); [AltexSoft](https://www.altexsoft.com/blog/hotelbeds-api-integration/)
- **Sandbox vs production** distinction confused the AltexSoft author. Sandbox bookings create no real reservations — [AltexSoft](https://www.altexsoft.com/blog/hotelbeds-api-integration/)
- **Pricing compliance:** non-compliance with `hotelMandatory` selling rates "may face exclusions" — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **Taxes:** adding included taxes on top of the price is wrong ("must not be added to the final price") — [Tax breakdown](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/tax-breakdown/)
- **Cancellation times:** showing deadlines in customer-local time is wrong; they are destination-based — [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/)
- **Images:** a missing image size returns 403 `AccessDenied`. Handle it or you get broken images — [Use of images](https://developer.hotelbeds.com/documentation/hotels/content-api/use-images)
- **mTLS cutover / cert expiry:** an expired certificate cannot connect — [Mutual authentication](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/mutual-authentication/). Possible 14-day loss of non-mTLS access after association **[UNVERIFIED]**.
- **Test environment:** don't do hotel mappings there — [Best practices](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/best-practices/)
- **Doc inconsistencies to clarify with Hotelbeds before cert:**
  - CheckRate batching (≤10 vs 1 per call).
  - CheckRate on BOOKABLE (never vs only for rate comments).
  - Image sizes (two pages disagree).
  - The workflow page's prose date (2020-06-15) vs its JSON (2021-06-15).
  - [Certification process](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/certification-process/); [Best practices](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/best-practices/); [Booking workflow](https://developer.hotelbeds.com/documentation/hotels/booking-api/workflow/)

### Inferences
- **bodiala-specific likely failures, in priority order:**
  1. No GZIP (`Accept-Encoding` omitted).
  2. No rate comments in UI or voucher.
  3. No voucher with the "Payable through … VAT … Reference" line.
  4. CheckRate always called, with multiple rateKeys in one call.
  5. Hotel-ID batches of ~50 instead of up to 2000.
  6. Holder fallback "Guest Guest".
  7. `hotelMandatory` / taxes ignored.
  8. Source market unset.
  9. Possible repeated availability on the detail page (check the frontend).
  10. Content refresh not scheduled weekly.
- Third-party "pre-certified" aggregators (e.g. ZentrumHub, Vervotech) market themselves as a way to skip certification work. They are not neutral sources — [ZentrumHub](https://www.zentrumhub.com/hotelbeds-api-integration/)

### Gaps
- No official or community list of actual certification rejection reasons was found. The items above are inferred from the documented checklist.
- No public post-mortems or forum threads about Hotelbeds certification failures were found in this session.
