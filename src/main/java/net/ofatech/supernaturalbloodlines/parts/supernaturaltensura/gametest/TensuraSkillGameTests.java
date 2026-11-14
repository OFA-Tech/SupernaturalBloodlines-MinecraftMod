package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.gametest;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import java.util.Set;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.supernatural.skill.SkillService;

/** The TENSURA skills: granted when bought, taken back when dormant, but only the ones this mod granted. */
@PrefixGameTestTemplate(false)
public final class TensuraSkillGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final ResourceLocation MAGIC_SENSE = ResourceLocation.parse("tensura:magic_sense");

    private TensuraSkillGameTests() {}

    private static ServerPlayer witch(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().sp(9).spEarned(9).rung(SupernaturalRace.WITCH, 3).build());
        return p;
    }

    private static boolean holds(ServerPlayer p) {
        return SkillAPI.getSkillsFrom(p).getSkill(MAGIC_SENSE).isPresent();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void boughtTensuraSkillIsGrantedAndTakenBack(GameTestHelper helper) {
        ServerPlayer p = witch(helper);
        helper.assertTrue(!holds(p), "the test player already has magic sense");
        helper.assertTrue(SkillService.buy(p, "S-WIT-5").ok(), "the purchase was refused");
        helper.assertTrue(holds(p) && SupernaturalAccess.get(p).granted().contains("tensura:magic_sense"), "the skill was not granted and tracked");
        // no longer a witch: the human tree does not list S-WIT-5, so the skill is dormant and the grant is taken back
        BloodlineAccess.set(p, BloodlineState.NONE);
        helper.assertTrue(!holds(p) && SupernaturalAccess.get(p).granted().isEmpty(), "the dormant skill was kept");
        helper.assertTrue(SupernaturalAccess.get(p).skills().contains("S-WIT-5"), "dormant lost the purchase");
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        helper.assertTrue(holds(p) && SupernaturalAccess.get(p).granted().contains("tensura:magic_sense"), "the skill did not come back");
        helper.succeed();
    }

    /** A skill the player had before is theirs: buying and then losing the tree skill never forgets it. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void tensuraSkillOnlyOursRemoved(GameTestHelper helper) {
        ServerPlayer p = witch(helper);
        ManasSkill skill = SkillAPI.getSkillRegistry().get(MAGIC_SENSE);
        helper.assertTrue(skill != null, "this Tensura has no magic_sense");
        var own = skill.createDefaultInstance();
        own.getOrCreateTag().putBoolean("NoMagiculeCost", true); // Tensura vetoes a learned skill when max magicule is too low; no grant mark
        SkillAPI.getSkillsFrom(p).learnSkill(own);
        helper.assertTrue(holds(p), "could not give the player the skill");
        helper.assertTrue(SkillService.buy(p, "S-WIT-5").ok(), "the purchase was refused");
        helper.assertTrue(SupernaturalAccess.get(p).granted().equals(Set.of()), "a skill the player had was tracked as granted: " + SupernaturalAccess.get(p).granted());
        BloodlineAccess.set(p, BloodlineState.NONE);
        helper.assertTrue(holds(p), "a skill the player had before was forgotten");
        helper.succeed();
    }
}
