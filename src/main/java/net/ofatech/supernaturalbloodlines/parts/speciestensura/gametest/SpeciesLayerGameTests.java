package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import io.github.manasmods.tensura.race.RaceUtils;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesLayer;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.TensuraHumanLikeCheck;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.HolyDamageBridge;
import net.ofatech.supernaturalbloodlines.shared.tensura.LayerQuery;

/** Registered by the part itself (RegisterGameTestsEvent), so these only exist when species_tensura is enabled. */
@PrefixGameTestTemplate(false)
public final class SpeciesLayerGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final Set<String> ORIGINAL_SKILLS =
            Set.of("tensura:blood_mist", "tensura:drain", "tensura:charm", "tensura:self_regeneration");

    private SpeciesLayerGameTests() {}

    private static void setRace(LivingEntity e, String id) {
        RaceAPI.getRaceFrom(e).setRace(ResourceLocation.parse(id), false);
    }

    private static String race(LivingEntity e) {
        return RaceAPI.getRaceFrom(e).getRace().map(r -> r.getRaceId().toString()).orElse(null);
    }

    private static void species(ServerPlayer p, Species s) {
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().species(s).build());
    }

    private static boolean has(ServerPlayer p, String skill) {
        return SkillAPI.getSkillsFrom(p).getSkill(ResourceLocation.parse(skill)).isPresent();
    }

    private static void learn(ServerPlayer p, String skill) {
        ManasSkill s = SkillAPI.getSkillRegistry().get(ResourceLocation.parse(skill));
        ManasSkillInstance instance = s.createDefaultInstance();
        instance.getOrCreateTag().putBoolean("NoMagiculeCost", true);
        SkillAPI.getSkillsFrom(p).learnSkill(instance);
    }

    private static AttributeModifier modifier(ServerPlayer p, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance a = p.getAttribute(attribute);
        return a == null ? null : a.getModifier(id);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void originalElfKeepsRaceAndGetsSkills(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:elf");
        species(p, Species.ORIGINAL);

        helper.assertTrue("tensura:elf".equals(race(p)), "the race changed to " + race(p));
        for (String skill : ORIGINAL_SKILLS) {
            helper.assertTrue(has(p, skill), "missing skill " + skill);
        }
        helper.assertTrue(BloodlineAccess.get(p).layerSkills().equals(ORIGINAL_SKILLS),
                "tracked " + BloodlineAccess.get(p).layerSkills());
        AttributeModifier m = modifier(p, TensuraAttributes.MAX_MAGICULE, SpeciesLayer.MAGICULE_ID);
        helper.assertTrue(m != null && Math.abs(m.amount() - 0.20) < 1e-9
                && m.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE, "magicule modifier " + m);
        helper.assertTrue(RaceUtils.isUndead(p), "an Original is not undead for Tensura");
        helper.assertTrue(RaceUtils.isAffectedByHolyExposure(p), "an Original is not affected by holy exposure");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void layerSurvivesTensuraEvolution(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:elf");
        species(p, Species.ORIGINAL);

        setRace(p, "tensura:enlightened_elf");
        for (String skill : ORIGINAL_SKILLS) {
            helper.assertTrue(has(p, skill), "evolving lost " + skill);
        }
        helper.assertTrue(BloodlineAccess.get(p).species() == Species.ORIGINAL, "the species changed");
        helper.assertTrue(modifier(p, TensuraAttributes.MAX_MAGICULE, SpeciesLayer.MAGICULE_ID) != null, "evolving lost the modifier");

        SpeciesLayer.sync(p);
        helper.assertTrue("tensura:enlightened_elf".equals(race(p)), "race " + race(p));
        helper.assertTrue(has(p, "tensura:drain") && BloodlineAccess.get(p).layerSkills().equals(ORIGINAL_SKILLS), "a sync after evolving changed the layer");
        helper.assertTrue(RaceUtils.isUndead(p), "no longer undead after evolving");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void preexistingSkillKept(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:elf");
        learn(p, "tensura:drain");

        species(p, Species.ORIGINAL);
        helper.assertTrue(!BloodlineAccess.get(p).layerSkills().contains("tensura:drain"), "tracked a skill the player already had");
        helper.assertTrue(BloodlineAccess.get(p).layerSkills().contains("tensura:blood_mist"), "did not track the granted skill");

        species(p, Species.NONE);
        helper.assertTrue(has(p, "tensura:drain"), "the layer took away a skill the player had before it");
        helper.assertTrue(!has(p, "tensura:blood_mist"), "the granted skill stayed");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void curedLayerRemovesOnlyGrantedSkills(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:elf");
        species(p, Species.ORIGINAL);
        learn(p, "tensura:strength"); // learned later, from somewhere else

        species(p, Species.NONE);
        for (String skill : ORIGINAL_SKILLS) {
            helper.assertTrue(!has(p, skill), "still has " + skill + " after the cure");
        }
        helper.assertTrue(has(p, "tensura:strength"), "the cure took a skill the layer never granted");
        helper.assertTrue(BloodlineAccess.get(p).layerSkills().isEmpty(), "still tracking " + BloodlineAccess.get(p).layerSkills());
        helper.assertTrue(modifier(p, TensuraAttributes.MAX_MAGICULE, SpeciesLayer.MAGICULE_ID) == null, "the modifier stayed");
        helper.assertTrue(!RaceUtils.isUndead(p), "still undead after the cure");
        helper.succeed();
    }

    private static DamageSource tensuraHoly(GameTestHelper helper) {
        return helper.getLevel().damageSources().source(TensuraDamageTypes.HOLY_DAMAGE);
    }

    private static float healthLost(ServerPlayer p, DamageSource source, float amount) {
        p.setHealth(p.getMaxHealth());
        float before = p.getHealth();
        p.invulnerableTime = 0;
        p.hurt(source, amount);
        return before - p.getHealth();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void vampireLayerTakesTensuraHolyDamage(GameTestHelper helper) {
        ServerPlayer vampire = TestPlayers.createVulnerable(helper);
        setRace(vampire, "tensura:elf");
        VampirismAPI.factionPlayerHandler(vampire).setFactionAndLevel(VReference.VAMPIRE_FACTION, 5);
        helper.assertTrue(BloodlineAccess.get(vampire).species() == Species.NONE, "control: the vampire has a species");
        helper.assertTrue(RaceUtils.isUndead(vampire), "a plain Vampirism vampire is not undead for Tensura");
        helper.assertTrue(has(vampire, "tensura:drain"), "a plain vampire did not get drain");
        helper.assertTrue(BloodlineAccess.get(vampire).layerSkills().equals(Set.of("tensura:drain")), "tracked " + BloodlineAccess.get(vampire).layerSkills());

        ServerPlayer control = TestPlayers.createVulnerable(helper);
        setRace(control, "tensura:elf");
        // Tensura's own x3 for undead is applied after the damage event, so measure what the health lost
        float base = healthLost(control, tensuraHoly(helper), 2.0F);
        float lost = healthLost(vampire, tensuraHoly(helper), 2.0F);
        helper.assertTrue(base > 0, "control: not hurt by Tensura holy damage (" + base + ")");
        // a layered player who is not a faction vampire shows what Tensura's own handling does: x3 for undead
        ServerPlayer original = TestPlayers.createVulnerable(helper);
        setRace(original, "tensura:elf");
        species(original, Species.ORIGINAL);
        float originalLost = healthLost(original, tensuraHoly(helper), 2.0F);
        helper.assertTrue(Math.abs(originalLost - base * 3.0F) < 0.01F, "a layered Original lost " + originalLost + ", a control " + base + " (want x3)");
        // the vampire (Vampirism's own damage handling adds a little on its own) must be near x3, not the stacked x4.5
        helper.assertTrue(lost > base * 2.5F && lost < base * 3.5F, "vampire lost " + lost + ", a control " + base + " (want about x3, not x4.5)");
        helper.assertTrue(HolyDamageBridge.factor(vampire, tensuraHoly(helper)) == 1.0F, "the bridge still multiplies");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void witchGetsMagiculeBonus(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:human");
        AttributeInstance max = p.getAttribute(TensuraAttributes.MAX_MAGICULE);
        helper.assertTrue(max != null, "a player has no max magicule attribute");
        double before = max.getValue();

        species(p, Species.WITCH);
        AttributeModifier mag = modifier(p, TensuraAttributes.MAX_MAGICULE, SpeciesLayer.MAGICULE_ID);
        AttributeModifier aura = modifier(p, TensuraAttributes.MAX_AURA, SpeciesLayer.AURA_ID);
        helper.assertTrue(mag != null && Math.abs(mag.amount() - 0.30) < 1e-9, "magicule modifier " + mag);
        helper.assertTrue(aura != null && Math.abs(aura.amount() - 0.10) < 1e-9, "aura modifier " + aura);
        helper.assertTrue(max.getValue() > before, "max magicule did not grow: " + before + " -> " + max.getValue());
        helper.assertTrue(BloodlineAccess.get(p).layerSkills().isEmpty(), "a witch was granted skills");
        helper.assertTrue(!RaceUtils.isUndead(p), "a witch counts as undead");

        species(p, Species.NONE);
        helper.assertTrue(modifier(p, TensuraAttributes.MAX_MAGICULE, SpeciesLayer.MAGICULE_ID) == null, "the bonus stayed");
        helper.assertTrue(Math.abs(max.getValue() - before) < 1e-6, "max magicule is not back: " + before + " vs " + max.getValue());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void undeadRaceStillEjectsAVampireTypeSpeciesFromTheHunters(GameTestHelper helper) {
        ServerPlayer plain = TestPlayers.create(helper);
        VampirismAPI.factionPlayerHandler(plain).setFactionAndLevel(VReference.HUNTER_FACTION, 2);
        setRace(plain, "tensura:ghoul");
        helper.assertTrue(VampirismAPI.factionPlayerHandler(plain).getCurrentFaction() != VReference.HUNTER_FACTION,
                "control: a hunter turned undead stayed a hunter");

        // R103: only an apex race is the exception, a Tensura ghoul is not, whatever the species
        ServerPlayer original = TestPlayers.create(helper);
        species(original, Species.ORIGINAL);
        VampirismAPI.factionPlayerHandler(original).setFactionAndLevel(VReference.HUNTER_FACTION, 2);
        setRace(original, "tensura:ghoul");
        helper.assertTrue(VampirismAPI.factionPlayerHandler(original).getCurrentFaction() != VReference.HUNTER_FACTION,
                "an Original with a Tensura undead race was not ejected from the hunters");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void apexExemptionNeedsAnApexRaceAndAVampireTypeSpecies(GameTestHelper helper) {
        ResourceLocation apex = ResourceLocation.parse("supernaturalbloodlines:primordial_vampire");
        ResourceLocation ghoul = ResourceLocation.parse("tensura:ghoul");
        for (Species s : new Species[] {Species.ORIGINAL, Species.ORIGINAL_HYBRID, Species.HYBRID, Species.HERETIC, Species.TRIBRID}) {
            ServerPlayer p = TestPlayers.create(helper);
            species(p, s);
            helper.assertTrue(LayerQuery.isApexVampireType(p, apex), s + " with an apex race is not exempt");
            helper.assertTrue(!LayerQuery.isApexVampireType(p, ghoul), s + " with a Tensura race is exempt");
        }
        for (Species s : new Species[] {Species.NONE, Species.WITCH, Species.SIPHONER, Species.HUNTER_OF_THE_FIVE}) {
            ServerPlayer p = TestPlayers.create(helper);
            species(p, s);
            helper.assertTrue(!LayerQuery.isApexVampireType(p, apex), s + " is exempt");
        }
        helper.succeed();
    }
    @GameTest(template = "empty", templateNamespace = NS)
    public static void restoreKeepsTheCurrentLayerSkills(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        setRace(p, "tensura:elf");
        species(p, Species.ORIGINAL);
        BloodlineState before = BloodlineAccess.get(p);
        helper.assertTrue(before.layerSkills().equals(ORIGINAL_SKILLS), "control: " + before.layerSkills());

        // the species goes, the layer lets go of its skills, then something rolls the state back to the snapshot
        species(p, Species.NONE);
        helper.assertTrue(BloodlineAccess.get(p).layerSkills().isEmpty(), "control: the layer kept " + BloodlineAccess.get(p).layerSkills());
        BloodlineAccess.restore(p, before.toBuilder().species(Species.NONE).build());
        helper.assertTrue(BloodlineAccess.get(p).layerSkills().isEmpty(), "the rollback brought back " + BloodlineAccess.get(p).layerSkills());
        helper.assertTrue(!has(p, "tensura:drain"), "the skill came back");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void humanLikeCheckFollowsTheLines(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(TensuraHumanLikeCheck.INSTANCE.isHumanLike(p), "a player with no race is not eligible");
        for (String id : new String[] {"tensura:human", "tensura:elf", "tensura:enlightened_elf", "tensura:elf_saint",
                "tensura:divine_fish", "tensura:spirit_beast"}) {
            setRace(p, id);
            helper.assertTrue(TensuraHumanLikeCheck.INSTANCE.isHumanLike(p), id + " is not human-like");
        }
        for (String id : new String[] {"tensura:slime", "tensura:vampire", "tensura:ghoul", "tensura:orc"}) {
            setRace(p, id);
            helper.assertTrue(!TensuraHumanLikeCheck.INSTANCE.isHumanLike(p), id + " is human-like");
        }
        helper.succeed();
    }
}
