package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerGroup;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerKind;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.TensuraProvider;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelCatalogue;

/**
 * The wheel's Tensura entries: skills, spells, battlewill arts, the race ability, and the kind rule pinned with real ids (the table was read from Tensura 2.0.1.5 by
 * {@code TensuraKindProbe}). Needs Tensura; skips cleanly without it.
 */
@PrefixGameTestTemplate(false)
public final class WheelTensuraGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private WheelTensuraGameTests() {
    }

    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("tensura", path);
    }

    static Optional<WheelEntry> entry(ServerPlayer p, ResourceLocation id) {
        return WheelCatalogueGameTests.find(p, PowerSource.TENSURA, id);
    }

    private static WheelEntry learnedEntry(GameTestHelper helper, ServerPlayer p, String path) {
        WheelTestKit.learn(p, id(path));
        Optional<WheelEntry> e = entry(p, id(path));
        helper.assertTrue(e.isPresent(), "tensura:" + path + " is learned but not listed");
        return e.get();
    }

    /** The reflection that tells a held skill from an instant one runs once per class, not once per build. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theKindReflectionIsCachedPerClass(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        for (String path : List.of("coercion", "gravity_flight", "heavy_slash", "magic_wall", "gravity_field")) {
            WheelTestKit.learn(p, id(path));
        }
        WheelCatalogue.build(p);
        int warm = TensuraProvider.REFLECTIONS.get();
        for (int i = 0; i < 5; i++) {
            WheelCatalogue.build(p);
        }
        helper.assertTrue(TensuraProvider.REFLECTIONS.get() == warm, "class walks grew from " + warm + " to " + TensuraProvider.REFLECTIONS.get() + " over five builds");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void coercionIsInstant(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        WheelEntry e = learnedEntry(helper, p, "coercion");
        helper.assertTrue(e.kind() == PowerKind.INSTANT && e.group() == PowerGroup.SKILLS, "coercion: " + e.kind() + " " + e.group());
        helper.assertTrue(e.slottable(), "coercion cannot be slotted");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void selfRegenerationIsToggle(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        WheelEntry e = learnedEntry(helper, p, "self_regeneration");
        helper.assertTrue(e.kind() == PowerKind.TOGGLE && !e.state().on(), "self_regeneration: " + e.kind() + " on=" + e.state().on());
        helper.assertTrue(!e.slottable(), "a passive switch is not slottable");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void gravityFlightIsHeld(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(learnedEntry(helper, p, "gravity_flight").kind() == PowerKind.HELD, "gravity_flight is not held");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void passiveSkillIsNotListed(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        // a resistance is always on (ResistSkill); eye of truth cannot be slotted or switched: neither is a power
        WheelTestKit.learn(p, id("cold_resistance"));
        WheelTestKit.learn(p, id("eye_of_truth"));
        helper.assertTrue(entry(p, id("cold_resistance")).isEmpty(), "a resistance is listed");
        helper.assertTrue(entry(p, id("eye_of_truth")).isEmpty(), "eye of truth is listed");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void multiModeSkillHasOneEntryWithModeNames(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        learnedEntry(helper, p, "gravity_field");
        List<WheelEntry> all = WheelCatalogue.build(p).stream().filter(e -> e.ref().id().equals(id("gravity_field"))).toList();
        helper.assertTrue(all.size() == 1, "entries for gravity_field: " + all.size());
        WheelEntry e = all.get(0);
        helper.assertTrue(e.state().modeCount() == 3 && e.modeNames().size() == 3, "modes: " + e.state().modeCount() + " names " + e.modeNames().size());
        helper.assertTrue(!e.modeName().getString().isEmpty(), "no current mode name");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unlearnedSkillIsNotListed(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(entry(p, id("coercion")).isEmpty(), "an unlearned skill is listed");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void namespaceInfinityIsListedEvenIfNotSlottable(GameTestHelper helper) {
        ResourceLocation stone = null;
        for (ManasSkill s : SkillAPI.getSkillRegistry()) {
            ResourceLocation rid = SkillAPI.getSkillRegistry().getId(s);
            if (rid != null && rid.getNamespace().equals("infinity")) {
                stone = rid;
                break;
            }
        }
        if (stone == null) {
            helper.succeed(); // Infinity Stones is not installed: nothing to list
            return;
        }
        ServerPlayer p = TestPlayers.create(helper);
        WheelTestKit.learn(p, stone);
        helper.assertTrue(entry(p, stone).isPresent(), stone + " is not listed");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSpellIsInTheMagicGroup(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        WheelEntry e = learnedEntry(helper, p, "magic_wall");
        helper.assertTrue(e.group() == PowerGroup.MAGIC, "a spell is in " + e.group());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSpellWithACastTimeIsHeld(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(learnedEntry(helper, p, "magic_wall").kind() == PowerKind.HELD, "a spell with a cast time is not HELD");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMasteredSpellUnderChantAnnulmentIsInstant(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        ManasSkillInstance chant = WheelTestKit.learn(p, id("chant_annulment"));
        chant.setToggled(true);
        ManasSkillInstance wall = WheelTestKit.learn(p, id("magic_wall"));
        wall.setMastery(wall.getMaxMastery());
        helper.assertTrue(entry(p, id("magic_wall")).orElseThrow().kind() == PowerKind.INSTANT, "an instant-cast spell is not INSTANT");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anArtIsInTheBattlewillGroup(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        WheelEntry slash = learnedEntry(helper, p, "heavy_slash");
        helper.assertTrue(slash.group() == PowerGroup.BATTLEWILL && slash.kind() == PowerKind.INSTANT, "heavy_slash: " + slash.group() + " " + slash.kind());
        WheelEntry flight = learnedEntry(helper, p, "air_flight");
        helper.assertTrue(flight.group() == PowerGroup.BATTLEWILL && flight.kind() == PowerKind.HELD, "air_flight: " + flight.group() + " " + flight.kind());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void silencedPlayerSeesSpellsLocked(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        learnedEntry(helper, p, "magic_wall");
        helper.assertTrue(entry(p, id("magic_wall")).orElseThrow().state().lock() == LockReason.NONE || entry(p, id("magic_wall")).orElseThrow().state().lock() == LockReason.NEEDS_SLOT,
                "setup: the spell is locked before the silence");
        p.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(TensuraMobEffects.SILENCE.get()), 200));
        LockReason lock = entry(p, id("magic_wall")).orElseThrow().state().lock();
        helper.assertTrue(lock == LockReason.REFUSED_NOW, "a silenced player's spell is " + lock);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyTheHighestRegenerationTierIsListed(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        WheelTestKit.learn(p, id("self_regeneration"));
        helper.assertTrue(entry(p, id("self_regeneration")).isPresent(), "setup: the first tier is not listed alone");
        WheelTestKit.learn(p, id("ultraspeed_regeneration"));
        helper.assertTrue(entry(p, id("self_regeneration")).isEmpty() && entry(p, id("ultraspeed_regeneration")).isPresent(), "tier 2 does not hide tier 1");
        WheelTestKit.learn(p, id("infinite_regeneration"));
        helper.assertTrue(entry(p, id("self_regeneration")).isEmpty() && entry(p, id("ultraspeed_regeneration")).isEmpty() && entry(p, id("infinite_regeneration")).isPresent(),
                "only the highest tier may be listed");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void raceAbilityEntryIsHeldInSkills(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        RaceAPI.getRaceFrom(p).setRace(id("harpy"), false);
        Optional<WheelEntry> e = entry(p, TensuraProvider.RACE_ABILITY);
        helper.assertTrue(e.isPresent(), "a harpy has no race ability entry");
        helper.assertTrue(e.get().kind() == PowerKind.HELD && e.get().group() == PowerGroup.SKILLS, "race ability: " + e.get().kind() + " " + e.get().group());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void playerWithoutARaceAbilityHasNoRaceEntry(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(entry(p, TensuraProvider.RACE_ABILITY).isEmpty(), "a player with no race has a race entry");
        RaceAPI.getRaceFrom(p).setRace(id("beastfolk"), false);
        helper.assertTrue(entry(p, TensuraProvider.RACE_ABILITY).isEmpty(), "a beastfolk has a race ability entry");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlayerWhoCannotActSeesTensuraPowersLocked(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        learnedEntry(helper, p, "coercion");
        p.setHealth(0f);
        helper.assertTrue(entry(p, id("coercion")).orElseThrow().state().lock() != LockReason.NONE, "a dead player's skill is not locked");
        helper.succeed();
    }

    /** The registry names the wheel's kind table is pinned on: a Tensura update that renames one fails here, not in a player's game. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void theKindTableIdsStillExist(GameTestHelper helper) {
        for (String path : List.of("coercion", "self_regeneration", "ultraspeed_regeneration", "infinite_regeneration", "gravity_flight", "gravity_field", "cold_resistance",
                "eye_of_truth", "magic_wall", "chant_annulment", "heavy_slash", "air_flight", "harpy", "beastfolk")) {
            ResourceLocation rid = id(path);
            helper.assertTrue(SkillAPI.getSkillRegistry().get(rid) != null || RaceAPI.getRaceRegistry().get(rid) != null, rid + " is gone");
        }
        helper.succeed();
    }
}
