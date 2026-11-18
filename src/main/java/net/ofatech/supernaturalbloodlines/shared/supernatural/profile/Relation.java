package net.ofatech.supernaturalbloodlines.shared.supernatural.profile;

/** What the observer is to the subject, as the server's own indexes say (never as the client says). The first that applies, in this order. */
public enum Relation {
    SELF, SAME_PACK, ALLY_LEADER, DIRECT_SIRE, DIRECT_SIRED, OTHER_IN_LINE, NPC_CREATURE, NPC_ANCESTOR, STRANGER, STRANGER_OFFLINE,
    /** An OP in the Admin page's own view; reached only through the Admin permission check. */
    OP_VIEW
}

