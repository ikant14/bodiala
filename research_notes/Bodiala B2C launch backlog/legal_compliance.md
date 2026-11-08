# Legal & compliance requirements for a public B2C hotel booking website selling to EU consumers

Scope: a small online travel agency (OTA) reselling Hotelbeds hotel rooms to EU consumers via bodiala-web/bodiala. Status as of November 2026. This is research, not legal advice. Items marked [LAWYER] need confirmation by qualified EU (and, if relevant, Georgian/UK) counsel.

Tag legend used in the Inferences/task bullets: **[LAW]** hard legal requirement (EU-wide); **[LAW-NAT]** requirement that depends on national law or transposition; **[BP]** best practice or regulator expectation, not a black-letter rule; **[LAWYER]** must be confirmed by counsel; **[CHANGED 2025-26]** new or changed in 2025–2026.

Primary-law note: provisions cited to EUR-Lex ELI links (GDPR, ePrivacy, CRD, UCPD, Omnibus, PID, E-Commerce Dir., ADR Dir., UCTD, PSD2, DSA, Geo-blocking, Rome I, Brussels Ia, PTD 2015) are stated from the official texts. Only the EAA text was re-fetched in this session; pinpoint the other article numbers before you rely on them in a contract or a notice.

Codebase facts observed during this research (local files, not web sources):
- `C:\Users\user\IdeaProjects\bodiala-web\src\styles.css` line 1 loads Lato from Google's servers: `@import url('https://fonts.googleapis.com/css2?family=Lato...')`. This is directly relevant to the Google Fonts ruling below.
- A grep of bodiala-web found no Google Analytics, gtag or Tag Manager calls. There is currently no analytics consent problem, but there is also no consent-management layer yet.
- The backend carries a `guestNationality` field through search, prebook and book (`search/DestinationSearchRequest.java`, `booking/PrebookRequest.java`, `booking/BookRequest.java`, `search/SearchAggregationService.java`). Hotelbeds `sourceMarket` is one global config value (`provider/hotelbeds/HotelbedsProperties.java`, sent in `HotelbedsSearchProvider.java` line ~99). The booking holder is sent in `HotelbedsBookingProvider.java` line ~105. These matter for data minimisation and the Geo-blocking Regulation (see below).

Compact 2025–2026 change log (details and sources in the sections below):
- EAA applies from 28 June 2025.
- The ODR platform closed and the ODR link duty ended on 20 July 2025.
- The UK drip-pricing ban applies from 6 April 2025.
- UK DUAA 2025: Royal Assent 19 June 2025, main data and cookie changes commenced 5 February 2026.
- The EU online "withdrawal function" applies from 19 June 2026.
- Revised Package Travel Directive (EU) 2026/1024: in force 28 May 2026, applies from 29 March 2029, abolishes linked travel arrangements.
- First EAA court orders in France (Carrefour, 4 June 2026).
- The Digital Fairness Act proposal is expected in Q4 2026 and is not yet law.

---

## 1. GDPR: lawful basis, privacy notice, minimisation and retention (incl. passport-type data), data subject rights, processors/DPAs, international transfers, records of processing, breach notification

### Takeaway
GDPR applies in full, including to a non-EU operator targeting EU consumers. The core build items are:
- map each data field to a lawful basis, mostly "contract" (Art. 6(1)(b));
- collect only what Hotelbeds and the hotel need, and no passport numbers by default;
- publish an Art. 13 privacy notice;
- sign Art. 28 DPAs with true processors;
- keep an Art. 30 record (the small-company exemption will not apply because booking processing is not occasional);
- run a 72-hour breach procedure.

For Spanish hotels, a contested Spanish decree may oblige intermediaries to collect and report extended traveller data. That needs a lawyer's call before launch.

### Cited Findings
- **Lawful bases:** Art. 6(1) GDPR lists consent (a), contract performance or pre-contract steps (b), legal obligation (c) and legitimate interests (f). Principles include data minimisation (Art. 5(1)(c)) and storage limitation (Art. 5(1)(e)) — [GDPR, EUR-Lex](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
- **Special categories:** processing data revealing health or religious beliefs is prohibited unless an Art. 9(2) exception applies, e.g. explicit consent. This matters for free-text "special requests" such as wheelchair access or dietary/religious needs — [GDPR Art. 9](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
- **Privacy notice content (Art. 13), where data is collected from the data subject:**
  - controller identity and contact details, plus the EU representative where applicable;
  - DPO contact, if any;
  - purposes and legal basis, and the legitimate interests relied on;
  - recipients or categories of recipients;
  - third-country transfers and safeguards;
  - retention period or the criteria for it;
  - the rights of access, rectification, erasure, restriction, objection and portability;
  - the right to withdraw consent;
  - the right to complain to a supervisory authority;
  - whether providing the data is a statutory or contractual requirement;
  - any automated decision-making.

  Art. 12 requires concise, transparent, plain language — [GDPR Arts 12–13](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
- **Data subject rights (Arts 15–22):** respond within one month of receipt, extendable by two further months where necessary (Art. 12(3)) — [GDPR](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
- **Data protection by design and by default (Art. 25)** and **security of processing (Art. 32)** — [GDPR](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
- **Processors:** processing by a processor must be governed by a contract with the Art. 28(3) content: documented instructions, confidentiality, security, sub-processor conditions, assistance with rights and breaches, deletion or return at the end, and audits — [GDPR Art. 28](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
- **Records of processing (Art. 30):** required. The Art. 30(5) exemption for organisations under 250 employees does not apply where processing "is not occasional", is likely to result in a risk, or includes special-category or criminal data — [GDPR Art. 30](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
- **Breach notification:**
  - notify the supervisory authority without undue delay and, where feasible, within 72 hours, unless the breach is unlikely to result in a risk (Art. 33(1));
  - document all breaches (Art. 33(5));
  - notify data subjects without undue delay when the risk is high (Art. 34) — [GDPR Arts 33–34](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
- **Fines:** up to EUR 20m or 4% of worldwide annual turnover for breaches of principles, lawful basis, rights and transfers (Art. 83(5)) — [GDPR](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
- **Transfers:**
  - Chapter V (Arts 44–49) governs transfers to third countries: adequacy decision, Art. 46 safeguards such as SCCs, or Art. 49 derogations — [GDPR](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
  - Georgia is not on the Commission's list of adequate countries — [European Commission, adequacy decisions](https://commission.europa.eu/law/law-topic/data-protection/international-dimension-data-protection/adequacy-decisions_en)
  - EDPB Guidelines 05/2021 (final version 2.0, 24 Feb 2023) set three cumulative criteria for a "transfer" — [EDPB](https://www.edpb.europa.eu/our-work-tools/our-documents/guidelines/guidelines-052021-interplay-between-application-article-3_en)
  - The EDPB's position is that transfers to an importer outside the EU need a Chapter V mechanism even if the importer is itself subject to GDPR via Art. 3. Commentators call this approach "cumulative" — [Inside Privacy (Covington)](https://www.insideprivacy.com/gdpr/edpb-publishes-draft-guidelines-on-interplay-of-article-3-gdpr-and-the-gdprs-cross-border-transfer-rules/)
- **Spain traveller registration (Real Decreto 933/2021):**
  - The Interior Ministry's FAQ (updated 10/08/2026) says the obligations cover everyone carrying out lodging activities in Spain "or act[ing] as intermediaries". Duties are to communicate the Annex I/II data via SES.HOSPEDAJES and to keep a computer register. Obligations apply from 2 December 2024 — [Interior Ministry FAQ PDF](https://www.interior.gob.es/opencms/pdf/servicios-al-ciudadano/Hospedajes-y-Alquiler-de-Vehiculos/Preguntas_frecuentes_hospedajes_alquiler_vehiculos_10_08_2026.pdf) (the PDF returned 403 to direct fetch; content via search snippet); [Interior Ministry page](https://www.interior.gob.es/opencms/ca/servicios-al-ciudadano/hospedajes-y-alquiler-de-vehiculos/); [BOE consolidated text](https://www.boe.es/eli/es/rd/2021/10/26/933/con)
  - Travel-agency associations (CEAV, FETAVE, UNAV, ACAVE) contest including agencies, arguing Organic Law 4/2015 Art. 25.1 does not list intermediaries. One report says the ministerial implementing order is paused pending a European Commission infringement procedure against Spain — [Hosteltur](https://www.hosteltur.com/177175_registro-de-viajeros-interior-da-una-de-cal-y-otra-de-arena-a-las-agencias.html); [Forbes España](https://forbes.es/turismo/489885/ceav-advierte-que-las-agencias-no-podran-aplicar-el-registro-documental-que-entrara-en-vigor-en-octubre-es-imposible/); [The Objective](https://theobjective.com/economia/2024-09-09/agencias-viajes-nuevo-registro/)
- **Payment surcharges:** surcharging consumer card payments covered by the Interchange Fee Regulation is banned (PSD2 Art. 62(4)) — [PSD2, EUR-Lex](https://eur-lex.europa.eu/eli/dir/2015/2366/oj). Strong customer authentication duties for electronic payments sit with the PSP under PSD2 Art. 97 — [PSD2](https://eur-lex.europa.eu/eli/dir/2015/2366/oj)

### Inferences
Lawful basis:
- **[LAW]** Build a data-field inventory with one lawful basis per field:
  - Booking holder name, email, phone, guest names, dates and payment: Art. 6(1)(b), contract.
  - Fraud screening and basic service analytics: Art. 6(1)(f), legitimate interests. Document a balancing test.
  - Marketing email: Art. 6(1)(a) consent, or the ePrivacy soft opt-in (see Section 9).
  - Accounting and tax records: Art. 6(1)(c), legal obligation.

Minimisation:
- **[LAW]** Drop or justify `guestNationality`. Nationality is not needed to book a hotel through Hotelbeds; the API uses `sourceMarket`. If nationality is not used for pricing or a legal duty, stop collecting it. If it is used for pricing, see the geo-blocking risk in Section 7.
- **[LAW]** Do not collect passport or ID numbers, date of birth or addresses by default. Hotels collect ID at check-in under national rules.
- **[LAWYER]** Get a lawyer's opinion on whether RD 933/2021 binds bodiala as an "intermediary" for Spanish hotels (Mallorca/PMI and Barcelona/BCN are in the stub catalog). If it does, a separate, restricted-access data flow and retention regime is needed.
- **[BP]** Replace free-text "special requests" with structured, non-sensitive options. Or warn users not to include health or religious information, and route accessibility needs carefully because Art. 9 applies.

Privacy notice and rights:
- **[LAW]** Publish an Art. 13 privacy notice. Name:
  - the controller (the legal entity) and the EU representative if the operator is non-EU;
  - recipients: Hotelbeds, the hotel, the PSP, the hosting and email providers;
  - each transfer and its mechanism;
  - retention periods per data category;
  - the complaint route to a DPA.

  Link it from the booking form at the point of collection and in the footer.
- **[LAW]** Rights handling: build a DSR intake (email or form), identity verification, and export/delete tooling over `HotelBooking` and any user tables, with a one-month SLA. Erasure must respect legal-hold retention: accounting records are kept, marketing data is deleted.

Retention:
- **[LAW]** Define a retention schedule and implement automated purge or anonymisation jobs. For example:
  - abandoned prebook data: days;
  - completed bookings: per tax and limitation rules;
  - logs: short.

  **[LAWYER]** sets the actual periods per jurisdiction.

Processors and controllers:
- **[LAW]** Sign Art. 28 DPAs with processors: hosting/cloud, transactional email, error monitoring, support desk. Keep a sub-processor list.
- **[LAWYER]** Classify Hotelbeds and the hotel. They are likely independent controllers for fulfilment, not processors. Classify the PSP: often an independent controller for fraud/AML and a processor for some functions. Then document the Art. 26 or controller-to-controller terms.
- **[LAW]** Keep an Art. 30 record. Booking processing is continuous, so the under-250 exemption is unlikely to help.
- **[BP]** A DPIA (Art. 35) is likely not mandatory for a small OTA without profiling or large-scale sensitive data, but a light one is good practice. **[LAWYER]** to confirm against the target countries' DPIA lists. A DPO (Art. 37) is likely not mandatory.

Breach response:
- **[LAW]** Write an incident runbook: detect, assess risk, notify the DPA within 72 hours, notify data subjects if the risk is high, log every breach. The relevant authority is the lead DPA if EU-established, otherwise each affected country's DPA via the Art. 27 representative.

Payments:
- **[LAW]** Do not surcharge EU consumer card payments.
- **[BP]** Use PSP-hosted payment fields or a redirect so card data never touches bodiala. This keeps PCI DSS scope minimal; PCI DSS is a contractual card-scheme requirement, not EU law.

### Gaps
- **Retention periods:** no EU-wide statutory period for booking or guest data. Periods come from national tax, commercial and limitation laws in the operator's country of establishment. Not researched; [LAWYER].
- **Hotelbeds' data role:** whether Hotelbeds acts as processor or independent controller, and whether its supplier contract includes data clauses. Not researched; check the Hotelbeds agreement.
- **RD 933/2021 for foreign intermediaries:** the exact data fields (reportedly including payment data) and whether a non-Spanish online intermediary must comply. Could not be verified because the FAQ PDF returned 403. The status of the Commission infringement procedure is also unknown.
- **EDPB 05/2021 direct collection:** whether the guidelines say that data a data subject enters directly into a non-EU company's website form is not a "transfer". I recall this is the guidelines' Example 1, but the PDF was not opened in this session, so it is unverified. [LAWYER]
- **EU–US Data Privacy Framework:** status as of Nov 2026, relevant to US-based email/hosting/PSP providers, was not verified.

---

## 2. ePrivacy / cookies: consent banner (reject as easy as accept, no pre-ticked boxes), analytics and marketing tags, Google Fonts from Google servers, consent records

### Takeaway
Any non-essential cookie, pixel or similar technology needs prior, GDPR-standard consent:
- a "Reject all" as prominent as "Accept all" on the first layer;
- no pre-ticked boxes;
- no "legitimate interest" for non-essential trackers;
- withdrawal as easy as giving consent;
- stored proof of consent.

Separately, bodiala-web currently loads Google Fonts from fonts.googleapis.com. A Munich court held that this kind of embed, without consent, unlawfully discloses visitors' IP addresses. Self-host the font.

### Cited Findings
- **ePrivacy Art. 5(3):** storing or accessing information on a user's device requires consent after clear and comprehensive information. The only exceptions are technical storage for transmission and storage strictly necessary for a service the user explicitly requested — [ePrivacy Directive 2002/58/EC](https://eur-lex.europa.eu/eli/dir/2002/58/oj)
- **Consent standard:** GDPR requires freely given, specific, informed, unambiguous consent (Art. 4(11)). The controller must be able to demonstrate it (Art. 7(1)), and withdrawing must be as easy as giving it (Art. 7(3)) — [GDPR](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
- **Pre-ticked boxes:** a pre-ticked checkbox does not constitute valid cookie consent (CJEU, Planet49, C-673/17) — [CURIA](https://curia.europa.eu/juris/liste.jsf?num=C-673/17)
- **EDPB Cookie Banner Taskforce report (adopted 17 Jan 2023):**
  - flags the absence of a reject option on the first layer, and rejection buried in links;
  - flags deceptive button colours or contrast that make "reject" hard to read;
  - confirms pre-ticked boxes, including on the second layer, cannot give valid consent;
  - flags non-essential purposes such as targeted ads run on "legitimate interest" with default-on toggles.

  A few authorities reportedly did not treat a missing first-layer reject button as an ePrivacy infringement, so national practice varies — [EDPB report PDF](https://www.edpb.europa.eu/system/files/2023-01/edpb_20230118_report_cookie_banner_taskforce_en.pdf); [Reed Smith summary](https://www.reedsmith.com/our-insights/blogs/technology-law-dispatch/102k2wk/eu-cookie-banner-taskforce-report-what-you-need-to-know/)
- **CNIL (France) enforcement:**
  - January 2022: fined Google EUR 150m (EUR 90m Google LLC + EUR 60m Google Ireland) and Facebook EUR 60m because refusing cookies took several clicks while accepting took one. It ordered a refusal mechanism "as simple as" acceptance within three months — [SCL](https://www.scl.org/12482-french-data-protection-regulator-fines-google-and-facebook-for-making-it-harder-to-refuse-cookies-than-to-accept-them/)
  - Later fined TikTok EUR 5m on the same ground — [Lewis Silkin](https://dataprivacy.lewissilkin.com/post/102i856/cnil-fines-tiktok-5-million-for-lack-of-reject-all-cookies-button)
  - The CNIL is explicit that a "reject all" button must sit alongside "accept all" — [Lewis Silkin](https://www.lewissilkin.com/insights/2022/01/12/french-data-protection-regulator-the-cnil-proposes-cookie-fines-totalling-210-mi-102hg1x)
- **Google Fonts ruling (LG München I, 20 Jan 2022, 3 O 17493/20):**
  - Passing a visitor's dynamic IP address to Google through dynamically embedded Google Fonts, without consent, violated the visitor's right to informational self-determination.
  - Legitimate interests (Art. 6(1)(f)) could not justify it, because the fonts can be self-hosted.
  - The court awarded EUR 100 damages and an injunction.
  
  Commentators note the ruling opened the door to mass claims and warning letters ("Abmahnungen") — [Bavarian court judgment text](https://www.gesetze-bayern.de/Content/Document/Y-300-Z-GRURRS-B-2022-N-612?view=Print); [Otto Schmidt](https://www.otto-schmidt.de/news/wirtschaftsrecht/dynamische-einbindung-von-google-fonts-in-internetseite-birgt-risiko-einer-datenschutzrechtlichen-abmahnung-2022-08-17.html); [IT-Recht Kanzlei](https://www.it-recht-kanzlei.de/lg-muenchen-I-webfonts-einwilligung-schadensersatz.html)
  - Source conflict: commentaries differ on whether the damages rested on Art. 82 GDPR or German civil law (§ 823 BGB) — [Otto Schmidt](https://www.otto-schmidt.de/news/wirtschaftsrecht/dynamische-einbindung-von-google-fonts-in-internetseite-birgt-risiko-einer-datenschutzrechtlichen-abmahnung-2022-08-17.html) vs [IT-Recht Kanzlei](https://www.it-recht-kanzlei.de/lg-muenchen-I-webfonts-einwilligung-schadensersatz.html)
- **Pending EU reform:** the Commission's "Digital Omnibus" package (late 2025) and the Digital Fairness Act are discussed as reshaping cookie and dark-pattern rules in 2026. Neither is in force as law — [Taylor Wessing](https://www.taylorwessing.com/en/interface/2025/predictions-2026/digital-fairness-act-and-digital-omnibus)

### Inferences
Third-party resources:
- **[LAW]** Self-host the Lato font: download the woff2 files into bodiala-web and replace the `@import url('https://fonts.googleapis.com/...')` in `src/styles.css`. Grep for other third-party embeds and fix them the same way. This removes the Munich-ruling exposure without needing consent.
- **[LAW]** Apply the same logic to every third-party resource the browser fetches:
  - CDNs (jsdelivr, unpkg);
  - map tiles (Google Maps, Mapbox);
  - YouTube embeds;
  - reCAPTCHA;
  - hotel images hot-linked from Hotelbeds/CDN hosts.

  Either self-host or proxy them, or gate them behind consent. **[LAWYER]** assess the hotel-image CDN, since images are core functionality.

Banner and consent records:
- **[LAW]** Before adding any analytics or marketing tag (GA4, Meta Pixel, Google Ads, Hotjar), ship a consent management platform (CMP):
  - first layer with "Accept all", "Reject all" with equal prominence, and "Settings";
  - no pre-ticked purposes;
  - tags blocked until consent (Google Consent Mode alone does not stop all pings — verify);
  - a persistent "Cookie settings" link in the footer for withdrawal;
  - consent re-asked on purpose changes, and at a reasonable interval.
- **[LAW]** Store consent records: timestamp, banner version or hash, the purposes accepted or rejected, and a pseudonymous ID. This demonstrates consent under Art. 7(1).
- **[BP]** Prefer cookieless or consent-exempt analytics. Some DPAs, such as the CNIL, accept narrowly configured audience measurement without consent; this varies by country, **[LAWYER]**. Alternatively, run analytics only after consent.
- **[BP]** Publish a cookie policy listing each cookie or SDK, its provider, purpose, duration and whether it transfers data outside the EU.
- **[LAW]** Essential cookies (session, CSRF, currency/locale, booking basket) need no consent but must be disclosed.

### Gaps
- No 2025–2026 CNIL or EDPB cookie guidance updates were verified in this session.
- The content and status of the Digital Omnibus cookie amendments (e.g. browser-signal consent, exemptions for low-risk purposes) were not verified.
- Whether hot-linked supplier hotel images trigger the same analysis as Google Fonts is untested in case law found here.

---

## 3. European Accessibility Act (Directive (EU) 2019/882): coverage of online accommodation booking and e-commerce, micro-enterprise exemption, required standard, accessibility statement, enforcement

### Takeaway
A B2C hotel booking website is very likely an "e-commerce service" under the EAA. It must meet the Annex I accessibility requirements from 28 June 2025 and publish Annex V accessibility information in its T&Cs or an equivalent document.

The only clean exemption is the microenterprise one: fewer than 10 staff AND at most EUR 2m turnover or balance sheet. That status must be re-checked as the business grows.

Enforcement started in 2025–26, with complaints, information requests (including to non-EU sellers) and a French court order against Carrefour.

### Cited Findings
- **Scope:**
  - The Directive applies to services provided to consumers after 28 June 2025, including "(f) e-commerce services" (Art. 2(2)).
  - "E-commerce services" are services provided at a distance, through websites and mobile device-based services, by electronic means, at the consumer's individual request, with a view to concluding a consumer contract (Art. 3(30)).
  - Recital 43 says the e-commerce obligations apply to "the online sale of any product or service".
  - Passenger transport elements (websites, e-ticketing, apps) are separately covered under Art. 2(2)(c) — [Directive (EU) 2019/882, EUR-Lex](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32019L0882)
- **Accommodation booking:** a hotel-tech source treats selling rooms to consumers via a website or app as within the EAA e-commerce scope. No explicit "accommodation" listing exists in the text, so this rests on interpretation — [Prostay](https://www.prostay.com/blog/hotel-website-accessibility-eaa-2026/)
- **Microenterprise:**
  - Definition: "employs fewer than 10 persons and which has an annual turnover not exceeding EUR 2 million or an annual balance sheet total not exceeding EUR 2 million" (Art. 3(23)).
  - "Microenterprises providing services shall be exempt" from the service accessibility requirements and related obligations (Art. 4(5)).
  - Secondary sources gave inconsistent thresholds; the Directive text above resolves that — [EUR-Lex](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32019L0882)
- **Service-provider obligations (Art. 13):**
  - design and provide services in line with Annex I (13(1));
  - prepare the Annex V information explaining how the service meets the requirements, make it public in written and oral formats, and keep it for as long as the service operates (13(2));
  - if non-compliant, take corrective measures and immediately inform the competent national authorities (13(4)) — [EUR-Lex](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32019L0882)
- **Annex I Section IV(g), specific to e-commerce:**
  - provide accessibility information about the products and services sold, where the responsible economic operator supplies it;
  - make identification, security and payment functions accessible ("identification methods, electronic signatures, and payment services which are perceivable, operable, understandable and robust") — [EUR-Lex](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32019L0882)
- **Annex V (the "accessibility statement" content):**
  - goes in the general terms and conditions or an equivalent document;
  - includes a general description of the service in accessible formats, how the service operates, and how it meets the Annex I requirements;
  - is in addition to the Consumer Rights Directive information — [EUR-Lex](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32019L0882)
- **Disproportionate burden (Art. 14):**
  - requirements apply only to the extent they don't fundamentally alter the service or impose a disproportionate burden;
  - the assessment must be documented and kept for five years after the last provision of the service;
  - service providers must renew it on service change, on authority request, and at least every five years;
  - an operator that receives external funding for accessibility cannot rely on the exception — [EUR-Lex](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32019L0882)
- **Transitional rules (Art. 32):** products already used for a service before 28 June 2025 may be used until 28 June 2030. Service contracts agreed before 28 June 2025 may continue until expiry, up to five years. Neither helps a new launch — [EUR-Lex](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32019L0882)
- **Penalties (Art. 30):** must be "effective, proportionate and dissuasive", plus remedial action — [EUR-Lex](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32019L0882)
- **Enforcement 2025–2026** [CHANGED 2025-26]. These come from accessibility-vendor trackers; treat figures with caution:
  - **France:** the Caen court ordered Carrefour on 4 June 2026 to make its e-commerce site fully accessible within six months, at EUR 500 per day of delay. A claim that the site met 71% of French criteria did not help. A parallel claim against Auchan was dismissed in Lille (May 2026) and is under appeal.
  - **Netherlands:** the ACM sent information requests to e-commerce operators worldwide, including companies with no EU office.
  - **Sweden:** supervision cases or notices against e-commerce operators; counts differ by source (28 cases vs 23 notices).
  - **National maximum fines:** Italy (AgID) up to EUR 40,000; Germany up to EUR 100,000; Ireland up to EUR 60,000 and 18 months' imprisonment on indictment.
  - **No fines yet:** one tracker (reviewed 19 Oct 2026) reports no published EAA fine in 14 countries.
  - **Standard:** citation of EN 301 549 v4.1.1 in the OJ is reportedly expected around 16 Dec 2026.

  Sources: [AccessiWay](https://www.accessiway.com/blog/eaa-enforcement); [Statutus](https://statutus.ch/en/guides/european-accessibility-act-enforcement-so-far/); [ComplyLoft tracker](https://www.complyloft.com/accessibility/eaa/enforcement); [li.solutions](https://li.solutions/blog/eaa-enforcement-2026/)

### Inferences
Exemption decision:
- **[LAWYER]** Decide whether the operating entity is a microenterprise: headcount under 10 and turnover or balance sheet at most EUR 2m. If yes, it is legally exempt. The exemption is lost on growth, so plan conformance regardless.
- **[BP]** Even if exempt, comply: accessibility is cheap to build in early, and enforcement and litigation risk follow growth.

Conformance work (**[LAW]** unless exempt):
- Target EN 301 549 / WCAG 2.1 AA conformance across the whole booking funnel: search form, date picker, results list, filters (star rating), hotel detail, room/rate selection, guest form, payment step and the bookings/cancel pages.
- Concrete build items:
  - keyboard operability of the date picker and filters;
  - focus management in modals;
  - labels and error messages on the guest form;
  - colour contrast (Lato weights, button colours);
  - alt text for hotel photos, since the "cover photo" feature just shipped;
  - accessible price breakdown;
  - screen-reader-announced availability and price changes;
  - no time-outs without warning on rate holds;
  - an accessible PSP payment step and 3-D Secure challenge (Annex I IV(g)(iii)).
- Annex I IV(g)(i): surface hotel accessibility information when the supplier provides it. Map Hotelbeds facility codes for accessible rooms, wheelchair access and similar into the detail view.
- Annex V: publish an accessibility statement covering the service description, how it meets the requirements, known limitations, and a contact/feedback channel. Put it in the T&Cs or a linked page, in accessible formats.
- Keep it current; Art. 13(2) requires the information to stay available for as long as the service runs.

Records and monitoring:
- **[BP]** Keep an internal conformance report (audit plus remediation log). If relying on disproportionate burden, keep the Art. 14 assessment and renew it every five years. Add automated a11y checks (axe or Lighthouse) to the bodiala-web CI plus periodic manual screen-reader testing.

### Gaps
- **EN 301 549 / WCAG mapping:** I did not verify against the ETSI text that EN 301 549 v3.2.1 maps its web requirements to WCAG 2.1 AA; this is widely cited. The v4.1.1 OJ citation date comes from a vendor source.
- **Commission guidance:** no official Commission guidance on whether accommodation booking intermediaries count as "e-commerce services" was found. The conclusion rests on the definition and Recital 43.
- **Micro-status in practice:** how national authorities verify microenterprise status was not found.
- **Non-EU operators:** whether a non-EU microenterprise can rely on the exemption was not found; presumably the same thresholds apply. [LAWYER]

---

## 4. Consumer law I: price transparency (total price, Price Indication Directive, Omnibus on reviews, rankings and price reductions) and the UCPD on dark patterns (fake urgency/scarcity; CPC actions against Booking.com and other OTAs)

### Takeaway
Hard rules:
- show the total price inclusive of all unavoidable taxes and fees (or how they are calculated) before purchase;
- don't use false urgency or scarcity;
- label paid placement in search results;
- disclose the main ranking parameters;
- disclose whether and how reviews are verified;
- never fake reviews.

The PID "lowest price in the last 30 days" rule for discounts formally covers goods. For hotel "was/now" claims the safest course is to apply it anyway and treat every strike-through as UCPD-regulated. EU regulators have already extracted commitments from Booking.com, Expedia and Airbnb on exactly these points.

### Cited Findings
- **Total price:**
  - CRD Art. 6(1)(e): before the contract, give the total price inclusive of taxes, or the manner of calculation if it can't be calculated in advance, plus all additional charges — [CRD 2011/83/EU](https://eur-lex.europa.eu/eli/dir/2011/83/oj)
  - UCPD Art. 7(4)(c): an "invitation to purchase" must state the price inclusive of taxes, or how it is calculated, and any additional charges. Omitting material information is a misleading omission — [UCPD 2005/29/EC](https://eur-lex.europa.eu/eli/dir/2005/29/oj)
- **Personalised pricing:** since the Omnibus, the trader must disclose "where applicable, that the price was personalised on the basis of automated decision-making" (CRD Art. 6(1)(ea)) — [Omnibus Directive 2019/2161](https://eur-lex.europa.eu/eli/dir/2019/2161/oj)
- **Price reductions (PID Art. 6a, added by Omnibus):** an announced price reduction must indicate the "prior price", meaning the lowest price applied in a period of at least 30 days before the reduction — [PID 98/6/EC](https://eur-lex.europa.eu/eli/dir/1998/6/oj)
- **CJEU Aldi Süd (C-330/23, 26 Sept 2024):** the percentage or "was" price must be calculated from that 30-day lowest price. Merely showing it "for information" is not enough — [CJEU press release](https://curia.europa.eu/jcms/upload/docs/application/pdf/2024-09/cp240152en.pdf); [Noerr](https://www.noerr.com/en/insights/impact-of-the-cjeu-ruling-on-discount-advertising-in-europe-what-it-means-for-businesses)
- **Rankings and paid placement (Omnibus):**
  - UCPD Art. 7(4a): where consumers can search products offered by different traders, information on the main ranking parameters and their relative importance is material and must be in a dedicated section directly and easily accessible from the results page — [Omnibus](https://eur-lex.europa.eu/eli/dir/2019/2161/oj); [Mondaq](https://www.mondaq.com/consumer-law/1158464/legal-aspects-of-customer-reviewssearch-rankings)
  - UCPD Annex I point 11a blacklists showing search results without clearly disclosing paid advertisement or payment for higher ranking — [Omnibus](https://eur-lex.europa.eu/eli/dir/2019/2161/oj)
- **Reviews (Omnibus):**
  - UCPD Art. 7(6): a trader giving access to consumer reviews must say whether and how it ensures reviews come from consumers who actually used or bought the product — [Commission TRIS comment](https://technical-regulation-information-system.ec.europa.eu/cs/notification/26584/message/110913/EN)
  - Annex I 23b: claiming reviews are genuine without reasonable and proportionate verification steps is always unfair.
  - Annex I 23c: submitting or commissioning fake reviews, or misrepresenting reviews (including selective display), is always unfair — [Mondaq](https://mondaq.com/dodd-frank-consumer-protection-act/1189542/the-omnibus-directive-consumer-reviews); [Omnibus](https://eur-lex.europa.eu/eli/dir/2019/2161/oj)
- **Fake urgency and scarcity:** UCPD Annex I point 7 blacklists "falsely stating that a product will only be available for a very limited time, or that it will only be available on particular terms for a very limited time, in order to elicit an immediate decision" — [UCPD](https://eur-lex.europa.eu/eli/dir/2005/29/oj)
- **Penalties after the Omnibus:** for widespread infringements in CPC coordinated actions, maximum fines must be at least 4% of annual turnover in the Member States concerned, or EUR 2m where turnover data is unavailable (UCPD Art. 13) — [Omnibus](https://eur-lex.europa.eu/eli/dir/2019/2161/oj)
- **CPC actions against accommodation platforms:**
  - Booking.com (EU-wide commitments, 18 Dec 2019) and Expedia Group (Expedia, ebookers, hotels.com; 18 Dec 2020) committed to change how they present offers, discounts and prices so consumers can "make informed comparisons".
  - Airbnb (CPC common position 5 Jul 2018; changes 11 Jul 2019) agreed to show "the total price inclusive of all the applicable mandatory charges and fees" and to distinguish professional from private hosts.
  
  The Commission page lists no actions after 2020 — [European Commission, coordinated action on accommodation booking](https://commission.europa.eu/topics/consumers/consumer-rights-and-complaints/enforcement-consumer-protection/coordinated-actions/accommodation-booking_en)
- **National pressure-selling cases:**
  - Hungary's competition authority (GVH) fined Booking.com a record EUR 7m in May 2020 for pressure selling, such as "X people are looking at this" and "only 1 room left" messaging. It found the statements as a whole amounted to pressure selling even if individually true — [Portfolio.hu](https://portfolio.hu/en/business/20200520/hungary-slaps-record-fine-of-eur-7-million-on-bookingcom-432994); [GVH](https://www.gvh.hu/en/press_room/press_releases/press_releases_2019/behaviour_of_online_hotel_booking_sites_once_again)
  - UK CMA (2019): six hotel booking sites committed to stop pressure selling (pop-ups claiming others are viewing a room, or a limited-time price), to disclose whether rankings are affected by commission, and to show the full price upfront — [CMS](https://cms.law/en/gbr/legal-updates/cma-secures-commitments-from-holiday-booking-sites-on-sales-practices); [MoneySavingExpert](https://blog.moneysavingexpert.com/news/2019/02/hotel-booking-sites-agree-to-end--misleading-sales-tactics-)
- **CPC dark-pattern sweep (Jan 2023):** of 399 online shops, 148 used at least one of three dark patterns. 42 used fake countdown timers, 54 used interfaces steering consumers to purchases or subscriptions, and 70 hid or obscured important information — [Commission press release IP/23/418](https://ec.europa.eu/commission/presscorner/api/files/document/print/en/ip_23_418/IP_23_418_EN.pdf)
- **Digital Fairness Act** [CHANGED 2025-26 — not yet law]:
  - The Commission's 2026 work programme announced a DFA proposal for Q4 2026 (EPRS briefing, Oct 2026); one law firm expected it on 11 Nov 2026.
  - Expected content: a ban on deceptive design (confirm-shaming, pre-selection, hidden options), an outright drip-pricing ban, addictive design, influencer marketing and personalised pricing. Negotiations are likely into 2027.
  - Wikipedia's "Q3 2026" timing is outdated — [EPRS briefing](https://eprs.europarl.europa.eu/contents/publications/EPRS/2026/10/EPRS_BRI(2026)791529.html); [Taylor Wessing, Oct 2026](https://www.taylorwessing.com/en/insights-and-events/insights/2026/10/eu-digital-fairness-act-dark-patterns-and-addictive-designs)

### Inferences
Price display:
- **[LAW]** Results and detail pages must show the all-in price for the stay (all nights, all rooms, VAT and mandatory fees) as the headline, not a per-night price that excludes unavoidable charges.
- **[LAW]** Hotelbeds rates can carry "taxes payable at the hotel", such as city or tourist tax. If the amount is known, include it in the total or show it clearly next to the headline price. If not known, state how it is calculated before booking.
- **[BP]** The CPC/Airbnb and UK DMCC direction of travel is to fold every unavoidable charge into the headline price.
- **[LAW]** No hidden service or booking fee added at checkout. Any OTA markup or fee must be inside the displayed total from the first price shown.
- **[LAW]** If prices are ever personalised per user by an algorithm (dynamic markup by device, history or similar), disclose it per Art. 6(1)(ea). **[BP]** Don't personalise prices at launch.
- **[LAW]** Currency conversion: if showing non-EUR prices converted from supplier EUR, the charged currency and amount must be clear before payment.

Price reductions:
- **[BP]/[LAWYER]** "Was/now", "-20%" or strike-through prices: Art. 6a PID is drafted for products. Whether it binds hotel services is for **[LAWYER]**; some Member States extend it.
  - Only show a reduction against a genuine reference price (the lowest price the OTA actually charged for that same rate in the prior 30 days, per Aldi), or don't show strike-throughs.
  - Supplier "discount" or "offer" flags from Hotelbeds must not be repackaged as an OTA discount without a real prior price.

Urgency and scarcity:
- **[LAW]** No fake urgency or scarcity:
  - no countdown timers unless tied to a real rate-hold expiry;
  - "only N rooms left" only if it comes from supplier allotment data and is accurate (Hotelbeds `allotment`);
  - no "X people viewing" unless true. Even true statements can form unlawful pressure selling in aggregate (Hungary GVH).
  
  **[BP]** At launch, avoid these widgets altogether.

Ranking:
- **[LAW]** Ranking transparency: publish a "How we rank results" page linked from the results list. Describe the default sort (e.g. price, star rating, relevance, supplier priority) and state whether any commission or payment influences position. **[LAWYER]** confirm whether Art. 7(4a) strictly applies to a merchant-model OTA; do it anyway, since CMA and CPC practice expects it.
- **[LAW]** If any placement is paid or boosted, label it "Sponsored" or "Ad" (Annex I 11a).

Reviews:
- **[LAW]** Reviews and ratings: if showing review scores (e.g. from Hotelbeds content or TripAdvisor feeds), state the source and whether and how they are verified (Art. 7(6)). Never write or seed reviews; never suppress negative ones. See Section 7 for hosting user reviews under the DSA.

Forward planning:
- **[BP]** Design for the DFA now: no confirm-shaming, no pre-selected add-ons, a symmetric cancel/close, and plain all-in pricing. These are low-cost changes that pre-empt likely 2027+ rules.

### Gaps
- **PID and services:** whether Art. 6a PID applies to services such as hotel rooms. Commission guidance on Art. 6a was not opened in this session, and national extensions were not mapped. [LAWYER]
- **City taxes:** no CPC guidance specifically on city or tourist taxes payable at the property (as opposed to platform fees) was found.
- **Post-2020 actions:** no post-2020 CPC action specific to hotel OTAs was found on the Commission page; there may be national actions not captured.

---

## 5. Consumer law II: Consumer Rights Directive (pre-contractual information, accommodation-with-specific-dates exemption from the 14-day withdrawal right, "order with obligation to pay" button), plus the 2026 withdrawal function

### Takeaway
Date-specific hotel bookings are exempt from the 14-day withdrawal right (CRD Art. 16(l)). The site must then clearly tell consumers that no statutory withdrawal right applies, and show the contractual cancellation policy of the chosen rate.

The checkout must:
- show the key information directly before the order;
- end with a button labelled "order with obligation to pay" or an unambiguous equivalent (e.g. "Book and pay now");
- send a confirmation on a durable medium.

The new 2026 "withdrawal function" only bites for contracts that carry a withdrawal right, so it is mostly irrelevant for dated room bookings.

### Cited Findings
- **Pre-contractual information (Art. 6(1)), in clear and comprehensible manner:**
  - main characteristics;
  - trader identity, geographical address, telephone and email;
  - total price including taxes and charges;
  - arrangements for payment and performance, and the complaint-handling policy (Art. 6(1)(g));
  - where there is no right of withdrawal, the information "that the consumer will not benefit from a right of withdrawal" (Art. 6(1)(k));
  - where applicable, the possibility of recourse to out-of-court complaint and redress (Art. 6(1)(t)) — [CRD](https://eur-lex.europa.eu/eli/dir/2011/83/oj)
- **Withdrawal exemption (Art. 16(l)):** no right of withdrawal for "the provision of accommodation other than for residential purpose, ... car rental services, catering or services related to leisure activities if the contract provides for a specific date or period of performance" — [CRD](https://eur-lex.europa.eu/eli/dir/2011/83/oj)
- **Checkout requirements:**
  - Art. 8(2): directly before the order, show the main characteristics, total price and (where applicable) duration in a clear and prominent manner. The order button must be labelled "order with obligation to pay" or a corresponding unambiguous formulation; otherwise the consumer is not bound.
  - Art. 8(3): payment means accepted and any delivery restrictions must be shown at the latest at the beginning of the ordering process.
  - Art. 8(7): confirmation of the contract on a durable medium within a reasonable time — [CRD](https://eur-lex.europa.eu/eli/dir/2011/83/oj)
- **Add-ons and payment fees:**
  - Art. 22: any extra payment beyond the main obligation needs the consumer's express consent. Consent inferred from default options (pre-ticked boxes) does not count, and the consumer is entitled to reimbursement.
  - Art. 19: no fees for using a payment means above the trader's cost — [CRD](https://eur-lex.europa.eu/eli/dir/2011/83/oj)
- **Unfair terms:** terms in T&Cs that are not individually negotiated and create a significant imbalance are not binding, and written terms must be in plain, intelligible language — [Unfair Contract Terms Directive 93/13/EEC](https://eur-lex.europa.eu/eli/dir/1993/13/oj)
- **Withdrawal function (Directive (EU) 2023/2673, new CRD Art. 11a)** [CHANGED 2025-26]:
  - Applies from 19 June 2026 (transposition deadline 19 December 2025) to distance contracts concluded via an online interface where a right of withdrawal exists.
  - Requires a withdrawal function labelled "withdraw from contract here" or an equivalent, plus a confirmation step. No mandated UI or click count.
  - Reaches non-EU traders directing activities to EU consumers.
  - Transposition is uneven (e.g. Belgium and Ireland reported late) — [Directive 2023/2673](https://eur-lex.europa.eu/eli/dir/2023/2673/oj); [iubenda](https://www.iubenda.com/en/blog/the-new-online-withdrawal-function-what-eu-directive-2023-2673-means-for-your-business/); [Andersen](https://www.be.andersen.com/en/news/the-eus-new-mandatory-withdrawal-button-key-compliance-steps-for-online-traders-before-june-2026); [Lexgo/VBB](https://www.lexgo.be/en/news-and-articles/14873-the-online-withdrawal-button-a-new-mandatory-obligation-for-online-traders); [RDJ](https://www.rdj.ie/insights/eu-introduces-mandatory-withdrawal-button-for-online-consumer-contracts)

### Inferences
Checkout and confirmation:
- **[LAW]** On the rate selection and checkout pages, state clearly: "No statutory 14-day right of withdrawal applies to this booking (accommodation for specific dates). Cancellation is governed by the rate's cancellation policy below." Repeat it in the confirmation email and the T&Cs.
- **[LAW]** Per rate, show before booking:
  - refundable vs non-refundable;
  - the free-cancellation deadline in the hotel's local time and the user's time zone;
  - penalty amounts by date, from Hotelbeds `cancellationPolicies` (amount + from date);
  - board type;
  - room description;
  - taxes included or excluded;
  - payment timing (pay now vs pay at hotel).

  Re-check after the checkrates call. If price or policy changed versus the search result, show the change prominently before the order button.
- **[LAW]** Order button: label it "Book and pay now", "Book with obligation to pay" or "Pay now". Never just "Continue" or "Book". Directly above it, summarise hotel, dates, room, board, total price incl. taxes, cancellation terms, and taxes payable at the hotel.
- **[LAW]** Show accepted payment methods at the start of checkout (Art. 8(3)).
- **[LAW]** Send a booking confirmation email (durable medium) with:
  - all Art. 6 information;
  - the Hotelbeds reference;
  - the supplier/hotel contact and voucher details;
  - the cancellation terms;
  - the T&Cs (attached or as a stable link);
  - the complaint contact.
  
  bodiala currently persists bookings, but transactional email is a new component.

Add-ons and fees:
- **[LAW]** No pre-ticked add-ons (insurance, upgrades, donations) (Art. 22). No card surcharges (Art. 19 and PSD2).

Withdrawal button:
- **[LAW]** The 2026 withdrawal button is not needed for date-specific room bookings, because no withdrawal right exists. It is needed if the site ever sells products that do carry withdrawal rights online: gift vouchers or credits without fixed dates, memberships or subscriptions, or goods. **[LAWYER]** confirm national transposition in each target market.
- **[BP]** Provide an easy self-service "cancel booking" flow anyway. bodiala already has cancel. Show the penalty before confirming, and send a cancellation confirmation by email.

Terms:
- **[LAW]** T&Cs must avoid unfair terms: no unlimited unilateral price changes after booking, no exclusion of liability for the OTA's own fault, no hidden fees. **[LAWYER]** to draft and review.

### Gaps
- **Withdrawal-button transposition:** the national transposition status of Directive 2023/2673 in key markets (DE, FR, ES, IT, NL) as of Nov 2026 was not verified.
- **Agent vs merchant:** CRD obligations attach to the "trader". Whether bodiala contracts as principal (merchant of record) or as agent for the hotel/Hotelbeds changes who is the counterparty and what must be disclosed. [LAWYER], and check the Hotelbeds agreement.

---

## 6. Package Travel Directive and linked travel arrangements (relevant if hotels are combined with flights or other services)

### Takeaway
Hotel-only bookings are not packages. Combining a hotel with a flight or car rental can make bodiala a package "organiser". That brings full liability for performance, insolvency protection and a standard information form.

Cross-selling after a hotel booking can trigger package or LTA status:
- under the current Directive 2015/2302: LTA rules;
- under the revised Directive (EU) 2026/1024 (applies from 29 March 2029): the new package definition, including the 24-hour data-passing rule. LTAs are abolished.

### Cited Findings
- **Current Directive (EU) 2015/2302 (applies now):**
  - Defines a "package" as a combination of at least two different types of travel service (carriage of passengers, accommodation, car rental, other tourist services) for the same trip, under defined conditions.
  - Defines "linked travel arrangements", e.g. a targeted facilitation of a further service within 24 hours of the first booking.
  - LTA facilitators must provide insolvency protection for payments they receive and specific information — [PTD 2015/2302, EUR-Lex](https://eur-lex.europa.eu/eli/dir/2015/2302/oj); [European Commission PTD page](https://commission.europa.eu/law/law-topic/consumer-protection-law/travel-and-timeshare-law/package-travel-directive_en)
- **Revision** [CHANGED 2025-26]:
  - Provisional political agreement on 2 December 2025 — [Travel Extra](https://www.travelextra.ie/provisional-agreement-reached-on-package-travel-directive-revision/)
  - Adopted as Directive (EU) 2026/1024 of 29 April 2026, in force 28 May 2026. Transposition by 29 September 2028; national rules apply from 29 March 2029 — [Garrigues](https://blogturismo.garrigues.com/en/european-union/the-european-union-discontinues-linked-travel-arrangements-and-reorganizes-the-legal-framework-governing-package-travel); [Tragento](https://tragento.com/en/eu-directive-on-package-travel-revised-rules-come-into-effect-in-may-2026-full-application-from-2029/); [EUbusiness](https://www.eubusiness.com/consumer/eu-parliament-greenlights-new-rules-on-package-travel/)
  - Source conflict: one pre-adoption source gave 5 September 2028 for transposition and 5 March 2029 for application. Garrigues gives 29 September 2028 and 29 March 2029. Verify in the OJ.
- **New package definition under 2026/1024:** two or more types of travel service for the same trip, combined by one trader before the contract (even if the traveller selected them). Separate contracts still form a package if any of these applies:
  1. bought at a single point of sale and selected before the traveller agrees to pay;
  2. offered, sold or charged at an inclusive or total price;
  3. advertised or sold as a "package" or similar;
  4. combined after a contract letting the traveller choose among services;
  5. through linked online processes, the first trader passes the traveller's personal data to another trader and a further service is booked within 24 hours of confirming the first.

  Exclusion: one main service (transport, accommodation or car rental) plus other tourist services that are under 25% of total value and not advertised as essential, or that are bought after the main service has started.
  
  Linked travel arrangements are abolished as a category — [Garrigues](https://blogturismo.garrigues.com/en/european-union/the-european-union-discontinues-linked-travel-arrangements-and-reorganizes-the-legal-framework-governing-package-travel)
- **Other changes:** the revision also changes rules on vouchers in certain cancellation cases, insolvency protection, organiser reimbursement rights and complaint handling. Industry welcomed the LTA removal; sources disagree on prepayment limits — [Garrigues](https://blogturismo.garrigues.com/en/european-union/the-european-union-discontinues-linked-travel-arrangements-and-reorganizes-the-legal-framework-governing-package-travel); [Travel Extra](https://www.travelextra.ie/provisional-agreement-reached-on-package-travel-directive-revision/)

### Inferences
- **[LAW]** Hotel-only sales (the current bodiala scope) are outside the PTD. Keep it that way at launch:
  - no "hotel + flight" bundles;
  - no "package", "holiday deal" or "all-in trip" wording;
  - no single total price across different service types.
- **[LAW]/[LAWYER]** Before adding any of these, get a PTD analysis:
  - cross-sell links (car rental, transfers, activities, flights);
  - affiliate widgets that pass guest data to a partner;
  - post-booking "add a flight" emails.

  Today they may create an LTA under 2015/2302 national law; from 29 March 2029 they may create a package under the 24-hour data-passing rule. Package status brings organiser liability, insolvency protection (bond or insurance), the standard information form and price-change limits.
- **[BP]** If adding affiliate cross-sell, avoid passing personal data to the partner automatically. Present it as a separate purchase. This avoids the data-passing trigger.
- Note the 25% exclusion: hotel plus low-value "other tourist services" (e.g. a museum ticket under 25% of value, not advertised as essential) may not create a package. **[LAWYER]**

### Gaps
- **Revised text details:** the full text of 2026/1024 was not read (prepayment caps, voucher rules, insolvency details).
- **Current national LTA rules:** national transposition of current LTA rules in the operator's target markets was not researched.
- **Hotelbeds activities and transfers:** whether Hotelbeds' own add-on products (it sells activities and transfers) would make a combined bodiala sale a package was not examined.

---

## 7. Digital Services Act (if user reviews are hosted) and the Geo-blocking Regulation

### Takeaway
DSA: hosting user reviews makes bodiala at least a "hosting service", and possibly an "online platform". Baseline duties then apply at any size:
- points of contact;
- T&Cs on content moderation;
- notice-and-action;
- statements of reasons;
- for a non-EU provider, an EU legal representative.

Micro and small enterprises are exempt from most extra online-platform duties. The simplest launch path is not to host user reviews.

Geo-blocking: hotel accommodation is expressly covered. Bodiala must not block, auto-redirect or apply different conditions or prices to EU customers based on nationality or residence. The existing `guestNationality` search parameter is a red flag if it ever changes price.

### Cited Findings
- **DSA (Regulation (EU) 2022/2065):**
  - Applies to intermediary services offered to recipients in the Union irrespective of where the provider is established (Art. 2(1)).
  - All intermediaries: single points of contact for authorities (Art. 11) and for users (Art. 12); a legal representative in the EU for providers not established in the Union (Art. 13); T&Cs explaining content-moderation restrictions (Art. 14); transparency reports (Art. 15, with micro/small exemption).
  - Hosting providers additionally: notice-and-action (Art. 16), statement of reasons (Art. 17), and notification of suspected criminal offences threatening life or safety (Art. 18) — [DSA, EUR-Lex](https://eur-lex.europa.eu/eli/reg/2022/2065/oj)
  - "Online platform" excludes cases where public dissemination is "a minor and purely ancillary feature" of another service (Art. 3(i)) — [DSA](https://eur-lex.europa.eu/eli/reg/2022/2065/oj)
  - DSA Art. 19: Section 3 (online-platform obligations, Arts 20–28, incl. internal complaints, out-of-court dispute settlement, trusted flaggers, the Art. 25 dark-pattern ban, ad transparency) "shall not apply to providers of online platforms that qualify as micro or small enterprises", except Art. 24(3), with a 12-month grace period after outgrowing the status. It does not apply to VLOPs — [Springlex Art. 19](https://www.springlex.eu/en/packages/dsa/dsa-regulation/article-19/)
- **Geo-blocking Regulation (EU) 2018/302 (applies since 3 Dec 2018):**
  - Art. 3: no blocking or limiting access to an online interface on grounds of nationality, residence or establishment, and no redirect without explicit consent.
  - Art. 4(1)(b): no different general conditions of access for services (other than electronically supplied ones) received in a physical location where the trader operates, e.g. "hotel accommodation" and car rental.
  - Art. 5: no payment discrimination by nationality, residence, account location or card issuance place within accepted payment methods — [Regulation 2018/302, EUR-Lex](https://eur-lex.europa.eu/eli/reg/2018/302/oj); [legislation.gov.uk Art. 4 (adopted text)](https://legislation.gov.uk/eur/2018/302/article/4/adopted); [Gide](https://www.gide.com/sites/default/files/gide_paris_nwsl_geoblocking_en_jul2018.pdf)
  - The Art. 4(1)(b) prohibition does not apply to traders exempted from VAT under the small-enterprise scheme — [legislation.gov.uk](https://legislation.gov.uk/eur/2018/302/article/4/adopted)
  - Art. 4(2) allows offering differing general conditions, including net prices, across Member States or to specific customer groups on a non-discriminatory basis — [EUR-Lex](https://eur-lex.europa.eu/eli/reg/2018/302/oj)

### Inferences
DSA:
- **[BP]** Launch without user-generated reviews. Show supplier or third-party rating data with attribution and verification disclosure (Section 4). This avoids DSA hosting duties and Omnibus verification burdens.
- **[LAW]** If user reviews are added:
  - a notice-and-action "Report this review" mechanism;
  - statements of reasons when removing or demoting;
  - a content-moderation section in the T&Cs;
  - published contact points;
  - a verification method (e.g. only guests with a completed stay booking can review), disclosed per Omnibus Art. 7(6);
  - for a non-EU operator, an Art. 13 DSA legal representative in the EU.

  **[LAWYER]** decide whether reviews on hotel pages are "purely ancillary" (hosting service only) or make the site an online platform. If micro/small, most platform duties are exempt either way.

Geo-blocking:
- **[LAW]** No geo-IP blocking or forced redirection of EU visitors. Locale or currency suggestions are fine if the user can stay on or choose any version.
- **[LAW]** Don't price differently by the user's nationality or residence:
  - audit whether `guestNationality` is sent to Hotelbeds or used in markup;
  - Hotelbeds `sourceMarket` is a single configured value in bodiala today;
  - any future per-user `sourceMarket` or nationality-based rates for EU customers would collide with Art. 4(1)(b) — **[LAWYER]** check against Art. 4(2) and Hotelbeds' rate-distribution terms (some "resident" rates are market-restricted).
  - **[BP]** Remove the nationality field from search, or default it, and never let it change EU prices.
- **[LAW]** Accept payment cards from any EU country on equal terms within the brands accepted (Art. 5).

### Gaps
- **Geo-blocking and non-EU traders:** whether the Regulation binds a trader established outside the EU (Georgia) selling to EU customers. Art. 1 scope was not fetched. [LAWYER]
- **Review rules:** the Commission's Geo-blocking review and any 2025–26 amendment proposals, and DSA guidance on review sections being "ancillary", were not found.
- **Supplier rates:** whether Hotelbeds' supplier-side source-market pricing is compatible with Geo-blocking when resold B2C was not covered by any source found.

---

## 8. Required pages and information: legal notice/imprint, T&Cs, cancellation/refund terms per rate, complaint handling, ODR/ADR information (ODR platform closure 2025), accessibility statement

### Takeaway
Minimum public pages at launch:
- Legal notice/imprint (trader identity);
- Terms & Conditions;
- Privacy notice;
- Cookie policy and settings;
- Accessibility statement (unless micro-exempt);
- "How we rank results";
- Complaints and contact (including ADR information where applicable);
- per-rate cancellation terms in the funnel and confirmation.

Remove any link to the EU ODR platform: it closed on 20 July 2025 and the duty to link to it ended then.

### Cited Findings
- **Imprint (E-Commerce Directive 2000/31/EC Art. 5(1)):** online service providers must make easily, directly and permanently accessible:
  - name;
  - geographic address;
  - details allowing rapid, direct contact, including email;
  - trade register and registration number;
  - supervisory authority if the activity needs authorisation;
  - VAT number;
  - professional details for regulated professions.

  Art. 6: commercial communications must be identifiable — [E-Commerce Directive](https://eur-lex.europa.eu/eli/dir/2000/31/oj)
- **CRD pre-contract information:** identity, address, phone, email, complaint-handling policy (Art. 6(1)(b),(c),(g)), and ADR recourse where applicable (Art. 6(1)(t)) — [CRD](https://eur-lex.europa.eu/eli/dir/2011/83/oj)
- **ADR Directive 2013/11/EU, for traders established in a Member State:**
  - Art. 13(1),(2): traders committed or obliged to use ADR must name the ADR entity and its website on their website and in their general T&Cs.
  - Art. 13(3): when a complaint can't be resolved directly, the trader must provide information on the relevant ADR entity and say whether it will use it, on paper or another durable medium — [ADR Directive](https://eur-lex.europa.eu/eli/dir/2013/11/oj)
- **ODR platform closure** [CHANGED 2025-26]: Regulation (EU) 2024/3228 repealed the ODR Regulation 524/2013 with effect from 20 July 2025.
  - The platform stopped accepting complaints from 20 March 2025, and case data was deleted by 20 July 2025.
  - The Art. 14 duty to link to the ODR platform ended on 20 July 2025. Traders should remove ODR references from T&Cs, imprints and websites — [Regulation 2024/3228](https://eur-lex.europa.eu/eli/reg/2024/3228/oj); [CMS](https://cms.law/en/deu/legal-updates/the-odr-platform-is-closing-what-businesses-need-to-know); [Taylor Wessing](https://taylorwessing.com/fr/insights-and-events/insights/2025/01/abolition-of-the-odr-platform); [iubenda](https://www.iubenda.com/en/blog/discontinuation-of-the-european-online-dispute-resolution-odr-platform/)
  - Source conflict: Andersen describes the platform as discontinued on 20 March 2025, conflating the complaint cut-off with closure — [Andersen](https://be.andersen.com/en/news/discontinuation-of-the-european-online-dispute-resolution-odr-platform-and-transition-to-new-digital-adr-tools)
- **Accessibility statement:** EAA Annex V information goes in the T&Cs or an equivalent document (Art. 13(2)) — [EAA, EUR-Lex](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32019L0882)
- **Ranking page:** UCPD Art. 7(4a) ranking-parameters information goes in a dedicated section directly accessible from the results page — [Omnibus](https://eur-lex.europa.eu/eli/dir/2019/2161/oj)

### Inferences
Each item below is a page or content task.
- **[LAW] Legal notice / Imprint** (footer link on every page): legal entity name and form, geographic address (no P.O. box), email, phone, company register and number, VAT ID, managing director(s). Add the travel-agency licence or registration if a target country requires one. For a non-EU entity, also the GDPR Art. 27 representative and, if hosting reviews, the DSA Art. 13 representative. **[LAW-NAT]** e.g. Germany's imprint law (§5 DDG, which replaced the TMG); confirm per market with **[LAWYER]**.
- **[LAW] T&Cs**, drafted by **[LAWYER]**:
  - contracting model: who the contract is with (bodiala as merchant/agent vs the hotel vs Hotelbeds);
  - booking formation (when the contract is concluded);
  - prices and taxes, including taxes payable at the hotel;
  - payment;
  - no statutory withdrawal right for date-specific accommodation;
  - cancellation and amendment per rate;
  - no-shows;
  - hotel responsibility vs OTA responsibility;
  - liability limits that are not unfair;
  - complaints;
  - governing law and jurisdiction clauses that respect Rome I/Brussels Ia (Section 10);
  - the Annex V accessibility information or a link to it;
  - language versions.

  Versioning: store the T&C version accepted with each `HotelBooking` row.
- **[LAW] Per-rate cancellation and refund terms** in the funnel, confirmation email and "My bookings" page (Section 5). Store the cancellation policy snapshot with the booking, so later disputes can be resolved against what was shown.
- **[LAW] Complaints page:** how to complain (email or form), the response-time commitment, and the escalation path. If bodiala is EU-established and has committed to, or is obliged to use, an ADR entity, name it with a link. If not committed, still tell consumers about ADR when a complaint can't be resolved, per ADR Art. 13(3) where applicable. **[LAWYER]** check whether a sectoral travel ADR body (several EU countries have one) applies.
- **[LAW] Remove any ODR link or text.** Templates (e.g. iubenda, Shopify-style T&C generators) may still include it.
- **[LAW]** Privacy notice and Cookie policy (Sections 1–2). Accessibility statement (Section 3, unless micro-exempt). "How we rank results" (Section 4).
- **[BP]** Help/FAQ covering city taxes, check-in ID requirements (the hotel may demand ID or passport at check-in), payment and refunds timing, and the support contact.

### Gaps
- **ADR reform:** status of the ADR Directive revision (Commission proposal 2023) as of Nov 2026 was not verified.
- **Licensing:** national travel-agency licensing or registration duties (e.g. Spain's regional tourism registers, Portugal's RNAVT, Italy's regional authorisation) for a hotel-only online agency were not researched; this is potentially significant. [LAWYER]
- **Mandatory languages:** language requirements for T&Cs and pre-contract information per Member State were not researched.

---

## 9. Marketing: email consent, transactional vs marketing emails

### Takeaway
Transactional emails need no marketing consent and some are legally required: the confirmation on a durable medium. Examples are booking confirmation, voucher, change and cancellation notices, and pre-arrival info.

Marketing emails need prior opt-in consent. The narrow exception is the "soft opt-in" to existing customers for the trader's own similar services, with an opt-out offered at collection and in every message.

### Cited Findings
- **Consent for marketing (ePrivacy Art. 13(1)):** use of electronic mail for direct marketing is allowed only with the subscriber's prior consent — [ePrivacy Directive](https://eur-lex.europa.eu/eli/dir/2002/58/oj)
- **Soft opt-in (Art. 13(2)):** where a trader obtains customers' email details in the context of a sale, it may use them for direct marketing of its own similar products or services. The customer must be clearly and distinctly given the opportunity to object, free of charge and easily, when the details are collected and with each message — [ePrivacy Directive](https://eur-lex.europa.eu/eli/dir/2002/58/oj)
- **Sender transparency (Art. 13(4)):** marketing emails must not disguise or conceal the sender's identity and must include a valid address for opt-out requests — [ePrivacy Directive](https://eur-lex.europa.eu/eli/dir/2002/58/oj)
- **Consent standard:** GDPR consent must be freely given, specific, informed and unambiguous, demonstrable, and as easy to withdraw as to give (Arts 4(11), 7) — [GDPR](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
- **Right to object:** people have an absolute right to object to direct marketing processing (Art. 21(2)–(3)) — [GDPR](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
- **Required confirmation:** CRD Art. 8(7) requires a contract confirmation on a durable medium, i.e. the transactional booking confirmation — [CRD](https://eur-lex.europa.eu/eli/dir/2011/83/oj)
- **Commercial communications:** must be clearly identifiable as such, and the sender must be identifiable (E-Commerce Directive Art. 6) — [E-Commerce Directive](https://eur-lex.europa.eu/eli/dir/2000/31/oj)

### Inferences
Consent capture:
- **[LAW]** Guest form: an unticked, optional checkbox "Send me offers and travel deals by email". Store the consent record: timestamp, the wording version, the source page and IP/user-agent. **[BP]** add double opt-in; it is de facto expected in Germany, where courts require proof of consent. **[LAWYER]** confirm per market.
- **[LAW]** If using the soft opt-in instead: the opt-out checkbox or notice must be shown at the moment the email is collected in checkout. Only market bodiala's own similar services (hotel stays), not third-party offers.
- **[LAW]** Every marketing email needs a one-click unsubscribe that works without login, plus an identifiable sender. Unsubscribes must propagate to the email service provider immediately.

Transactional email:
- **[LAW]** Keep transactional and marketing streams separate: different templates and ideally different sending sub-domains and IPs. **[BP]** Do not insert promotional blocks into transactional emails for users who haven't consented. Regulators may treat a mostly-promotional "transactional" email as marketing.
- **[LAW]** Choose the transactional email provider as a GDPR processor (DPA, sub-processor list, EU hosting or transfer safeguards).
- **[BP]** Abandoned-booking reminder emails are marketing unless strictly service-related. Treat them as needing consent or soft opt-in. **[LAWYER]**

### Gaps
- **National marketing rules:** stricter national unfair-competition rules on email marketing (e.g. German UWG §7, which closely limits soft opt-in) and B2C SMS/WhatsApp rules were not researched in detail.
- **Digital Omnibus:** whether the Digital Omnibus changes the ePrivacy marketing rules was not verified.

---

## 10. Jurisdiction: operator established outside the EU (e.g. Georgia) targeting EU consumers (GDPR Art. 3(2), Art. 27 EU representative, Rome I / Brussels I bis consumer protections), and brief UK differences

### Takeaway
Being established in Georgia does not take bodiala out of EU law. In summary:
- **GDPR** applies via Art. 3(2): offering services to people in the EU, and monitoring them via analytics. The operator needs an EU representative (Art. 27) and gets no one-stop-shop.
- **DSA, EAA and the withdrawal-button rules** are stated to reach non-EU traders targeting the EU.
- **Consumers** can sue at home under Brussels Ia, and choice-of-law clauses can't strip home-country protections under Rome I.
- **Rome I hotel carve-out:** services supplied exclusively outside the consumer's country are carved out of the Rome I consumer rule. That needs a lawyer's analysis for a reseller.

UK: separate but similar rules. UK GDPR, PECR as amended by the Data (Use and Access) Act 2025 (some cookie exemptions from 5 Feb 2026, higher fines), and the DMCC Act 2024 (drip-pricing ban since 6 April 2025, direct CMA fines). No EAA.

### Cited Findings
- **GDPR territorial scope (Art. 3(2)):** applies to a controller not established in the Union where processing relates to (a) offering goods or services to data subjects in the Union, or (b) monitoring their behaviour in the Union — [GDPR](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
- **EU representative (Art. 27):**
  - Art. 3(2) controllers must designate in writing a representative in a Member State where the data subjects are (Art. 27(1),(3)).
  - Exemption (Art. 27(2)(a)): processing that is occasional, does not include large-scale special-category or criminal data, and is unlikely to result in a risk.
  - The representative's identity goes in the Art. 13 notice (Art. 13(1)(a)) — [GDPR](https://eur-lex.europa.eu/eli/reg/2016/679/oj)
- **Transfers:** Georgia has no EU adequacy decision — [European Commission](https://commission.europa.eu/law/law-topic/data-protection/international-dimension-data-protection/adequacy-decisions_en). EDPB: flows to non-EU importers need a Chapter V tool even if the importer is subject to GDPR via Art. 3 — [EDPB 05/2021](https://www.edpb.europa.eu/our-work-tools/our-documents/guidelines/guidelines-052021-interplay-between-application-article-3_en); [Inside Privacy](https://www.insideprivacy.com/gdpr/edpb-publishes-draft-guidelines-on-interplay-of-article-3-gdpr-and-the-gdprs-cross-border-transfer-rules/)
- **Rome I (Regulation 593/2008):**
  - Art. 6(1): a consumer contract is governed by the law of the consumer's habitual residence if the professional pursues or directs activities to that country.
  - Art. 6(2): a choice of law cannot deprive the consumer of the mandatory protections of that law.
  - Art. 6(4)(a): Art. 6 does not apply to contracts for services "to be supplied to the consumer exclusively in a country other than the country in which he has his habitual residence" — the typical hotel-abroad case — [Rome I](https://eur-lex.europa.eu/eli/reg/2008/593/oj)
- **Brussels Ia (Regulation 1215/2012):**
  - Art. 17(1)(c): consumer jurisdiction rules apply where the trader directs activities to the consumer's Member State.
  - Art. 18(1): the consumer may sue in the courts of the consumer's domicile "regardless of the domicile of the other party".
  - Art. 18(2): the trader may sue the consumer only in the consumer's domicile.
  - Art. 19: limits jurisdiction clauses.
  - Art. 6(1): applies Art. 18(1) even to non-EU-domiciled defendants.
  - Art. 17(3): excludes transport contracts other than package travel, but not accommodation — [Brussels Ia](https://eur-lex.europa.eu/eli/reg/2012/1215/oj)
- **CJEU, Pammer and Hotel Alpenhof (C-585/08 and C-144/09, Grand Chamber, 7 Dec 2010):** concerned an Austrian hotel and a German consumer. The court addressed when a website "directs" activities to the consumer's Member State; mere accessibility of the site is not enough, and indicators of intent to trade with that state are required — [Wikipedia summary](https://en.wikipedia.org/wiki/Pammer_and_Alpenhof_cases); [Conflict of Laws.net](https://conflictoflaws.net/2010/ecj-on-pammer-and-hotel-alpenhof/?print=pdf); [Husovec commentary](https://husovec.eu/2010/12/when-does-the-website-direct-its-activity-to-consumers-country/)
  - The indicators commonly cited from the judgment are international nature, other languages or currencies, international phone codes, top-level domain, and itineraries from other states. These are from general knowledge; the search did not confirm paragraph numbers.
- **Other EU regimes reaching non-EU traders:**
  - DSA applies irrespective of the provider's establishment, and non-EU providers need an EU legal representative (Arts 2(1), 13) — [DSA](https://eur-lex.europa.eu/eli/reg/2022/2065/oj)
  - The Dutch ACM sent EAA information requests to e-commerce operators with no EU office — [AccessiWay](https://www.accessiway.com/blog/eaa-enforcement)
  - The withdrawal-function rules apply to non-EU businesses directing activities to EU consumers — [Andersen](https://www.be.andersen.com/en/news/the-eus-new-mandatory-withdrawal-button-key-compliance-steps-for-online-traders-before-june-2026)
- **UK, Data (Use and Access) Act 2025** [CHANGED 2025-26]:
  - Royal Assent 19 June 2025; the main data protection provisions came into force on 5 February 2026 (SI 2026/82).
  - s.112 amends PECR reg. 6 to add consent-exempt categories, including analytics where users can opt out. Advertising and social-media cookies still need consent, and users must still be informed.
  - PECR fines rise to up to GBP 17.5m or 4% of turnover.
  - Sources differ on the exact scope of the cookie exemptions — [Clifford Chance](https://www.cliffordchance.com/insights/resources/blogs/talking-tech/en/articles/2025/09/uk-ico-s-updated-guidance-for-new-exceptions-to-cookie-consents-.html); [Bratby Law](https://bratby.law/data-use-and-access-act-2025-commencement/); [Harper James](https://harperjames.co.uk/news/what-the-data-use-and-access-act-means-for-marketers-and-website-cookies/); [Addleshaw Goddard](https://www.addleshawgoddard.com/en/insights/insights-briefings/2025/data-protection/data-use-and-access-act-2025-top-7-changes/)
- **UK, DMCC Act 2024** [CHANGED 2025-26]:
  - A blanket ban on drip pricing (headline price, then mandatory charges added later, such as VAT or admin fees) applies from 6 April 2025.
  - The CMA finalised price-transparency guidance after a July 2025 consultation and issued its first drip-pricing fine, against the AA's driving-school businesses for their online booking process.
  - The DMCC also bans fake reviews.
  
  Sources: [Linklaters](https://www.linklaters.com/en/insights/blogs/linkingcompetition/2025/june/consumer-deep-dive_understanding-drip-pricing-under-the-dmcc); [Boyes Turner](https://www.boyesturner.com/news-and-insights/drip-pricing-ban-new-rules-now-force); [RPC](https://www.rpclegal.com/snapshots/consumer/winter-2025/cma-launches-first-dmcca-enforcement-cases-and-finalises-price-transparency-guidance/); [TLT](https://www.tlt.com/insights-and-events/insight/cma-issues-first-fine-for-breach-of-consumer-law-under-the-dmcc-act-in-drip-pricing-case); [Mondaq](https://www.mondaq.com/uk/antitrust-eu-competition/1607100/fake-reviews-and-drip-pricing-the-dmcc-act-expanding-enforcement-powers-of-the-competitions-and-markets-authority)
  - Source conflict: one DFA article stated UK drip-pricing rules "will apply from 19 June 2026". This conflicts with the multiple sources giving 6 April 2025 and is probably a confusion with another DMCC provision — [Taylor Wessing](https://www.taylorwessing.com/en/insights-and-events/insights/2026/10/eu-digital-fairness-act-dark-patterns-and-addictive-designs) (via search summary)
- **UK package travel:** the UK is reforming its package travel regulations in parallel with the EU — [Travel Weekly](https://travelweekly.co.uk/news/analysis-uk-and-europe-align-on-package-travel-reforms)

### Inferences
If the operator is established outside the EU (e.g. Georgia):
- **[LAW]** Appoint a GDPR Art. 27 EU representative. Booking processing is regular, not "occasional", so the exemption is unlikely. Name the representative in the privacy notice and the imprint.
- **[LAW]** There is no one-stop-shop: any EU DPA where users live can act.
- **[LAW]** If hosting reviews, also appoint a DSA Art. 13 legal representative; one provider can often act for both. **[LAWYER]**
- **[LAW]/[LAWYER]** Map data flows that are Chapter V "transfers" and put SCCs in place. Examples:
  - Hotelbeds (Spain) sending booking data to a Georgian controller;
  - EU-hosted servers accessed by Georgian staff;
  - EU processors returning data.

  Consider EU hosting plus an EU entity to simplify.
- **[BP]** Consider establishing an EU entity, or contracting through one, for the commercial relationship. This gives a lead DPA (one-stop-shop), simpler ADR participation, a clearer VAT position and easier PSP onboarding. **[LAWYER]** with tax advice.

Choice of law and jurisdiction:
- **[LAW]** T&Cs must not use a governing-law clause (e.g. Georgian law) to deprive EU consumers of home-country mandatory protections where Rome I Art. 6 applies. Clauses forcing consumers to sue in Tbilisi are unenforceable under Brussels Ia Arts 18–19. **[LAWYER]** apply Rome I Art. 6(4)(a) to the actual OTA contract structure: is bodiala's own service supplied "exclusively" in the hotel's country, or is it an online intermediation service supplied to the consumer at home? This determines which law governs the OTA–consumer contract.
- **[LAW]** Public-law enforcement (UCPD, CRD, Geo-blocking, EAA, DSA) by EU authorities and CPC cooperation targets the EU market regardless of the contract's governing law. The website will be judged under EU consumer law because it targets EU consumers: EU languages, EUR prices, EU destinations.

If the UK is in scope:
- **[LAW]** UK GDPR has its own representative requirement for non-UK controllers targeting the UK (UK GDPR Art. 27). From general knowledge; confirm with **[LAWYER]**.
- **[LAW]** UK cookie banner: the analytics exemption under DUAA may allow consent-free first-party analytics with opt-out. Marketing tags still need consent.
- **[LAW]** UK DMCC: all-in headline prices, including mandatory fees. The CMA can fine directly.
- **[LAW]** UK Consumer Contracts Regulations 2013 mirror the CRD accommodation exemption. From general knowledge; no source fetched.
- **[LAW]** The EAA does not apply in the UK; the Equality Act 2010 duties apply instead. From general knowledge; no source fetched.

### Gaps
- **Georgian law:** Georgian domestic law (Georgia's Law on Personal Data Protection 2023, which is EU-inspired; Georgian consumer law) was not researched.
- **Rome I Art. 6(4)(a):** how it applies to an OTA reselling foreign hotel rooms (agency vs merchant model). No authoritative source was found; it is a key question for counsel.
- **Pammer criteria:** the exact paragraph-level criteria (para. 93 indicator list) were not verified from the judgment text.
- **Geo-blocking for non-EU traders:** whether it binds a non-EU trader was not verified.
- **UK:** the UK GDPR Art. 27 representative point, UK package-travel reform details and the final CMA price-transparency guidance date were not verified from primary sources.
- **VAT and tourism taxes:** VAT treatment of hotel resale by a non-EU OTA (the EU Tour Operators' Margin Scheme vs agency) and tourism-tax collection duties are outside this brief but likely to generate launch tasks. [LAWYER] with a tax adviser.
