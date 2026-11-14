package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.favouriteless.enchanted.common.init.EItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes.Knowledge;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.rite.DoppelgangerRiteRules;
import net.ofatech.supernaturalbloodlines.parts.species.rite.RevelationRiteFactory;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.DoppelgangerRiteGameTests.taglock;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.*;

/** Plan 12b, task 3: the Rite of Revelation, and what is synced of a gene before and after it. Runs without Werewolves. */
@PrefixGameTestTemplate(false)
public final class RevelationGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final BloodlineState CARRIER = BloodlineState.NONE.toBuilder().doppelgangerGene(true).doppelgangerRolled(true)
            .wolf(WolfState.NONE.toBuilder().geneRolled(true).latentGene(true).build()).build();

    private RevelationGameTests() {
    }

    private static boolean skip(GameTestHelper helper) {
        if (type(helper, "rite_of_revelation") == null) {
            helper.succeed(); // gated on the species part
            return true;
        }
        return false;
    }

    private static Knowledge seen(BloodlineState server, String gene) {
        return Genes.ofClient(BloodlineAccess.clientView(server, true, SpeciesConfig.genesAlwaysVisible()), gene);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void revelationRiteDataLoads(GameTestHelper helper) {
        RiteType type = type(helper, "rite_of_revelation");
        helper.assertTrue(type != null, "rite_of_revelation is not in Enchanted's rite registry");
        helper.assertTrue(type.getPower() == 1000, "power is " + type.getPower());
        helper.assertTrue(type.getItems().size() == 1 && type.getItems().get(0).is(EItems.TAGLOCK_FILLED.get()), "it needs exactly one filled taglock");
        helper.assertTrue(type.getShapes().size() == 1, "it should have exactly one circle");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void revelationRevealsAllGenes(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        RecordingPlayer subject = RecordingPlayer.create(helper);
        place(helper, subject);
        try {
            BloodlineAccess.set(subject, CARRIER);
            start(helper, "rite_of_revelation", subject, List.of(taglock(subject.getUUID())));
            helper.assertTrue(BloodlineAccess.get(subject).revealedGenes().equals(Set.of(Genes.WEREWOLF, Genes.DOPPELGANGER)),
                    "revealed: " + BloodlineAccess.get(subject).revealedGenes());
            helper.assertTrue(subject.received(RevelationRiteFactory.HEADER), "the subject was not told");
            helper.assertTrue(BloodlineAccess.get(subject).doppelgangerGene() && BloodlineAccess.get(subject).wolf().latentGene(), "the rite changed the genes");
            helper.assertTrue(dropped(helper, "enchanted:taglock") == 0, "the taglock came back from a good rite");
        } finally {
            remove(helper, subject);
            BloodlineAccess.set(subject, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void revelationOfSomeoneElsesTaglockTellsOnlyTheirOwner(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        RecordingPlayer caster = RecordingPlayer.create(helper);
        RecordingPlayer owner = RecordingPlayer.create(helper);
        place(helper, caster);
        place(helper, owner);
        try {
            BloodlineAccess.set(owner, CARRIER);
            start(helper, "rite_of_revelation", caster, List.of(taglock(owner.getUUID())));
            helper.assertTrue(owner.received(RevelationRiteFactory.HEADER), "the owner was not told");
            helper.assertTrue(!caster.received(RevelationRiteFactory.HEADER), "the caster was told the owner's genes");
            helper.assertTrue(BloodlineAccess.get(caster).revealedGenes().isEmpty(), "the caster's genes were revealed");
        } finally {
            remove(helper, caster);
            remove(helper, owner);
            BloodlineAccess.set(owner, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void revelationRefusesWhatItCannotReveal(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        RecordingPlayer caster = RecordingPlayer.create(helper);
        place(helper, caster);
        SpeciesConfig.overrideDoppelgangerVillagerChance(1.0);
        Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 1, 1));
        SpeciesConfig.overrideDoppelgangerVillagerChance(null);
        try {
            start(helper, "rite_of_revelation", caster, List.of(taglock(villager.getUUID())));
            helper.assertTrue(caster.receivedOnActionBar(RevelationRiteFactory.PLAYERS_ONLY), "a villager's taglock was not refused");
            helper.assertTrue(dropped(helper, "enchanted:taglock") == 1, "the taglock was not given back");
            clearDropped(helper);

            start(helper, "rite_of_revelation", caster, List.of(taglock(UUID.randomUUID())));
            helper.assertTrue(caster.receivedOnActionBar(DoppelgangerRiteRules.OWNER_AWAY), "an owner nobody can find was not refused");
            clearDropped(helper);

            start(helper, "rite_of_revelation", caster, List.of(new ItemStack(EItems.TAGLOCK_FILLED.get())));
            helper.assertTrue(caster.receivedOnActionBar(DoppelgangerRiteRules.NO_OWNER), "a taglock with no owner was not refused");
            helper.assertTrue(BloodlineAccess.get(caster).revealedGenes().isEmpty(), "a refused rite revealed something");
        } finally {
            remove(helper, caster);
            villager.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void undiscoveredGeneNotSynced(GameTestHelper helper) {
        SpeciesConfig.overrideGenesAlwaysVisible(false);
        try {
            for (String gene : Genes.ALL) {
                helper.assertTrue(seen(CARRIER, gene) == Knowledge.UNKNOWN, gene + " was sent before it was discovered");
            }
            BloodlineState view = BloodlineAccess.clientView(CARRIER, true, false);
            helper.assertTrue(!view.doppelgangerGene() && !view.wolf().latentGene() && view.revealedGenes().isEmpty(),
                    "the raw values are in the client's state: " + view);

            // discovering one gene sends that one only
            BloodlineState found = Genes.reveal(CARRIER, Genes.DOPPELGANGER);
            helper.assertTrue(seen(found, Genes.DOPPELGANGER) == Knowledge.TRUE, "a discovered gene was not sent");
            helper.assertTrue(seen(found, Genes.WEREWOLF) == Knowledge.UNKNOWN, "the other gene was sent too");

            // and nobody else's client ever gets one
            BloodlineState others = BloodlineAccess.clientView(Genes.revealAll(CARRIER), false, false);
            helper.assertTrue(!others.doppelgangerGene() && !others.wolf().latentGene() && others.revealedGenes().isEmpty(),
                    "a tracking player was sent a gene: " + others);
        } finally {
            SpeciesConfig.overrideGenesAlwaysVisible(null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void alwaysVisibilitySendsGenes(GameTestHelper helper) {
        SpeciesConfig.overrideGenesAlwaysVisible(true);
        try {
            helper.assertTrue(seen(CARRIER, Genes.WEREWOLF) == Knowledge.TRUE && seen(CARRIER, Genes.DOPPELGANGER) == Knowledge.TRUE,
                    "the carrier's genes are not shown under always");
            helper.assertTrue(seen(BloodlineState.NONE, Genes.WEREWOLF) == Knowledge.FALSE && seen(BloodlineState.NONE, Genes.DOPPELGANGER) == Knowledge.FALSE,
                    "a player without the genes is not told so under always");
            BloodlineState others = BloodlineAccess.clientView(CARRIER, false, true);
            helper.assertTrue(!others.doppelgangerGene() && !others.wolf().latentGene(), "always sent the genes to other players");
        } finally {
            SpeciesConfig.overrideGenesAlwaysVisible(null);
        }
        helper.succeed();
    }
}
