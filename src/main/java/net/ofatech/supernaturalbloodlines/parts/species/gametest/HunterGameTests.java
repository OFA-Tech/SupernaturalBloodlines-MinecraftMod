package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import java.util.List;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.FiveRoster;
import net.ofatech.supernaturalbloodlines.parts.species.hunter.HunterEvents;

import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.*;

/** Hunters of the Five: the roster and the Rite of the Five. */
@PrefixGameTestTemplate(false)
public final class HunterGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private HunterGameTests() {}

    private static IFactionPlayerHandler handler(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p);
    }

    private static FiveRoster roster(ServerPlayer p) {
        return FiveRoster.get(p.serverLevel());
    }

    private static BloodlineState state(Species s) {
        return BloodlineState.NONE.toBuilder().species(s).build();
    }

    private static List<ItemStack> ingredients() {
        return List.of(new ItemStack(item("minecraft:candle"), 5), new ItemStack(item("minecraft:iron_ingot")),
                new ItemStack(item("minecraft:book")), new ItemStack(item("enchanted:taglock")));
    }

    private static RecordingPlayer hunter(GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        helper.assertTrue(handler(p).setFactionAndLevel(VReference.HUNTER_FACTION, 1), "could not make a hunter");
        return p;
    }

    private static void reset(ServerPlayer p) {
        BloodlineAccess.set(p, BloodlineState.NONE);
        handler(p).leaveFaction(false);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void riteOfTheFiveMarksHunter(GameTestHelper helper) {
        RiteType type = type(helper, "rite_of_the_five");
        helper.assertTrue(type != null && type.getPower() == 10000, "rite_of_the_five missing or wrong power");
        helper.assertTrue(type.getItems().size() == 4, "rite_of_the_five has " + type.getItems().size() + " ingredients");
        RecordingPlayer p = hunter(helper);
        place(helper, p);
        int before = roster(p).size();
        SpeciesConfig.overrideMaxFive(before + 1);
        try {
            start(helper, "rite_of_the_five", p, ingredients());
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(s.species() == Species.HUNTER_OF_THE_FIVE, "species is " + s.species());
            helper.assertTrue(s.markKills() == 0, "markKills is " + s.markKills());
            helper.assertTrue(roster(p).contains(p.getUUID()) && roster(p).size() == before + 1, "roster size " + roster(p).size());
            helper.assertTrue(dropped(helper, "minecraft:book") == 0, "a good rite gave the book back");
        } finally {
            SpeciesConfig.overrideMaxFive(null);
            remove(helper, p);
            reset(p);
        }
        helper.assertTrue(roster(p).size() == before, "the roster kept the hunter after the reset");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void riteRefusedWhenFiveFull(GameTestHelper helper) {
        RecordingPlayer first = hunter(helper);
        RecordingPlayer second = hunter(helper);
        int before = roster(first).size();
        SpeciesConfig.overrideMaxFive(before + 1);
        try {
            place(helper, first);
            start(helper, "rite_of_the_five", first, ingredients());
            helper.assertTrue(BloodlineAccess.get(first).species() == Species.HUNTER_OF_THE_FIVE, "the first rite did not work");
            remove(helper, first);

            place(helper, second);
            clearDropped(helper);
            start(helper, "rite_of_the_five", second, ingredients());
            String key = Outcome.REFUSAL_PREFIX + "five_full";
            helper.assertTrue(second.receivedOnActionBar(key), "the caster was not told the Five are complete");
            helper.assertTrue(BloodlineAccess.get(second).species() == Species.NONE, "species is " + BloodlineAccess.get(second).species());
            helper.assertTrue(!roster(second).contains(second.getUUID()), "a refused rite filled a slot");
            helper.assertTrue(dropped(helper, "minecraft:book") == 1, "the ingredients were not given back");
        } finally {
            SpeciesConfig.overrideMaxFive(null);
            clearDropped(helper);
            remove(helper, second);
            reset(first);
            reset(second);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void rosterFollowsTheSpecies(GameTestHelper helper) {
        RecordingPlayer p = hunter(helper);
        int before = roster(p).size();
        try {
            BloodlineAccess.set(p, state(Species.POTENTIAL));
            helper.assertTrue(roster(p).size() == before, "a potential took a slot");
            BloodlineAccess.set(p, state(Species.HUNTER_OF_THE_FIVE));
            helper.assertTrue(roster(p).size() == before + 1, "a set state did not fill the slot");
            BloodlineAccess.set(p, state(Species.NONE));
            helper.assertTrue(roster(p).size() == before, "losing the species kept the slot");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void leavingHunterFactionFreesSlot(GameTestHelper helper) {
        RecordingPlayer p = hunter(helper);
        int before = roster(p).size();
        try {
            BloodlineAccess.set(p, state(Species.HUNTER_OF_THE_FIVE));
            helper.assertTrue(roster(p).contains(p.getUUID()), "not in the roster");
            handler(p).leaveFaction(false);
            helper.assertTrue(!roster(p).contains(p.getUUID()) && roster(p).size() == before, "the slot is still taken");
            helper.assertTrue(BloodlineAccess.get(p).species() == Species.NONE, "species is " + BloodlineAccess.get(p).species());
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void logoutKeepsSlot(GameTestHelper helper) {
        RecordingPlayer p = hunter(helper);
        int before = roster(p).size();
        try {
            BloodlineAccess.set(p, state(Species.HUNTER_OF_THE_FIVE));
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(p));
            helper.assertTrue(roster(p).contains(p.getUUID()) && roster(p).size() == before + 1, "logging out freed the slot");
            helper.assertTrue(BloodlineAccess.get(p).species() == Species.HUNTER_OF_THE_FIVE, "logging out changed the species");

            // login heals a roster that lost them (the file, or a state set while the part was off)
            roster(p).remove(p.getUUID());
            HunterEvents.sync(p);
            helper.assertTrue(roster(p).contains(p.getUUID()), "login did not restore the slot");
            HunterEvents.sync(p);
            helper.assertTrue(roster(p).size() == before + 1, "a second sync counted them twice");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void contextReadsTheRosterAndMaxFive(GameTestHelper helper) {
        RecordingPlayer p = hunter(helper);
        int before = SpeciesContext.of(p, null).fiveCount();
        try {
            helper.assertTrue(SpeciesContext.of(p, null).maxFive() == 5 && SpeciesConfig.maxFive() == 5, "default max_five is " + SpeciesConfig.maxFive());
            BloodlineAccess.set(p, state(Species.HUNTER_OF_THE_FIVE));
            BloodlineContext ctx = SpeciesContext.of(p, null);
            helper.assertTrue(ctx.fiveCount() == before + 1, "fiveCount is " + ctx.fiveCount());
            SpeciesConfig.overrideMaxFive(2);
            helper.assertTrue(SpeciesContext.of(p, null).maxFive() == 2, "max_five override was not read");
        } finally {
            SpeciesConfig.overrideMaxFive(null);
            reset(p);
        }
        helper.succeed();
    }
}
