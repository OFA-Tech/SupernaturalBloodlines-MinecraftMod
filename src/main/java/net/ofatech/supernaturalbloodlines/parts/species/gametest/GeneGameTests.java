package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.doppelganger.DoppelgangerGene;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.BloodlineCommand;

/** Plan 12b, task 1: the doppelgänger gene, its roll, the admin command and what the client is told. Runs without Werewolves. */
@PrefixGameTestTemplate(false)
public final class GeneGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private GeneGameTests() {
    }

    private static boolean carries(ServerPlayer p) {
        return BloodlineAccess.get(p).doppelgangerGene();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void geneRolledOnceOnJoin(GameTestHelper helper) {
        ServerPlayer lucky = TestPlayers.createMortal(helper);
        ServerPlayer unlucky = TestPlayers.createMortal(helper);
        ServerPlayer edge = TestPlayers.createMortal(helper);
        SpeciesConfig.overrideDoppelgangerGeneChance(0.05);
        try {
            DoppelgangerGene.useRoll(() -> 0.049);
            DoppelgangerGene.onLogin(lucky);
            helper.assertTrue(BloodlineAccess.get(lucky).doppelgangerRolled() && carries(lucky), "a roll under the chance gave no gene");
            // the same player logs in again with a roll that would not give the gene: nothing is rerolled
            DoppelgangerGene.useRoll(() -> 0.99);
            DoppelgangerGene.onLogin(lucky);
            helper.assertTrue(carries(lucky), "the second login rerolled the gene away");

            DoppelgangerGene.onLogin(unlucky);
            helper.assertTrue(BloodlineAccess.get(unlucky).doppelgangerRolled() && !carries(unlucky), "a high roll gave a gene or no roll was recorded");
            DoppelgangerGene.useRoll(() -> 0.0);
            DoppelgangerGene.onLogin(unlucky);
            helper.assertTrue(!carries(unlucky), "the second login rerolled the gene in");

            DoppelgangerGene.useRoll(() -> 0.05); // the chance itself is not enough: the roll has to be below it
            DoppelgangerGene.onLogin(edge);
            helper.assertTrue(!carries(edge), "a roll equal to the chance gave the gene");

            helper.assertTrue(BloodlineAccess.get(lucky).revealedGenes().isEmpty(), "rolling the gene revealed it");
        } finally {
            DoppelgangerGene.resetRoll();
            SpeciesConfig.overrideDoppelgangerGeneChance(null);
            for (ServerPlayer p : new ServerPlayer[]{lucky, unlucky, edge}) {
                BloodlineAccess.set(p, BloodlineState.NONE);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void existingPlayerRolledOnNextLogin(GameTestHelper helper) {
        ServerPlayer old = TestPlayers.createMortal(helper);
        // a save from before the gene existed: everything else is set, the gene was never rolled
        BloodlineState before = BloodlineState.NONE.toBuilder().species(Species.WITCH).turnedAt(77)
                .wolf(WolfState.NONE.toBuilder().geneRolled(true).latentGene(true).build()).build();
        SpeciesConfig.overrideDoppelgangerGeneChance(0.5);
        try {
            BloodlineAccess.set(old, before);
            DoppelgangerGene.useRoll(() -> 0.1);
            DoppelgangerGene.onLogin(old);
            BloodlineState after = BloodlineAccess.get(old);
            helper.assertTrue(after.doppelgangerRolled() && after.doppelgangerGene(), "the old player was not rolled on login");
            helper.assertTrue(after.toBuilder().doppelgangerGene(false).doppelgangerRolled(false).build().equals(before),
                    "rolling changed something else: " + after);
        } finally {
            DoppelgangerGene.resetRoll();
            SpeciesConfig.overrideDoppelgangerGeneChance(null);
            BloodlineAccess.set(old, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void awakenedPlayerCountsAsDiscoveredOnTheFirstRoll(GameTestHelper helper) {
        ServerPlayer born = TestPlayers.createMortal(helper);
        try {
            BloodlineAccess.set(born, BloodlineState.NONE.toBuilder()
                    .wolf(WolfState.NONE.toBuilder().geneRolled(true).latentGene(true).lineage(WolfState.Lineage.BORN).build()).build());
            DoppelgangerGene.useRoll(() -> 0.99);
            DoppelgangerGene.onLogin(born);
            helper.assertTrue(BloodlineAccess.get(born).revealedGenes().equals(java.util.Set.of(Genes.WEREWOLF)),
                    "revealed: " + BloodlineAccess.get(born).revealedGenes());
        } finally {
            DoppelgangerGene.resetRoll();
            BloodlineAccess.set(born, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bothGenesCoexist(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        SpeciesConfig.overrideDoppelgangerGeneChance(0.05);
        try {
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(WolfState.NONE.toBuilder().geneRolled(true).latentGene(true).build()).build());
            DoppelgangerGene.useRoll(() -> 0.0);
            DoppelgangerGene.onLogin(p);
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(s.doppelgangerGene() && s.wolf().latentGene(), "the two genes do not sit together: " + s);
        } finally {
            DoppelgangerGene.resetRoll();
            SpeciesConfig.overrideDoppelgangerGeneChance(null);
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void adminCommandSetsGene(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        CommandSourceStack op = helper.getLevel().getServer().createCommandSourceStack().withPermission(2);
        CommandSourceStack nobody = helper.getLevel().getServer().createCommandSourceStack().withPermission(0);
        try {
            BloodlineCommand.setDoppelgangerGene(op, p, true);
            helper.assertTrue(carries(p) && BloodlineAccess.get(p).doppelgangerRolled(), "the command did not give the gene");
            helper.assertTrue(BloodlineAccess.get(p).revealedGenes().isEmpty(), "the command revealed the gene");
            // counts as rolled: the next login does not roll it away
            DoppelgangerGene.useRoll(() -> 0.99);
            DoppelgangerGene.onLogin(p);
            helper.assertTrue(carries(p), "a login rolled over the command");
            BloodlineCommand.setDoppelgangerGene(op, p, false);
            helper.assertTrue(!carries(p) && BloodlineAccess.get(p).doppelgangerRolled(), "the command did not take the gene away");
            DoppelgangerGene.useRoll(() -> 0.0);
            DoppelgangerGene.onLogin(p);
            helper.assertTrue(!carries(p), "a login rolled over the command");

            CommandDispatcher<CommandSourceStack> d = new CommandDispatcher<>();
            BloodlineCommand.register(d);
            var gene = d.getRoot().getChild("bloodline").getChild("gene");
            helper.assertTrue(gene != null && gene.canUse(op) && !gene.canUse(nobody), "the gene command is not op level 2");
            helper.assertTrue(gene.getChild("doppelganger").getChild("player").getChild("value") != null, "the command shape is wrong");
        } finally {
            DoppelgangerGene.resetRoll();
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void revealingOneGeneLeavesTheOtherUnknown(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().doppelgangerGene(true).build());
            BloodlineAccess.reveal(p, Genes.WEREWOLF);
            helper.assertTrue(BloodlineAccess.get(p).revealedGenes().equals(java.util.Set.of(Genes.WEREWOLF)), "revealed: " + BloodlineAccess.get(p).revealedGenes());
        } finally {
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void clientViewFollowsGeneVisibility(GameTestHelper helper) {
        BloodlineState carrier = BloodlineState.NONE.toBuilder().doppelgangerGene(true).doppelgangerRolled(true).build();
        try {
            SpeciesConfig.overrideGenesAlwaysVisible(false);
            helper.assertTrue(!BloodlineAccess.clientView(carrier, true, SpeciesConfig.genesAlwaysVisible()).doppelgangerGene(),
                    "an undiscovered gene was shown under discovered");
            SpeciesConfig.overrideGenesAlwaysVisible(true);
            helper.assertTrue(BloodlineAccess.clientView(carrier, true, SpeciesConfig.genesAlwaysVisible()).doppelgangerGene(),
                    "the gene was hidden under always");
        } finally {
            SpeciesConfig.overrideGenesAlwaysVisible(null);
        }
        helper.succeed();
    }
}
