package net.ofatech.supernaturalbloodlines.shared.hub.profile;

import static net.ofatech.supernaturalbloodlines.shared.hub.profile.ProfileActions.Role.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminCommands;
import net.ofatech.supernaturalbloodlines.shared.hub.profile.ProfileActions.Action;
import net.ofatech.supernaturalbloodlines.shared.hub.profile.ProfileActions.Role;
import net.ofatech.supernaturalbloodlines.shared.hub.profile.ProfileActions.Subject;
import net.ofatech.supernaturalbloodlines.shared.hub.profile.ProfileActions.Viewer;
import org.junit.jupiter.api.Test;

class ProfileActionsTest {
    private static final UUID ID = UUID.randomUUID();
    private static final Viewer AS_LEADER = new Viewer(true, true, false, true, false, true);
    private static final Viewer AS_COALPHA = new Viewer(true, false, true, true, false, true);
    private static final Viewer AS_BETA = new Viewer(true, false, false, false, false, true);
    private static final Viewer AS_LONER = new Viewer(false, false, false, false, false, false);

    private static Subject other(Role role) {
        return new Subject(ID, "Hayley", false, role, false, false);
    }

    private static List<String> ids(Viewer v, Subject s) {
        return ProfileActions.of(v, s, false).stream().map(Action::id).toList();
    }

    @Test
    void aSecondAlphaMayChallengeOnlyTheLeader() {
        assertEquals(List.of("challenge", "heal", "anchor"), ids(AS_COALPHA, other(LEADER)));
        assertFalse(ids(AS_COALPHA, other(BETA)).contains("challenge"));
        assertFalse(ids(AS_BETA, other(LEADER)).contains("challenge"), "a Beta cannot");
        assertFalse(ids(AS_LEADER, other(COALPHA)).contains("challenge"), "the leader is the one challenged");
        Action challenge = ProfileActions.of(AS_COALPHA, other(LEADER), false).get(0);
        assertTrue(challenge.confirm() && challenge.primary() && challenge.withTarget());
        assertEquals("P8", challenge.rowId());
    }

    @Test
    void theLeaderAnswersAChallengerWithAcceptAndDecline() {
        List<Action> a = ProfileActions.of(AS_LEADER, other(CHALLENGER), false);
        assertEquals(List.of("accept_duel", "decline_duel", "heal", "anchor"), a.stream().map(Action::id).toList());
        assertTrue(a.get(0).primary() && !a.get(1).primary(), "Decline is never the primary one");
        assertFalse(a.get(0).confirm(), "Decline is the safe one and neither asks");
    }

    @Test
    void theLeaderManagesMembers() {
        assertEquals(List.of("emissary", "heal", "kick", "anchor"), ids(AS_LEADER, other(BETA)));
        assertEquals(List.of("emissary", "heal", "kick", "anchor"), ids(AS_LEADER, other(PACKMATE)));
        assertEquals(List.of("emissary_clear", "heal", "anchor"), ids(AS_LEADER, other(EMISSARY)), "an Emissary is not kicked from here");
        assertEquals(List.of("heal", "kick", "anchor"), ids(AS_LEADER, other(COALPHA)));
        assertEquals(List.of("ally_remove", "anchor"), ids(AS_LEADER, other(ALLY)));
        assertEquals(List.of("invite", "anchor"), ids(AS_LEADER, other(NONE)), "someone in no pack can be invited");
    }

    @Test
    void aBetaCanDoNoneOfTheLeadersThings() {
        assertEquals(List.of("anchor"), ids(AS_BETA, other(BETA)));
        assertEquals(List.of("anchor"), ids(AS_BETA, other(NONE)));
        assertEquals(List.of(), ids(AS_LONER, other(NONE)));
    }

    @Test
    void healIsAnyAlphasAndNotATrueAlphas() {
        assertTrue(ProfileActions.of(AS_COALPHA, other(BETA), false).stream().anyMatch(a -> a.id().equals("heal") && a.enabled()));
        Viewer trueAlpha = new Viewer(true, true, false, true, true, true);
        Action heal = ProfileActions.of(trueAlpha, other(BETA), false).stream().filter(a -> a.id().equals("heal")).findFirst().orElseThrow();
        assertFalse(heal.enabled());
        assertEquals("true_alpha", heal.disabled(), "stays visible with its reason");
        assertTrue(heal.confirm());
        assertFalse(ids(AS_BETA, other(BETA)).contains("heal"), "a Beta has no spark to give");
        assertFalse(ids(AS_LEADER, other(NONE)).contains("heal"), "a stranger is not healed here");
    }

    @Test
    void yourOwnProfileHasLeaveAndTheAnchorClear() {
        Subject me = new Subject(ID, "Me", true, NONE, false, false);
        assertEquals(List.of("leave", "anchor_clear"), ids(AS_BETA, me));
        assertEquals(List.of("anchor_clear"), ids(new Viewer(false, false, false, false, false, true), me));
        // the protection toggle: only while the True Alpha path is open; turning it off asks first
        Viewer open = new Viewer(false, false, false, false, false, false, true, true);
        assertEquals(List.of("protect_off"), ids(open, me));
        assertTrue(ProfileActions.of(open, me, false).get(0).confirm(), "turning it off can end the path");
        Viewer off = new Viewer(false, false, false, false, false, false, true, false);
        assertEquals(List.of("protect_on"), ids(off, me));
        assertFalse(ProfileActions.of(off, me, false).get(0).confirm());
        assertEquals("P16", ProfileActions.of(off, me, false).get(0).rowId());
        assertEquals("P17", ProfileActions.of(open, me, false).get(0).rowId());
        assertEquals(List.of(), ids(new Viewer(false, false, false, false, false, false, false, true), me), "a closed path has nothing to protect");
        assertEquals(List.of(), ids(AS_LONER, me));
        Action leave = ProfileActions.of(AS_LEADER, me, false).get(0);
        assertFalse(leave.enabled());
        assertEquals("leader", leave.disabled(), "the leader can't leave");
        assertTrue(leave.confirm());
    }

    @Test
    void thereIsNoCallActionPerPersonAndTheSiredCanSeverTheirSire() {
        // B3 calls everyone the player sired: it lives on the Bloodline page, never on one person's page
        for (Viewer v : List.of(AS_LEADER, AS_COALPHA, AS_BETA, AS_LONER)) {
            assertFalse(ids(v, new Subject(ID, "Kid", false, NONE, true, false)).contains("call"), "a per-person call action came back");
        }
        List<Action> sire = ProfileActions.of(AS_LONER, new Subject(ID, "Mom", false, NONE, false, true), false);
        assertEquals(ProfileActions.Kind.OPEN_RITES, sire.get(0).kind());
        assertEquals("", sire.get(0).rowId(), "a link sends no command");
    }

    @Test
    void anOperatorGetsOpenInAdminLastAndOthersNever() {
        assertFalse(ids(AS_LONER, other(NONE)).contains("open_admin"));
        List<Action> a = ProfileActions.of(AS_LONER, other(NONE), true);
        assertEquals("open_admin", a.get(a.size() - 1).id());
        assertEquals(ProfileActions.Kind.OPEN_ADMIN, a.get(a.size() - 1).kind());
    }

    @Test
    void atMostOnePrimaryAndEveryCommandRowExistsWithTheRightTarget() {
        for (Viewer v : List.of(AS_LEADER, AS_COALPHA, AS_BETA, AS_LONER, new Viewer(true, true, true, true, false, true))) {
            for (Role r : Role.values()) {
                for (boolean sired : new boolean[] {false, true}) {
                    Subject s = new Subject(ID, "X", false, r, sired, !sired);
                    List<Action> actions = ProfileActions.of(v, s, true);
                    assertTrue(actions.stream().filter(Action::primary).count() <= 1, v + " " + r);
                    for (Action a : actions) {
                        if (a.kind() == ProfileActions.Kind.COMMAND) {
                            AdminCommands.Row row = AdminCommands.byId(a.rowId()).orElseThrow(() -> new AssertionError("no row " + a.rowId()));
                            assertEquals(0, row.level(), a.id() + " is a player command");
                            assertTrue(AdminCommands.command(row, a.args(ID)).isPresent(), a.id() + " builds a command with " + a.args(ID));
                        }
                    }
                }
            }
        }
    }

    @Test
    void theRolesComeFromTheSnapshotRankWords() {
        assertEquals(LEADER, ProfileActions.roleOf("alpha"));
        assertEquals(COALPHA, ProfileActions.roleOf("coalpha"));
        assertEquals(CHALLENGER, ProfileActions.roleOf("coalpha_challenger"));
        assertEquals(PACKMATE, ProfileActions.roleOf("packmate"));
        assertEquals(NONE, ProfileActions.roleOf("anything else"));
    }
}
