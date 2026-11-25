package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import net.ofatech.supernaturalbloodlines.core.group.GroupType;

/** The one rule behind every brotherhood perk: the group is a brotherhood, perks are on, and the member fits. */
public final class ScopeRules {
    private ScopeRules() {
    }

    public static boolean appliesTo(GroupType type, boolean perksOn, boolean fits) {
        return type == GroupType.BROTHERHOOD && perksOn && fits;
    }
}