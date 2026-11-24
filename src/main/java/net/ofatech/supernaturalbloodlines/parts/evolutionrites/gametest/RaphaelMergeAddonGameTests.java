package net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest;

import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.give;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.has;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.onTheCircle;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.remove;

import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.unique.ITrulyUnique;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoGrant;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.Recipes;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.merge.RaphaelMerge;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.FoxAblazeProbe;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe.Hooks;

/**
 * The Raphael merge against the real Great Sage and FoxAblaze (plan 19b task 5; {@code -PaddonJars} with both jars). Great Sage's own evolution is its private
 * {@code RaphaelEvolutionService.finish}, called by reflection: its road is closed in 0.0.7.1, so no player can reach it, but what it does when it runs is exactly
 * what the merge must survive. Without the jars every test succeeds at once.
 */
@PrefixGameTestTemplate(false)
public final class RaphaelMergeAddonGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String GS_RAPHAEL = "greatsage:raphael";
    private static final String FOX_RAPHAEL = "foxablazeultimate:raphael";
    private static final String GREAT_SAGE = "tensura:great_sage";
    private static final String DEGENERATE = "tensura:degenerate";
    private static final double EP = 5_000_000;

    private RaphaelMergeAddonGameTests() {
    }

    private static boolean both(GameTestHelper helper) {
        if (Recipes.modLoaded("greatsage") && Recipes.modLoaded("foxablazeultimate")) {
            return true;
        }
        helper.succeed();
        return false;
    }

    private static RecordingPlayer player(GameTestHelper helper) {
        return onTheCircle(helper, false, EP);
    }

    /** Great Sage's own ending of the evolution; what it throws after the grant (a packet to a fake player) is not what is under test. */
    private static void finish(ServerPlayer p) throws ReflectiveOperationException {
        Method m = Class.forName("dev.dedek.greatsage.server.RaphaelEvolutionService").getDeclaredMethod("finish", ServerPlayer.class);
        m.setAccessible(true);
        try {
            m.invoke(null, p);
        } catch (InvocationTargetException e) {
            if (!(e.getCause() instanceof NullPointerException)) {
                throw e;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean setRule(GameTestHelper helper, boolean on) {
        var key = (GameRules.Key<GameRules.BooleanValue>) Hooks.staticField("com.foxablazeultimate.world.FoxAblazeGameRules", "RAPHAEL_TRULY_UNIQUE").value().orElseThrow();
        var server = helper.getLevel().getServer();
        GameRules.BooleanValue rule = server.getGameRules().getRule(key);
        boolean before = rule.get();
        rule.set(on, server);
        return before;
    }

    private static boolean holdsRaphael(LivingEntity e) throws ReflectiveOperationException {
        return (boolean) Class.forName("dev.dedek.greatsage.GreatSageGameRules").getMethod("holdsRaphael", LivingEntity.class).invoke(null, e);
    }

    private static void clean(GameTestHelper helper, boolean ruleBefore, ServerPlayer... ps) {
        RaphaelMerge.override(null);
        FoxAblazeProbe.ownedHook = null;
        setRule(helper, ruleBefore);
        for (ServerPlayer p : ps) {
            EvoTestSupport.forgetAll(p);
            remove(helper, p);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        ITrulyUnique storage = TensuraStorages.getUniqueStorageFrom(helper.getLevel().getServer().overworld());
        for (String id : new String[] {FOX_RAPHAEL, "foxablazeultimate:ciel"}) {
            storage.removeSkill(ResourceLocation.parse(id));
        }
        storage.markDirty();
    }

    private static boolean raphaels(ServerPlayer p, boolean gs, boolean fox) {
        return has(p, GS_RAPHAEL) == gs && has(p, FOX_RAPHAEL) == fox;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void greatSagesEvolutionEndsWithFoxAblazesRaphael(GameTestHelper helper) throws ReflectiveOperationException {
        if (!both(helper)) {
            return;
        }
        boolean rule = setRule(helper, false);
        RecordingPlayer p = player(helper);
        try {
            give(p, GREAT_SAGE, 100);
            give(p, DEGENERATE, 100);
            finish(p);
            helper.assertTrue(raphaels(p, false, true), "after Great Sage's evolution: greatsage " + has(p, GS_RAPHAEL) + ", foxablaze " + has(p, FOX_RAPHAEL));
            helper.assertTrue(!has(p, GREAT_SAGE) && !has(p, DEGENERATE), "the evolution did not take what it takes");
            helper.assertTrue(holdsRaphael(p), "Great Sage does not see FoxAblaze's Raphael as Raphael");
        } finally {
            clean(helper, rule, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCommandGrantIsRedirectedToo(GameTestHelper helper) {
        if (!both(helper)) {
            return;
        }
        boolean rule = setRule(helper, false);
        RecordingPlayer p = player(helper);
        try {
            var gs = SkillAPI.getSkillRegistry().get(ResourceLocation.parse(GS_RAPHAEL));
            helper.assertTrue(!EvoGrant.learnFree(p, gs), "the learn of Great Sage's Raphael reported success");
            helper.assertTrue(raphaels(p, false, true), "greatsage " + has(p, GS_RAPHAEL) + ", foxablaze " + has(p, FOX_RAPHAEL));
        } finally {
            clean(helper, rule, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRedirectedLearnRegistersTheOwnerAndTheNextPlayerIsRefused(GameTestHelper helper) throws ReflectiveOperationException {
        if (!both(helper)) {
            return;
        }
        boolean rule = setRule(helper, true);
        RecordingPlayer first = player(helper);
        RecordingPlayer second = player(helper);
        try {
            give(second, GREAT_SAGE, 100);
            give(second, DEGENERATE, 100);
            var gs = SkillAPI.getSkillRegistry().get(ResourceLocation.parse(GS_RAPHAEL));
            EvoGrant.learnFree(first, gs);
            ITrulyUnique storage = TensuraStorages.getUniqueStorageFrom(helper.getLevel().getServer().overworld());
            UUID owner = storage.getOwner(ResourceLocation.parse(FOX_RAPHAEL));
            helper.assertTrue(first.getUUID().equals(owner), "FoxAblaze did not register the owner of the redirected learn: " + owner);
            finish(second);
            helper.assertTrue(raphaels(second, false, false), "a second owner got a Raphael: greatsage " + has(second, GS_RAPHAEL) + ", foxablaze " + has(second, FOX_RAPHAEL));
            helper.assertTrue(has(second, GREAT_SAGE) && has(second, DEGENERATE), "the refusal did not give Great Sage and Degenerate back");
            helper.assertTrue(second.received(RaphaelMerge.NOT_READY) || second.receivedOnActionBar(RaphaelMerge.NOT_READY), "the refusal line: " + second.heard());
        } finally {
            clean(helper, rule, first, second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void loginConvertsAHeldGreatSageRaphael(GameTestHelper helper) {
        if (!both(helper)) {
            return;
        }
        boolean rule = setRule(helper, false);
        RecordingPlayer p = player(helper);
        try {
            var gs = SkillAPI.getSkillRegistry().get(ResourceLocation.parse(GS_RAPHAEL));
            RaphaelMerge.override(new RaphaelMerge.Merge(ResourceLocation.parse(GS_RAPHAEL), ResourceLocation.parse(FOX_RAPHAEL),
                    java.util.List.of(ResourceLocation.parse(GS_RAPHAEL), ResourceLocation.parse(FOX_RAPHAEL)), java.util.List.of(), () -> false));
            helper.assertTrue(EvoGrant.learnFree(p, gs), "setup: Great Sage's Raphael was not learned with the merge off");
            RaphaelMerge.override(null);
            RaphaelMerge.sweep(p);
            helper.assertTrue(raphaels(p, false, true), "after the login sweep: greatsage " + has(p, GS_RAPHAEL) + ", foxablaze " + has(p, FOX_RAPHAEL));
        } finally {
            clean(helper, rule, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void holdingFoxAblazesRaphaelBlocksGreatSages(GameTestHelper helper) {
        if (!both(helper)) {
            return;
        }
        boolean rule = setRule(helper, false);
        RecordingPlayer p = player(helper);
        try {
            give(p, FOX_RAPHAEL, 10);
            var gs = SkillAPI.getSkillRegistry().get(ResourceLocation.parse(GS_RAPHAEL));
            helper.assertTrue(!EvoGrant.learnFree(p, gs), "a second Raphael was learned");
            helper.assertTrue(raphaels(p, false, true), "greatsage " + has(p, GS_RAPHAEL) + ", foxablaze " + has(p, FOX_RAPHAEL));
        } finally {
            clean(helper, rule, p);
        }
        helper.succeed();
    }

    /** The run with Great Sage alone: nothing is redirected and Great Sage's Raphael is itself. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void withoutFoxAblazeNothingIsRedirected(GameTestHelper helper) {
        if (!Recipes.modLoaded("greatsage") || Recipes.modLoaded("foxablazeultimate")) {
            helper.succeed();
            return;
        }
        RecordingPlayer p = player(helper);
        try {
            var gs = SkillAPI.getSkillRegistry().get(ResourceLocation.parse(GS_RAPHAEL));
            helper.assertTrue(EvoGrant.learnFree(p, gs), "Great Sage's Raphael was not learned as itself");
            helper.assertTrue(has(p, GS_RAPHAEL), "it is not held");
        } finally {
            RaphaelMerge.override(null);
            EvoTestSupport.forgetAll(p);
            remove(helper, p);
        }
        helper.succeed();
    }
}
