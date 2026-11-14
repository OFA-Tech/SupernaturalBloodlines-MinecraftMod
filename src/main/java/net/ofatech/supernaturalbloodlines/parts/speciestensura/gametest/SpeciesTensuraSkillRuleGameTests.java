package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SpeciesLayer;

/** Phase 12c Task 2, the skills that add to the layer's magicule and aura bonus (S-ORI-4, S-WIT-7). */
@PrefixGameTestTemplate(false)
public final class SpeciesTensuraSkillRuleGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private SpeciesTensuraSkillRuleGameTests() {}

    private static double amount(AttributeModifier m) {
        return m == null ? 0 : m.amount();
    }

    private static void own(ServerPlayer p, String... skills) {
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().skills(Set.of(skills)).build());
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void primordialMindAddsMagicule(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).turnedAt(1).build());
        var magicule = p.getAttribute(TensuraAttributes.MAX_MAGICULE);
        double plain = amount(magicule.getModifier(SpeciesLayer.MAGICULE_ID));
        own(p, "S-ORI-4");
        double skilled = amount(magicule.getModifier(SpeciesLayer.MAGICULE_ID));
        helper.assertTrue(plain > 0 && Math.abs(skilled - plain - 0.05) < 1e-9, "magicule bonus " + plain + " without the skill, " + skilled + " with it");
        own(p);
        helper.assertTrue(Math.abs(amount(magicule.getModifier(SpeciesLayer.MAGICULE_ID)) - plain) < 1e-9, "the bonus stayed after the skill went");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void covensWardAddsAuraAndHealth(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.WITCH).turnedAt(1).build());
        var aura = p.getAttribute(TensuraAttributes.MAX_AURA);
        double plain = amount(aura.getModifier(SpeciesLayer.AURA_ID));
        own(p, "S-WIT-7");
        double skilled = amount(aura.getModifier(SpeciesLayer.AURA_ID));
        helper.assertTrue(Math.abs(skilled - plain - 0.05) < 1e-9, "aura bonus " + plain + " without the skill, " + skilled + " with it");
        helper.assertTrue(p.getAttribute(Attributes.MAX_HEALTH).hasModifier(
                net.ofatech.supernaturalbloodlines.parts.supernatural.skill.SkillEffects.modifierId("S-WIT-7")), "no +2 health");
        helper.succeed();
    }
}