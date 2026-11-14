package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.race.RaceHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;

/** The Tensura side of the Ghoul (R20e): the race shown, and no evolution while a Ghoul. Needs species and species_tensura. */
@PrefixGameTestTemplate(false)
public final class GhoulTensuraGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String NO_EVOLVE = "supernaturalbloodlines.turning.ghoul_no_evolve";

    private GhoulTensuraGameTests() {
    }

    private static void setRace(LivingEntity e, String id) {
        RaceAPI.getRaceFrom(e).setRace(ResourceLocation.parse(id), false);
    }

    private static String race(LivingEntity e) {
        return RaceAPI.getRaceFrom(e).getRace().map(r -> r.getRaceId().toString()).orElse("none");
    }

    /** A real Sanguinare finish: Vampirism's own effect code joins the faction. */
    private static void finishSanguinare(ServerPlayer p) {
        var sanguinare = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("vampirism:sanguinare")).orElseThrow();
        p.addEffect(new MobEffectInstance(sanguinare, 100));
        sanguinare.value().applyEffectTick(p, 0);
    }

    private static boolean evolveTo(GameTestHelper helper, ServerPlayer p, String id) {
        ManasRace target = RaceAPI.getRaceRegistry().get(ResourceLocation.parse(id));
        helper.assertTrue(target != null, id + " is not a race");
        return RaceHelper.evolveRace(p, target, true);
    }

    private static void reset(ServerPlayer p) {
        BloodlineAccess.set(p, BloodlineState.NONE);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void humanGhoulShowsGhoulRace(GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        try {
            setRace(p, "tensura:human");
            finishSanguinare(p);
            helper.assertTrue(BloodlineAccess.get(p).ghoul(), "not a Ghoul");
            helper.assertTrue("tensura:ghoul".equals(race(p)), "race is " + race(p));
            helper.assertTrue(VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == VReference.VAMPIRE_FACTION,
                    "not in the vampire faction");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void elfGhoulKeepsRace(GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        try {
            setRace(p, "tensura:elf");
            finishSanguinare(p);
            helper.assertTrue(BloodlineAccess.get(p).ghoul(), "not a Ghoul");
            helper.assertTrue("tensura:elf".equals(race(p)), "race is " + race(p));
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ghoulCannotEvolve(GameTestHelper helper) {
        RecordingPlayer ghoul = RecordingPlayer.create(helper);
        RecordingPlayer control = RecordingPlayer.create(helper);
        try {
            // control: the same call goes through for a player who is not a Ghoul, so it does reach SET_RACE
            setRace(control, "tensura:human");
            helper.assertTrue(evolveTo(helper, control, "tensura:vampire") && "tensura:vampire".equals(race(control)),
                    "the control evolution failed: " + race(control));
            helper.assertTrue(!control.received(NO_EVOLVE), "a non-Ghoul was told to finish the turning");

            setRace(ghoul, "tensura:human");
            finishSanguinare(ghoul);
            helper.assertTrue("tensura:ghoul".equals(race(ghoul)), "setup: race is " + race(ghoul));
            // RaceHelper.evolveRace answers true after the attempt whatever setRace said (its code returns true on every path
            // past the checks), so the race is what proves the refusal; ManasCore's own call answers false
            evolveTo(helper, ghoul, "tensura:vampire");
            helper.assertTrue(!RaceAPI.getRaceFrom(ghoul).evolveRace(ResourceLocation.parse("tensura:vampire")), "Races.evolveRace worked for a Ghoul");
            helper.assertTrue("tensura:ghoul".equals(race(ghoul)), "race is " + race(ghoul));
            helper.assertTrue(ghoul.received(NO_EVOLVE), "the player was not told why");
            // only evolutions are refused: a plain race change (a command, a cure) is not
            helper.assertTrue(RaceAPI.getRaceFrom(ghoul).setRace(ResourceLocation.parse("tensura:human"), false) && "tensura:human".equals(race(ghoul)),
                    "a plain race change was refused");
        } finally {
            reset(ghoul);
            reset(control);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ghoulCompletesTurning(GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        try {
            setRace(p, "tensura:human");
            finishSanguinare(p);
            helper.assertTrue("tensura:ghoul".equals(race(p)), "setup: race is " + race(p));
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().turningTicksLeft(9000).build());
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("vampirism:blood_bottle"))));
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND));
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(!s.ghoul() && !s.inTurning(), "the turning did not complete: " + s);
            helper.assertTrue("tensura:vampire".equals(race(p)), "race is " + race(p));
            helper.assertTrue(VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == VReference.VAMPIRE_FACTION
                    && VampirismAPI.factionPlayerHandler(p).getCurrentLevel() == 1, "not a level 1 vampire");
            // evolutions work again
            helper.assertTrue(evolveTo(helper, p, "tensura:vampire_overcomer") && "tensura:vampire_overcomer".equals(race(p)), "no evolution after the turning: " + race(p));
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void elfGhoulCompletesTurningKeepingRace(GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        try {
            setRace(p, "tensura:elf");
            finishSanguinare(p);
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().turningTicksLeft(9000).build());
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("vampirism:blood_bottle"))));
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND));
            helper.assertTrue(!BloodlineAccess.get(p).ghoul() && "tensura:elf".equals(race(p)), "race is " + race(p) + ", state " + BloodlineAccess.get(p));
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    /** vampirism_tensura (VampireSync.raceOnCure) may be off, so species_tensura sets the race back to human itself. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void clearedGhoulOutsideTheVampiresIsHuman(GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        try {
            setRace(p, "tensura:ghoul");
            // a Ghoul flag with no vampire faction behind it (what is left when the cure mapping is not there to act first)
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().ghoul(true).build());
            helper.assertTrue("tensura:ghoul".equals(race(p)), "setup: race is " + race(p));
            BloodlineAccess.set(p, BloodlineState.NONE);
            helper.assertTrue("tensura:human".equals(race(p)), "race is " + race(p));
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void leavingTheVampiresAsAGhoulGoesBackToHuman(GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        try {
            setRace(p, "tensura:human");
            finishSanguinare(p);
            VampirismAPI.factionPlayerHandler(p).leaveFaction(true);
            helper.assertTrue(!BloodlineAccess.get(p).ghoul() && "tensura:human".equals(race(p)), "race is " + race(p) + ", state " + BloodlineAccess.get(p));
        } finally {
            reset(p);
        }
        helper.succeed();
    }
}
