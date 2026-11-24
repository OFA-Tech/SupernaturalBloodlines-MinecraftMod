package net.ofatech.supernaturalbloodlines.parts.species.group;

import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;

/** The wording of the shared actions: a pack keeps its old keys, the other types say their own word (R248). */
public final class GroupText {
    private static final String PREFIX = "supernaturalbloodlines.group.";
    private static final String PACK_PREFIX = "supernaturalbloodlines.pack.";

    /** Shared base to the key a pack has always used; a base not listed here and not the same word has a {@code group.<base>.pack} key. */
    private static final Map<String, String> PACK_KEY = Map.ofEntries(
            Map.entry("joined_leader", "joined_alpha"), Map.entry("in_group", "has_pack"), Map.entry("target_in_group", "target_has_pack"),
            Map.entry("leader_gone", "alpha_gone"), Map.entry("leader_offline", "alpha_offline"), Map.entry("no_group", "no_pack"),
            Map.entry("leader_cannot_leave", "alpha_cannot_leave"), Map.entry("not_fit", "not_alpha"));
    private static final List<String> OWN_PACK_KEY = List.of("group_off", "full", "wrong_type", "type_changed");

    private static final List<String> SHARED = List.of("invited", "invite", "invite_merge", "accept", "accept_hover", "joined", "joined_leader", "left",
            "kicked", "kicked_ok", "ally_invite", "ally_invited", "ally_made", "ally_ended", "ally_none", "ally_already", "ally_not_leader",
            "not_leader", "not_member", "self", "in_group", "target_in_group", "expired", "leader_gone", "leader_offline", "stale_invite", "refused",
            "no_group", "leader_cannot_leave", "not_fit", "group_off", "full", "wrong_type", "type_changed");

    private GroupText() {
    }

    public static List<String> sharedBases() {
        return SHARED;
    }

    public static String key(GroupType type, String base) {
        if (type == GroupType.PACK && !OWN_PACK_KEY.contains(base)) {
            return PACK_PREFIX + PACK_KEY.getOrDefault(base, base);
        }
        return PREFIX + base + "." + type.id();
    }

    public static Component of(GroupType type, String base, Object... args) {
        return Component.translatable(key(type, base), args);
    }

    public static Component error(GroupType type, String base, Object... args) {
        return of(type, base, args);
    }

    /** The noun in the player's language: pack, clan, coven, brotherhood. */
    public static Component noun(GroupType type) {
        return Component.translatable(PREFIX + "noun." + type.id());
    }
}
