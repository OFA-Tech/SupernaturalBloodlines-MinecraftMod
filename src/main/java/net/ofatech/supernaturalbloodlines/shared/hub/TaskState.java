package net.ofatech.supernaturalbloodlines.shared.hub;

/** A task as the hub shows it: done and claimable, not done, claimed, frozen (a Ghoul), or unavailable (a rite task while the mod cannot see rites finish). */
public enum TaskState { CLAIMABLE, OPEN, CLAIMED, FROZEN, UNAVAILABLE }
