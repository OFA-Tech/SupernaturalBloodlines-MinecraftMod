package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.GroupView;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.GroupClient;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.PackRing;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/**
 * What the Group page calls things. A pack keeps every word and every key it had; a clan, a coven and a brotherhood say their own
 * ({@code hub.group.<name>.<type>}), and the wolf-only parts of the page are left out for them. The server's view decides; the page only reads it.
 */
public final class GroupLabels {
    public static final String HUB = PageKit.HUB;
    private static final String TAB_PACK = "supernaturalbloodlines.hub.page.pack";

    private GroupLabels() {
    }

    public static GroupView view() {
        return GroupClient.view();
    }

    /** True for a clan, a coven or a brotherhood (and for a player whose own kind leads one): the page leaves the wolf parts out. */
    public static boolean active(GroupView v) {
        return v.type() != GroupType.PACK;
    }

    /** The command root the page types: the pack's own tree for a pack (it needs Werewolves), the shared one for the rest. */
    public static String root(GroupView v) {
        return active(v) ? "group" : "pack";
    }

    public static Component noun(GroupType type) {
        return Component.translatable(HUB + "group.noun." + type.id());
    }

    /** The key of the hub tab: "Pack" for a pack, the noun for a group the player is in or could found, "Groups" for anyone else. */
    public static String tabKey(GroupView v) {
        if (v.type() == GroupType.PACK) {
            return TAB_PACK;
        }
        return v.hasGroup() || v.viewerRank() == GroupView.LEADER ? HUB + "group.noun." + v.type().id() : HUB + "group.noun.none";
    }

    /** {@code packKey} for a pack (exactly as before), {@code hub.group.<name>.<type>} for the other types. */
    public static Component t(GroupView v, String packKey, String name, Object... args) {
        return active(v) ? Component.translatable(HUB + "group." + name + "." + v.type().id(), args) : Component.translatable(packKey, args);
    }

    /** The members the way the pack page keeps them, so the ledger, the ring and the detail pane draw them unchanged. */
    public static List<HubSocial.Member> members(GroupView v) {
        return v.members().stream().map(r -> new HubSocial.Member(r.name(), r.rank(), r.online(), r.id())).toList();
    }

    public static Component memberRank(GroupView v, String base) {
        return Component.translatable(HUB + "group.member." + v.type().id() + "." + base);
    }

    /** The viewer's own place in the group, in the group's words. */
    public static Component myRank(GroupView v) {
        String base = switch (v.viewerRank()) {
            case GroupView.LEADER -> "leader";
            case GroupView.DAY_WALKER -> "day_walker";
            case GroupView.ELDER -> "elder";
            default -> "member";
        };
        return memberRank(v, base);
    }

    /** The one line of the ledger when nobody is there yet. */
    public static Component alone(GroupView v) {
        return Component.translatable(HUB + "group.alone." + (v.viewerRank() == GroupView.LEADER ? v.type().id() : "none"));
    }

    /** What the empty page says: how to found a group of the viewer's kind, or that they wait to be invited. */
    public static String emptyName(GroupView v) {
        return v.viewerRank() == GroupView.LEADER ? "group_" + v.type().id() : "group_none";
    }

    /** The ring's colours for a group (placeholder art until the ring sprites of 20b to 20d): leader red, the next rank gold, members blue or brown. */
    public static PackRing.Role role(GroupType type, String rank) {
        return switch (rank) {
            case "leader" -> PackRing.Role.ALPHA;
            case "day_walker", "elder" -> PackRing.Role.BETA;
            case "ally" -> PackRing.Role.SUBORDINATE;
            default -> type == GroupType.BROTHERHOOD ? PackRing.Role.BROTHER : PackRing.Role.PACKMATE;
        };
    }
}
