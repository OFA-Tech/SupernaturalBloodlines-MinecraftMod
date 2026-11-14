package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.werewolves.api.WReference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.turning.TurningHandler;

/** The turning for a werewolf (review I1, M1). Registered only when Werewolves is loaded, so nothing here links it without. */
@PrefixGameTestTemplate(false)
public final class TurningWerewolfGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private TurningWerewolfGameTests() {
    }

    private static IFactionPlayerHandler handler(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p);
    }

    private static ServerPlayer wolf(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        helper.assertTrue(handler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        return p;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void werewolfInTurningDrinksBottleAndStaysWerewolf(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        try {
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().turningTicksLeft(9000).build());
            p.setItemInHand(InteractionHand.MAIN_HAND,
                    new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("vampirism:blood_bottle")), 2));
            var click = new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND);
            NeoForge.EVENT_BUS.post(click);
            // (something else, Vampirism or Werewolves, may cancel the click; ours must not use the bottle up)
            helper.assertTrue(p.getMainHandItem().getCount() == 2, "the bottle was used up for a werewolf");
            TurningHandler.drankHumanBlood(p);
            helper.assertTrue(handler(p).getCurrentFaction() == WReference.WEREWOLF_FACTION, "the werewolf is now " + handler(p).getCurrentFaction());
            helper.assertTrue(!BloodlineAccess.get(p).inTurning(), "the turning is still running");
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().turningTicksLeft(20).build());
            TurningHandler.secondPassed(p);
            helper.assertTrue(!p.isDeadOrDying(), "the werewolf was killed by the turning");
        } finally {
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void werewolfIsNotHumanLikeBlood(GameTestHelper helper) {
        ServerPlayer p = wolf(helper);
        helper.assertTrue(!TurningHandler.isHumanLikeBlood(p), "a werewolf is human-like blood");
        helper.succeed();
    }
}
