package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
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
}
