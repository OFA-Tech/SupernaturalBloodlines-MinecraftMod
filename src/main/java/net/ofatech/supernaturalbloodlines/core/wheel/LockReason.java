package net.ofatech.supernaturalbloodlines.core.wheel;

/** Why a known power cannot be used now. Append-only: the wire sends the ordinal; lang supernaturalbloodlines.wheel.lock.<lowercase name>. */
public enum LockReason {
    NONE, NOT_UNLOCKED, CANNOT_ACT, NEEDS_SLOT, REFUSED_NOW
}
