package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.StatePredicates;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.supernatural.Migration;
import net.ofatech.supernaturalbloodlines.parts.supernatural.task.Detectors;
import net.ofatech.supernaturalbloodlines.parts.supernatural.task.TaskTracker;

/** The login path on real players: migration first, then the first read of the state. */
@PrefixGameTestTemplate(false)
public final class MigrationGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final long DAY = StatePredicates.DAY_TICKS;

    private MigrationGameTests() {}

    /** The test world is a few hundred ticks old: move its clock forward so "60 days ago" is a real time. Put back by {@link #restore}. */
    private static long clock(GameTestHelper helper) {
        long before = helper.getLevel().getGameTime();
        ((net.minecraft.world.level.storage.ServerLevelData) helper.getLevel().getLevelData()).setGameTime(before + 200 * DAY);
        return before;
    }

    private static void restore(GameTestHelper helper, long before) {
        ((net.minecraft.world.level.storage.ServerLevelData) helper.getLevel().getLevelData()).setGameTime(before + 1);
    }

    private static ServerPlayer longTimeVampire(GameTestHelper helper, long daysAgo) {
        ServerPlayer p = TestPlayers.create(helper);
        long now = helper.getLevel().getGameTime();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().turnedAt(Math.max(1, now - daysAgo * DAY)).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 14), "could not make a vampire");
        return p;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLongTimeVampireKeepsTheirDaysAfterTheUpdate(GameTestHelper helper) {
        long before = clock(helper);
        try {
            checkDays(helper);
        } finally {
            restore(helper, before);
        }
        helper.succeed();
    }

    private static void checkDays(GameTestHelper helper) {
        ServerPlayer p = longTimeVampire(helper, 60);
        // the player is as the update found them: no schema yet. The login must migrate first and stamp from turnedAt, not "now".
        SupernaturalAccess.set(p, SupernaturalState.NONE);
        long turned = BloodlineAccess.get(p).turnedAt();
        Detectors.onLogin(p);
        SupernaturalState s = SupernaturalAccess.get(p);
        helper.assertTrue(s.schema() == 1, "not migrated: " + s);
        Long since = s.raceSince().get("vampire");
        helper.assertTrue(since != null && since == turned, "race_since is " + since + ", turnedAt is " + turned + ": the days were lost");
        helper.assertTrue(s.done().contains("L-VAM-1a"), "the level step was not proven: " + s);
        // and the days run from it: the recheck after the migration did not move it
        TaskTracker.recheck(p);
        helper.assertTrue(SupernaturalAccess.get(p).raceSince().get("vampire") == turned, "a recheck moved race_since");
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSecondLoginChangesNothing(GameTestHelper helper) {
        ServerPlayer p = longTimeVampire(helper, 10);
        SupernaturalAccess.set(p, SupernaturalState.NONE);
        Detectors.onLogin(p);
        SupernaturalState first = SupernaturalAccess.get(p);
        Detectors.onLogin(p);
        helper.assertTrue(SupernaturalAccess.get(p).equals(first), "a second login changed the state: " + SupernaturalAccess.get(p));
        helper.assertTrue(!Migration.runIfNeeded(p), "the migration ran twice");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOriginalKeepsTheirLayerRung(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).turnedAt(5).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 4), "setup");
        SupernaturalAccess.set(p, SupernaturalState.NONE);
        Detectors.onLogin(p);
        SupernaturalState s = SupernaturalAccess.get(p);
        helper.assertTrue(s.rung(SupernaturalRace.ORIGINAL) >= 3, "an Original is below the layer rung: " + s);
        helper.assertTrue(s.sp() > 0 && s.sp() == s.spEarned(), "no skill points for the rungs given: " + s);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aGhoulIsNotMigratedIntoAnything(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().ghoul(true).build());
        SupernaturalAccess.set(p, SupernaturalState.NONE);
        Detectors.onLogin(p);
        SupernaturalState s = SupernaturalAccess.get(p);
        helper.assertTrue(s.sp() == 0 && s.rungs().isEmpty() && s.raceSince().isEmpty(), "a Ghoul was given something: " + s);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aFakePlayerIsNotMigrated(GameTestHelper helper) {
        ServerPlayer p = longTimeVampire(helper, 60);
        SupernaturalAccess.set(p, SupernaturalState.NONE);
        SupernaturalAccess.trackFakePlayers(false);
        try {
            Detectors.onLogin(p);
        } finally {
            SupernaturalAccess.trackFakePlayers(true);
        }
        helper.assertTrue(SupernaturalAccess.get(p).equals(SupernaturalState.NONE), "a FakePlayer was migrated");
        helper.succeed();
    }
}
