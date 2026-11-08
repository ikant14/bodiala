# B2C hotel-booking UX, conversion and growth best practices (input for the Bodiala launch backlog)

Research date: November 2026. Sources are dated where possible. Anything older than 2023 is marked **[OLDER]**. Much of Baymard's detailed travel research sits behind a paywall, so only its free articles are used here. Effort tags in the Inferences (S/M/L = small/medium/large for a 1–3 person team) are my own estimates, not sourced.

---

## 1. Search: destination autocomplete, date pickers, rooms/guests (incl. children's ages), defaults, recent searches

### Takeaway
The booking search box is the main way into an accommodation site. 99% of Baymard test participants looked for it first, so it must be the biggest, highest-contrast element above the fold, and nothing (cookie banners, chat widgets) may cover it. Use an autocomplete destination field capped at about 8 suggestions on mobile and 10 on desktop. Use a date picker that shows unavailable dates and, ideally, price hints. Use a guest selector that asks for each child's exact age, because supplier APIs need ages to price correctly.

### Cited Findings
- 99% of participants on OTA, large-brand hotel and whole-property-rental sites immediately looked for the "Booking" search on the homepage. Baymard (Aug 2022) found 25% of sites don't make it the primary homepage content [OLDER] — [Baymard](https://baymard.com/research-articles/travel-accommodations-booking-search). A July 2025 Baymard article gives 30% — [Baymard 2025](https://baymard.com/blog/travel-site-ux-best-practices). (The two figures come from different benchmark versions, not necessarily a trend.)
- At IHG, the booking search fell below the fold for 78% of participants. One Marriott participant took nearly 30 seconds to find it after scrolling. At Sonder a cookie notice partly covered it. At Plum Guide a cookie notice and a sticky chat element obscured it on mobile — [Baymard](https://baymard.com/research-articles/travel-accommodations-booking-search)
- At Marriott, participants had to scroll in the middle of filling the form to reach the "Rooms & Guests" and "Special Rates" menus, which adds effort and error risk — [Baymard](https://baymard.com/research-articles/travel-accommodations-booking-search)
- Recommended styling: place it in the upper part of the page, use strong contrast against background imagery, a vibrant "Search" button, large inputs and font, bold borders, and visually wrap the whole form including the button. Plum Guide's search spans nearly the full page width — [Baymard 2025](https://baymard.com/blog/travel-site-ux-best-practices); [Baymard 2022](https://baymard.com/research-articles/travel-accommodations-booking-search)
- Boutique-hotel shoppers behave differently: 73% browsed property and room information first and 27% checked availability first. Progressive disclosure works there (e.g., show check-in/out + "Book Now" first, then reveal more fields) — [Baymard](https://baymard.com/research-articles/travel-accommodations-booking-search)
- Date pickers: show minimum nightly rates by date. A NobleDEN user couldn't tell whether shifting dates would save money. Expedia's picker showed weekday/weekend price differences and clearly marked unavailable dates, and users responded positively [OLDER, Feb 2022] — [Baymard](https://baymard.com/research-articles/new-research-travel-accommodations)
- NN/g's general date-input guidance: let users type a date even when other input methods exist, and avoid ambiguous formats — [NN/g](https://www.nngroup.com/articles/date-input/). The US Web Design System notes that some users prefer typing to using a calendar — [USWDS](https://designsystem.digital.gov/components/date-picker/)
- Autocomplete: once suggestions exceed about 10 on desktop or about 8 on mobile, users ignore them or spend more effort scanning. Baymard recommends at most 10 on desktop and 4–8 on mobile (Wayfair caps at 9) — [Baymard](https://baymard.com/research-articles/autocomplete-design). Redundant or irrelevant suggestions add clutter and make picking harder — [Baymard guideline 368](https://baymard.com/guidelines/368-redundant-and-irrelevant-suggestions)
- Recent/trending searches: vendor guidance (not Baymard) suggests 5–10 items per section — [Voyado](https://help.elevate.voyado.com/hc/en-gb/articles/27610388933532)
- Tiket.com (an Indonesian OTA) ranks destination autocomplete with a learning-to-rank model trained on historical autocomplete events (which items users saw and which they chose) — [Tiket.com engineering](https://medium.com/tiket-com/algorithm-behind-search-in-tiket-com-4bfaeb42980b)
- Flexible dates: Booking.com researchers describe a "Dates Flexibility" model that estimates how flexible a user is and decides whether to recommend alternative dates. It is one of hundreds of ML models, all validated by randomized controlled experiments — [Booking.com paper, arXiv](https://arxiv.org/pdf/1802.03102.pdf). Airbnb's flexible search was positioned as adding demand by moving "beyond intent to inspiration" [OLDER, 2022] — [Skift](https://skift.com/2022/05/04/airbnbs-flexible-searches-working-to-add-demand-moving-beyond-intent-to-inspiration/)
- Children: Booking.com's Demand API expects the age of every child and says to keep ages intact through the whole flow, never converting children to adults to get a price or make an allocation fit — [Booking.com developers](https://developers.booking.com/demand/docs/accommodations/occupancy-use-cases). Chain booking engines ask only for ages and then classify each child against each hotel's own age bands — [Mirai](https://www.mirai.com/blog/mirai-optimised-for-any-type-of-occupancy-guests-adults-and-children-or-adults-only/). One booking help centre counts a child's age on the check-out date, so the UI should say which date applies — [HalalBooking help](https://halalbookinghelp.freshdesk.com/support/solutions/articles/44002781638-how-do-i-calculate-my-child-s-age-)

### Inferences
- (S) Make the search panel the hero on the home page and destination pages. Make sure the cookie banner and any chat bubble never overlap it on a 375px viewport.
- (M) Replace any city dropdown with a typeahead over the cached Hotelbeds catalog: destinations/cities first, then hotels by name, with a type label and icon per row, 8 rows max on mobile. Show recent searches (localStorage) when the field gets focus. Landmarks/POIs need geo data the current cache may not have (L).
- (S) Default to 2 adults, 1 room, check-in a few weeks out, 1–3 nights. Require an age dropdown (0–17) for each child before search is enabled, because Hotelbeds occupancy pricing depends on ages. Persist the last search in the URL so results are shareable, bookmarkable and crawlable.
- (M) On mobile, a full-screen date sheet with a two-tap range selection, showing nights selected. Price-per-date hints need a cheap calendar-price source; skip until the Hotelbeds quota allows (L).

### Gaps
- No free, quantitative study comparing autocomplete with dropdowns for destination entry in travel. Baymard's detailed date-picker and guest-selector guidelines are paywalled.
- No primary Booking.com/Expedia engineering data on flexible-date lift found.

---

## 2. Results page: what drives clicks, filters, sort, pagination, price transparency, badges, and what regulators treat as manipulative

### Takeaway
Show photo, total stay price including all mandatory taxes and fees (shown as the most prominent figure in the US and UK by law), review score and count, free-cancellation and board status, and location or distance, with a map view and 5–10 travel-specific filters (price, rating, stars, free cancellation, board, amenities). Urgency or scarcity claims must be true and tied to the user's dates. Discounts must reference a genuine price. Rankings influenced by commission must be disclosed.

### Cited Findings
**Information and filters**
- Users who couldn't find the "Map View" button at IHG copied addresses into external searches. Maps and lists become cluttered in large cities such as New York [OLDER, 2022] — [Baymard](https://baymard.com/research-articles/new-research-travel-accommodations)
- Essential filters are price and guest rating. Industry-specific ones include pool and on-site restaurant. Users wanted a bed-size filter: an Expedia user looking for a king bed found none [OLDER, 2022] — [Baymard](https://baymard.com/research-articles/new-research-travel-accommodations)
- 40% of travel sites don't provide popular industry-specific filters (pool, on-site restaurant, free parking). Baymard recommends at least 5–10 specific filter types. A Marriott shopper abandoned the site after finding no star-rating filter. Expedia and TurnKey shoppers wanted bed-size, air-conditioning and parking filters (Jul 2025) — [Baymard](https://baymard.com/blog/travel-site-ux-best-practices)
- Good filter categories are relevant, predictable, jargon-free and prioritized. Derive them from user interviews and search logs. NN/g's example is Expedia's amenity filters (pool, free parking) — [NN/g](https://www.nngroup.com/articles/filter-categories-values/)
- The Jan 2026 benchmark of 9 sites (Booking.com, Airbnb, Hyatt, Best Western, Trip.com, Expedia, Shangri-La, Esmé, HomeToGo) found most get basics such as price visibility and imagery right. They often fail on "deeper essentials": transparent pricing, effective filters and review features. Only one site scored "decent" and none scored "good" — [Baymard 2026](https://baymard.com/research-articles/travel-accommodations-ux-benchmark-2026)
- Most sites show nightly rates early but often fail to convey total stay cost, fee explanations or per-night breakdowns. One Sohotel "Standard Queen" room showed 4 different prices, each "Excluding Taxes & Fees" — [Baymard](https://baymard.com/research-articles/new-research-travel-accommodations)

**Pagination vs infinite scroll**
- Baymard (via Smashing Magazine) [OLDER, 2016]: infinite scroll led users to browse more but scan more shallowly. The recommended hybrid is to lazy-load about 10–30 items, then switch to a "Load more" button after roughly 50–100 items — [Smashing/Baymard](https://smashingmagazine.com/2016/03/pagination-infinite-scrolling-load-more-buttons); [Baymard](https://Baymard.com/blog/external-load-more-vs-pagination-vs-infinite-scrolling)
- NN/g covers alternatives to pagination and notes that shoppers had no problems with "Show More" buttons — [NN/g](https://www.nngroup.com/articles/alternatives-pagination-listing-pages/)
- Googlebot can't scroll or click "Load more", so crawlable listing pages need real paginated URLs (secondary source) — [Meilisearch](https://meilisearch.com/blog/pagination-vs-infinite-scroll-vs-load-more)

**Regulation: price display**
- **US, FTC Rule on Unfair or Deceptive Fees (16 CFR 464), in force 12 May 2025:** it covers short-term lodging (hotels, motels, vacation rentals), including third-party platforms and travel agents. Any displayed price must clearly, conspicuously and prominently disclose the total price. Fees may be itemized, but the total must be the most prominent figure. Government charges, shipping and optional add-ons may be excluded — [Barnes & Thornburg](https://btlaw.com/en/insights/alerts/2025/ftc-finalizes-junk-fees-rule-new-pricing-disclosure-requirements-take-effect-may-12-2025); [NatLawReview FAQs](https://natlawreview.com/article/ftc-rule-unfair-or-deceptive-junk-fees-faqs). Per one source, civil penalties are up to $51,744 per violation as of 2024 — [same](https://natlawreview.com/article/ftc-finalizes-junk-fees-rule-new-pricing-disclosure-requirements-take-effect-may-12)
- **UK, DMCC Act drip-pricing ban, in force 6 April 2025:** mandatory charges (admin and booking fees, service charges, taxes, resort fees, tourist taxes) must be in the headline price. Optional extras such as breakfast can be excluded. A CMA example: a £100 hotel rate that reveals a mandatory "weekend surcharge" at the last step — [CMS law](https://cms.law/en/gbr/legal-updates/navigating-the-new-consumer-protection-regime-what-travel-companies-need-to-know); [Linklaters](https://www.linklaters.com/en/insights/blogs/linkingcompetition/2025/june/consumer-deep-dive_understanding-drip-pricing-under-the-dmcc); [Which?](https://www.which.co.uk/news/article/are-travel-firms-breaking-the-law-on-drip-pricing-a86Ag8b7ZT5W). A law-firm roundup reports a CMA fine of about £900k on StubHub UK (23 Jun 2026) and drip-pricing investigations opened 19 Aug 2026 into Trainline, Virgin Atlantic (resort fees and local taxes on packages) and others. This comes from a single secondary source; verify on CMA case pages — [Mondaq](https://www.mondaq.com/antitrust-eu-competition/1688286/drip-pricing-ban-what-online-businesses-need-to-know)
- **California:** under its junk-fee law, Expedia displays the total cost upfront in bold, including taxes and fees, for hotels. Booking.com, Airbnb and Vrbo were reported as not showing the full total upfront at the time — [Skift](https://skift.com/?p=691472)
- **EU, CPC network:** Booking.com (commitments dated 18 Dec 2019) and Expedia Group (Expedia, ebookers, hotels.com; 18 Dec 2020) committed to change how they present offers, discounts and prices in line with EU consumer law. Airbnb's total price must include all mandatory charges (2018–2019) — [European Commission](https://commission.europa.eu/topics/consumers/consumer-rights-and-complaints/enforcement-consumer-protection/coordinated-actions/accommodation-booking_en). Secondary summaries of the Booking.com commitments: show the total price including all unavoidable charges, fees and taxes that can reasonably be calculated in advance, clearly and prominently. Discounts must represent genuine savings, with details of the reference "standard rate". Authorities had concerns about hidden sponsoring in ranking, undue time pressure and misrepresented rebates — [EUbusiness](https://eubusiness.com/post/booking-com-eu-20ek); [CDE UAL](https://www.cde.ual.es/en/booking-com-commits-to-align-practices-presenting-offers-and-prices-with-eu-law-following-eu-action/)
- **EU Omnibus price-reduction rule:** any announced price reduction must show the "prior price", which is the lowest price in the preceding 30 days. Strike-through prices should match that 30-day low, not an "original" price — [RPC](https://www.rpclegal.com/snapshots/consumer/spring-2022/european-commission-publishes-guidance-on-price-promotions-under-the-omnibus-directive/); [Talon.One](https://www.talon.one/blog/eu-requirements-for-advertising-with-price-reductions). No hotel- or dynamic-pricing-specific guidance was found.
- **EU Digital Fairness Act:** consultation ran Jul–Oct 2025 and the summary was published Dec 2025. The Commission aims to adopt a proposal in late 2026. Targets include dark patterns, drip pricing, and "starting from" prices where dynamic pricing applies. The proposal was not confirmed as tabled when this research was done — [Taylor Wessing](https://www.taylorwessing.com/de/interface/2025/the-dmcca/eu-consumer-law-reform-is-coming-but-when-will-it-happen-and-what-will-it-cover); [Slaughter and May](https://thelens.slaughterandmay.com/post/102m222/digital-fairness-act-european-commission-publishes-responses-to-consultation)

**Regulation: urgency, scarcity and ranking (UK CMA undertakings, 6 Feb 2019, in force 1 Sep 2019; Agoda, Booking.com, ebookers, Expedia, Hotels.com, trivago)**
- Make clearer how hotels are ranked after a search, and say when commission paid by a hotel affected ranking. Don't give a false impression of a hotel's availability or popularity, or rush customers on incomplete information. When saying others are viewing a hotel, make clear they may be searching for different dates. Don't strategically place sold-out hotels in results to pressure people. Promote only deals actually available, without comparisons to irrelevant prices. Include compulsory charges (taxes, booking and resort fees) in the headline price. A breakdown is fine but the total must be shown upfront — [GOV.UK/CMA](https://www.gov.uk/government/news/hotel-booking-sites-to-make-major-changes-after-cma-probe) [OLDER but still the reference standard]
- NN/g lists social proof and scarcity cues among deceptive patterns. It also notes that Booking.com hides full reviews behind "Read more", so the visible review text skews positive — [NN/g](https://www.nngroup.com/articles/deceptive-patterns/)

### Inferences
- (S) Results card, in priority order: hero photo; hotel name, stars and area; review score and count with the source; **total stay price incl. mandatory taxes/fees as the largest figure**, with "€X / night" secondary and a "taxes & fees included" or "+ €Y city tax payable at hotel" line; free-cancellation date or "Non-refundable"; board type (Room only, Breakfast incl.). Hotelbeds gives rate-level `boardName`, cancellation policies and taxes, so most of this is data the backend already has.
- (S) Hotels with no availability for the searched dates should be filtered out, or shown only at the end, clearly labelled. Never mix sold-out hotels in to create urgency.
- (S) Only show "X rooms left" if it comes straight from supplier `allotment` for the user's exact dates and rate. Don't show "N people looking". Don't show strike-through prices unless a genuine 30-day-low reference exists. Safest launch posture: no urgency badges at all.
- (S–M) Filters: price slider (total stay), review score, stars (already built), free cancellation, board type, and 3–5 amenities from the facility catalog (pool, parking, Wi-Fi, A/C, family). Sort options: Recommended (with a one-line "how we rank" explanation link), lowest price, highest rating, distance. Disclose if any ranking is paid.
- (M) Map view toggle with clustered pins showing price. (L) Map-area search.
- (S) "Load more" after about 20–30 results rather than pure infinite scroll. Destination SEO pages need real paginated URLs.

### Gaps
- No public A/B data quantifying the click impact of total-vs-per-night price display or of individual card elements at OTAs. Baymard's quantified results-page guidelines are paywalled.
- No authoritative data found on most-used filters at OTAs (usage share per filter).
- No source found on default sort order performance.
- No EU text was retrieved on whether accurate low-availability messages ("1 room left") are acceptable. The CMA's principle targets *false impressions*. EU Omnibus/UCPD ranking-disclosure obligations weren't verified in this research.

---

## 3. Hotel page: room/rate presentation, photos, reviews, maps, policies, trust

### Takeaway
Users want a rich gallery, detailed room content (bed type, bathroom, size, view), and a clear rate table where each rate shows total price, board and cancellation terms in plain words. Third-party review data is trusted more than site-curated testimonials, and should link through to the source.

### Cited Findings
- Boutique-hotel users scroll first to get a feel for the property, then look for the photo gallery and amenities list. Room descriptions need detail: a NobleDEN user couldn't tell whether rooms had a shower or bathtub, and missing room photos made it worse. Unclear or inconsistent offer messaging caused confusion and mistrust. Optional extras (floor preference, early check-in) add complexity and distract from booking [OLDER, 2022] — [Baymard](https://baymard.com/research-articles/new-research-travel-accommodations)
- Review functionality is a frequent weakness even among top sites (Jan 2026) — [Baymard 2026](https://baymard.com/research-articles/travel-accommodations-ux-benchmark-2026)
- 85% of travel (tours) sites don't consistently link to third-party reviews. Site-curated testimonials seemed biased to users. Baymard recommends that aggregate ratings (Tripadvisor, Google) link to the underlying reviews (Jul 2025) — [Baymard](https://baymard.com/blog/travel-site-ux-best-practices)
- 57% of tour sites lack a map on the details page. Users Googled meeting points and risked finding a competitor. A map showing location relative to their accommodation gave a shopper the confidence to book — [Baymard](https://baymard.com/blog/travel-site-ux-best-practices). (Tours, but the same pattern applies to hotel location.)
- Expedia eye-tracking study [OLDER, 2014]: the image that evoked most delight was a bedroom with a window view. Natural-looking photos built trust, while fisheye or telephoto distortion drew suspicion. Close-ups help only when paired with whole-room shots — [Hotel-Online / Expedia](https://www.hotel-online.com/news/hotel-images-matter-an-empirical-based-guide-to-hotel-photos). Expedia's partner tool targets 20+ photos per property — [same](https://www.hotel-online.com/news/hotel-images-matter-an-empirical-based-guide-to-hotel-photos)
- Expedia/Unabashed Research conjoint study, 903 US consumers [OLDER, 2019]: price was the single biggest factor. Brand mattered slightly more than renovations, room photos or star rating — [Travel Weekly AU](https://www.travelweekly.com.au/article/rooms-with-views-sell-expedia/)
- Reviews: 95% of travelers read reviews before booking, per a TrustYou survey of 800 US bookers (Sep 2022) — [TrustYou via search summary](https://en.beta.10minhotel.com/author/catalina-brinzatrustyou-net). YouGov (2023, 18 markets): 57% check other travelers' experiences before booking a hotel — [YouGov](https://business.yougov.com/content/8244-how-do-global-travelers-research-before-booking-a-hotel). An older Tripadvisor/PhoCusWright survey found travelers read 6–12 reviews and 78% focus on the most recent [OLDER, undated] — [Hospitality Net](https://hospitalitynet.org/news/4064007.html)
- Google Hotel Center landing-page rules (good practice even outside Google): show the total price, ideally with a tax/fee breakdown. Clearly display refundable-rate terms on landing and booking pages; "free cancellation until" refers to the hotel's local time, defaulting to midnight. Mark member rates as such and say whether sign-in is required — [Google Price Accuracy Policy](https://support.google.com/hotelprices/answer/6064419)

### Inferences
- (M) Rate table grouped by room type (room photo, bed type, size, max occupancy). Under each room, one row per rate with: board type; a cancellation line ("Free cancellation until 14 Mar 23:59 hotel time" / "Non-refundable"); payment timing; total for the stay incl. taxes; and a single "Select" CTA. Hotelbeds returns rates per room with `boardName`, `cancellationPolicies[]` (from/amount), and `rateType` (BOOKABLE/RECHECK). Convert the cancellation policies into one human sentence.
- (S) Gallery: hero plus a grid of 4 on desktop, swipeable full-screen on mobile, lazy-load all but the first image (see §8). Use Hotelbeds image types (room vs general) to put room photos inside each room group.
- (S–M) Reviews: if supplier review data (e.g., Tripadvisor rating and count) is available through Hotelbeds content or a licence, show score, count and source, and link to the source per Tripadvisor terms. Don't invent or curate testimonials. Unverified: whether Hotelbeds supplies Tripadvisor data to this account (see Gaps).
- (S) Embedded map with hotel pin (Leaflet/OSM to avoid Google Maps costs), plus the address and distance to the city centre.
- (S) Policies block: check-in/out times, city-tax note (payable at hotel), child/extra-bed policy, essential remarks from the supplier (Hotelbeds `rateComments`, which are legally important to display).

### Gaps
- Could not confirm whether Hotelbeds APItude exposes Tripadvisor review scores and counts (search found nothing). Check developer.hotelbeds.com or ask the account manager.
- No current (2023+) quantitative data on photo count or gallery design vs conversion. The Expedia data is 2014/2019.
- No public data on rate-table layouts (grouped vs flat) and conversion.

---

## 4. Checkout: steps, fields, guest checkout, payment, price change/sold-out handling, confirmation (Baymard findings)

### Takeaway
Unexpected costs are the top reason for abandonment (40%), followed by trust, forced accounts, length and errors. A hotel checkout should be guest-by-default with about 7–8 fields, show a final cost summary with no new fees, explain why phone and email are needed, and handle supplier rechecks (price changes or sold-out rates) explicitly instead of failing silently.

### Cited Findings
- Average documented cart abandonment is 70.22% (unweighted average of 50 studies, 2006–2025; range 55–84%). Page updated 28 Oct 2026 — [Baymard](https://baymard.com/lists/cart-abandonment-rate)
- Checkout abandonment reasons (US, excluding "just browsing"): extra costs too high 40%; didn't trust the site with card details 19%; site required account creation 18%; checkout too long or complicated 17%; website errors/crashes 17%; couldn't see total cost up front 12%; card declined 10%; not enough payment methods 9% — [Baymard](https://baymard.com/lists/cart-abandonment-rate). (Secondary sites quote 18–24% for forced accounts; Baymard's own page says 18%.)
- The average US checkout shows 23.48 form elements by default (14.88 fields). The ideal is 12–14 elements (7–8 fields). Solvable checkout UX issues could lift conversion by 35.26% for an average large e-commerce site — [Baymard](https://baymard.com/lists/cart-abandonment-rate)
- Baymard "Current state of checkout UX" (Nov 2024, updated Nov 2026; retest due Q2 2027):
  - Save account creation for the confirmation step (56% of sites don't).
  - Collapse completed accordion steps into summaries (72% don't).
  - Mark both required and optional fields (50% don't). 32% of test participants missed a required field when only optional fields were marked.
  - Explain why a phone number is required (47% don't). 68% of respondents were reluctant to give a phone number.
  - Related articles: make guest checkout the most prominent option (47% don't); avoid complex password rules (82% don't); adaptive validation error messages (98% don't); localized input masks (64% don't use them).
  - — [Baymard](https://baymard.com/blog/current-state-of-checkout-ux)
- Guest-checkout prominence: other Baymard pages cite 62% of sites failing it, and an older version cited 65% (figures differ by benchmark version). Guest checkout should be at the top of the account-selection step — [Baymard](https://baymard.com/blog/make-guest-checkout-prominent)
- Travel-specific: taxes and fees added at checkout are numerous and hard to understand. Accommodation users are more suspicious of last-minute fees than general e-commerce shoppers. A subgroup drop the property if new or unexplained fees appear in the final cost summary. Users scrutinize the final summary and reservation details closely [OLDER, 2022] — [Baymard](https://baymard.com/research-articles/new-research-travel-accommodations)
- Google Price Accuracy Policy (relevant if listing on Google later):
  - The total on the booking page must be prominently displayed and match Google's price exactly. No numeric tolerance is stated.
  - Mandatory fees (incl. resort fees) included. Optional fees not included by default.
  - Taxes and fees disclosed on the final booking page without extra user action.
  - Price matches the occupancy and dates selected.
  - The user must be able to complete the booking at the shown price, resulting in a confirmed booking within 24 hours.
  - — [Google](https://support.google.com/hotelprices/answer/6064419)
- Payments/SCA (EU PSD2): 3-D Secure challenges cause drop-off, especially at the last mobile step. A Stripe deck cited about 11% average EU drop-off when 3DS is used [OLDER, directional] — [Paybyrd](https://paybyrd.com/knowledge-hub/how-airlines-can-meet-psd2-and-sca-requirements-without-sacrificing-conversion); [SlideShare/Stripe](https://www.slideshare.net/slideshow/optimising-payments-for-strong-customer-authentication-sca/150154513). Card wallets such as Apple Pay have multi-factor authentication built in, which gives SCA-compliant, lower-friction checkout — [Checkout.com](https://www.checkout.com/docs/business-operations/ensure-regulatory-compliance/sca-compliance-guide). Exemptions (low value under €30, low risk/TRA, merchant-initiated) need PSP support — [2Checkout](https://blog.2checkout.com/?p=20931)

### Inferences
- (S) Two steps after rate selection: (1) Guest details plus payment on one page, with a sticky price summary; (2) confirmation. Minimum viable fields:
  - Lead guest first name and surname (Hotelbeds `holder` + `paxes`)
  - Email (with "your confirmation goes here")
  - Phone (explain "only used by the hotel/us if there's an issue with your booking")
  - Optional special request (free text, "not guaranteed")
  - Payment
  - Collect other room occupants' names only if the supplier requires them per room
- (S) No account wall. Offer "save your details / create an account" on the confirmation page only.
- (S) Final summary: hotel, room, board, dates and nights, guests (incl. child ages), cancellation sentence, total paid now, amounts payable at hotel (city tax) in local currency. These must equal what was shown on results and the hotel page. Show a "Price confirmed" state after Hotelbeds `checkrates`.
- (M) Price-change and sold-out handling: `checkrates` (always called) may return a different `net` or fail. If the price goes up, show an interstitial with old vs new total and require explicit acceptance. If it goes down, honour the lower price and say so. If sold out, offer the other rates/rooms at the same hotel and a "back to results" link with the search preserved. Never charge before `POST /bookings` succeeds. Pair this with the existing idempotency (`agent_ref_no`).
- (M) Payments: use a PSP with hosted fields plus Apple Pay/Google Pay and 3DS2 with exemption handling. Card data never touches bodiala (keeps PCI scope at SAQ-A).
- (S) Confirmation page: booking reference big and copyable, hotel contact, check-in instructions, cancellation deadline, "Add to calendar", "Manage booking" link. Send the same as a transactional email immediately.

### Gaps
- No free Baymard data specific to hotel checkout step count or field count (only general e-commerce).
- No hotel-specific data on 3DS drop-off or wallet uplift. Measure in-house (split funnel by 3DS challenge or frictionless, and by wallet).
- No studies found on price-change interstitial designs.

---

## 5. Post-booking: manage booking, self-service cancellation, reminders, support channels

### Takeaway
Self-service handles most post-booking contacts at the big OTAs and is rated far higher than calling. A small OTA should ship a lookup by reference + email, one-click cancellation within policy, and well-timed transactional emails before investing in phone support.

### Cited Findings
- Expedia: its AI service agent handles more than 143 million conversations a year and has helped over half of travelers self-serve without calling. Travelers who self-serve report satisfaction roughly double that of those who call (undated newsroom page) — [Expedia newsroom](https://www.expedia.com/newsroom/?p=49550)
- During COVID, Expedia's share of air-cancellation inquiries handled without an agent rose from about 65% (Feb 2020) to more than 95% the next month [OLDER] — [Travel Daily via PressReader](https://www.pressreader.com/australia/travel-daily/20200522/281578062862577). Expedia Partner Solutions reported 35% of traveller requests resolved by its virtual agent [OLDER] — [TravelMole](https://travelmole.com/news/expedia-partner-solutions-launches-ai-virtual-agent)
- About 20% of hotel reservations are cancelled, and 22% of rates were sold as non-refundable in 2024 (Mews PMS data, vendor-specific) — [Mews](https://www.mews.com/en/blog/non-refundable-hotel-rates)
- Kayak notes that hotel prices can drop after booking and suggests free-cancellation rates so users can rebook — [Kayak](https://www.kayak.com/news/?p=126850)

### Inferences
- (S) "Manage booking" page: look up by booking reference + email (no account). Rate-limit it and return a generic "not found" to prevent enumeration. Show status, cancellation deadline and the fee if cancelled now (from stored policies), with a Cancel button and confirm dialog. Show "Cancelled, refund of €X on your card within N days" and email it. This maps onto the existing `/api/bookings/{id}` + `DELETE` cancel flow and the stored-booking repo.
- (S) Transactional emails:
  - Confirmation (immediately)
  - "Free cancellation ends in 48h" reminder for refundable bookings (a retention and trust signal, and it reduces disputes)
  - Pre-arrival (3 days before: address, check-in time, city tax reminder)
  - Post-stay review request
  - Cancellation confirmation
- (S) Support: start with email or a contact form plus an FAQ that answers the top contacts (cancel, change name or dates, payment at hotel, invoice). Add chat later. Show support contact on the confirmation page and in emails.
- (M) Changes (dates or guests) are usually cancel-and-rebook with Hotelbeds. Present them that way rather than promising modification.

### Gaps
- No public research found on reference+email lookup UX or on reminder-email impact for OTAs. Recommendations above are inferred from common practice.
- No small-OTA benchmark on contacts per booking or channel mix.

---

## 6. Mobile: traffic and booking share, mobile-first patterns

### Takeaway
Mobile is now the majority channel. Booking.com books mid-50s % of room nights in its apps alone, and secondary sources put mobile at 55–65% of online travel bookings and about 70% of traffic. Mobile usability gaps still push many users to finish on desktop, so mobile-first design and speed are table stakes.

### Cited Findings
- Booking Holdings FY2025 10-K: 1,235M room nights in 2025 (+8%). The share booked on its mobile apps was in the "mid-fifties" % in 2025, up from "low-fifties" in 2024. A significant majority of app room nights are direct — [SEC 10-K FY2025](https://www.sec.gov/Archives/edgar/data/1075531/000107553126000009/bkng-20251231.htm)
- Secondary aggregators put mobile at roughly 55–63% of online travel bookings in 2025 and 70.5% of travel traffic, with desktop still leading completed bookings in some reports. Sources are low-reliability and definitions vary — [ColorWhistle](https://colorwhistle.com/hotel-booking-statistics-trends/); [Perk](https://www.perk.com/uk/blog/online-travel-booking-statistics/). Ireland: 59% of hotel bookings from smartphones in April 2025, vs 52% a year earlier — [Net Affinity](https://www.netaffinity.com/blog/shifting-booking-trends-2025/)
- Think with Google [OLDER, about 2016–2018]: 54% of leisure travelers cite mobile limitations or usability as a reason for booking on another device. Nearly two-thirds double-check prices on a computer after shopping on a phone — [Think with Google](https://thinkwithgoogle.com/consumer-insights/consumer-trends/travel-booking-trends-book-it-moments)
- Mobile-specific issues seen in testing: a cookie notice and sticky chat covered the booking search on mobile. At IHG, mobile users had to scroll or tap "Book Now" to reach the search — [Baymard](https://baymard.com/research-articles/travel-accommodations-booking-search)
- Autocomplete on mobile: target 4–8 suggestions — [Baymard](https://baymard.com/research-articles/autocomplete-design)
- Deloitte/55 for Google (2020) [OLDER]: across 37 brands, a 0.1s mobile speed improvement was associated with +10.1% travel conversions (7.4M travel sessions), including +2.2% checkout completion. This is correlational and Google-commissioned — [web.dev](https://web.dev/case-studies/milliseconds-make-millions); [Deloitte PDF](https://www2.deloitte.com/content/dam/Deloitte/ie/Documents/Consulting/Milliseconds_Make_Millions_report.pdf)

### Inferences
- (S) Design every screen at 375px first:
  - Search as full-screen sheets (destination, dates, guests)
  - Sticky "Search"/"Select room"/"Pay" CTA at the bottom
  - Filters in a bottom sheet with a live result count
  - Map as a toggle, not side-by-side
  - Numeric/email/tel input types and `autocomplete` attributes on checkout fields
  - Wallets (Apple Pay/Google Pay) on mobile
- (S) Keep the mobile search reachable without scrolling and never covered by banners or chat.
- (M) A native app is out of scope for a small team. A fast, installable PWA and good transactional emails cover most of the repeat-use value.

### Gaps
- No primary 2024–2026 Google/Phocuswright figure for mobile share of *web* (non-app) hotel bookings. Most numbers are secondary aggregators. Bodiala should instrument its own device split from day one.

---

## 7. SEO for OTAs: landing pages, structured data, SPA rendering, unique content

### Takeaway
A client-rendered React SPA is a real SEO handicap. Google recommends SSR, static rendering or hydration (dynamic rendering is only a workaround). Supplier content shared verbatim with every other Hotelbeds reseller risks Google's "thin affiliation" and "scaled content" policies. Build a limited set of server-rendered destination and hotel pages with original, useful additions and `ItemList`/`Hotel` structured data.

### Cited Findings
- JavaScript SEO, per Google:
  - Googlebot queues 200-status pages for rendering, which may be delayed.
  - Use the History API with real `<a href>` URLs, not `#/` fragments.
  - Client-side routing can't return real status codes, so redirect to a server 404 or add `noindex` for error views (soft 404s).
  - Give every view a unique `<title>` and meta description.
  - Set the canonical in the initial HTML.
  - SSR or prerendering is still worthwhile because "not all bots can run JavaScript".
  - Google can only index content in the rendered HTML.
  - — [Google Search Central](https://developers.google.com/search/docs/crawling-indexing/javascript/javascript-seo-basics)
- Google says dynamic rendering "was a workaround and not a long-term solution" and recommends server-side rendering, static rendering or hydration instead — [Google Search Central](https://developers.google.com/search/docs/crawling-indexing/javascript/dynamic-rendering)
- **Thin affiliation:** pages whose descriptions and reviews are copied from the merchant with little original material. Google says affiliate networks distributing identical content produce "cookie-cutter sites or templates with the same or similar content replicated". Good affiliates add value, e.g., extra price information, original reviews, testing and ratings — [Google spam policies](https://developers.google.com/search/docs/essentials/spam-policies)
- **Scaled content abuse:** many pages generated (by AI or otherwise) without adding value, or by scraping or stitching other content. **Doorway abuse:** many city or region pages that funnel to one page, or "substantially similar pages that are closer to search results than a clearly defined, browseable hierarchy" — [Google spam policies](https://developers.google.com/search/docs/essentials/spam-policies)
- Structured data carousels (beta): available in the EEA for hotels and vacation rentals (also Türkiye and South Africa).
  - Uses `ItemList` (3+ items, 1-based `position`) on a summary/category page, with each item a `LocalBusiness` subtype such as `Hotel` that has `image`, `name` and a unique same-domain `url`.
  - Recommended: `priceRange`, `amenityFeature` (LodgingBusiness), `aggregateRating`.
  - Images must be crawlable, in 16:9, 4:3 and 1:1 ratios, at least 50K pixels.
  - For paginated lists, put an ItemList on each page. For infinite scroll, mark up the initially loaded items.
  - — [Google Search Central](https://developers.google.com/search/docs/appearance/structured-data/carousels-beta)
- Google's hotel-price structured data (schema.org `Hotel`/`HotelRoom` JSON-LD) was used to validate prices for Google Hotels. JSON-LD is now deprecated *for price-accuracy validation only*, not for other Search uses — [Google Hotels developers](https://developers.google.com/hotels/hotel-prices/structured-data/hotel-price-structured-data)
- Don't mark up information users can't see (secondary summary of Google guidance) — [Obvlo](https://obvlo.com/resources/structured-data-markup-hotels/)

### Inferences
- (M–L) Add prerendering or SSR for public pages only:
  - `/hotels/{country}/{city}` destination pages
  - `/hotel/{code}-{slug}` hotel pages
  - Options: Vite SSG/prerender of a curated set, or move public pages to an SSR framework (React Router framework mode / Next.js) while keeping the booking app SPA.
  - Search results with dates stay `noindex`. Index only date-less destination and hotel pages.
- (S) Real URLs via History API, a unique title and description per page, a canonical in the initial HTML, a 404 status for unknown hotels, and an XML sitemap of destination and hotel pages.
- (M) Avoid thin-affiliate signals:
  - Index only the ~5 seeded destinations and their best hotels at first, not thousands of Hotelbeds pages.
  - Add original elements: neighbourhood guides, "best area to stay", own photos, price-by-month guidance, curated lists, honest pros and cons.
  - Rewrite, don't just copy, the Hotelbeds descriptions on indexed pages.
- (S) JSON-LD: `ItemList` of `Hotel` on destination pages (EEA carousel eligibility: PMI, BCN, LIS are EEA; AYT is Türkiye, also covered; LON is not covered). Each hotel page gets `Hotel` with `address`, `geo`, `image`, `starRating`, `amenityFeature`, and `aggregateRating` only if reviews are shown on the page and licensed.

### Gaps
- No quantitative data found on how much organic traffic new OTAs get from destination or hotel pages, or on Google's treatment of reseller-shared hotel descriptions specifically.
- Google rich results for individual hotel *prices* come through Hotel Center feeds, not page markup. No evidence that `Offer` markup on hotel pages produces price rich results in organic Search.

---

## 8. Performance: Core Web Vitals and conversion; hotel image optimisation

### Takeaway
Speed has documented conversion impact in travel. Redbus doubled mobile conversion after CWV fixes, and Deloitte/Google associate 0.1s with about 10% more travel conversions. The biggest lever on an image-heavy hotel site is LCP: put the hero image in the server HTML with `fetchpriority="high"`, never lazy-load it, serve AVIF/WebP via `srcset` from a CDN, and avoid client-side-only rendering.

### Cited Findings
- Redbus (bus-booking OTA):
  - CLS from 1.65 to 0 (fixed component slots, removed unoptimized tag scripts)
  - TTI from about 8s to about 4s
  - TBT from about 1200ms to about 700ms
  - Mobile conversion rate up 80–100% across global properties, with domain ranking uplift
  - — [web.dev](https://web.dev/case-studies/vitals-business-impact?hl=en)
- Rakuten 24 (retail, A/B test): CWV investment brought +33.13% conversion rate and +53.37% revenue per visitor — [web.dev](https://web.dev/case-studies/rakuten). Other web.dev cases: Vodafone LCP −31% gave +8% sales; Lazada tripled LCP and got +16.9% mobile conversion — [web.dev](https://web.dev/case-studies/vitals-business-impact?hl=en)
- Deloitte (2020) [OLDER]: 0.1s faster mobile gave +10.1% travel conversion — [web.dev](https://web.dev/case-studies/milliseconds-make-millions)
- Bookaway (travel booking) found FID/CLS fixes relatively straightforward, while LCP was the hardest because of many contributing factors (2023) — [Smashing Magazine](https://shop.smashingmagazine.com/2023/08/optimize-performance-serve-global-audience/)
- LCP thresholds: good is ≤2.5s and poor is >4.0s, measured at the 75th percentile of real-user visits. Target split: TTFB about 40%, resource load delay under 10%, load duration about 40%, render delay under 10% — [web.dev](https://web.dev/articles/optimize-lcp)
- Image LCP fixes:
  - Image discoverable in the initial HTML (not JS-injected, `data-src` or CSS background)
  - `fetchpriority="high"` on the LCP image only (1–2 images max)
  - Never `loading="lazy"` on it
  - Lower priority for hidden carousel slides
  - Same-origin or preconnected image host
  - AVIF/WebP, `srcset`/`sizes`, compression, an image CDN, long cache-control
  - SSR or prerendering when the image depends on JS
  - Don't hide the LCP element during A/B tests
  - — [web.dev](https://web.dev/articles/optimize-lcp)

### Inferences
- (S) Results list: the first 2–3 card images eager with `fetchpriority="high"` on the first only, the rest `loading="lazy"`, explicit `width`/`height` or `aspect-ratio` to prevent CLS. Hotel page: hero eager and high priority, gallery lazy.
- (M) Proxy Hotelbeds photos through an image CDN or resizer (e.g., Cloudflare Images, imgproxy, or a CDN with on-the-fly AVIF/WebP and width params), cached long-term. Hotelbeds image URLs come in fixed size folders (`bigger`, `original` etc.), so pick the smallest size that fits the slot.
- (S) Show skeletons with fixed dimensions while availability loads (search calls are slow), so content doesn't shift (CLS).
- (S) Add RUM with the `web-vitals` library posting to the backend or analytics. Budget: LCP ≤2.5s and CLS ≤0.1 at p75 on mobile.

### Gaps
- No hotel-OTA-specific (as opposed to bus/retail) public CWV case study with conversion numbers found. No data on AVIF vs WebP conversion impact.

---

## 9. Growth and retention: price alerts, saved hotels, loyalty, email, metasearch (Google, trivago), funnel metrics

### Takeaway
At scale, retention comes from loyalty (Booking.com's top Genius tiers drive a high-50s % of room nights) and owned channels (email, app). Metasearch (Google Hotel Ads/free booking links, trivago) is the main paid acquisition channel for OTAs. It requires a Hotel Center integration (hotel list, price feed, landing pages) and strict price accuracy, which Google tightened from 22 Sep 2025. Track a funnel from search to booking and benchmark against your own baseline rather than published averages, which vary widely.

### Cited Findings
**Loyalty, alerts, email**
- Booking.com Genius levels 2–3: over 30% of the active base, a high-50% share of room nights in 2025 (up from mid-50% in 2024), and a "meaningfully higher direct booking rate". Genius members book more often, further ahead, and return more consistently — [Motley Fool transcript Q4 2025](https://www.fool.com/earnings/call-transcripts/2026/02/18/booking-bkng-q4-2025-earnings-call-transcript/); [Booking Q4-25 prepared remarks](https://s201.q4cdn.com/865305287/files/doc_financials/2025/q4/Q4-25-Prepared-Remarks-for-Website.pdf)
- Hopper (self-reported, 2023): 1B+ notifications sent; "Watch a hotel" price monitoring includes private-rate alerts — [Hopper](https://hopper.com/hopback-2023); [Hopper media](https://media.hopper.com/news/hopper-now-offers-price-monitoring-and-private-rates-for-hotels-across-the-globe). Kayak: tap a heart to track a hotel's price — [Kayak](https://www.kayak.com/news/?p=126850). No independent data found on alert-driven retention or conversion lift.
- IATA (flights): 86% of searches come from returning users, and users start watching trips about 4 months ahead — [IATA](https://www.iata.org/contentassets/ac09c0acbb7844029ffb74a6f98f78da/what-is-going-on-in-business-and-leisure-travel.pdf)
- Email: travel and hospitality click rate about 2.43% (MailerLite, as cited). Open-rate benchmarks range from 17–28% to 42%, inflated by Apple Mail Privacy Protection — [MailerLite](https://www.mailerlite.com/blog/email-marketing-statistics); [Prospeo](https://prospeo.io/s/average-open-and-click-rates). Browse-abandonment emails are cited at 5.44% conversion (cross-industry, secondary) — [Digital Applied](https://digitalapplied.com/blog/email-marketing-statistics-2026-data-points)

**Metasearch: Google**
- Hotel Center integration needs:
  - A Hotel List feed (`<listings>`)
  - Pricing via Pull, Changed Pricing or ARI push
  - A Landing Pages file (dynamic URLs, tracking)
  - Room/package metadata about weekly
  - A Price Accuracy Score kept at "Excellent"
  - Bids and budgets via Google Ads for Hotel Ads
  - Property eligibility: rooms for paying guests, a physical fixed location, minimum stay ≤7 days
  - — [Google Hotels developers](https://developers.google.com/hotels/hotel-prices/dev-guide/data-feeds)
- Free booking links: "Any property with a bid in a hotel campaign, available rates, and a landing page will be eligible." Bids don't affect free-link ranking — [Google Hotel Center help](https://support.google.com/hotelprices/answer/10472993). Vendors say OTAs can appear but need an ARI feed through Hotel Center and to be active metasearch advertisers — [AxisRooms](https://blog.axisrooms.com/how-free-booking-links-hotels-strengthen-your-metasearch/)
- Price accuracy score tiers:
  - Excellent: preferred placement.
  - Fair/Poor: worse auction position and cost, and worse free-link placement.
  - At Risk: most ads and links off.
  - Failed: all off.
  - Interrupting Google's validation leads to Failed.
  - Prices must match on landing and booking pages (total, occupancy, dates, currency refreshed every few hours).
  - The booking must be confirmable within 24h.
  - — [Google Price Accuracy Policy](https://support.google.com/hotelprices/answer/6064419)
- From 22 Sep 2025, Google said it "will start removing more inaccurate prices" from ads and free booking links — [Search Engine Roundtable](https://www.seroundtable.com/google-hotel-center-enforcement-on-pricing-accuracy-40053.html)
- Google is deprecating Third-Party Rates (participating without one's own Hotel Center feed) — [Adchieve](https://www.adchieve.com/en/blog/third-party-rates-for-google-hotel-ads)

**Metasearch: trivago**
- CPC model. Advertisers send an inventory feed of bookable hotels with regular updates — [Lodgify](https://encyclopedia.lodgify.com/trivago); [Supergood](https://supergood.ai/api-report-card/trivago)
- Approval is reportedly gated on inventory volume, coverage and audience fit, with new entrants facing more scrutiny (vendor blog) — [Revenue Hub](https://revenue-hub.com/?p=7739)
- A trivago executive referred to "minimum monetization expectations" for advertisers — [iTiger](https://itiger.com/hans/news/2481963617)
- Server-side conversion tracking is assumed (third-party assessment) — [Supergood](https://supergood.ai/api-report-card/trivago)

**Funnel metrics and experimentation**
- Look-to-book is often 100:1 or worse in travel (1%) — [Track360](https://track360.io/blog/look-to-book-conversion-rate-optimization-travel-operator-guide-2026)
- Hotel website conversion: 0.73% for independents vs 1.9% for hotel groups [OLDER, 2019] — [eHotelier](https://insights.ehotelier.com/?p=54256)
- A 2026 roundup gives a median of 2.2–3.9% (low reliability) — [Foundry CRO](https://foundrycro.com/blog/travel-hospitality-marketing-benchmarks-2026/)
- Vacation-rental OTAs: 1–5% — [Lodgify](https://encyclopedia.lodgify.com/look-to-book-ratio)
- Booking.com reportedly runs 1,000+ concurrent experiments [secondary] — [GIGAZINE](https://gigazine.net/gsc_news/en/20161002-booking-com-ab-test). Its ML models are each validated by randomized controlled experiments — [arXiv](https://arxiv.org/pdf/1802.03102.pdf)

### Inferences
- (S) Instrument the funnel from day one:
  - Events: `search_submitted`, `results_viewed` (count, latency, zero-results), `hotel_viewed`, `rate_selected`, `checkrate_ok/price_changed/sold_out`, `checkout_started`, `payment_submitted`, `booking_confirmed/failed`, `cancelled`.
  - KPIs: search-to-book conversion (bookings ÷ sessions with a search), look-to-book (supplier availability calls ÷ bookings, which matters for Hotelbeds look-to-book terms and quotas), hotel-page-to-checkout, checkout completion, price-change rate at checkrates, sold-out rate, supplier error rate, cancellations ÷ bookings, repeat-booking rate, p75 LCP by template.
- (S) Saved hotels (heart) in localStorage first, synced to an account later. "Email me this search" (price alert) is (M), but supplier call quotas limit how often you can recheck prices. Batch checks nightly for saved searches only.
- (S) Email capture: confirmation emails (transactional) plus an opt-in checkbox for deals (unticked by default, GDPR). Abandoned-checkout emails are allowed only for users who entered email and consented, depending on jurisdiction.
- (L) Metasearch is a later-stage task. It requires a Hotel Center partnership, the hotel list mapped to Google property IDs, a high-frequency price feed, a landing-page template that reproduces Google's exact total (incl. taxes, currency, occupancy), and Excellent price accuracy. Hotelbeds `RECHECK` rates and net-rate markups make exact price matching hard. The SSR hotel landing pages and transparent total pricing above are prerequisites. trivago similarly needs feeds, a CPC budget and approval.
- (M) Loyalty: for a new OTA, start with a simple "member price" (e.g., a small discount for signed-in users). Label it clearly per Google's member-rate rules, and only where the supplier contract allows it.

### Gaps
- No official Google page found stating OTA (non-hotel) onboarding criteria for Hotel Center. A third-party claim of a ">95% price accuracy" requirement was unverified.
- No official trivago minimum inventory or CPC floor found.
- No independent evidence quantifying price-alert or saved-hotel effects on conversion or retention.
- Published conversion benchmarks are inconsistent in definition (sessions vs unique visitors vs searches) and mostly from vendors. Use internal baselines.
