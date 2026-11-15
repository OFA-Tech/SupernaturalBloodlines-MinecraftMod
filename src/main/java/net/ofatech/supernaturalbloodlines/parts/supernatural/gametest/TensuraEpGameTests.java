package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.supernatural.MigrationRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.supernatural.task.TaskTracker;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.supernatural.LadderService;

/** Claims pay Tensura EP through the supernatural_tensura part: a flat sum into the player's existence. Registered only when that part is on. */
@PrefixGameTestTemplate(false)
public final class TensuraEpGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private TensuraEpGameTests() {}

    @GameTest(template = "empty", templateNamespace = NS)
    public static void claimPaysSpAndEp(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.ORIGINAL).build());
        SupernaturalState s = SupernaturalAccess.get(p);
        s = LadderRules.markDone(LadderRules.markDone(s, "L-ORI-1a"), "L-ORI-1b");
        SupernaturalAccess.set(p, s);
        double ep = TensuraStorages.getExistenceFrom(p).getEP();
        LadderService.claim(p, "L-ORI-1a");
        helper.assertTrue(TensuraStorages.getExistenceFrom(p).getEP() == ep + 2_500, "half of the rung's 5,000 EP was not paid: " + (TensuraStorages.getExistenceFrom(p).getEP() - ep));
        LadderService.claim(p, "L-ORI-1b");
        helper.assertTrue(TensuraStorages.getExistenceFrom(p).getEP() == ep + 5_000, "the rung's EP is not 5,000: " + (TensuraStorages.getExistenceFrom(p).getEP() - ep));
        helper.assertTrue(SupernaturalAccess.get(p).sp() == 3, "the rung's 3 SP were not paid: " + SupernaturalAccess.get(p).sp());
        helper.succeed();
    }

    private static ServerPlayer atRung(GameTestHelper helper, Species species, SupernaturalRace race, int rung, String tensuraRace) {
        ServerPlayer p = TestPlayers.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(species).build());
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().schema(MigrationRules.SCHEMA).rung(race, rung).build());
        RaceAPI.getRaceFrom(p).setRace(ResourceLocation.parse(tensuraRace), false);
        return p;
    }

    private static boolean tierDone(ServerPlayer p, String taskId) {
        TaskTracker.recheck(p);
        return SupernaturalAccess.get(p).done().contains(taskId);
    }

    /** The {@code tensura_tier} task of the apex rung is done while the Tensura race is at the apex tier of a line the race may be on. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void tensuraTierFollowsTheLine(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("species")) {
            helper.succeed(); // the species ladders only exist with the species part
            return;
        }
        ServerPlayer original = atRung(helper, Species.ORIGINAL, SupernaturalRace.ORIGINAL, 4, "tensura:elf");
        helper.assertTrue(!tierDone(original, "L-ORI-5a"), "an elf is at the apex tier");
        RaceAPI.getRaceFrom(original).setRace(ResourceLocation.parse("tensura:vampire_overcomer"), false);
        helper.assertTrue(!tierDone(original, "L-ORI-5a"), "a vampire overcomer is at the apex tier");
        RaceAPI.getRaceFrom(original).setRace(ResourceLocation.parse("tensura:vampire_lord"), false);
        helper.assertTrue(tierDone(original, "L-ORI-5a"), "a vampire lord is not at the apex tier");

        ServerPlayer saint = atRung(helper, Species.ORIGINAL, SupernaturalRace.ORIGINAL, 4, "tensura:elf_saint");
        helper.assertTrue(tierDone(saint, "L-ORI-5a"), "an elf saint is not at the apex tier");
        helper.assertTrue(!SupernaturalAccess.get(saint).done().contains("L-ORI-5b"), "the 60 days were counted");

        // a hybrid is on either line, a tribrid only on Beastfolk, an Original hybrid only on the vampire line
        helper.assertTrue(tierDone(atRung(helper, Species.HYBRID, SupernaturalRace.HYBRID, 4, "tensura:vampire_lord"), "L-HYB-5a"), "hybrid on the vampire line");
        helper.assertTrue(tierDone(atRung(helper, Species.HYBRID, SupernaturalRace.HYBRID, 4, "tensura:spirit_beast"), "L-HYB-5a"), "hybrid on Beastfolk");
        helper.assertTrue(!tierDone(atRung(helper, Species.TRIBRID, SupernaturalRace.TRIBRID, 4, "tensura:vampire_lord"), "L-TRI-5a"), "tribrid on the vampire line");
        helper.assertTrue(tierDone(atRung(helper, Species.TRIBRID, SupernaturalRace.TRIBRID, 4, "tensura:spirit_beast"), "L-TRI-5a"), "tribrid on Beastfolk");
        helper.assertTrue(!tierDone(atRung(helper, Species.ORIGINAL_HYBRID, SupernaturalRace.ORIGINAL_HYBRID, 4, "tensura:spirit_beast"), "L-OHY-5a"),
                "Original hybrid on Beastfolk");
        helper.assertTrue(tierDone(atRung(helper, Species.ORIGINAL_HYBRID, SupernaturalRace.ORIGINAL_HYBRID, 4, "tensura:vampire_lord"), "L-OHY-5a"),
                "Original hybrid on the vampire line");
        helper.succeed();
    }

    /** A witch reads the tier against the witch line table, not the creature race's. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void tensuraTierIsReadPerRace(GameTestHelper helper) {
        if (!EnabledParts.isEnabled("species")) {
            helper.succeed();
            return;
        }
        ServerPlayer witch = atRung(helper, Species.WITCH, SupernaturalRace.WITCH, 4, "tensura:vampire_lord");
        helper.assertTrue(!tierDone(witch, "L-WIT-5a"), "a witch on the vampire line is at the apex tier");
        RaceAPI.getRaceFrom(witch).setRace(ResourceLocation.parse("tensura:human_saint"), false);
        helper.assertTrue(tierDone(witch, "L-WIT-5a"), "a witch on a human saint is not at the apex tier");
        // on Beastfolk a witch is at the tier only as a werewolf (its magic ladder stacks on the werewolf's line)
        RaceAPI.getRaceFrom(witch).setRace(ResourceLocation.parse("tensura:spirit_beast"), false);
        helper.assertTrue(!tierDone(atRung(helper, Species.WITCH, SupernaturalRace.WITCH, 4, "tensura:spirit_beast"), "L-WIT-5a"),
                "a witch who is not a werewolf is at the apex tier on Beastfolk");
        helper.succeed();
    }
}
