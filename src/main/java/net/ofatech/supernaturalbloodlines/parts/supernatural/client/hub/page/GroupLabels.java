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

    /** The sprite of the viewer's group type, for the Group page's tab and anywhere else the paw stood for "your group". */
    public static String iconName() {
        GroupView v = view();
        return net.ofatech.supernaturalbloodlines.core.group.GroupIcons.name(v.type(), v.hasGroup());
    }

    /** True for a clan, a coven or a brotherhood (and for a player whose own kind leads one): the page leaves the wolf parts out. */
    public static boolean active(GroupView v) {
        return v.type() != GroupType.PACK;
    }

    /** The viewer is in no group and could not found one: the page says "Groups" and what a leader is, with no rank and no type. */
    public static boolean neutral(GroupView v) {
        return active(v) && !v.hasGroup() && v.viewerRank() != GroupView.LEADER;
    }

    /** The type's word in a lang key, or {@code none} for a viewer with no group who could found none. */
    public static String slot(GroupView v) {
        return neutral(v) ? "none" : v.type().id();
    }

    /** The command root the shared buttons type. Always {@code group}: {@code /pack} does not exist in a world without Werewolves, and a pack's shared actions are the group tree's too. */
    public static String root(GroupView v) {
        return "group";
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
        return active(v) ? Component.translatable(HUB + "group." + name + "." + slot(v), args) : Component.translatable(packKey, args);
    }

    /** {@link #t(GroupView, String, String, Object...)} for a group seen from outside (a profile): its own type, not the viewer's. */
    public static Component t(GroupType type, String packKey, String name, Object... args) {
        return type == GroupType.PACK ? Component.translatable(packKey, args) : Component.translatable(HUB + "group." + name + "." + type.id(), args);
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

    static Component myRank(GroupView v, java.util.UUID viewer) {
        if (v.type() == GroupType.BROTHERHOOD && v.viewerRank() != GroupView.LEADER && v.members().stream()
                .anyMatch(row -> row.rank().equals("potential") && row.id().filter(viewer::equals).isPresent())) {
            return memberRank(v, "potential");
        }
        return myRank(v);
    }

    /** The one line of the ledger when nobody is there yet. */
    public static Component alone(GroupView v) {
        return Component.translatable(HUB + "group.alone." + slot(v));
    }

    /** What the empty page says: how to found a group of the viewer's kind, or that they wait to be invited. */
    public static String emptyName(GroupView v) {
        return "group_" + slot(v);
    }

    /** Whether the page offers "Leave": somebody in the group who is not its leader (the leader is the first row); the server refuses a leader's leave, so the button is not shown. */
    public static boolean mayLeave(List<HubSocial.Member> members, String viewerName) {
        return !members.isEmpty() && !members.get(0).name().equals(viewerName);
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
