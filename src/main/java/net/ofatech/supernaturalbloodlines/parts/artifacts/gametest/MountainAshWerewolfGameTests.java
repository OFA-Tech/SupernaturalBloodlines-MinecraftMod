package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;

/** Mountain ash against werewolves. Links Werewolves, so it is registered only when Werewolves is loaded. */
@PrefixGameTestTemplate(false)
public final class MountainAshWerewolfGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private MountainAshWerewolfGameTests() {
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void werewolfCannotCrossAshLine(GameTestHelper helper) {
        MountainAshGameTests.lay(helper);
        ServerPlayer werewolf = TestPlayers.createMortal(helper);
        werewolf.setPos(helper.absoluteVec(new Vec3(4.5, 1, 1.5)));
        helper.assertTrue(VampirismAPI.factionPlayerHandler(werewolf).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        MountainAshGameTests.assertStopped(helper, werewolf, "a werewolf player");
        helper.succeed();
    }
}
