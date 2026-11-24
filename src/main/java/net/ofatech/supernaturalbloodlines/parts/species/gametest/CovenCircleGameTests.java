package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.key;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.vampire;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.witch;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.level.Level;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.group.circle.Consent;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupSwitcher;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleLink;
import net.ofatech.supernaturalbloodlines.parts.species.group.coven.CovenCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.CallRequests;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;

/** Plan 20b Task 4 (R252, R258): opening a circle, calling the coven, joining by token, consent, and when a circle closes. Every test finishes in the tick it starts. */
@PrefixGameTestTemplate(false)
public final class CovenCircleGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String ERR = "supernaturalbloodlines.coven.err.";

    private CovenCircleGameTests() {
    }

    /** A gold chalk with a floor at the cell's front, where the test players stand. */
    static BlockPos chalkAt(GameTestHelper helper, int x) {
        BlockPos pos = helper.absolutePos(new BlockPos(x, 1, 1));
        WitchGameTests.chalk(helper, pos, java.util.UUID.randomUUID());
        return pos;
    }

    private static void tidy(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            CircleLink.forget(p.getUUID());
        }
        PackIndex.overrideClock(null);
        clean(helper, players);
    }

    private static String say(net.minecraft.network.chat.Component c) {
        return key(c);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAnchorOpensACircleAtAChalk(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist h = human(helper, 2.5);
        online(w, h);
        try {
            chalkAt(helper, 2);
            helper.assertTrue(join(w, h), "the human could not join the coven");
            helper.assertTrue(CircleLink.open(w) == null, "the anchor could not open a circle");
            CircleLink.Session s = CircleLink.of(w.getUUID());
            helper.assertTrue(s != null && s.anchor().equals(w.getUUID()) && s.members().equals(List.of(w.getUUID())), "the circle is " + s);
            helper.assertTrue(CircleLink.calls().tokenOf(w.getUUID(), h.getUUID()).isPresent(), "the coven member was not called");
        } finally {
            tidy(helper, w, h);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aNonMemberCannotOpenOne(GameTestHelper helper) {
        TalkingDuelist loner = human(helper, 1.5);
        TalkingDuelist leaderOfNobody = witch(helper, 2.5);
        online(loner, leaderOfNobody);
        try {
            chalkAt(helper, 2);
            int before = CircleLink.sessions();
            for (ServerPlayer p : List.of(loner, leaderOfNobody)) {
                var error = CircleLink.open(p);
                helper.assertTrue(error != null && say(error).contains(".no_group."), "a player with no coven was answered " + say(error));
                helper.assertTrue(CircleLink.of(p.getUUID()) == null, "a circle opened");
            }
            helper.assertTrue(CircleLink.sessions() == before, "a session was made");
        } finally {
            tidy(helper, loner, leaderOfNobody);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberOfAnotherCovenIsNotCalled(GameTestHelper helper) {
        TalkingDuelist w1 = witch(helper, 1.5);
        TalkingDuelist h1 = human(helper, 2.0);
        TalkingDuelist w2 = witch(helper, 2.5);
        TalkingDuelist h2 = human(helper, 3.0);
        online(w1, h1, w2, h2);
        try {
            chalkAt(helper, 2);
            helper.assertTrue(join(w1, h1) && join(w2, h2), "the covens did not form");
            helper.assertTrue(CircleLink.open(w1) == null, "no circle");
            helper.assertTrue(CircleLink.calls().tokenOf(w1.getUUID(), h1.getUUID()).isPresent(), "the own coven was not called");
            helper.assertTrue(CircleLink.calls().tokenOf(w1.getUUID(), h2.getUUID()).isEmpty() && CircleLink.calls().tokenOf(w1.getUUID(), w2.getUUID()).isEmpty(),
                    "the other coven was called");
            var refused = CircleLink.join(h2, null);
            helper.assertTrue(refused != null && CircleLink.of(h2.getUUID()) == null, "a member of another coven joined");
        } finally {
            tidy(helper, w1, h1, w2, h2);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void joiningNeedsTheToken(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist h1 = human(helper, 2.0);
        TalkingDuelist h2 = human(helper, 2.5);
        online(w, h1, h2);
        try {
            chalkAt(helper, 2);
            helper.assertTrue(join(w, h1) && join(w, h2), "the coven did not form");
            helper.assertTrue(CircleLink.open(w) == null, "no circle");
            String token = CircleLink.calls().tokenOf(w.getUUID(), h1.getUUID()).orElseThrow();
            helper.assertTrue(CircleLink.join(h2, token) != null, "somebody else used the token");
            helper.assertTrue(CircleLink.join(h1, "nonsense") != null && CircleLink.of(h1.getUUID()) == null, "a made-up token worked");
            long now = helper.getLevel().getGameTime();
            helper.assertTrue(CircleLink.calls().peek(token, h1.getUUID(), now + CallRequests.TTL_TICKS + 1).isEmpty(), "the call outlived its minute");
            String again = CircleLink.calls().open(w.getUUID(), h1.getUUID(), now);
            helper.assertTrue(CircleLink.join(h1, again) == null, "the right token did not work");
            helper.assertTrue(CircleLink.join(h1, again) != null, "the token worked twice");
            helper.assertTrue(CircleLink.of(h1.getUUID()).members().equals(List.of(w.getUUID(), h1.getUUID())), "the circle is " + CircleLink.of(h1.getUUID()).members());
        } finally {
            tidy(helper, w, h1, h2);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCircleHoldsThirteen(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.0);
        List<TalkingDuelist> crowd = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            crowd.add(human(helper, 1.2 + 0.3 * i));
        }
        List<ServerPlayer> everyone = new ArrayList<>(crowd);
        everyone.add(w);
        online(everyone.toArray(new ServerPlayer[0]));
        try {
            chalkAt(helper, 2);
            for (TalkingDuelist h : crowd) {
                helper.assertTrue(join(w, h), "the coven did not take everyone");
            }
            helper.assertTrue(CircleLink.open(w) == null, "no circle");
            for (int i = 0; i < 12; i++) {
                helper.assertTrue(CircleLink.join(crowd.get(i), null) == null, "member " + i + " could not join");
            }
            var refused = CircleLink.join(crowd.get(12), null);
            helper.assertTrue(refused != null && say(refused).equals(ERR + "circle_full"), "the fourteenth was answered " + say(refused));
            helper.assertTrue(CircleLink.of(w.getUUID()).members().size() == 13, "the circle holds " + CircleLink.of(w.getUUID()).members().size());
        } finally {
            tidy(helper, everyone.toArray(new ServerPlayer[0]));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlayerIsInOneCircleAtATime(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist h = human(helper, 2.5);
        online(w, h);
        try {
            chalkAt(helper, 2);
            helper.assertTrue(join(w, h) && CircleLink.open(w) == null && CircleLink.join(h, null) == null, "no circle with two in it");
            var again = CircleLink.open(w);
            helper.assertTrue(again != null && say(again).equals(ERR + "in_circle"), "the anchor opened a second one: " + say(again));
            var member = CircleLink.open(h);
            helper.assertTrue(member != null && say(member).equals(ERR + "in_circle"), "a member opened a circle of their own: " + say(member));
            helper.assertTrue(CircleLink.sessions() >= 1 && CircleLink.of(h.getUUID()).anchor().equals(w.getUUID()), "the member is not in the anchor's circle");
        } finally {
            tidy(helper, w, h);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCircleBelongsToItsAnchorAndItsChalk(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist h = human(helper, 2.5);
        online(w, h);
        try {
            BlockPos here = chalkAt(helper, 2);
            BlockPos elsewhere = chalkAt(helper, 12);
            helper.assertTrue(join(w, h) && CircleLink.open(w) == null, "no circle");
            var level = helper.getLevel();
            helper.assertTrue(CircleLink.current(level, here, w.getUUID()) != null, "the anchor does not have their own circle at their chalk");
            helper.assertTrue(CircleLink.current(level, here, h.getUUID()) == null, "another caster at the same chalk got the circle");
            helper.assertTrue(CircleLink.current(level, elsewhere, w.getUUID()) == null, "the anchor got a circle at another chalk");
            helper.assertTrue(CircleLink.current(level, here, null) == null, "a rite with no caster got the circle");
        } finally {
            tidy(helper, w, h);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCircleClosesOnATypeSwitch(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist h = human(helper, 2.5);
        online(w, h);
        try {
            chalkAt(helper, 2);
            helper.assertTrue(join(w, h) && CircleLink.open(w) == null && CircleLink.of(w.getUUID()) != null, "no circle");
            BloodlineAccess.set(w, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            GroupSwitcher.refresh(w);
            helper.assertTrue(CircleLink.of(w.getUUID()) == null && CircleLink.of(h.getUUID()) == null, "the circle survived the type switch");
        } finally {
            tidy(helper, w, h);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCircleClosesWhenTheAnchorLeavesTheDimension(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist h = human(helper, 2.5);
        online(w, h);
        try {
            chalkAt(helper, 2);
            helper.assertTrue(join(w, h) && CircleLink.open(w) == null && CircleLink.join(h, null) == null, "no circle");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(h, Level.OVERWORLD, Level.NETHER));
            helper.assertTrue(CircleLink.of(w.getUUID()) != null, "a member changing dimension closed the circle");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(w, Level.OVERWORLD, Level.NETHER));
            helper.assertTrue(CircleLink.of(w.getUUID()) == null && CircleLink.of(h.getUUID()) == null, "the circle outlived its anchor's dimension change");
        } finally {
            tidy(helper, w, h);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberWhoLeavesTheCovenLeavesTheCircle(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist h1 = human(helper, 2.0);
        TalkingDuelist h2 = human(helper, 2.5);
        online(w, h1, h2);
        try {
            chalkAt(helper, 2);
            helper.assertTrue(join(w, h1) && join(w, h2) && CircleLink.open(w) == null, "no circle");
            helper.assertTrue(CircleLink.join(h1, null) == null && CircleLink.join(h2, null) == null, "they could not join");
            helper.assertTrue(PackCommand.leave(h1) == null, "h1 could not leave");
            helper.assertTrue(CircleLink.of(h1.getUUID()) == null, "the leaver stayed in the circle");
            helper.assertTrue(CircleLink.of(w.getUUID()).members().equals(List.of(w.getUUID(), h2.getUUID())), "the others were touched: " + CircleLink.of(w.getUUID()).members());
            helper.assertTrue(PackCommand.kick(w, h2.getUUID(), "h2") == null && CircleLink.of(h2.getUUID()) == null, "a kicked member stayed");
            helper.assertTrue(CircleLink.of(w.getUUID()) != null, "the circle closed with its members");
        } finally {
            tidy(helper, w, h1, h2);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void withCovensOffNoCircleOpensOrIsJoined(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist h1 = human(helper, 2.0);
        online(w, h1);
        try {
            chalkAt(helper, 2);
            helper.assertTrue(join(w, h1) && CircleLink.open(w) == null, "the circle did not open with covens on");
            GroupsConfig.override("covens_enabled", false);
            var joined = CircleLink.join(h1, null);
            helper.assertTrue(joined != null && key(joined).equals("supernaturalbloodlines.coven.err.covens_off"), "a call was answered while covens are off: " + (joined == null ? "joined" : key(joined)));
            helper.assertTrue(CircleLink.close(w) == null, "the anchor could not close");
            var open = CircleLink.open(w);
            helper.assertTrue(open != null && key(open).equals("supernaturalbloodlines.coven.err.covens_off"), "a circle opened while covens are off: " + (open == null ? "opened" : key(open)));
        } finally {
            GroupsConfig.override("covens_enabled", null);
            tidy(helper, w, h1);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void idleCirclesClose(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist h = human(helper, 2.5);
        online(w, h);
        long[] clock = {1_000_000L};
        PackIndex.overrideClock(() -> clock[0]);
        try {
            chalkAt(helper, 2);
            helper.assertTrue(join(w, h) && CircleLink.open(w) == null, "no circle");
            clock[0] += 4 * 60_000L;
            CircleLink.sweep(helper.getLevel().getServer());
            helper.assertTrue(CircleLink.of(w.getUUID()) != null, "the circle closed after four idle minutes");
            clock[0] += 2 * 60_000L;
            CircleLink.sweep(helper.getLevel().getServer());
            helper.assertTrue(CircleLink.of(w.getUUID()) == null, "the circle stayed open after six idle minutes");
        } finally {
            tidy(helper, w, h);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void consentIsKeptAcrossARelog(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist h = human(helper, 2.5);
        online(w, h);
        try {
            helper.assertTrue(join(w, h), "no coven");
            helper.assertTrue(GroupIndex.get(helper.getLevel()).consentOf(h.getUUID()) == Consent.NONE, "the default is not NONE");
            helper.assertTrue(CovenCommand.consent(h, "bogus") != null, "an unknown level was accepted");
            helper.assertTrue(CovenCommand.consent(h, "damage") == null, "the level was refused");
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(h));
            GroupIndex saved = GroupIndex.load(GroupIndex.get(helper.getLevel()).save(new net.minecraft.nbt.CompoundTag(), null), null);
            helper.assertTrue(saved.consentOf(h.getUUID()) == Consent.DAMAGE, "the level was not kept: " + saved.consentOf(h.getUUID()));
            helper.assertTrue(GroupIndex.get(helper.getLevel()).consentOf(h.getUUID()) == Consent.DAMAGE, "the live level changed at logout");
            helper.assertTrue(CovenCommand.consent(h, "none") == null && GroupIndex.get(helper.getLevel()).consentOf(h.getUUID()) == Consent.NONE, "the level did not go back");
        } finally {
            GroupIndex.get(helper.getLevel()).setConsent(h.getUUID(), Consent.NONE);
            tidy(helper, w, h);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLinkLiteralsAnswerWrongTypeInAPack(GameTestHelper helper) {
        TalkingDuelist v = vampire(helper, 1.5);
        TalkingDuelist h = human(helper, 2.5);
        online(v, h);
        try {
            chalkAt(helper, 2);
            helper.assertTrue(join(v, h), "no clan");
            for (ServerPlayer p : List.of(v, h)) {
                for (var error : new net.minecraft.network.chat.Component[] {CircleLink.open(p), CircleLink.join(p, null), CovenCommand.consent(p, "none")}) {
                    helper.assertTrue(error != null && say(error).equals("supernaturalbloodlines.group.wrong_type.clan"), "a clan was answered " + say(error));
                }
            }
            helper.assertTrue(CircleLink.of(v.getUUID()) == null, "a clan opened a circle");
        } finally {
            tidy(helper, v, h);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCardOpensForTheInvitee(GameTestHelper helper) {
        TalkingDuelist w = witch(helper, 1.5);
        TalkingDuelist h = human(helper, 2.5);
        online(w, h);
        try {
            chalkAt(helper, 2);
            helper.assertTrue(join(w, h) && CircleLink.open(w) == null, "no circle");
            var calls = HubRequests.of(h).stream().filter(r -> r.kind() == HubRequests.Kind.CIRCLE_CALL).toList();
            helper.assertTrue(calls.size() == 1, "the invitee has " + calls.size() + " circle calls");
            HubRequests.Request card = calls.get(0);
            helper.assertTrue(card.from().equals(w.getUUID()) && !card.token().isEmpty() && card.ticksLeft() > 0, "the card is " + card);
            helper.assertTrue(CircleLink.join(h, card.token()) == null, "the card's token did not join");
            helper.assertTrue(HubRequests.of(h).stream().noneMatch(r -> r.kind() == HubRequests.Kind.CIRCLE_CALL), "the card stayed after joining");
        } finally {
            tidy(helper, w, h);
        }
        helper.succeed();
    }
}