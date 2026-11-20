package net.ofatech.supernaturalbloodlines.core.boss;

/** What the director knows about a boss before it asks to spawn it. */
public record BossFacts(boolean enabled, boolean modsPresent, BossState state, boolean cooling) {
}