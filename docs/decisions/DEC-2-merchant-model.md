# DEC-2 · Contracting and merchant model

- **Status:** Accepted as the working assumption for phase 1 (local build). Revised the same day to an agent-assisted flow. It still needs confirming before launch; see "Still to confirm" below.
- **Date:** 2026-11-08
- **Jira:** [KAN-24](https://iraklikantar.atlassian.net/browse/KAN-24). The flow is built in BOOK-10.
- **Options and evidence:** `docs/backlog.md`, section "DEC-2 · Choose the contracting and merchant model"

## Decision

**We are the merchant of record and sell prepaid `AT_WEB` rates in our own name (option A).** At launch, the sale is agent-assisted:

1. **Online, the customer only requests a booking.** They choose a room and see the price, then submit their details. Nothing is charged, and nothing is booked at Hotelbeds yet.
2. **We save the quote.** It holds the rate, the price, the cancellation policy and the time it was shown. That is the price "frozen" on our side.
3. **An agent contacts the customer** and collects payment by payment link or bank transfer.
4. **The agent re-checks the rate**, with a fresh availability search and CheckRate, then **books it at Hotelbeds**. Hotelbeds invoices us, and we pay them under our account terms.

**The frozen price is our promise, not Hotelbeds'.** Hotelbeds has no call that holds a price or room, and how long a `rateKey` stays valid is not published. Between the request and the agent's booking, the price can change or the room can sell out:

- **Price went down:** we honour the lower price.
- **Price went up, within the tolerance set in DEC-5:** we absorb the difference.
- **Price went up by more than that:** the agent asks the customer before booking.
- **Room gone:** the agent offers alternatives, or refunds in full if payment was already taken.

## Rejected for now: booking as the hold

The other option was to book at Hotelbeds the moment the customer clicks, using only free-cancellation rates, and cancel automatically if they don't pay. It freezes the price and the room for real. We rejected it for launch as too risky:

- every unpaid request becomes a live booking;
- a missed cancellation deadline costs real money;
- it depends on Hotelbeds credit terms;
- a high cancellation rate may bother Hotelbeds.

Revisit it once the request flow is running and the volumes are known.

## What this means for the build

- **Only `AT_WEB` rates are offered.** `AT_HOTEL` rates are filtered out with `filter.paymentType` (HB-5).
- **No online payment at launch.** The online card checkout tasks (PAY-2 to PAY-5 and the other PAY tasks built on them) are not needed for launch.
- **No card data, ever.** The agent never takes card numbers by phone, email or chat; payment is by payment link or bank transfer only. This keeps us out of PCI scope for phone payments.
- **The quote is stored server-side**, and the agent re-checks it before booking (BOOK-4, BOOK-5, BOOK-10).
- **The button says "Request booking", not "Book".** The page says plainly that nothing is charged yet and that an agent will confirm the availability and price (CHK-2).
- **The agent screen sits behind a login** (SEC-1). Booking at Hotelbeds is only possible from that screen, and only after payment has been recorded.

## Risks we accept

- **Price movement and sell-outs** between the request and the booking. They cost margin up to the DEC-5 tolerance, or a call to the customer.
- **A slower service**, since the customer waits for an agent. Requests need a response-time target.
- **We are still the seller.** Refunds and disputes on payments we collect are ours to handle.

## Still to confirm (phase 2)

- **Counsel:** that the request-then-pay flow, its wording and the T&Cs are sound, and that the contract forms when payment is taken (LEGAL-2).
- **Hotelbeds:** that our account gets `AT_WEB` net rates; how we pay them (credit line, deposit or prepayment); and whether this agent flow is acceptable for certification (DEC-3, HB-15, HB-16).
- **Tax adviser:** that the TOMS margin scheme applies to this model in the chosen country (DEC-1).
- **Payment links:** these need a PSP account, so PAY-1 still applies, in a much lighter form.

If any of these comes back negative, reopen KAN-24 and this record.
