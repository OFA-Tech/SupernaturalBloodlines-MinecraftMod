package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.Map;
import java.util.Set;

/**
 * Every literal of the {@code /group} tree and the control of the hub that sends it. A command with no control is a bug the UI-first rule
 * catches ({@code GroupUiCoverageTest}, which also checks that each id is really drawn by a page); the plans for the other types add their
 * literals here. The pages use the same ids whatever the group's type is.
 */
public final class GroupButtons {
    /** A literal path of the tree to the id of the control that sends it. A challenge is the pack's duel (the other types answer {@code wrong_type} until their own plans). */
    public static final Map<String, String> IDS = Map.ofEntries(
            Map.entry("invite", "stage:pack:invite"),
            Map.entry("accept", "pack:accept"),
            Map.entry("leave", "pack:leave"),
            Map.entry("kick", "pack:kick"),
            Map.entry("ally invite", "stage:pack:ally"),
            Map.entry("ally accept", "P6"),
            Map.entry("ally remove", "pack:ally_end"),
            Map.entry("challenge", "pack:challenge"),
            Map.entry("challenge accept", "pack:duel_accept"),
            Map.entry("challenge decline", "pack:duel_decline"),
            Map.entry("link open", "coven:link_open"),
            Map.entry("link close", "coven:link_close"),
            Map.entry("link join", "coven:link_join"),
            Map.entry("link leave", "coven:link_leave"),
            Map.entry("link consent", "coven:consent"));

    /** The lang key of what each control says. */
    public static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("stage:pack:invite", GroupLabels.HUB + "pack.invite_btn"),
            Map.entry("pack:accept", GroupLabels.HUB + "pack.accept"),
            Map.entry("pack:leave", GroupLabels.HUB + "pack.leave"),
            Map.entry("pack:kick", GroupLabels.HUB + "pack.kick"),
            Map.entry("stage:pack:ally", GroupLabels.HUB + "pack.ally_btn"),
            Map.entry("P6", GroupLabels.HUB + "pack.accept"),
            Map.entry("pack:ally_end", GroupLabels.HUB + "pack.ally_end"),
            Map.entry("pack:challenge", GroupLabels.HUB + "pack.challenge"),
            Map.entry("pack:duel_accept", GroupLabels.HUB + "pack.duel_accept"),
            Map.entry("pack:duel_decline", GroupLabels.HUB + "pack.duel_decline"),
            Map.entry("coven:link_open", GroupLabels.HUB + "coven.link_open"),
            Map.entry("coven:link_close", GroupLabels.HUB + "coven.link_close"),
            Map.entry("coven:link_join", GroupLabels.HUB + "coven.link_join"),
            Map.entry("coven:link_leave", GroupLabels.HUB + "coven.link_leave"),
            Map.entry("coven:consent", GroupLabels.HUB + "coven.consent"));

    /** The coven controls whose card is drawn by plan 20b Task 12; until then they are registered and not yet on a page. */
    public static final Set<String> NOT_DRAWN_YET = Set.of("coven:link_open", "coven:link_close", "coven:link_join", "coven:link_leave", "coven:consent");

    /** Literals the page shows by itself and needs no control for. */
    public static final Set<String> NO_BUTTON = Set.of("info");

    private GroupButtons() {
    }
}
