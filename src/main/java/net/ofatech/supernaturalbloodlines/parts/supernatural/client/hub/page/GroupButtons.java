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
    public static final Map<String, String> IDS = Map.of(
            "invite", "stage:pack:invite",
            "accept", "pack:accept",
            "leave", "pack:leave",
            "kick", "pack:kick",
            "ally invite", "stage:pack:ally",
            "ally accept", "P6",
            "ally remove", "pack:ally_end",
            "challenge", "pack:challenge",
            "challenge accept", "pack:duel_accept",
            "challenge decline", "pack:duel_decline");

    /** The lang key of what each control says. */
    public static final Map<String, String> LABELS = Map.of(
            "stage:pack:invite", GroupLabels.HUB + "pack.invite_btn",
            "pack:accept", GroupLabels.HUB + "pack.accept",
            "pack:leave", GroupLabels.HUB + "pack.leave",
            "pack:kick", GroupLabels.HUB + "pack.kick",
            "stage:pack:ally", GroupLabels.HUB + "pack.ally_btn",
            "P6", GroupLabels.HUB + "pack.accept",
            "pack:ally_end", GroupLabels.HUB + "pack.ally_end",
            "pack:challenge", GroupLabels.HUB + "pack.challenge",
            "pack:duel_accept", GroupLabels.HUB + "pack.duel_accept",
            "pack:duel_decline", GroupLabels.HUB + "pack.duel_decline");

    /** Literals the page shows by itself and needs no control for. */
    public static final Set<String> NO_BUTTON = Set.of("info");

    private GroupButtons() {
    }
}
