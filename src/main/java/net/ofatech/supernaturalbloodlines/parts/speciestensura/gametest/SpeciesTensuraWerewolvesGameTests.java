package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.werewolves.api.WReference;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;

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
