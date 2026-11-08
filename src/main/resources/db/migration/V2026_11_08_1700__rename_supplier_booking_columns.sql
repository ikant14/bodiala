-- OPS-1 / KAN-84: drop the RezLive-era names from hotel_booking. The columns hold the supplier's own
-- booking reference (Hotelbeds "reference") and a legacy second identifier that Hotelbeds leaves null.
-- A rename keeps the data, the NOT NULL and the unique constraint (Hibernate's ddl-auto could only
-- have added new columns next to the old ones).

ALTER TABLE hotel_booking RENAME COLUMN rez_booking_id TO supplier_booking_ref;
ALTER TABLE hotel_booking RENAME COLUMN rez_booking_code TO supplier_booking_code;

-- Renaming the unique constraint renames its index too.
ALTER TABLE hotel_booking RENAME CONSTRAINT idx_booking_rez_id TO idx_booking_supplier_ref;
