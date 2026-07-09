package com.miirphys.bodiala.provider.hotelbeds;

/** Resolves a Hotelbeds facility's human name from its {@code (facilityGroupCode, facilityCode)}. */
@FunctionalInterface
public interface FacilityNameResolver {

    /** The name for a facility, or {@code null} if the pair is unknown. */
    String nameFor(Integer facilityGroupCode, Integer facilityCode);
}
