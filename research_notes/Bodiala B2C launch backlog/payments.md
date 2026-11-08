# Payments for a small B2C OTA reselling Hotelbeds inventory (EU consumers, EUR, ES/PT hotels)

Research date: 2026-11-08. Sources dated before 2025 are flagged **[OLD]**. Third-party or vendor-competitor sources are flagged **[2nd-hand]**. Internal bodiala project docs are cited as file links; they record what our integration contract says, not Hotelbeds' commercial terms.

## 1. Merchant model: merchant-of-record vs agency / pay-at-hotel, cash flow and risk, settlement with Hotelbeds

### Takeaway
A bedbank reseller is normally the merchant of record: it charges the guest, owns the chargeback and refund risk, and pays Hotelbeds the net rate on terms set in its own contract. Pay-at-hotel removes payment risk but leaves little room for margin. Hotelbeds' public docs describe its pricing models (net vs commissionable) but not its client payment or credit terms, so those must be settled with the HBX Group sales manager before launch.

### Cited Findings
- Definitions. Under the agency model the hotel remains the seller of record. Under the merchant model the distributor collects payment from the traveller and remits an agreed net amount to the hotel — [AltexSoft glossary: merchant model](https://www.altexsoft.com/glossary/merchant-model/)
- Online travel agencies have traditionally offered either prepayment (merchant model) or pay-at-hotel (agency model) — [Hospitality Net opinion](https://www.hospitalitynet.org/opinion/4110788.html) **[2nd-hand, undated]**
- Bedbanks sell mostly through third-party OTAs. In that model the OTA keeps the customer's cash and passes it to the bedbank only on departure (per an industry commentary by Steve Endacott) — [Travel Weekly comment](https://www.travelweekly.co.uk/articles/316101/comment-bookingcom-grabs-the-cash-but-will-it-impact-sales) **[2nd-hand; date not checked]**. This describes how large OTAs negotiate, not a Hotelbeds default for small clients.
- Hotelbeds has two pricing models:
  - **Net:** prices are net. If `hotelMandatory=true` you must sell at `sellingRate`; otherwise you may add your own markup.
  - **Commissionable:** commission is included in `sellingRate`, and that price is final regardless of `hotelMandatory`.
  - Depending on the model, up to three currencies (EUR, USD, GBP) can be offered by destination.
  - Hotelbeds tells clients to contact their HBX Group sales manager to choose a model.
  - Source: [Hotelbeds developer docs: Pricing models](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/pricing-models)
- That page's sample commissionable rate is `paymentType: AT_HOTEL`, with net 171.72, sellingRate 190.80 and commission 10%. This shows that commissionable (pay-at-hotel style) rates exist alongside net rates. The page does not explain AT_WEB vs AT_HOTEL — [Hotelbeds Pricing models](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/pricing-models)
- Our own contract notes record:
  - Rates carry `paymentType (AT_WEB|AT_HOTEL)`, a `cancellationPolicies[]{amount,from}`, and booking-time `paymentData?{paymentCard,contactData}`.
  - The booking response has `totalNet`, `pendingAmount` and `status (CONFIRMED|PRECONFIRMED|CANCELLED)`.
  - Source: [bodiala docs/hotelbeds/api-contract.md](file:///C:/Users/user/IdeaProjects/bodiala/docs/hotelbeds/api-contract.md)
- **Internal conflict:** the two internal docs disagree on when `paymentData` is needed. The API contract says it is required for AT_HOTEL rates and credit-card accounts ([api-contract.md](file:///C:/Users/user/IdeaProjects/bodiala/docs/hotelbeds/api-contract.md)). The plan says AT_WEB "card-required" rates need `paymentData.paymentCard` ([multi-provider-plan.md](file:///C:/Users/user/IdeaProjects/bodiala/docs/hotelbeds/multi-provider-plan.md)). This must be confirmed with Hotelbeds.
- On the supply side, hotels invoice Hotelbeds on the check-out date and Hotelbeds settles about 30–45 days later (virtual-card contracting model). This shows Hotelbeds itself works on deferred settlement to hotels — [NightsBridge help: Hotelbeds payments](https://nightsbridge.screenstepslive.com/a/1614932-hotelbeds-payments) **[2nd-hand, undated]**
- HBX Group (Hotelbeds' parent) has partnered with YouLend to connect travel businesses to funding. This suggests working capital is a known constraint for Hotelbeds clients — [The Paypers](https://thepaypers.com/payments/news/hbx-group-connects-travel-businesses-to-funding-via-youlends-solution) (headline only)

### Inferences
- **Merchant-of-record cash flow.** The guest pays at booking. Hotelbeds is paid either up front (prepayment or deposit) or on credit terms, which are not public. On credit terms the reseller holds a positive float, but must treat that cash as a liability, because cancellations and chargebacks can arrive months later.
- **Risks the reseller carries as merchant of record:**
  - Chargebacks for "service not provided" when a hotel walks a guest or the bedbank fails.
  - Refunds on refundable rates.
  - The asymmetric case where a non-refundable (NRF) rate is non-refundable at Hotelbeds but a card dispute forces a refund to the guest.
  - FX, if the Hotelbeds contract currency differs from the EUR the site sells in. Selling EUR on EUR-contracted ES/PT inventory largely removes this.
- **Pay-at-hotel (AT_HOTEL / commissionable)** largely avoids PSP and PCI exposure. But the sell price is fixed, so margin comes only from commission paid later, and Hotelbeds may need guest card details (`paymentData`) as a guarantee. Passing the guest's raw card to Hotelbeds through our backend would put us in PCI scope well beyond SAQ A (see §3).
- **PSP delayed-delivery risk.** Stays can be weeks or months after payment, so the PSP's exposure is the full value of future stays. This drives reserves (see §2).

#### Recommended tasks
- **[Biz]** Get from HBX Group, in writing:
  - Pricing model (net vs commissionable).
  - How we pay Hotelbeds: credit line, prepayment or deposit, or virtual card per booking. Also the payment terms, the settlement currency, and whether a bank guarantee or deposit is required.
  - Whether `paymentData` is ever required for our account.
  - Whether all returned rates may be sold standalone to consumers (see §6).
- **[Biz]** Choose merchant of record for launch: sell only AT_WEB, net-priced rates we can mark up and charge ourselves. Hide AT_HOTEL rates at first, unless Hotelbeds confirms no guest card needs to pass through us.
- **[Eng]** Add a `paymentType` filter (the availability request supports `filter.paymentType` per [api-contract.md](file:///C:/Users/user/IdeaProjects/bodiala/docs/hotelbeds/api-contract.md)). Persist `paymentType`, `rateClass` (NOR/NRF), `totalNet`, the sell price, `currency` and `pendingAmount` on every booking row.
- **[Finance]** Model a cash-flow ledger with these accounts:
  - Customer funds held (liability).
  - Payable to Hotelbeds (net).
  - Margin earned.
  - Reserve held by PSP.
  - Chargeback provisions.

### Gaps
- No public Hotelbeds documentation found on client payment terms: credit limits, prepayment, deposits or guarantees, settlement frequency, or whether small B2C startups get credit. This is contractual and not public.
- The exact meaning of `AT_WEB` and `AT_HOTEL`, and of `pendingAmount`, could not be confirmed from a public Hotelbeds page. The developer portal search did not surface it.

## 2. PSP choice for a European OTA: Stripe vs Adyen vs others

### Takeaway
Stripe lists "travel reservation services and clubs" as a restricted business that needs extra due diligence, and one of its support FAQs still says it does not support them. Expect manual underwriting, and possibly a reserve, before a Hotelbeds-reselling OTA goes live. Adyen publishes IC++ pricing with an industry-dependent minimum invoice, and competitor sources describe its onboarding as selective for small travel merchants. Both support Bizum and MB WAY, but these wallets cannot do authorise-then-capture.

### Cited Findings
- **Stripe restricted list (updated 2026-09-22):**
  - "Travel reservation services and clubs" is restricted globally (requires additional due diligence; approval not guaranteed).
  - Commercial airlines and cruises, cross-border charter airlines and timeshares are prohibited.
  - There are no EU- or UK-specific travel entries, and no entry for travel agencies, online travel or lodging.
  - Source: [Stripe restricted businesses](https://stripe.com/legal/restricted-businesses)
- **Conflict:** Stripe's support FAQ (undated) says Stripe and its partners deem travel reservation services and clubs high risk and do not support them. It adds that some listed businesses may be supported with explicit prior approval — [Stripe support FAQ](https://support.stripe.com/questions/prohibited-and-restricted-businesses-faqs). The dated legal list (Sept 2026) is the more authoritative.
- Reserves are funds a PSP holds to cover chargebacks on higher-risk accounts. One blog cites a typical 5–10% of volume for 30–180 days — [Chargeflow blog](https://www.chargeflow.io/es/blog/stripe-high-risk-business) **[2nd-hand, unverified figure]**. No Stripe source confirms travel-specific reserve terms.
- **Adyen published pricing:**
  - Card fees: fixed $0.13 processing fee + Interchange++ + 0.60% (Visa/Mastercard).
  - iDEAL $0.13 + €0.22. MB WAY $0.13 + 1.40% + €0.22. Bizum requires a direct contract with Bizum plus a management fee. Apple Pay and Google Pay are priced as the underlying card.
  - No setup or monthly fees, but there is a minimum invoice that depends on industry or business model.
  - Fees are indicative, with custom pricing via sales. The page rendered in USD, possibly because of geolocation.
  - Source: [Adyen pricing](https://www.adyen.com/pricing)
- Competitor and affiliate reviews describe Adyen as best suited to merchants above about $1M a year. They also describe it as conservative on travel because of the long gap between payment and delivery, and selective in onboarding — [Dodo Payments review](https://dodopayments.com/blogs/adyen-review-alternative), [Convesio](https://convesio.com/knowledgebase/article/adyen-not-for-mid-market/) **[2nd-hand, competitor sources: low confidence]**
- **Stripe local methods:**
  - **Bizum (Spain):** EUR only. Customer-initiated real-time payment. No recurring payments, no manual capture. Full and partial refunds up to 395 days. Customers can dispute within 120 days. Limits €0.50–€5,000. Needs a tax ID at onboarding, and the capability stays pending until Bizum verifies it. Not supported in the Express Checkout Element — [Stripe Bizum](https://docs.stripe.com/payments/bizum)
  - **MB WAY (Portugal):** EUR, B2C only, Portuguese customers. No manual, partial or multi-capture. No extended auth. No off-session or setup_future_usage. Refunds within 1 year. Full disputes only, with 7 days to submit evidence. Radar is not supported. Statement shows Stripe's name, not ours. Limits €0.50–€5,000, and the user's default daily limit is €1,000 — [Stripe MB WAY](https://docs.stripe.com/payments/mb-way). Added September 2025 — [Stripe changelog](https://docs.stripe.com/changelog/clover/2025-09-30/mb-way-payments)
- Stripe extended authorisation (holds up to 30 days) is offered on IC+ pricing. Blended-pricing users must contact support — [Stripe extended authorization](https://docs.stripe.com/payments/extended-authorization.md?platform=web&ui=elements)

### Inferences
- For a small startup, Stripe is the realistic first PSP: self-serve integration, Payment Element, Radar, Bizum and MB WAY. But apply early and describe the model honestly: hotel resale, prepaid, typical booking-to-stay lead time, and refund policy. Restricted-category review can mean a rolling reserve or delayed payouts.
- Adyen is a later option, once volume justifies it, or if Stripe declines.
- Alternatives worth a quote: Checkout.com, Mollie, and travel-specialist acquirers or orchestrators. **None were researched in this pass.**
- Bizum and MB WAY are capture-at-confirmation methods, so the booking flow must support pay-first-then-book with automatic refund (see §5).
- MB WAY's €1,000 default daily limit will block many hotel stays.

#### Recommended tasks
- **[Biz]** Open a Stripe account under the operating entity. Complete the restricted-business questionnaire. Ask directly about reserves, payout schedule and IC+ pricing (needed for extended auth). In parallel, request quotes from Adyen and one travel-specialist acquirer.
- **[Eng]** Build the payment layer behind an interface (`PaymentProvider`), like the existing `*Registry` pattern, so the PSP can be swapped.
- **[Eng]** Launch with cards plus Apple Pay and Google Pay. Add Bizum, MB WAY and iDEAL behind a flag using the pay-first flow.

### Gaps
- No primary source on Stripe's or Adyen's actual reserve policy for OTAs.
- Did not verify MCC assignment: whether a reseller is coded 4722 (travel agencies) or 7011 (hotels). This affects extended-auth eligibility (§4) and scheme high-risk programs.
- Stripe's standard EEA card pricing, multi-currency settlement and Apple/Google Pay specifics were not re-verified in this pass.
- Stripe is generally not available to businesses incorporated in Georgia (not verified here). A Georgian operator would likely need an EU or UK entity to contract with Stripe or Adyen. Check Stripe's supported-countries list.

## 3. PCI DSS v4.0.1 scope: staying in SAQ A

### Takeaway
Using a PSP-hosted payment form (Stripe Payment Element or Checkout; Adyen Drop-in, Components or hosted page) keeps us eligible for SAQ A. Since the January 2025 SAQ A revision, the payment-page script controls (6.4.3 and 11.6.1) are no longer SAQ A line items. In their place, the merchant must confirm the site is not susceptible to script attacks, and an August 2026 FAQ revision narrowed the easy "not applicable" path. Never let raw card data touch the bodiala backend, including when passing cards to Hotelbeds.

### Cited Findings
- **SAQ A r1 (January 2025)** removed requirements 6.4.3 (payment-page script management), 11.6.1 (change and tamper detection) and, per some sources, 12.3.1 from SAQ A. It added a new eligibility criterion. Effective 31 March 2025, alongside PCI DSS v4.0.1 — [usd.de](https://www.usd.de/en/update-pci-saq-a-2025/), [Adyen SAQ A eligibility](https://docs.adyen.com/development-resources/pci-dss-compliance-guide/saq-a-eligibility)
- **SAQ A eligibility (as Adyen summarises it):**
  - All elements of the payment pages delivered to the browser come only and directly from a PCI DSS compliant third-party service provider or processor.
  - The merchant confirms the site is "not susceptible to attacks from scripts that could affect your e-commerce systems".
  - Merchants still run vulnerability scans (SAQ A 11.3.2).
  - Adyen supplies an annual Attestation of Compliance (AoC) and scan information.
  - 6.4.3 and 11.6.1 still apply to SAQ D merchants and to anyone needing an onsite assessment.
  - Source: [Adyen SAQ A eligibility](https://docs.adyen.com/development-resources/pci-dss-compliance-guide/saq-a-eligibility)
- **FAQ history:**
  - FAQ 1588 (Feb 2025): merchants can meet the script criterion using techniques like those in 6.4.3/11.6.1, or with a service-provider attestation.
  - FAQ 1331 (23 May 2025): let a merchant and its assessor (QSA) treat SAQ eligibility as grounds to mark 6.4.3/11.6.1 not applicable.
  - FAQ 1331 revision (4 Aug 2026): merchants must now consult their acquirer or card brands. QSA agreement alone is no longer enough.
  - Source: [Reflectiz blog](https://www.reflectiz.com/blog/pci-faq-1331-revision-2026/) **[2nd-hand vendor blog; verify against PCI SSC FAQ text]**
- Some sources note the new criterion applies to the whole website, not just the payment page — [usd.de](https://www.usd.de/en/update-pci-saq-a-2025/) (via search summary).

### Inferences
- Stripe Payment Element and Checkout (iframe or redirect) keep bodiala on SAQ A. The React app and Spring backend never see a card number (PAN): the backend handles only PaymentIntent IDs and client secrets.
- The new script criterion still means hardening the checkout page:
  - A strict Content-Security-Policy allow-listing only Stripe's domains, plus our own bundle.
  - No third-party marketing or analytics tags on checkout routes.
  - Subresource Integrity on static assets where possible.
  - Monitoring of script changes.
- A Vite single-page app that loads tag managers on every route is the main risk.
- If any flow requires sending the guest's card to Hotelbeds (`paymentData.paymentCard`), bodiala would store, process or transmit the card number and fall into SAQ D. Avoid such rates, or use a PSP "vault and forward" service (not researched).

#### Recommended tasks
- **[Eng]** Use Stripe Payment Element (or Checkout). Add CSP headers on `/checkout`. Keep checkout routes free of third-party scripts. Add an automated test that fails if new script origins appear.
- **[Eng/Sec]** Schedule quarterly external vulnerability scans (ASV) of the public site. Keep the PSP's AoC on file.
- **[Biz]** Complete SAQ A annually. Ask the acquirer whether it accepts SAQ A, and which version and FAQ interpretation it expects.

### Gaps
- PCI SSC's own SAQ A r1 PDF and FAQ 1588/1331 text were not fetched directly; findings rely on Adyen's doc plus vendor blogs.
- Stripe's own statement of which integrations qualify for SAQ A was not fetched in this pass.
- Retirement dates of v3.2.1 and v4.0 are not sourced here.

## 4. PSD2 SCA: when 3DS2 is required, exemptions, later charges, auth vs capture timing

### Takeaway
Every EEA-to-EEA online card checkout must be ready for 3DS2. Exemptions (low value, transaction risk analysis) are requested by the PSP but granted by the issuer. Later charges, such as cancellation or no-show fees on a saved card, require SCA when the card mandate is set up and then run as merchant- or payee-initiated transactions. For an OTA that books the supplier within seconds of authorising, the standard 7-day authorisation is enough. Extended 30-day holds are meant for unknown final amounts and are limited by card brand and merchant category.

### Cited Findings
- **Scope:**
  - SCA applies to customer-initiated online card payments where both the business's acquirer and the cardholder's bank are in the EEA. The UK applies equivalent rules with £ thresholds.
  - Out of scope: merchant-initiated transactions (MITs), one-leg-out payments (acquirer or issuer outside the EEA), and MOTO. The issuer can still demand authentication.
  - Source: [Stripe SCA guide](https://stripe.com/guides/strong-customer-authentication)
- **Exemptions:**
  - Low value: under €30 / £25. Authentication is required after 5 exempted payments or €100 / £85 cumulative.
  - Transaction risk analysis: the PSP's fraud rate must stay under 0.13% (up to €100), 0.06% (up to €250) or 0.01% (up to €500).
  - Recurring fixed amount, trusted beneficiaries, and secure corporate payments (lodged or virtual travel cards).
  - Source: [Stripe SCA guide](https://stripe.com/guides/strong-customer-authentication)
- **Liability:** successful or frictionless 3DS shifts fraud liability to the issuer. With an exemption or no 3DS, liability stays with the merchant. Merchants on fraud monitoring programs, or in ineligible categories, may not get the shift — [Stripe SCA guide](https://stripe.com/guides/strong-customer-authentication)
- **MIT flow:** authenticate when the card is first saved or on the first payment, obtain a mandate, then flag later charges as MITs — [Stripe SCA guide](https://stripe.com/guides/strong-customer-authentication)
- **EBA Q&A 2019_4792** (final 12 Mar 2021, answered by the Commission). Card details taken remotely as a guarantee create a mandate whose setup is subject to SCA. Later hotel-initiated charges (deposits, late-cancellation or no-show fees, post-stay charges) are payee-initiated transactions under the general PSD2 rules — [EBA Q&A 2019_4792](https://eba.europa.eu/single-rule-book-qa/qna/view/publicId/2019_4792) **[2021, pre-PSR]**
- **EBA Q&A 2019_4797** (final 5 Nov 2021). SCA performed at booking by an OTA under its merchant ID can support a future-dated payment, provided the payee and amount do not change. Intermediaries need not be disclosed — [EBA Q&A 2019_4797](https://eba.europa.eu/single-rule-book-qa/qna/view/publicId/2019_4797) **[2021]**
- PSD2/RTS set no time limit on how far ahead SCA can be applied for a future-dated payment (EBA Q&A 2019_4795, as cited in 2019_4797) — [EBA Q&A 2019_4797](https://eba.europa.eu/single-rule-book-qa/qna/view/publicId/2019_4797)
- **PSD3 / Payment Services Regulation (PSR)** — successor rules to PSD2:
  - Provisional agreement reached 27 Nov 2025. It keeps SCA, adds risk assessment, spending limits and non-smartphone authentication options, and extends fraud liability to PSPs and online platforms.
  - COREPER (the EU ambassadors' committee) endorsed the texts 22 Apr 2026, and final compromise texts were published 24 Apr 2026.
  - Publication in the Official Journal was expected mid-2026 but is **not confirmed** in sources found.
  - Sources: [Mondaq](https://webiis08.mondaq.com/unitedstates/financial-services/1713372/european-council-and-parliament-reach-provisional-agreement-on-eu-payments-package), [Finance Malta](https://financemalta.org/industry-updates/eu-reaches-compromise-on-psd3-and-psr), [Fiskil tracker](https://www.fiskil.com/open-finance-tracker/standard/psd3) **[2nd-hand; check the Official Journal]**
- **Authorisation timing (Stripe):**
  - Default online card authorisation validity is 7 days.
  - Extended authorisation (`request_extended_authorization=if_available`, manual capture) gives up to 30 days, by brand:

    | Brand | Who qualifies | Validity |
    |---|---|---|
    | Visa | Hotel, lodging, vehicle rental, cruise | 29 days 18 h |
    | Visa | Other categories (+0.08% fee, customer-initiated only) | Same extended window |
    | Mastercard | All categories | 30 days |
    | Amex | Lodging and vehicle rental (capture by end of stay) | 30 days |

  - Many networks allow extended windows only when the final amount is unknown at authorisation.
  - Rely on `capture_before` on the charge.
  - Source: [Stripe extended authorization](https://docs.stripe.com/payments/extended-authorization.md?platform=web&ui=elements)

### Inferences
- **Default flow:** a customer-initiated payment with 3DS2 handled by the Payment Element, manual capture, supplier booking within the same session, then immediate capture. No extended auth is needed, and our known-amount prepaid sales likely don't qualify anyway.
- **"Book now, pay later" or cancellation-fee charging later:** save the card with SCA (SetupIntent or `setup_future_usage=off_session`), show a clear mandate (amount and dates from the Hotelbeds `cancellationPolicies`), and charge later as an MIT.
- That later charge can be declined or soft-declined. The guest then has to come back on-session, so the business must accept the risk of unpaid supplier penalties.
- A non-EU acquirer (for example, via a Georgian entity) would make payments one-leg-out. That is technically outside SCA, but EEA issuers may still decline or demand 3DS, and approval rates suffer.

#### Recommended tasks
- **[Eng]** Use PaymentIntent `capture_method=manual` with 3DS automatic. Never call Hotelbeds book until the PaymentIntent is `requires_capture`. Capture right after Hotelbeds returns `CONFIRMED`.
- **[Eng]** Persist `capture_before`. Run a job that alerts on any uncaptured authorisation older than 24 h.
- **[Eng/Product]** Defer "pay later" and stored-card MITs to phase 2. When built, use SetupIntent with SCA plus mandate text stored with the booking.
- **[Biz]** Track PSR final adoption and application date. Re-check the EBA Q&As once the PSR replaces PSD2's SCA provisions.

### Gaps
- No primary source on whether a reseller coded MCC 4722 gets Visa lodging treatment.
- Did not fetch the RTS (Reg. 2018/389) text directly; thresholds come from Stripe's guide.
- UK post-Brexit SCA divergences (FCA) were not researched beyond Stripe's £ thresholds.

## 5. Payment–booking orchestration: authorise, book, capture; failures, idempotency, webhooks, refunds, reconciliation

### Takeaway
Use a persisted state machine: price re-check, then authorise, then book with Hotelbeds, then capture. Every external call carries an idempotency key, and every outcome is reconciled. The dangerous states are "supplier booked but not captured" and "supplier outcome unknown after a timeout", and both need automated reconciliation. Bizum and MB WAY can't hold funds, so they need a pay-then-book-then-auto-refund variant.

### Cited Findings
- **Stripe idempotency keys:**
  - Up to 255 characters; V4 UUIDs suggested; never use personal data as a key.
  - Keys may be pruned after at least 24 h.
  - Retries return the saved first response, including 500 errors.
  - Mismatched parameters on a reused key raise an error.
  - Accepted on all v1 POST requests, with no effect on GET or DELETE.
  - Source: [Stripe idempotent requests](https://docs.stripe.com/api/idempotent_requests)
- Bizum and MB WAY refunds are asynchronous; final status arrives by `refund.updated` / `refund.failed` webhook. A failed refund returns the funds to the balance and the merchant must refund by another means — [Stripe Bizum](https://docs.stripe.com/payments/bizum), [Stripe MB WAY](https://docs.stripe.com/payments/mb-way)
- Bizum and MB WAY support neither manual capture nor off-session charges — [Stripe Bizum](https://docs.stripe.com/payments/bizum), [Stripe MB WAY](https://docs.stripe.com/payments/mb-way)
- **Hotelbeds side (our contract notes):**
  - Booking needs a `clientReference` of up to 20 characters, which Hotelbeds does **not** deduplicate.
  - A `tolerance` (% price increase accepted, default 2) is sent at booking.
  - Responses carry `totalNet` and `pendingAmount`.
  - Cancel returns `cancellationAmount` (0 means free).
  - Source: [api-contract.md](file:///C:/Users/user/IdeaProjects/bodiala/docs/hotelbeds/api-contract.md)
- bodiala's `book()` and `cancel()` are deliberately not `@Transactional`. Idempotency is best-effort, using a unique `agent_ref_no` and a pre-call lookup. Reserving before the call and reconciling orphan bookings are deferred — [bodiala CLAUDE.md](file:///C:/Users/user/IdeaProjects/bodiala/CLAUDE.md)

### Inferences

#### Recommended state machine (card flow)
1. **QUOTED.** Call checkrates (always). Freeze the net rate, cancellation policy and sell price. Create the booking row with `clientReference` and generate the Stripe idempotency key from it.
2. **AUTH_PENDING.** Create the PaymentIntent (manual capture, amount = sell price, metadata `booking_id`) and confirm it client-side with 3DS. Advance on the synchronous result *and* on the `payment_intent.amount_capturable_updated` webhook, whichever comes first, deduplicated by booking state.
3. **AUTHORISED → SUPPLIER_PENDING.** Write the state *before* calling Hotelbeds `POST /bookings`.
   - Set `tolerance` to 0, or to a margin smaller than our markup, so we never book at a net price higher than we've accounted for.
   - Outcomes:
     - **Definite failure** (4xx/5xx with error body): cancel the PaymentIntent (void the authorisation) and show the guest an error.
     - **Timeout or unknown:** keep the authorisation. Reconcile by `clientReference` against Hotelbeds' booking list before voiding. Because Hotelbeds doesn't deduplicate, a blind retry may double-book.
     - **CONFIRMED:** capture the PaymentIntent (with idempotency key), then mark the booking CONFIRMED.
     - **PRECONFIRMED:** hold, poll, and decide on a policy.
4. **CAPTURE_FAILED with supplier booked.** Retry the capture. If the authorisation is gone, cancel at Hotelbeds immediately while it's free (check `cancellationPolicies.from`). Otherwise, flag for manual handling: we owe Hotelbeds but hold no funds.
5. **CANCELLED.**
   - Call Hotelbeds cancel to get `cancellationAmount` (the net-side penalty).
   - Compute the guest refund as paid minus the sell-side penalty from our own published policy.
   - Issue the Stripe refund (partial if needed) with an idempotency key.
   - Store the refund ID and the expected supplier credit.

#### Wallets (Bizum / MB WAY)
- Payment is captured at confirmation, so book with Hotelbeds after `payment_intent.succeeded`.
- On supplier failure, auto-refund and track the refund webhook to `succeeded`.

#### Webhooks and reconciliation
- **Webhooks:** verify signatures, store raw events, and process idempotently by event ID.
- **Daily reconciliation:**
  - Stripe balance transactions and payouts against booking rows (captured amount, fees, refunds, disputes).
  - Hotelbeds booking list and supplier statements against booking rows (net payable, cancellation charges).
  - Alert on any booking that is CONFIRMED at Hotelbeds without a capture, or captured without a Hotelbeds reference.
- **Accounting records per booking:**
  - Gross sale, PSP fee, net cost, margin, VAT on margin (§7), refunds and penalties, and chargeback status.
  - An invoice or receipt number from a gap-free sequence.

#### Recommended tasks
- **[Eng]** Add a `payment` table: PaymentIntent ID, status, `capture_before`, amounts, refund IDs. Add a state column on `HotelBooking`.
- **[Eng]** Build a Stripe webhook endpoint (signature-verified, idempotent).
- **[Eng]** Add a reconciliation job that finds orphan bookings at Hotelbeds by `clientReference`.
- **[Eng]** Add a test matrix using the existing `hotelbeds-stub` to simulate: Hotelbeds timeout, Hotelbeds 5xx, price change beyond tolerance, capture failure, and partial refund. Add stub endpoints to support these.

### Gaps
- Whether Hotelbeds' booking-list endpoint can filter by `clientReference` (needed for timeout reconciliation) was not confirmed. Our contract lists `clientReference` dedup as unverified.
- The meaning of `PRECONFIRMED` timing and of `pendingAmount` is not confirmed from a public source.
- No source fetched on Stripe webhook event ordering and retry guarantees (standard behaviour, but not cited here).

## 6. Pricing and margin: markup on net, Hotelbeds selling-price and rate-integrity rules, total price display

### Takeaway
On the net model we may mark up the net rate unless `hotelMandatory=true`. In that case we must sell at Hotelbeds' `sellingRate`, and our margin is `sellingRate − net`. Hotelbeds polices "rate integrity", meaning rates meant for offline or packaged channels must not leak into OTA channels, so standalone B2C resale must be cleared contractually. EU consumer enforcement expects the headline price to be the total, including all unavoidable charges.

### Cited Findings
- In the net model, `sellingRate` appears only when `hotelMandatory=true` and must then be respected; otherwise our own markup is allowed. In the commissionable model, `sellingRate` is always final — [Hotelbeds Pricing models](https://developer.hotelbeds.com/documentation/hotels/knowledge-base/pricing-models)
- Our current mapping already displays `sellingRate` when present, otherwise `net`, and never exposes `net` as the customer price — [multi-provider-plan.md](file:///C:/Users/user/IdeaProjects/bodiala/docs/hotelbeds/multi-provider-plan.md), [api-contract.md](file:///C:/Users/user/IdeaProjects/bodiala/docs/hotelbeds/api-contract.md)
- **Hotelbeds rate integrity (2019):** a dedicated team ensures rates intended for offline channels aren't sold by OTAs. A three-strike policy disconnects partners found distributing opaque rates in the OTA channel. Hotelbeds said it gave up about €300M in sales to enforce this — [Travelmole, 4 Nov 2019](https://travelmole.com/news/hotelbeds-outlines-commitment-to-rate-integrity) **[OLD]**. Related coverage: [Travolution: combatting rogue OTAs](https://www.travolution.com/articles/113038/combatting-rogue-otas-highlighted-as-one-advantage-of-bed-banks-in-hotelbeds-report)
- **CPC network (EU consumer enforcement):**
  - Booking.com committed on 18 Dec 2019, and Expedia on 18 Dec 2020, to change how offers, discounts and prices are presented.
  - Airbnb agreed to show the total price including all applicable mandatory charges and fees (factsheet July 2019).
  - Source: [European Commission: accommodation booking coordinated action](https://commission.europa.eu/live-work-travel-eu/consumer-rights-and-complaints/enforcement-consumer-protection/coordinated-actions/accommodation-booking_en) **[OLD actions, still the reference standard]**
- Expedia's fix was to show a total including unavoidable charges such as city taxes, resort fees and cleaning fees — [Commission Expedia factsheet (Dec 2020)](https://commission.europa.eu/system/files/2020-12/factsheet-expedia_enforcement_action_1.pdf) (via search summary) **[OLD]**

### Inferences
- **Markup engine:** `sell = max(net × (1 + markup%) + fixed, sellingRate if hotelMandatory)`.
  - Round in EUR.
  - Never go below `sellingRate` when mandatory, or ideally whenever `sellingRate` is present.
  - Cover the PSP fee (the actual rate depends on the PSP contract; Adyen's indicative rate is IC++ + 0.60% + $0.13), the chargeback provision, and VAT on the margin (§7).
- Rate integrity is the biggest **commercial** risk to a standalone B2C site built on bedbank net rates. If Hotelbeds classifies some rates as package-only or opaque, selling them standalone could get the account disconnected. Confirm in the contract which rates or flags may be sold standalone.
- **Total price display:** show the all-in price from search results onward. Show local tourist taxes payable at the hotel separately but prominently (Hotelbeds `taxes` / `rateComments`). Don't run "fake discount" or strikethrough pricing without a genuine reference price.

#### Recommended tasks
- **[Biz]** Confirm with Hotelbeds: which rates may be shown standalone to consumers, whether any "packaging-only" rates are returned, minimum selling-price rules, and any rate-parity clauses.
- **[Eng]** Build a configurable markup service (by destination, star rating and rate class). Enforce a `hotelMandatory` floor in code. Log net, sell and margin per booking.
- **[Eng/UX]** Display the total price including taxes and fees on result cards. Label "taxes payable at hotel" from Hotelbeds tax data. Show the cancellation-policy amount and deadline in the guest's timezone.

### Gaps
- No public Hotelbeds doc describing a packaging-only flag or standalone-sale restriction in APItude (searched; not found).
- No current (2025–26) EU enforcement update on accommodation price display found. The Price Indication Directive and Unfair Commercial Practices Directive texts were not fetched.

## 7. Tax: EU VAT on travel resale (TOMS / Art. 306), tourist taxes, invoicing; where the operator is established

### Takeaway
An OTA that sells hotel rooms in its own name, rather than as a disclosed agent, falls under the EU special scheme for travel agents (TOMS, Arts. 306–310). The CJEU confirmed in 2023 that even accommodation-only resale qualifies. The single service is taxed where the agent is established, with VAT charged on the margin. A non-EU operator (for example, Georgia-based) has no EU establishment for Article 307 to attach to. Member States disagree on whether such operators may use TOMS, and if not, they may face normal VAT on accommodation in each hotel country. Get specialist advice before choosing the entity.

### Cited Findings
- **Article 306** — the scheme applies to:
  - Travel agents who deal with customers in their own name and use supplies of other taxable persons to provide travel facilities.
  - Not to agents acting solely as intermediaries.
  - Tour operators are treated as travel agents.
  - Source: [Directive 2006/112/EC Art. 306 (legislation.gov.uk copy)](https://www.legislation.gov.uk/eudr/2006/112/article/306) (UK-retained copy as at 2020; wording of the EU original)
- **Article 307** — such transactions are a single service, taxable in the Member State where the travel agent "has established his business or has a fixed establishment" from which it supplied the service — [Art. 307](https://www.legislation.gov.uk/eudr/2006/112/article/307)
- **CJEU C-108/22:** the special scheme applies to mere resale of accommodation services — [PwC Austria tax news, 22 Aug 2023](https://steuernachrichten.pwc.at/en/blog/2023/08/22/cjeu-special-vat-scheme-applicable-to-travel-agents-also-in-case-of-mere-resale-of-accommodation-services) (headline; URL now redirects)
- Whether a seller acts in its own name (Art. 306(1)(a)) or as an intermediary (306(1)(b)) is the decisive question, litigated in UK cases on online hotel resale — [UK First-tier Tribunal decision](https://financeandtax.decisions.tribunals.gov.uk/judgmentfiles/j12765/TC%2008852.pdf), [Secret Hotels2 v HMRC, EWCA 2012](https://caselaw.nationalarchives.gov.uk/ewca/civ/2012/1571) **[OLD; UK]**
- **Non-EU operators, Germany:**
  - The German Finance Ministry (BMF) held in a letter of 29 Jan 2021 that § 25 UStG (German TOMS) does not apply to businesses established in third countries without an EU fixed establishment. It tolerated continued use for 2021 and 2022 — [FGS](https://fgs.de/news-and-insights/blog/detail/federal-ministry-of-finance-locks-third-country-taxable-persons-out-of-applying-the-toms-regime-special-regulation-for-travel-agents-in-acc-with-art-306-eu-vat-directive), [Mazars](https://eng.mazars.de/Home/Services/Tax/Global-Indirect-Tax-Value-Added-Tax-and-Customs/Latest-VAT-News/BMF-circular-letter-dated-March-29-2021), [PwC DE](https://blogs.pwc.de/en/steuern-und-recht/article/254459/bmf-besteuerung-von-reiseleistungen-von-drittlandsunternehmen/) **[OLD]**
  - The Lower Saxony Fiscal Court (FG Niedersachsen, 17 Nov 2022, 5 V 96/22; interim relief) disagreed and held § 25 is not limited to EU-established businesses — [ETOA copy of decision](https://www.etoa.org/wp-content/uploads/2023/01/230120-Google-Translate-FG-NDS-Beschl-2022-11-17-5-V-96-22-USt-NonEU-RV-in-TOMS.pdf) **[OLD]**
- The EU VAT Committee's Working Paper 814 (online supplies by travel agents to final consumers) flags unresolved scope questions where the travel agent is not established in the EU — [VAT Committee WP 814](https://www.vatupdate.com/wp-content/uploads/2022/05/WP-814-Online-supplies-made-by-a-travel-agent-to-final-consumers-1.pdf) **[2022]**
- The Commission once proposed that non-EU travel agents register and pay VAT in the Member State they supply to. Adoption is not confirmed — [COM(2002)64](https://www.parlementairemonitor.nl/9353000/1/j4nvke1fm2yd1u0_j9vvij5epmj1ey0/vkcweed68lsr/v=s7z/f=/com(2002)64_en.pdf) **[OLD, historical]**
- UK: TOMS continues domestically after Brexit, and the ICAEW discussed UK VAT simplification in 2026 — [ICAEW Taxline 2026](https://www.icaew.com/technical/tax/tax-faculty/taxline/articles/2026/simplifying-uk-vat) (content not fetched)

### Inferences
- **EU-established operator** (for example, a Spanish or Portuguese SL/Lda, or any EU company) selling in its own name:
  - Apply TOMS in its Member State of establishment.
  - VAT is due on the margin (sell price minus the cost of the bought-in accommodation).
  - No VAT registration is needed in each hotel country.
  - Input VAT on bought-in travel costs is not recoverable.
  - Invoices likely need margin-scheme wording and no separately shown VAT on the margin. This is an assumption from general TOMS practice, not sourced here; confirm with an adviser.
  - Bedbank B2B supplies may themselves be under TOMS, which the margin calculation must respect.
- **Georgia-established operator with no EU fixed establishment:**
  - Article 307's link to "the Member State of establishment" fails.
  - Where Member States follow Germany's 2021 view, the operator falls back to normal rules. Accommodation would then likely be taxable where the hotel is (Spain or Portugal), at full VAT on the gross sale, requiring local VAT registration and possibly a fiscal representative.
  - That would cost far more than margin VAT. This is a strong reason to set up an EU entity (likely Spain or Portugal, given the inventory). The same entity also eases PSP onboarding (§2) and SCA (§4).
- **UK customers or a UK entity:** UK TOMS is a separate regime post-Brexit; out of scope for EUR/EU launch.
- **Tourist taxes:** city or tourist taxes are typically paid by the guest at the hotel and appear in Hotelbeds rate data. Display them as "payable at property" and do not collect them, unless the contract says rates include them.

#### Recommended tasks
- **[Biz/Tax]** Before incorporating, get written advice from an EU VAT adviser (Spain or Portugal) on:
  - Entity location.
  - TOMS application to accommodation-only resale (post C-108/22).
  - The treatment of a Georgian parent.
  - Invoicing format.
  - Whether Hotelbeds' invoices to us are themselves under the margin scheme.
- **[Eng]** Store per booking the net cost, sell price, margin, scheme flag and destination country. Generate invoices or receipts with sequential numbering and the margin-scheme wording.

### Gaps
- Spanish (AEAT) and Portuguese (AT) treatment of non-EU travel agents under TOMS was not found. Current German status after 2022 (BMF or BFH outcome) was not found.
- Articles 308–310 (margin definition; trips outside the EU) and Art. 47 (place of supply for immovable property) were not fetched.
- VAT rates on accommodation in Spain and Portugal were not sourced.
- CJEU C-108/22 full judgment not fetched (headline only).
- Georgian domestic tax treatment of foreign-source travel margin was not researched.
- Tourist tax rules (for example, Catalonia and Lisbon) were not sourced.

## 8. Fraud: card testing, travel fraud patterns, Radar / RevenueProtect, velocity limits

### Takeaway
Card testing is the first attack a new checkout will see. The PSP's hosted form (Payment Element or Checkout) gives built-in CAPTCHA, rate limiting and model-based blocking, and our side should add login or session gating, rate limits and Radar velocity rules. Travel-specific fraud patterns and Adyen RevenueProtect were not covered by primary sources in this pass.

### Cited Findings
- **How card testing works (Stripe):**
  - Fraudsters validate stolen cards by card setup (preferred, because it doesn't show on statements) or by small payments.
  - Symptoms: spikes in failed or blocked payments, spikes in 402 / `generic_decline`, and low-value payments with nonsense names.
  - Consequences: disputes, higher decline rates for legitimate traffic, fees, and possible card-monitoring programs.
  - Source: [Stripe card testing](https://docs.stripe.com/disputes/prevention/card-testing)
- **Stripe mitigations:**
  - The latest Payment Element or Checkout includes rate limiters, AI models and CAPTCHA triggers ("Blocked by Stripe").
  - Send IP, email, name, billing address and advanced fraud signals.
  - Add server-side CAPTCHA, require login or session validation before payment, use CSRF tokens, and set network rate limits (for example, new customers per IP per day).
  - Use Radar custom velocity rules (plan-dependent) and CVC checks.
  - Don't retry cards in bulk.
  - Source: [Stripe card testing](https://docs.stripe.com/disputes/prevention/card-testing)
- Radar is not supported for MB WAY payments — [Stripe MB WAY](https://docs.stripe.com/payments/mb-way)
- Under the PSD3/PSR deal, PSPs lacking adequate fraud prevention become liable for customer losses, and online platforms can bear liability where fraud originated on them — [Mondaq](https://webiis08.mondaq.com/unitedstates/financial-services/1713372/european-council-and-parliament-reach-provisional-agreement-on-eu-payments-package), [TechRadar](https://www.techradar.com/pro/eu-clamps-down-on-online-fraud-and-hidden-fees-affecting-online-payment-platforms) **[2nd-hand; final text pending]**
- 3DS-authenticated transactions shift fraud liability to the issuer; exempted ones don't — [Stripe SCA guide](https://stripe.com/guides/strong-customer-authentication)

### Inferences
- Run 3DS on all first-time cards. Avoid requesting transaction-risk-analysis exemptions at launch: liability stays with us, and a new merchant has no fraud history.
- Watch prepaid hotel bookings for:
  - Last-minute check-in.
  - Guest name different from the cardholder.
  - Mismatch between IP country and card country.
  - Several bookings across cards from one device.
- Add a manual review queue for bookings above an amount threshold, before calling Hotelbeds book. This is cheap because authorisation already holds the funds.

#### Recommended tasks
- **[Eng]** Rate-limit `/api/bookings/prebook` and `/book` per IP and session. Add CAPTCHA (or rely on Stripe's) on checkout. Require an email or OTP-verified session before payment.
- **[Eng]** Pass customer email, name and IP into the PaymentIntent. Enable Radar.
- **[Eng]** Start with these rules:
  - Block when the CVC check fails.
  - Review when lead time is under 24 h and the amount is above €X.
  - Velocity: at most N cards per IP or device per day, and at most N bookings per card per day.
- **[Eng]** Build a manual-review state between AUTHORISED and SUPPLIER_PENDING (§5).
- **[Ops]** Run a dispute playbook. Evidence to keep: booking confirmation, Hotelbeds reference, cancellation policy accepted (with timestamp and IP), 3DS result, and hotel stay confirmation.

### Gaps
- No primary source fetched on travel-specific fraud patterns (Phocuswire or Skift style), Adyen RevenueProtect, or scheme dispute timelines and reason codes for lodging. Card chargeback windows were not sourced here; only Bizum's 120-day claim window is.
- No data on typical fraud or chargeback rates for small OTAs.
