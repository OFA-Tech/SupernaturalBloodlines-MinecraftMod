package net.ofatech.supernaturalbloodlines.shared.hub.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Which actions a person's Profile offers the viewer, by their relationship (the IA doc, 3.6). Pure: the client draws what this says and
 * every action is a command the server checks again; a reason the client cannot know (a cooldown, a distance) is the command's own reply.
 * An action that can never apply to this viewer is absent; one that is only unavailable now stays, disabled, with its reason.
 */
public final class ProfileActions {
    /** The role of a person in the viewer's pack, from the hub snapshot's rank words. */
    public enum Role { NONE, LEADER, COALPHA, CHALLENGER, BETA, PACKMATE, EMISSARY, ALLY }

    /** @param leader the viewer leads their pack; {@code coalpha} is a second Alpha of it; {@code trueAlpha} cannot give up the spark */
    public record Viewer(boolean inPack, boolean leader, boolean coalpha, boolean alphaRank, boolean trueAlpha, boolean werewolf, boolean caller) {
        public static final Viewer NONE = new Viewer(false, false, false, false, false, false, false);
    }

    /** @param self the viewer's own profile; {@code sired} one of the viewer's sired; {@code sire} the viewer's sire */
    public record Subject(UUID id, String name, boolean self, Role role, boolean sired, boolean sire) {
    }

    public enum Kind { COMMAND, OPEN_RITES, OPEN_ADMIN }

    /**
     * @param id        the lang key suffix of its label and reasons ({@code supernaturalbloodlines.hub.profile.action.<id>})
     * @param rowId     the {@code AdminCommands} row the server runs; empty for a link
     * @param withTarget whether the row takes the subject's UUID
     * @param danger    asks first with the safe button focused; {@code confirm} names the lang key of its body (empty: no dialog)
     */
    public record Action(String id, Kind kind, String rowId, boolean withTarget, boolean primary, boolean enabled, @Nullable String disabled,
                         boolean confirm) {
        public List<String> args(UUID subject) {
            return withTarget ? List.of(subject.toString()) : List.of();
        }
    }

    private ProfileActions() {}

    public static Role roleOf(String base) {
        return switch (base) {
            case "alpha" -> Role.LEADER;
            case "coalpha" -> Role.COALPHA;
            case "coalpha_challenger" -> Role.CHALLENGER;
            case "beta" -> Role.BETA;
            case "packmate" -> Role.PACKMATE;
            case "emissary" -> Role.EMISSARY;
            case "ally" -> Role.ALLY;
            default -> Role.NONE;
        };
    }

    public static List<Action> of(Viewer v, Subject s, boolean op) {
        List<Action> out = new ArrayList<>();
        if (s.self()) {
            if (v.inPack()) {
                out.add(new Action("leave", Kind.COMMAND, "P3", false, false, !v.leader(), v.leader() ? "leader" : null, true));
            }
            if (v.werewolf()) {
                out.add(new Action("anchor_clear", Kind.COMMAND, "P15", false, false, true, null, false));
            }
            return out;
        }
        boolean inMyPack = v.inPack() && s.role() != Role.NONE && s.role() != Role.ALLY;
        if (v.coalpha() && s.role() == Role.LEADER) {
            out.add(new Action("challenge", Kind.COMMAND, "P8", true, true, true, null, true));
        }
        if (v.leader() && s.role() == Role.CHALLENGER) {
            out.add(new Action("accept_duel", Kind.COMMAND, "P9", false, true, true, null, false));
            out.add(new Action("decline_duel", Kind.COMMAND, "P10", false, false, true, null, false));
        }
        if (v.leader() && !inMyPack && s.role() == Role.NONE) {
            out.add(new Action("invite", Kind.COMMAND, "P1", true, true, true, null, false));
        }
        if (v.leader() && (s.role() == Role.BETA || s.role() == Role.PACKMATE)) {
            out.add(new Action("emissary", Kind.COMMAND, "P13", true, false, true, null, false));
        }
        if (v.leader() && s.role() == Role.EMISSARY) {
            out.add(new Action("emissary_clear", Kind.COMMAND, "P13", false, false, true, null, false));
        }
        if (v.alphaRank() && inMyPack) {
            out.add(new Action("heal", Kind.COMMAND, "P11", true, false, !v.trueAlpha(), v.trueAlpha() ? "true_alpha" : null, true));
        }
        if (v.leader() && (s.role() == Role.BETA || s.role() == Role.PACKMATE || s.role() == Role.COALPHA)) {
            out.add(new Action("kick", Kind.COMMAND, "P4", true, false, true, null, true));
        }
        if (v.leader() && s.role() == Role.ALLY) {
            out.add(new Action("ally_remove", Kind.COMMAND, "P7", true, false, true, null, true));
        }
        if (v.werewolf()) {
            out.add(new Action("anchor", Kind.COMMAND, "P15", true, false, true, null, false));
        }
        if (s.sired() && v.caller()) {
            out.add(new Action("call", Kind.COMMAND, "B3", false, false, true, null, false));
        }
        if (s.sire()) {
            out.add(new Action("severance", Kind.OPEN_RITES, "", false, false, true, null, false));
        }
        if (op) {
            out.add(new Action("open_admin", Kind.OPEN_ADMIN, "", true, false, true, null, false));
        }
        // at most one primary: the first stays
        boolean seen = false;
        for (int i = 0; i < out.size(); i++) {
            Action a = out.get(i);
            if (a.primary() && seen) {
                out.set(i, new Action(a.id(), a.kind(), a.rowId(), a.withTarget(), false, a.enabled(), a.disabled(), a.confirm()));
            }
            seen |= a.primary();
        }
        return out;
    }
}
