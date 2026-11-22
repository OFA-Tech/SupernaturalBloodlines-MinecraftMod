package net.ofatech.supernaturalbloodlines.core.blood;

import java.util.UUID;

/** Whose blood a bottle holds. Server-side only: a client is never told any of it except a label ({@code BloodStamp}). */
public record BloodSource(UUID id, String name, boolean npc, BloodKind kind) {
}
