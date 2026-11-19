package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.doppelganger.DoppelgangerGene;
import net.ofatech.supernaturalbloodlines.parts.species.pack.GeneHandler;

import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.*;

/** Plan 12b, task 1, the parts that need Werewolves: the two genes at one login and the werewolf gene's discovery. */
@PrefixGameTestTemplate(false)
public final class GeneWerewolfGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final WolfState GENE = WolfState.NONE.toBuilder().geneRolled(true).latentGene(true).build();

    private GeneWerewolfGameTests() {
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bothGenesCoexistAtOneLogin(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        SpeciesConfig.overrideLatentGeneChance(0.15);
        SpeciesConfig.overrideDoppelgangerGeneChance(0.05);
        try {
            GeneHandler.useRoll(() -> 0.0);
            DoppelgangerGene.useRoll(() -> 0.0);
            GeneHandler.onLogin(p);
            DoppelgangerGene.onLogin(p);
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(s.wolf().latentGene() && s.wolf().geneRolled() && s.doppelgangerGene() && s.doppelgangerRolled(),
                    "the two genes do not sit together: " + s);
            helper.assertTrue(s.revealedGenes().isEmpty(), "a roll revealed a gene");
        } finally {
            GeneHandler.resetRoll();
            DoppelgangerGene.resetRoll();
            SpeciesConfig.overrideLatentGeneChance(null);
            SpeciesConfig.overrideDoppelgangerGeneChance(null);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void awakeningRevealsWerewolfGene(GameTestHelper helper) {
        ServerPlayer human = TestPlayers.createMortal(helper);
        ServerPlayer vampire = TestPlayers.createMortal(helper);
        SpeciesConfig.overrideDelayedAwakeningNights(0); // R142 delays a human's awakening by default; MoonWerewolfGameTests covers that
        try {
            BloodlineAccess.set(human, BloodlineState.NONE.toBuilder().wolf(GENE).doppelgangerGene(true).build());
            helper.assertTrue(BloodlineAccess.get(human).revealedGenes().isEmpty(), "the gene started revealed");
            Villager v = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 1));
            v.hurt(human.damageSources().playerAttack(human), 1000f);
            helper.assertTrue(v.isDeadOrDying(), "the villager survived");
            helper.assertTrue(BloodlineAccess.get(human).wolf().lineage() == WolfState.Lineage.BORN, "the human did not awaken");
            helper.assertTrue(BloodlineAccess.get(human).revealedGenes().equals(Set.of(Genes.WEREWOLF)),
                    "the awakening revealed " + BloodlineAccess.get(human).revealedGenes());

            // a vampire's awakening (R24) shows it too
            VampirismAPI.factionPlayerHandler(vampire).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1);
            BloodlineAccess.set(vampire, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).wolf(GENE).build());
            Villager v2 = helper.spawn(EntityType.VILLAGER, new BlockPos(2, 1, 1));
            v2.hurt(vampire.damageSources().playerAttack(vampire), 1000f);
            helper.assertTrue(BloodlineAccess.get(vampire).species() == Species.ORIGINAL_HYBRID, "the vampire did not awaken");
            helper.assertTrue(BloodlineAccess.get(vampire).revealedGenes().equals(Set.of(Genes.WEREWOLF)),
                    "the vampire's awakening revealed " + BloodlineAccess.get(vampire).revealedGenes());
        } finally {
            SpeciesConfig.overrideDelayedAwakeningNights(null);
            BloodlineAccess.set(human, BloodlineState.NONE);
            BloodlineAccess.set(vampire, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void lineageRevealRiteRevealsTheWerewolfGene(GameTestHelper helper) {
        RecordingPlayer target = RecordingPlayer.create(helper);
        place(helper, target);
        try {
            BloodlineAccess.set(target, BloodlineState.NONE.toBuilder().wolf(GENE).doppelgangerGene(true).build());
            start(helper, "rite_of_lineage_reveal", target, List.of(new ItemStack(Items.BONE), new ItemStack(Items.GLOWSTONE_DUST), new ItemStack(Items.PAPER)));
            helper.assertTrue(target.received("supernaturalbloodlines.lineage.gene_yes"), "the target was not told");
            helper.assertTrue(BloodlineAccess.get(target).revealedGenes().equals(Set.of(Genes.WEREWOLF)),
                    "the reveal rite revealed " + BloodlineAccess.get(target).revealedGenes());
        } finally {
            remove(helper, target);
            BloodlineAccess.set(target, BloodlineState.NONE);
        }
        helper.succeed();
    }
}
