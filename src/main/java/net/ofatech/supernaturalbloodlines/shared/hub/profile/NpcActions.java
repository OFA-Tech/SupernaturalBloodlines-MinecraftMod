package net.ofatech.supernaturalbloodlines.shared.hub.profile;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/**
 * What the hub offers for one NPC member of the pack (R148, R149): Follow, Guard, Rest, Hunt, Sort and Release, each as the command row it runs (P21 to P24)
 * and, when it is off, why. The server checks every one again; this only decides the buttons. A Tensura subordinate keeps Tensura's own orders, so every
 * button of it is off.
 */
public final class NpcActions {
    /** @param why empty when on offer, else {@code tensura}, {@code owner} or {@code loaded} (a lang suffix); {@code confirm} whether it asks first */
    public record Act(String id, String rowId, List<String> args, String why, boolean confirm, boolean current) {
        public boolean enabled() {
            return why.isEmpty();
        }
    }

    private NpcActions() {
    }

    /** Why the orders are off for this member, or empty. The reason that cannot change comes first. */
    public static String refusal(HubSocial.Npc n) {
        if (!n.ours()) {
            return "tensura";
        }
        if (!n.canOrder()) {
            return "owner";
        }
        return n.loaded() ? "" : "loaded";
    }

    public static List<Act> of(HubSocial.Npc n) {
        String why = refusal(n);
        String token = String.valueOf(n.token());
        List<Act> out = new ArrayList<>();
        out.add(new Act("follow", "P21", List.of(token, "follow"), why, false, n.order().equals("follow")));
        out.add(new Act("guard", "P21", List.of(token, "guard"), why, false, n.order().equals("guard")));
        out.add(new Act("rest", "P21", List.of(token, "rest"), why, false, n.order().equals("rest")));
        out.add(new Act("hunt", "P22", List.of(token), why, false, n.hunt()));
        out.add(new Act("sort", "P23", List.of(token), why, false, n.sort()));
        // releasing works on a wolf that is not loaded too (the ledger entry goes), so only who may and what kind decide it
        out.add(new Act("release", "P24", List.of(token), why.equals("loaded") ? "" : why, true, false));
        return out;
    }
}
