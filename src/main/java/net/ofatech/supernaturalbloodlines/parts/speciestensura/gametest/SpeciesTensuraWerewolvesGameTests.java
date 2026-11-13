package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import de.teamlapen.werewolves.api.WReference;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.pack.GeneHandler;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;
import net.ofatech.supernaturalbloodlines.shared.tensura.SkillOwners;

/**
 * Needs Werewolves and vampirism_tensura (the werewolf level grants), so the part registers it only then; it links
 * {@code WReference}.
 */
@PrefixGameTestTemplate(false)
public final class SpeciesTensuraWerewolvesGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final ResourceLocation GODWOLF_SENSE = ResourceLocation.parse("tensura:godwolf_sense");

    private SpeciesTensuraWerewolvesGameTests() {}

    private static boolean has(ServerPlayer p) {
        return SkillAPI.getSkillsFrom(p).getSkill(GODWOLF_SENSE).isPresent();
    }

    private static void species(ServerPlayer p, Species s) {
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().species(s).build());
    }

    private static void becomeMaxWerewolf(GameTestHelper helper, ServerPlayer p) {
        IPlayableFaction<?> wolf = WReference.WEREWOLF_FACTION;
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(wolf, wolf.getHighestReachableLevel()),
                "could not reach the max werewolf level");
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse("tensura:human"), false);
        return p;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void layerRemovedWhileTheWerewolfLevelStillWantsTheSkill(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        species(p, Species.ORIGINAL_HYBRID);
        helper.assertTrue(has(p), "control: the layer did not grant godwolf_sense");
        becomeMaxWerewolf(helper, p);
        helper.assertTrue(has(p), "no godwolf_sense for a max werewolf with the layer");

        species(p, Species.NONE);
        helper.assertTrue(has(p), "removing the layer took a skill the werewolf level still wants");
        VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(null, 0);
        helper.assertTrue(!has(p), "godwolf_sense stayed for good after both sources let go");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void leavingWerewolvesWhileTheLayerStillWantsTheSkill(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        becomeMaxWerewolf(helper, p);
        helper.assertTrue(has(p), "control: the werewolf level did not grant godwolf_sense");
        species(p, Species.ORIGINAL_HYBRID);

        VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(null, 0);
        helper.assertTrue(has(p), "leaving the werewolves took a skill the layer still wants");
        species(p, Species.NONE);
        helper.assertTrue(!has(p), "godwolf_sense stayed for good after both sources let go");
        helper.succeed();
    }

    /** A late veto of the werewolf join rolls the bloodline state back; it must not bring back an old layerSkills. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void rolledBackJoinKeepsTheCurrentLayerSkills(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        // no species and no vampire faction: the layer wants nothing, so the first listener pass after the lineage
        // change lets go of the tracked drain skill
        SkillOwners.grant(p, SkillAPI.getSkillRegistry().get(ResourceLocation.parse("tensura:drain")));
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().layerSkills(Set.of("tensura:drain"))
                .wolf(WolfState.NONE.toBuilder().geneRolled(true).latentGene(true).build()).build());

        boolean[] vetoed = {false};
        Consumer<PlayerFactionEvent.FactionLevelChangePre> veto = ev -> {
            if (ev.getNewFaction() == WReference.WEREWOLF_FACTION) {
                vetoed[0] = true;
                ev.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(veto);
        String refusal;
        try {
            refusal = GeneHandler.awakenHuman(p, PackEvent.GENE_AWAKENED);
        } finally {
            NeoForge.EVENT_BUS.unregister(veto);
        }
        helper.assertTrue(vetoed[0] && FactionRiteFactory.JOIN_DENIED.equals(refusal), "control: the late veto did not refuse the join (" + refusal + ")");
        helper.assertTrue(BloodlineAccess.get(p).wolf().lineage() == WolfState.Lineage.NONE, "control: the lineage was not rolled back");
        helper.assertTrue(BloodlineAccess.get(p).layerSkills().isEmpty(), "the rollback brought back " + BloodlineAccess.get(p).layerSkills());
        helper.assertTrue(!SkillAPI.getSkillsFrom(p).getSkill(ResourceLocation.parse("tensura:drain")).isPresent(), "the skill stayed");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aSkillThePlayerHadIsNeverTakenByEitherSource(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        var skill = SkillAPI.getSkillRegistry().get(GODWOLF_SENSE);
        var instance = skill.createDefaultInstance();
        instance.getOrCreateTag().putBoolean("NoMagiculeCost", true);
        SkillAPI.getSkillsFrom(p).learnSkill(instance);

        species(p, Species.ORIGINAL_HYBRID);
        becomeMaxWerewolf(helper, p);
        species(p, Species.NONE);
        VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(null, 0);
        helper.assertTrue(has(p), "a skill the player had before was taken");
        helper.succeed();
    }
}
