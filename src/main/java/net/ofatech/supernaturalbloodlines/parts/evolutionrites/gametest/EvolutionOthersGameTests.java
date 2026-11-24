package net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest;

import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.atNight;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.has;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.onTheCircle;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.performer;
import static net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest.EvoTestSupport.stacks;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.clearDropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.dropped;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.remove;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.start;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.rite.EvolutionRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;

/**
 * The four Uniques of plan 19d against the real addons. Each test skips itself when its addon is not loaded (CI never has them; {@code -PaddonJars} does), and
 * then checks that the rite does not exist. Where an installed addon closes Tensura's own learn (Mysticism's Soul Energy price), the grant test proves the clean
 * refusal instead.
 */
@PrefixGameTestTemplate(false)
public final class EvolutionOthersGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String NOT_READY = "supernaturalbloodlines.refusal.evolution.not_ready";
    private static final double EP = 5_000_000;

    private EvolutionOthersGameTests() {
    }

    private record Rite(String id, String mod, String grants, List<ItemStack> items, String marker) {
    }

    private static Rite greatMage() {
        return new Rite("evolve_great_mage", "ascension", "ascension:great_mage",
                stacks("minecraft:lapis_block", 2, "minecraft:enchanted_book", 1, "minecraft:amethyst_shard", 4, "enchanted:spirit_of_otherwhere", 1), "minecraft:lapis_block");
    }

    private static Rite kingOfCurses() {
        return new Rite("evolve_king_of_curses", "ascension", "ascension:king_of_curses",
                stacks("minecraft:wither_skeleton_skull", 1, "minecraft:crying_obsidian", 2, "minecraft:amethyst_shard", 4, "enchanted:spirit_of_otherwhere", 1),
                "minecraft:crying_obsidian");
    }

    private static Rite butcher() {
        return new Rite("evolve_butcher", "mysticism", "mysticism:butcher",
                stacks("minecraft:beef", 8, "minecraft:bone_block", 2, "minecraft:amethyst_shard", 4, "enchanted:spirit_of_otherwhere", 1), "minecraft:bone_block");
    }

    private static Rite imitator() {
        return new Rite("evolve_imitator", "stextras", "stextras:imitator",
                stacks("minecraft:echo_shard", 2, "minecraft:glass", 4, "minecraft:amethyst_shard", 4, "enchanted:spirit_of_otherwhere", 1), "minecraft:echo_shard");
    }

    /** True when there is nothing to run: the addon is absent (and then the rite must not exist) or the part is off. */
    private static boolean skipped(GameTestHelper helper, Rite r) {
        if (!ModList.get().isLoaded(r.mod())) {
            helper.assertTrue(type(helper, r.id()) == null, r.id() + " exists without " + r.mod());
            helper.succeed();
            return true;
        }
        return EvoTestSupport.skipped(helper, r.id());
    }

    private static void clean(GameTestHelper helper, RecordingPlayer... ps) {
        clearDropped(helper);
        for (RecordingPlayer p : ps) {
            EvoTestSupport.forgetAll(p);
            remove(helper, p);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
    }

    private static void setRace(RecordingPlayer p, String id) {
        RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse(id), false);
    }

    /** A consenting target of the right kind gets the skill and pays the cost after the learn; where an addon closes the learn, nothing is taken. */
    private static void grants(GameTestHelper helper, Rite r, String race) {
        if (skipped(helper, r)) {
            return;
        }
        boolean open = EvoTestSupport.learnOpen(helper, r.grants());
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            if (race != null) {
                setRace(target, race);
            }
            atNight(helper, true, () -> start(helper, r.id(), caster, r.items()));
            if (open) {
                helper.assertTrue(has(target, r.grants()), "no " + r.grants() + "; the target heard " + target.heard() + ", the caster " + caster.heard());
                helper.assertTrue(target.received(EvolutionRiteFactory.DONE), "the target was not told: " + target.heard());
                helper.assertTrue(EnergyHelper.getBaseMaxEP(target) <= EP - 200_000, "the cost was not taken: " + EnergyHelper.getBaseMaxEP(target));
                helper.assertTrue(dropped(helper, r.marker()) == 0, "a good rite gave the ingredients back");
            } else {
                helper.assertTrue(!has(target, r.grants()), r.grants() + " was learned although an addon closes it");
                helper.assertTrue(EnergyHelper.getBaseMaxEP(target) == EP, "EP changed: " + EnergyHelper.getBaseMaxEP(target));
                helper.assertTrue(target.received(NOT_READY), "the target heard " + target.heard());
                helper.assertTrue(dropped(helper, r.marker()) > 0, "the ingredients were not given back");
            }
            helper.assertTrue(open || caster.heard().equals(FactionRiteFactory.NO_EFFECT), "the performer heard " + caster.heard());
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void greatMageFollowsAscensionsRoad(GameTestHelper helper) {
        grants(helper, greatMage(), null);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void kingOfCursesFollowsAscensionsRoad(GameTestHelper helper) {
        grants(helper, kingOfCurses(), null);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void imitatorOpensWithNoSwitch(GameTestHelper helper) {
        grants(helper, imitator(), null);
    }

    /** The race link end to end: Mysticism's direwolf is the werewolf the Butcher rite asks for. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMysticismDirewolfCountsAsTheWerewolfOfTheButcherRite(GameTestHelper helper) {
        grants(helper, butcher(), "mysticism:direwolf");
    }

    /** No oracle: a target who is not a werewolf is refused, the performer hears only the common line, and the ingredients come back. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void butcherRefusesANonWerewolfWithTheCommonLine(GameTestHelper helper) {
        Rite r = butcher();
        if (skipped(helper, r)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, true, EP);
        try {
            setRace(target, "tensura:human");
            atNight(helper, true, () -> start(helper, r.id(), caster, r.items()));
            helper.assertTrue(!has(target, r.grants()), "a human was given Butcher");
            helper.assertTrue(EnergyHelper.getBaseMaxEP(target) == EP, "EP changed");
            helper.assertTrue(target.received(NOT_READY), "the target heard " + target.heard());
            helper.assertTrue(caster.heard().equals(FactionRiteFactory.NO_EFFECT), "the performer heard " + caster.heard());
            helper.assertTrue(dropped(helper, r.marker()) > 0, "the ingredients were not given back");
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }

    /** A target who does not sneak is asked, and nothing changes. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void anUnwillingTargetIsAskedAndNothingChanges(GameTestHelper helper) {
        Rite r = imitator();
        if (skipped(helper, r)) {
            return;
        }
        RecordingPlayer caster = performer(helper, Species.WITCH);
        RecordingPlayer target = onTheCircle(helper, false, EP);
        try {
            atNight(helper, true, () -> start(helper, r.id(), caster, r.items()));
            helper.assertTrue(!has(target, r.grants()), "an unwilling target was evolved");
            helper.assertTrue(target.receivedOnActionBar("supernaturalbloodlines.rite.sneak_to_evolve"), "the target was not asked: " + target.heard());
            helper.assertTrue(dropped(helper, r.marker()) > 0, "the ingredients were not given back");
        } finally {
            clean(helper, caster, target);
        }
        helper.succeed();
    }
}
