package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.shared.supernatural.PlayerRaces;

/**
 * Home's kind chip, server side (plan 23 Task 4): the inputs of {@link PlayerRaces#of} for a player made a level-1 vampire each way a player can, and after a
 * faction change. The client reads the same two inputs from its synced copies.
 */
@PrefixGameTestTemplate(false)
public final class HubKindGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private HubKindGameTests() {
    }

    private static void run(ServerPlayer p, String command) {
        p.getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), command);
    }

    private static String kind(ServerPlayer p) {
        var f = VampirismAPI.factionPlayerHandler(p).getCurrentFaction();
        return (f == null ? "none" : f.getID() + "@" + VampirismAPI.factionPlayerHandler(p).getCurrentLevel()) + " " + PlayerRaces.of(p);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theChipInputsMatchTheServerForEveryWayToBecomeAVampire(GameTestHelper helper) {
        // (a) the owner's command
        ServerPlayer byCommand = TestPlayers.createMortal(helper);
        run(byCommand, "vampirism level vampirism:vampire 1"); // run as the player, the way the owner typed it
        helper.assertTrue(PlayerRaces.of(byCommand).equals(List.of(SupernaturalRace.VAMPIRE)), "by the command: " + kind(byCommand));
        // (c) our own faction rites and the turning join through the handler, the same call
        ServerPlayer byHandler = TestPlayers.createMortal(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(byHandler).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not join");
        helper.assertTrue(PlayerRaces.of(byHandler).equals(List.of(SupernaturalRace.VAMPIRE)), "by the handler: " + kind(byHandler));
        // (b) a Tensura vampire race: the vampirism_tensura part joins the faction
        if (ModList.get().isLoaded("tensura") && net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("vampirismTensura")) {
            ServerPlayer byRace = TestPlayers.createMortal(helper);
            run(byRace, "tensura edit race @s set tensura:vampire");
            helper.assertTrue(PlayerRaces.of(byRace).equals(List.of(SupernaturalRace.VAMPIRE)), "by the Tensura race: " + kind(byRace));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theKindFollowsAFactionChangeWithoutARelog(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        var handler = VampirismAPI.factionPlayerHandler(p);
        helper.assertTrue(handler.setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not join the vampires");
        helper.assertTrue(PlayerRaces.of(p).equals(List.of(SupernaturalRace.VAMPIRE)), "vampire: " + kind(p));
        helper.assertTrue(handler.setFactionAndLevel(null, 0), "could not leave");
        helper.assertTrue(PlayerRaces.of(p).equals(List.of(SupernaturalRace.HUMAN)), "cured: " + kind(p));
        helper.assertTrue(handler.setFactionAndLevel(VReference.HUNTER_FACTION, 1), "could not join the hunters");
        helper.assertTrue(PlayerRaces.of(p).equals(List.of(SupernaturalRace.HUNTER)), "hunter: " + kind(p));
        BloodlineAccess.set(p, BloodlineState.NONE);
        helper.succeed();
    }
}
