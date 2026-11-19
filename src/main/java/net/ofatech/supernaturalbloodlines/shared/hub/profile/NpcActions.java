package net.ofatech.supernaturalbloodlines.shared.hub.profile;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/**
 * What the hub offers for one NPC member of the pack (R148, R149): Follow, Guard, Rest, Hunt, Sort and Release, each as the command row it runs (P21 to P24)
 * and, when it is off, why. The server checks every one again; this only decides the buttons. A Tensura subordinate keeps Tensura's own orders, so every
 * button of it is off. The owner and the pack's leader give orders; only the owner releases.
 */
public final class NpcActions {
    /** @param why empty when on offer, else {@code tensura}, {@code owner}, {@code owner_release}, {@code loaded} or {@code follow} (a lang suffix); {@code confirm} whether it asks first */
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
        // a Brother hunts and sorts only from its post: while it follows, turning them on does nothing, so the button says to give another order first
        String post = why.isEmpty() && n.order().equals("follow") ? "follow" : why;
        List<Act> out = new ArrayList<>();
        out.add(new Act("follow", "P21", List.of(token, "follow"), why, false, n.order().equals("follow")));
        out.add(new Act("guard", "P21", List.of(token, "guard"), why, false, n.order().equals("guard")));
        out.add(new Act("rest", "P21", List.of(token, "rest"), why, false, n.order().equals("rest")));
        out.add(new Act("hunt", "P22", List.of(token), n.hunt() ? why : post, false, n.hunt()));
        out.add(new Act("sort", "P23", List.of(token), n.sort() ? why : post, false, n.sort()));
        // releasing works on a wolf that is not loaded too (the ledger remembers it), but only its owner may do it
        String release = !n.ours() ? "tensura" : !n.mine() ? "owner_release" : "";
        out.add(new Act("release", "P24", List.of(token), release, true, false));
        return out;
    }

    /**
     * The same actions for the Blood Moon's floating bar, which keeps the last ones and moves the first ones behind "More" when the bar is narrow: so Hunt, Sort and
     * Release come first and Follow, Guard and Rest last, and they are what stays on screen.
     */
    public static List<Act> forBar(HubSocial.Npc n) {
        List<Act> all = of(n);
        List<Act> out = new ArrayList<>();
        for (String id : List.of("release", "sort", "hunt", "follow", "guard", "rest")) {
            all.stream().filter(a -> a.id().equals(id)).findFirst().ifPresent(out::add);
        }
        return out;
    }
}
