package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.KolEntity;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.BreakerRow;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood.KolShadow;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/** Plan 20d Task 10 (R296): Kol's shadow. Lives with the bosses, so it only runs where Kol exists. Kol is placed by hand, never by a tick loop. */
@PrefixGameTestTemplate(false)
public final class KolShadowGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String WARN = "supernaturalbloodlines.brotherhood.kol_warning";
    private static final String HINT = "supernaturalbloodlines.boss.notice.kol.hint";

    private KolShadowGameTests() {
    }

    private static TalkingDuelist hunter(GameTestHelper helper, double x) {
        TalkingDuelist p = human(helper, x);
        VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.HUNTER_FACTION, 1);
        return p;
    }

    /** A hunter-led brotherhood with two plain humans; everybody is online. */
    private record Bh(TalkingDuelist leader, TalkingDuelist a, TalkingDuelist b) {
        ServerPlayer[] all() {
            return new ServerPlayer[] {leader, a, b};
        }
    }

    private static Bh bh(GameTestHelper helper) {
        TalkingDuelist leader = hunter(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        TalkingDuelist b = human(helper, 3.5);
        online(leader, a, b);
        if (!join(leader, a) || !join(leader, b)) {
            throw new AssertionError("no brotherhood");
        }
        return new Bh(leader, a, b);
    }

    private static Optional<KolEntity> kol(GameTestHelper helper, ServerPlayer target) {
        return BossTestRig.placeKol(helper, target);
    }

    private static void finish(GameTestHelper helper, Optional<KolEntity> kol, ServerPlayer... players) {
        KolShadow.overrideProbe(null);
        kol.ifPresent(KolEntity::discard);
        BossIndex.get(helper.getLevel().getServer()).forget(BossId.KOL);
        clean(helper, players);
    }

    private static int warnings(ServerPlayer p) {
        return ((TalkingDuelist) p).count(WARN);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void kolForABrotherWarnsTheOtherOnlineBrothers(GameTestHelper helper) {
        Bh g = bh(helper);
        Optional<KolEntity> k = Optional.empty();
        try {
            k = kol(helper, g.a());
            helper.assertTrue(k.isPresent(), "Kol was not placed");
            helper.assertTrue(warnings(g.leader()) == 1 && warnings(g.b()) == 1, "one line each, got " + warnings(g.leader()) + "/" + warnings(g.b()));
            helper.assertTrue(g.leader().countOnActionBar(WARN) == 1 && g.b().countOnActionBar(WARN) == 1, "the same line on the action bar");
        } finally {
            finish(helper, k, g.all());
        }
        helper.succeed();
    }

    private static String langValue(String key) {
        try (var in = KolShadowGameTests.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json")) {
            return com.google.gson.JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().get(key).getAsString();
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLineNamesTheTargetAndNeverKol(GameTestHelper helper) {
        Bh g = bh(helper);
        Optional<KolEntity> k = Optional.empty();
        try {
            k = kol(helper, g.a());
            Component line = g.leader().message(WARN);
            helper.assertTrue(line != null && ((TranslatableContents) line.getContents()).getArgs()[0].equals(g.a().getGameProfile().getName()), "the line does not name the target: " + line);
            String text = langValue(WARN).toLowerCase(java.util.Locale.ROOT);
            for (String word : List.of("kol", "original", "lineage", "line ", "sire", "mikael", "nature", "vampire")) {
                helper.assertTrue(!text.contains(word), "the line says '" + word + "': " + text);
            }
            helper.assertTrue(text.contains("%s"), "the line has no name slot");
        } finally {
            finish(helper, k, g.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theTargetGetsOnlyTheirOwnNotices(GameTestHelper helper) {
        Bh g = bh(helper);
        Optional<KolEntity> k = Optional.empty();
        boolean[] ownFirst = {false};
        try {
            KolShadow.overrideProbe(p -> ownFirst[0] = ((TalkingDuelist) p).received(HINT));
            k = kol(helper, g.a());
            helper.assertTrue(warnings(g.a()) == 0 && g.a().countOnActionBar(WARN) == 0, "the target was warned about themself");
            helper.assertTrue(g.a().received(HINT) && (g.a().received("supernaturalbloodlines.boss.notice.kol.known") || g.a().received("supernaturalbloodlines.boss.notice.kol.unknown")), "the target lost their own notices");
            helper.assertTrue(ownFirst[0], "the warning ran before the target's own notices");
        } finally {
            finish(helper, k, g.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBreakerStillHearsIt(GameTestHelper helper) {
        Bh g = bh(helper);
        Optional<KolEntity> k = Optional.empty();
        GroupIndex groups = GroupIndex.get(helper.getLevel());
        try {
            groups.setBreaker(g.b().getUUID(), new BreakerRow(PackIndex.now() + 3_600_000L, List.of()));
            k = kol(helper, g.a());
            helper.assertTrue(warnings(g.b()) == 1, "a Code-breaker did not hear it");
        } finally {
            groups.clearBreaker(g.b().getUUID());
            finish(helper, k, g.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMemberWhoDoesNotFitHearsNothing(GameTestHelper helper) {
        Bh g = bh(helper);
        TalkingDuelist witch = human(helper, 4.5);
        TalkingDuelist vampire = human(helper, 5.5);
        online(g.leader(), g.a(), g.b(), witch, vampire);
        Optional<KolEntity> k = Optional.empty();
        try {
            BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
            VampirismAPI.factionPlayerHandler(vampire).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            helper.assertTrue(join(g.leader(), witch) && join(g.leader(), vampire), "setup: they could not join");
            k = kol(helper, g.a());
            helper.assertTrue(warnings(witch) == 0 && warnings(vampire) == 0, "a member who does not fit heard it");
            helper.assertTrue(warnings(g.leader()) == 1, "control: the leader did not hear it");
        } finally {
            finish(helper, k, g.leader(), g.a(), g.b(), witch, vampire);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOfflineMemberIsNotWritten(GameTestHelper helper) {
        Bh g = bh(helper);
        Optional<KolEntity> k = Optional.empty();
        try {
            online(g.leader(), g.a());
            k = kol(helper, g.a());
            helper.assertTrue(warnings(g.b()) == 0, "an offline member was written");
            helper.assertTrue(PackIndex.get(helper.getLevel()).takeNotices(g.b().getUUID()).isEmpty(), "a notice waits for the offline member");
        } finally {
            finish(helper, k, g.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void warningOffSaysNothing(GameTestHelper helper) {
        Bh g = bh(helper);
        Optional<KolEntity> k = Optional.empty();
        try {
            GroupsConfig.override("kol_warning_enabled", false);
            k = kol(helper, g.a());
            helper.assertTrue(warnings(g.leader()) == 0 && warnings(g.b()) == 0, "sent with the switch off");
        } finally {
            finish(helper, k, g.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void brotherhoodOffSaysNothing(GameTestHelper helper) {
        Bh g = bh(helper);
        Optional<KolEntity> k = Optional.empty();
        try {
            GroupsConfig.override("brotherhood_enabled", false);
            k = kol(helper, g.a());
            helper.assertTrue(warnings(g.leader()) == 0 && warnings(g.b()) == 0, "sent with brotherhoods off");
        } finally {
            finish(helper, k, g.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTargetInNoGroupWarnsNobody(GameTestHelper helper) {
        Bh g = bh(helper);
        TalkingDuelist stranger = human(helper, 4.5);
        online(g.leader(), g.a(), g.b(), stranger);
        Optional<KolEntity> k = Optional.empty();
        try {
            k = kol(helper, stranger);
            helper.assertTrue(warnings(g.leader()) == 0 && warnings(g.a()) == 0 && warnings(g.b()) == 0 && warnings(stranger) == 0, "somebody was warned about a target in no group");
        } finally {
            finish(helper, k, g.leader(), g.a(), g.b(), stranger);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPackOrClanTargetWarnsNobody(GameTestHelper helper) {
        TalkingDuelist clanLeader = human(helper, 1.5);
        TalkingDuelist clanMember = human(helper, 2.5);
        online(clanLeader, clanMember);
        Optional<KolEntity> k = Optional.empty();
        try {
            VampirismAPI.factionPlayerHandler(clanLeader).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            helper.assertTrue(join(clanLeader, clanMember), "no clan");
            k = kol(helper, clanMember);
            helper.assertTrue(warnings(clanLeader) == 0 && warnings(clanMember) == 0, "a clan was warned");
        } finally {
            finish(helper, k, clanLeader, clanMember);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTargetWhoDoesNotFitWarnsNobody(GameTestHelper helper) {
        Bh g = bh(helper);
        TalkingDuelist vampire = human(helper, 4.5);
        online(g.leader(), g.a(), g.b(), vampire);
        Optional<KolEntity> k = Optional.empty();
        try {
            helper.assertTrue(join(g.leader(), vampire), "the vampire could not join");
            VampirismAPI.factionPlayerHandler(vampire).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            k = kol(helper, vampire);
            helper.assertTrue(warnings(g.leader()) == 0 && warnings(g.a()) == 0 && warnings(g.b()) == 0, "the brothers were warned about a member who does not fit");
        } finally {
            finish(helper, k, g.leader(), g.a(), g.b(), vampire);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSpawnWithNoTargetWarnsNobody(GameTestHelper helper) {
        Bh g = bh(helper);
        Optional<KolEntity> k = Optional.empty();
        try {
            k = BossTestRig.placeKol(helper, null);
            helper.assertTrue(k.isPresent() && warnings(g.leader()) == 0 && warnings(g.a()) == 0 && warnings(g.b()) == 0, "a spawn with no target warned somebody (the debug command does this)");
        } finally {
            finish(helper, k, g.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAlliedBrotherhoodHearsNothing(GameTestHelper helper) {
        Bh g = bh(helper);
        TalkingDuelist otherLeader = hunter(helper, 4.5);
        TalkingDuelist otherMember = human(helper, 5.5);
        online(g.leader(), g.a(), g.b(), otherLeader, otherMember);
        Optional<KolEntity> k = Optional.empty();
        try {
            helper.assertTrue(join(otherLeader, otherMember), "no second brotherhood");
            helper.assertTrue(PackIndex.get(helper.getLevel()).ally(g.leader().getUUID(), otherLeader.getUUID()), "no alliance");
            k = kol(helper, g.a());
            helper.assertTrue(warnings(otherLeader) == 0 && warnings(otherMember) == 0, "an allied brotherhood was warned");
            helper.assertTrue(warnings(g.leader()) == 1, "control: the brotherhood was not warned");
        } finally {
            finish(helper, k, g.leader(), g.a(), g.b(), otherLeader, otherMember);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aThrowingWarningDoesNotBreakTheSpawn(GameTestHelper helper) {
        Bh g = bh(helper);
        Optional<KolEntity> k = Optional.empty();
        try {
            KolShadow.overrideProbe(p -> {
                throw new IllegalStateException("EXPECTED: a made-up failure inside the warning");
            });
            k = kol(helper, g.a());
            helper.assertTrue(k.isPresent(), "Kol was not placed");
            helper.assertTrue(k.get().isAlive() && !k.get().isRemoved(), "Kol was removed");
            helper.assertTrue(g.a().received(HINT), "the target lost their own notices");
            helper.assertTrue(warnings(g.leader()) == 0, "a failed warning still wrote");
            helper.assertTrue(BossIndex.get(helper.getLevel().getServer()).of(BossId.KOL).appearances > 0, "the appearance was not written down");
        } finally {
            finish(helper, k, g.all());
        }
        helper.succeed();
    }
}
