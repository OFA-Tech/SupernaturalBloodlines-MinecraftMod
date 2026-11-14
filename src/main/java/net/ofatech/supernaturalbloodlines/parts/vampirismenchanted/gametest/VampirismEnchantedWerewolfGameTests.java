package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.werewolves.api.WReference;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import java.util.List;
import java.util.function.Consumer;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;

import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.*;

/**
 * The rites that need Werewolves classes. Registered by the part only when Werewolves is loaded, so nothing here is
 * ever linked without it.
 */
@PrefixGameTestTemplate(false)
public final class VampirismEnchantedWerewolfGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String NEEDS_FULL_MOON = "supernaturalbloodlines.rite.needs_full_moon";
    private static final long FULL_MOON_CYCLE = 24000L * 8;

    private VampirismEnchantedWerewolfGameTests() {
    }

    private static IFactionPlayerHandler handler(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p);
    }

    private static List<ItemStack> silver() {
        return List.of(new ItemStack(item("werewolves:silver_ingot"), 2));
    }

    private static boolean isWolf(ServerPlayer p) {
        return handler(p).getCurrentFaction() == WReference.WEREWOLF_FACTION;
    }

    /** Runs {@code body} with the world clock on a full moon night (phase 0, 18000) or a new moon (phase 4), then restores it. */
    private static void atMoon(GameTestHelper helper, boolean full, Runnable body) {
        ServerLevel level = helper.getLevel();
        long before = level.getDayTime();
        long base = before - Math.floorMod(before, FULL_MOON_CYCLE);
        level.setDayTime(full ? base + 18000L : base + 24000L * 4);
        try {
            helper.assertTrue((level.getMoonPhase() == 0) == full, "could not set the moon, phase is " + level.getMoonPhase());
            body.run();
        } finally {
            level.setDayTime(before);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void silverMoonCuresPlainWerewolf(GameTestHelper helper) {
        RecordingPlayer caster = RecordingPlayer.create(helper);
        place(helper, caster);
        try {
            helper.assertTrue(handler(caster).setFactionAndLevel(WReference.WEREWOLF_FACTION, 2), "could not make a werewolf");
            atMoon(helper, true, () -> start(helper, "silver_moon", caster, silver()));
            helper.assertTrue(handler(caster).getCurrentFaction() == null, "still in faction " + handler(caster).getCurrentFaction());
            helper.assertTrue(!caster.received(NEEDS_FULL_MOON), "a full moon was refused");
            helper.assertTrue(dropped(helper, "werewolves:silver_ingot") == 0, "a good cure gave the ingredients back");
        } finally {
            remove(helper, caster);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void silverMoonOnHybridMakesPlainVampire(GameTestHelper helper) {
        RecordingPlayer hybrid = RecordingPlayer.create(helper);
        place(helper, hybrid);
        try {
            // a hybrid is a werewolf-faction player whose species layer is HYBRID
            helper.assertTrue(handler(hybrid).setFactionAndLevel(WReference.WEREWOLF_FACTION, 2), "could not make a werewolf");
            BloodlineAccess.set(hybrid, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            atMoon(helper, true, () -> start(helper, "silver_moon", hybrid, silver()));
            helper.assertTrue(handler(hybrid).getCurrentFaction() == VReference.VAMPIRE_FACTION,
                    "the hybrid ended in faction " + handler(hybrid).getCurrentFaction());
            helper.assertTrue(handler(hybrid).getCurrentLevel() >= 1, "the new vampire has level " + handler(hybrid).getCurrentLevel());
            helper.assertTrue(BloodlineAccess.get(hybrid).species() == Species.NONE, "species is " + BloodlineAccess.get(hybrid).species());
        } finally {
            remove(helper, hybrid);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void silverMoonOutsideFullMoonIsRefused(GameTestHelper helper) {
        clearDropped(helper);
        RecordingPlayer caster = RecordingPlayer.create(helper);
        place(helper, caster);
        try {
            helper.assertTrue(handler(caster).setFactionAndLevel(WReference.WEREWOLF_FACTION, 2), "could not make a werewolf");
            atMoon(helper, false, () -> start(helper, "silver_moon", caster, silver()));
            helper.assertTrue(caster.receivedOnActionBar(NEEDS_FULL_MOON), "the caster was not told it needs a full moon");
            helper.assertTrue(isWolf(caster), "the werewolf was cured without a full moon");
            helper.assertTrue(dropped(helper, "werewolves:silver_ingot") == 2, "the silver was not given back");
        } finally {
            clearDropped(helper);
            remove(helper, caster);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void silverMoonOnNonWerewolfFails(GameTestHelper helper) {
        clearDropped(helper);
        RecordingPlayer vampire = RecordingPlayer.create(helper);
        place(helper, vampire);
        try {
            helper.assertTrue(handler(vampire).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3), "could not make a vampire");
            atMoon(helper, true, () -> start(helper, "silver_moon", vampire, silver()));
            helper.assertTrue(vampire.receivedOnActionBar(VampirismEnchantedGameTests.WRONG_FACTION), "no wrong faction message");
            helper.assertTrue(handler(vampire).getCurrentFaction() == VReference.VAMPIRE_FACTION, "the vampire lost the faction");
            helper.assertTrue(dropped(helper, "werewolves:silver_ingot") == 2, "the silver was not given back");
        } finally {
            clearDropped(helper);
            remove(helper, vampire);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void silverMoonOnHereticHasNoEffect(GameTestHelper helper) {
        clearDropped(helper);
        RecordingPlayer heretic = RecordingPlayer.create(helper);
        place(helper, heretic);
        try {
            BloodlineAccess.set(heretic, BloodlineState.NONE.toBuilder().species(Species.HERETIC).build());
            atMoon(helper, true, () -> start(helper, "silver_moon", heretic, silver()));
            helper.assertTrue(heretic.receivedOnActionBar("supernaturalbloodlines.rite.no_effect"), "no no_effect message");
            helper.assertTrue(BloodlineAccess.get(heretic).species() == Species.HERETIC, "the heretic changed");
            helper.assertTrue(dropped(helper, "werewolves:silver_ingot") == 2, "the silver was not given back");
        } finally {
            clearDropped(helper);
            remove(helper, heretic);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void deniedJoinChangesNothing(GameTestHelper helper) {
        clearDropped(helper);
        RecordingPlayer hybrid = RecordingPlayer.create(helper);
        place(helper, hybrid);
        Consumer<PlayerFactionEvent.CanJoinFaction> deny = ev -> {
            if (ev.getFactionToJoin() == VReference.VAMPIRE_FACTION) {
                ev.setBehavior(PlayerFactionEvent.CanJoinFaction.Behavior.DENY);
            }
        };
        try {
            helper.assertTrue(handler(hybrid).setFactionAndLevel(WReference.WEREWOLF_FACTION, 2), "could not make a werewolf");
            BloodlineAccess.set(hybrid, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            // read back: with species_tensura on, the layer has already added the skills it tracks
            BloodlineState hybridState = BloodlineAccess.get(hybrid);
            NeoForge.EVENT_BUS.addListener(deny);
            try {
                atMoon(helper, true, () -> start(helper, "silver_moon", hybrid, silver()));
            } finally {
                NeoForge.EVENT_BUS.unregister(deny);
            }
            helper.assertTrue(hybrid.receivedOnActionBar("supernaturalbloodlines.rite.join_denied"), "no join_denied message");
            helper.assertTrue(BloodlineAccess.get(hybrid).equals(hybridState), "the state changed: " + BloodlineAccess.get(hybrid));
            helper.assertTrue(isWolf(hybrid) && handler(hybrid).getCurrentLevel() == 2, "the werewolf faction or level changed");
            helper.assertTrue(dropped(helper, "werewolves:silver_ingot") == 2, "the silver was not given back");
        } finally {
            clearDropped(helper);
            remove(helper, hybrid);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void cleansingKeepsHybridAWerewolf(GameTestHelper helper) {
        RecordingPlayer hybrid = RecordingPlayer.create(helper);
        place(helper, hybrid);
        try {
            helper.assertTrue(handler(hybrid).setFactionAndLevel(WReference.WEREWOLF_FACTION, 2), "could not make a werewolf");
            BloodlineAccess.set(hybrid, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
            start(helper, "cleansing_blood", hybrid, List.of(new ItemStack(item("vampirism:garlic"))));
            helper.assertTrue(!hybrid.received(VampirismEnchantedGameTests.WRONG_FACTION), "a hybrid was refused as the wrong faction");
            helper.assertTrue(BloodlineAccess.get(hybrid).species() == Species.NONE, "species is " + BloodlineAccess.get(hybrid).species());
            helper.assertTrue(isWolf(hybrid), "the cured hybrid is no longer a werewolf");
        } finally {
            remove(helper, hybrid);
        }
        helper.succeed();
    }
}
