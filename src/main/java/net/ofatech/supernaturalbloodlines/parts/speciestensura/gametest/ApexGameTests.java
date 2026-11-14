package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.Env;
import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.race.impl.RaceStorage;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.handler.DeathHandler;
import io.github.manasmods.tensura.network.c2s.RequestEvolutionPacket;
import io.github.manasmods.tensura.race.RaceUtils;
import io.github.manasmods.tensura.race.template.EvolutionRequirement;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Part;
import net.ofatech.supernaturalbloodlines.core.StalePartModifiers;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.mixin.SpeciesTensuraMixinPlugin;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesLayer;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesTensuraConfig;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.apex.ApexOffer;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.apex.ApexRace;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.apex.ApexRaces;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.apex.ApexSpec;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.apex.EvolutionBoost;
import net.ofatech.supernaturalbloodlines.shared.tensura.RaceTags;

/** Apex races and the evolution bonus. Registered by the part itself, so these only exist when species_tensura is enabled. */
@PrefixGameTestTemplate(false)
public final class ApexGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final Set<String> ORIGINAL_SKILLS =
            Set.of("tensura:blood_mist", "tensura:drain", "tensura:charm", "tensura:self_regeneration");
    private static final TagKey<ManasRace> HUMAN_LIKE = raceTag("human_like");
    private static final TagKey<ManasRace> UNDEAD = raceTag("undead");
    private static final TagKey<ManasRace> NO_BLOOD = raceTag("no_blood");

    private ApexGameTests() {}

    private static TagKey<ManasRace> raceTag(String path) {
        return TagKey.create(RaceAPI.getRaceRegistryKey(), ResourceLocation.fromNamespaceAndPath("tensura", path));
    }

    private static void setRace(LivingEntity e, String id) {
        RaceAPI.getRaceFrom(e).setRace(ResourceLocation.parse(id), false);
    }

    private static String race(LivingEntity e) {
        return RaceAPI.getRaceFrom(e).getRace().map(r -> r.getRaceId().toString()).orElse(null);
    }

    private static ManasRaceInstance instance(LivingEntity e) {
        return RaceAPI.getRaceFrom(e).getRace().orElseThrow();
    }

    private static ManasRace registered(String id) {
        return RaceAPI.getRaceRegistry().get(ResourceLocation.parse(id));
    }

    private static void species(ServerPlayer p, Species s) {
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().species(s).build());
    }

    private static boolean has(ServerPlayer p, String skill) {
        return SkillAPI.getSkillsFrom(p).getSkill(ResourceLocation.parse(skill)).isPresent();
    }

    private static List<String> offered(ServerPlayer p) {
        return instance(p).getNextEvolutions(p).stream().map(r -> r.getRegistryName().toString()).toList();
    }

    private static boolean offersApex(ServerPlayer p) {
        return offered(p).stream().anyMatch(id -> id.startsWith(NS + ":"));
    }

    private static void setEnergy(ServerPlayer p, double each) {
        p.getAttribute(TensuraAttributes.MAX_MAGICULE).setBaseValue(each);
        p.getAttribute(TensuraAttributes.MAX_AURA).setBaseValue(each);
    }

    /** Tensura's own server handler for the evolution screen's "evolve" button, run on the test thread. */
    private static void pressEvolve(ServerPlayer p, String target) {
        NetworkManager.PacketContext context = (NetworkManager.PacketContext) Proxy.newProxyInstance(
                ApexGameTests.class.getClassLoader(), new Class<?>[] {NetworkManager.PacketContext.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getPlayer":
                            return p;
                        case "getEnvironment":
                            return Env.SERVER;
                        case "queue":
                            ((Runnable) args[0]).run();
                            return null;
                        default:
                            return null;
                    }
                });
        RequestEvolutionPacket.getEvolutionPacket(ResourceLocation.parse(target)).handle(context);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyApexRaceIsRegisteredAndTagged(GameTestHelper helper) {
        for (ApexSpec spec : ApexSpec.ALL) {
            ResourceLocation id = ApexRaces.id(spec);
            ManasRace race = RaceAPI.getRaceRegistry().get(id);
            helper.assertTrue(race instanceof ApexRace, id + " is not registered (" + race + ")");
            ManasRaceInstance inst = race.createDefaultInstance();
            helper.assertTrue(inst.is(HUMAN_LIKE), id + " is not human_like");
            helper.assertTrue(inst.is(UNDEAD) == spec.undead(), id + " undead tag");
            helper.assertTrue(inst.is(NO_BLOOD) == spec.undead(), id + (spec.undead() ? " is undead, so Tensura's no_blood tag (it includes #tensura:undead) must still hold: our bitable tag overrides it, the tag itself is not touched"
                    : " is not undead, so it must not carry Tensura's no_blood tag"));
            helper.assertTrue(RaceTags.isBitable(inst), id + " is not bitable: undead apex races must still be bitten");
            for (String skill : spec.intrinsicSkills()) {
                helper.assertTrue(SkillAPI.getSkillRegistry().get(ResourceLocation.parse(skill)) != null, id + " lists an unknown skill " + skill);
            }
            helper.assertTrue(race.getNextEvolutions(inst, null).isEmpty(), id + " has a next evolution");
        }
        helper.assertTrue(ApexRaces.apexFor(Species.TRIBRID).orElseThrow().equals(ResourceLocation.parse(NS + ":tribrid_ascendant")), "apexFor");
        helper.succeed();
    }

    /** Review round: Tensura's own Drain, Blood Mist and VampireRace ask RaceUtils.isBloodless, which the mixin answers false for a bitable race. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void apexRaceNotBloodlessToTensura(GameTestHelper helper) {
        int undead = 0;
        for (ApexSpec spec : ApexSpec.ALL) {
            ServerPlayer p = TestPlayers.create(helper);
            setRace(p, NS + ":" + spec.id());
            helper.assertTrue(!RaceUtils.isBloodless(p), spec.id() + " is bloodless to Tensura");
            undead += spec.undead() ? 1 : 0;
        }
        helper.assertTrue(undead == 4, "vampire-type apex races: " + undead);
        // controls: a Tensura undead race is still bloodless, and a plain human is not
        ServerPlayer ghoul = TestPlayers.create(helper);
        setRace(ghoul, "tensura:ghoul");
        helper.assertTrue(RaceUtils.isBloodless(ghoul), "control: a ghoul is not bloodless to Tensura");
        ServerPlayer human = TestPlayers.create(helper);
        setRace(human, "tensura:human");
        helper.assertTrue(!RaceUtils.isBloodless(human), "control: a human is bloodless to Tensura");
        helper.assertTrue(SpeciesTensuraMixinPlugin.raceUtilsHooked(), "the RaceUtils hooks are not in place");
        helper.succeed();
    }

    /** The EP boost modifier is saved on the player: with the part off it must be stripped on login (StalePartModifiers). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void epBoostIsStrippedWhenThePartIsOff(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        var aura = p.getAttribute(TensuraAttributes.AURA_GAIN);
        var magicule = p.getAttribute(TensuraAttributes.MAGICULE_GAIN);
        var modifier = new AttributeModifier(EvolutionBoost.ID, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        aura.addOrReplacePermanentModifier(modifier);
        magicule.addOrReplacePermanentModifier(modifier);
        StalePartModifiers.strip(p, EnumSet.of(Part.SPECIES_TENSURA));
        helper.assertTrue(aura.hasModifier(EvolutionBoost.ID) && magicule.hasModifier(EvolutionBoost.ID), "an enabled part lost its ep_boost");
        StalePartModifiers.strip(p, EnumSet.noneOf(Part.class));
        helper.assertTrue(!aura.hasModifier(EvolutionBoost.ID) && !magicule.hasModifier(EvolutionBoost.ID), "ep_boost stayed with species_tensura off");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void apexEpRequirementIsTwoMillion(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        for (ApexSpec spec : ApexSpec.ALL) {
            ApexRace race = (ApexRace) registered(NS + ":" + spec.id());
            var requirements = race.getEvolutionRequirements(race.createDefaultInstance(), p).keySet();
            helper.assertTrue(requirements.size() == 1, spec.id() + " requirements " + requirements);
            helper.assertTrue(requirements.iterator().next() instanceof EvolutionRequirement.EPRequirement ep && ep.getRequirement() == 2_000_000,
                    spec.id() + " does not ask for 2,000,000 EP: " + requirements);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void apexOfferedAtHighTierWithSpecies(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:human_saint");
        for (Species s : Species.values()) {
            species(p, s);
            var apex = ApexSpec.forSpecies(s);
            helper.assertTrue(offersApex(p) == apex.isPresent(), s + " offered " + offered(p));
            apex.ifPresent(a -> helper.assertTrue(offered(p).contains(NS + ":" + a.id()), s + " is not offered " + a.id() + ": " + offered(p)));
        }
        // tier 3 and 4 of the other lines, and the list is the race's own plus ours (divine races have none of their own)
        species(p, Species.WITCH);
        for (String id : new String[] {"tensura:divine_human", "tensura:elf_saint", "tensura:divine_fish", "tensura:spirit_beast", "tensura:divine_beast"}) {
            setRace(p, id);
            helper.assertTrue(offered(p).contains(NS + ":ancestral_witch"), id + " is not offered the apex race: " + offered(p));
        }
        setRace(p, "tensura:human_saint");
        helper.assertTrue(offered(p).contains("tensura:divine_human"), "Tensura's own evolution is gone: " + offered(p));
        helper.assertTrue(ApexOffer.hookApplied(), "the getNextEvolutions hook is not in place");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void apexNotOfferedWithoutSpecies(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:human_saint");
        helper.assertTrue(!offersApex(p), "offered without a species: " + offered(p));
        species(p, Species.POTENTIAL);
        helper.assertTrue(!offersApex(p), "offered to a potential: " + offered(p));
        species(p, Species.ORIGINAL);
        helper.assertTrue(offersApex(p), "control: not offered with a species");
        species(p, Species.NONE);
        helper.assertTrue(!offersApex(p), "still offered after the species went: " + offered(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void apexNotOfferedToNonHumanLike(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        species(p, Species.ORIGINAL);
        // below saint tier of a human-like line, and lines that are not human-like
        for (String id : new String[] {"tensura:human", "tensura:elf", "tensura:enlightened_elf", "tensura:beast_lord",
                "tensura:ghoul", "tensura:orc", "tensura:slime", "tensura:vampire", "tensura:wight"}) {
            setRace(p, id);
            helper.assertTrue(!offersApex(p), id + " is offered the apex race: " + offered(p));
        }
        // an apex race is the end of the line
        setRace(p, NS + ":primordial_vampire");
        helper.assertTrue(offered(p).isEmpty(), "an apex race has a next evolution: " + offered(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void apexEvolutionRunsThroughTensurasPacketHandler(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:human_saint");
        species(p, Species.ORIGINAL);

        setEnergy(p, 1000);
        pressEvolve(p, NS + ":primordial_vampire");
        helper.assertTrue("tensura:human_saint".equals(race(p)), "evolved without the energy: " + race(p));

        setEnergy(p, 2_000_000);
        helper.assertTrue(instance(p).getEvolutionProgress(p, registered(NS + ":primordial_vampire")) >= 100.0F, "progress "
                + instance(p).getEvolutionProgress(p, registered(NS + ":primordial_vampire")));
        pressEvolve(p, NS + ":primordial_vampire");
        helper.assertTrue((NS + ":primordial_vampire").equals(race(p)), "the race did not change: " + race(p));
        helper.assertTrue(BloodlineAccess.get(p).species() == Species.ORIGINAL, "the species changed");
        helper.assertTrue(has(p, "tensura:infinite_regeneration"), "the signature skill was not learned");
        for (String skill : ORIGINAL_SKILLS) {
            helper.assertTrue(has(p, skill), "the layer lost " + skill);
        }
        helper.assertTrue(RaceUtils.isUndead(p), "an apex vampire is not undead");

        // the server's own check: no species, no apex (a forged packet gets nothing)
        ServerPlayer forger = TestPlayers.create(helper);
        setRace(forger, "tensura:human_saint");
        setEnergy(forger, 2_000_000);
        pressEvolve(forger, NS + ":primordial_vampire");
        helper.assertTrue("tensura:human_saint".equals(race(forger)), "a player without the species evolved: " + race(forger));
        species(forger, Species.WITCH);
        pressEvolve(forger, NS + ":primordial_vampire");
        helper.assertTrue("tensura:human_saint".equals(race(forger)), "a witch took the Original's apex race: " + race(forger));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void apexKeptWhenSpeciesLost(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:elf_saint");
        species(p, Species.ORIGINAL);
        setEnergy(p, 2_000_000);
        pressEvolve(p, NS + ":primordial_vampire");
        helper.assertTrue((NS + ":primordial_vampire").equals(race(p)), "control: not evolved: " + race(p));

        species(p, Species.NONE);
        helper.assertTrue((NS + ":primordial_vampire").equals(race(p)), "losing the species changed the race: " + race(p));
        for (String skill : ORIGINAL_SKILLS) {
            helper.assertTrue(!has(p, skill), "the species-only skill " + skill + " stayed");
        }
        helper.assertTrue(BloodlineAccess.get(p).layerSkills().isEmpty(), "still tracking " + BloodlineAccess.get(p).layerSkills());
        helper.assertTrue(has(p, "tensura:infinite_regeneration"), "the race lost its own signature skill");

        species(p, Species.ORIGINAL);
        for (String skill : ORIGINAL_SKILLS) {
            helper.assertTrue(has(p, skill), "regaining the species did not bring back " + skill);
        }
        helper.assertTrue((NS + ":primordial_vampire").equals(race(p)), "regaining the species changed the race: " + race(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void apexHookMissingIsHarmless(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:human_saint");
        species(p, Species.ORIGINAL);
        helper.assertTrue(offersApex(p), "control: the apex race is not offered");

        // the same answer the player gets when the mixin did not apply: Tensura's own list, nothing of ours
        SpeciesTensuraConfig.overrideApexEnabled(false);
        try {
            helper.assertTrue(!offersApex(p), "offered with the hook off: " + offered(p));
            helper.assertTrue(offered(p).contains("tensura:divine_human"), "Tensura's own list changed: " + offered(p));
            setEnergy(p, 2_000_000);
            pressEvolve(p, NS + ":primordial_vampire");
            helper.assertTrue("tensura:human_saint".equals(race(p)), "evolved with the hook off: " + race(p));
            // the species keep working
            species(p, Species.NONE);
            species(p, Species.TRIBRID);
            SpeciesLayer.sync(p);
            helper.assertTrue(has(p, "tensura:beast_transformation") && has(p, "tensura:drain"), "the layer stopped working");
        } finally {
            SpeciesTensuraConfig.overrideApexEnabled(null);
        }
        helper.assertTrue(offersApex(p), "the apex race did not come back");
        helper.succeed();
    }

    /** What Tensura adds to the base max aura and magicule when {@code gainer} gets {@code ep} EP from killing {@code victim}. */
    private static double[] epGained(GameTestHelper helper, ServerPlayer gainer, double ep) {
        double aura = gainer.getAttribute(TensuraAttributes.MAX_AURA).getBaseValue();
        double magicule = gainer.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue();
        DeathHandler.gainEPEntity(gainer, helper.spawnWithNoFreeWill(EntityType.PIG, 1, 2, 1), ep);
        return new double[] {gainer.getAttribute(TensuraAttributes.MAX_AURA).getBaseValue() - aura,
                gainer.getAttribute(TensuraAttributes.MAX_MAGICULE).getBaseValue() - magicule};
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void boostAddsQuarterEpForSpeciesPlayers(GameTestHelper helper) {
        ServerPlayer original = TestPlayers.create(helper);
        setRace(original, "tensura:human");
        species(original, Species.ORIGINAL);
        ServerPlayer control = TestPlayers.create(helper);
        setRace(control, "tensura:human");

        double[] plain = epGained(helper, control, 1000);
        helper.assertTrue(plain[0] > 0 && plain[1] > 0, "control: no EP was credited " + plain[0] + "/" + plain[1]);
        double[] boosted = epGained(helper, original, 1000);
        helper.assertTrue(Math.abs(boosted[0] - EvolutionBoost.boostedEp(original, plain[0])) < 1e-6
                && Math.abs(boosted[1] - EvolutionBoost.boostedEp(original, plain[1])) < 1e-6,
                "credited " + boosted[0] + "/" + boosted[1] + " from " + plain[0] + "/" + plain[1] + " (want x1.25)");
        helper.assertTrue(Math.abs(boosted[0] / plain[0] - 1.25) < 1e-6, "aura ratio " + boosted[0] / plain[0]);
        helper.assertTrue(Math.abs(boosted[1] / plain[1] - 1.25) < 1e-6, "magicule ratio " + boosted[1] / plain[1]);

        // the bonus follows the species, and a player with a species but an empty layer gets none
        species(original, Species.NONE);
        double[] lost = epGained(helper, original, 1000);
        helper.assertTrue(Math.abs(lost[0] - plain[0]) < 1e-6 && Math.abs(lost[1] - plain[1]) < 1e-6,
                "the bonus stayed after the species went: " + lost[0] + "/" + lost[1]);
        species(original, Species.TRIBRID);
        double[] back = epGained(helper, original, 1000);
        helper.assertTrue(Math.abs(back[0] / plain[0] - 1.25) < 1e-6, "the bonus did not come back: " + back[0]);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void boostNoLongerRaisesEvolutionProgress(GameTestHelper helper) {
        ServerPlayer original = TestPlayers.create(helper);
        setRace(original, "tensura:elf");
        species(original, Species.ORIGINAL);
        ServerPlayer control = TestPlayers.create(helper);
        setRace(control, "tensura:elf");
        ManasRace target = registered("tensura:enlightened_elf");

        for (double each : new double[] {1000, 40_000, 100_000_000}) {
            setEnergy(original, each);
            setEnergy(control, each);
            float raw = instance(control).getEvolutionProgress(control, target);
            helper.assertTrue(raw > 0, "no progress at " + each);
            helper.assertTrue(instance(original).getEvolutionProgress(original, target) == raw,
                    "progress " + instance(original).getEvolutionProgress(original, target) + " against " + raw + " at " + each);
            helper.assertTrue(raw == instance(original).getRace().getEvolutionProgress(instance(original), original, target),
                    "the species player's progress is not Tensura's own at " + each);
        }
        helper.succeed();
    }

    /** ManasCore does not fall back for a saved race that is no longer registered (it throws), see docs/parts/species_tensura.md. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void manasCoreThrowsForAnUnregisteredSavedRace(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:human");
        CompoundTag saved = new CompoundTag();
        CompoundTag raceTag = new CompoundTag();
        raceTag.putString("race", NS + ":not_registered_any_more");
        saved.put("race_key", raceTag);
        boolean threw = false;
        try {
            ((RaceStorage) RaceAPI.getRaceFrom(p)).load(saved);
        } catch (NullPointerException expected) {
            threw = true;
        }
        helper.assertTrue(threw, "ManasCore now falls back for an unregistered race: the always-registered apex races "
                + "(SupernaturalBloodlines constructor) are no longer needed");
        helper.assertTrue("tensura:human".equals(race(p)), "the failed load changed the race: " + race(p));
        helper.succeed();
    }
}
