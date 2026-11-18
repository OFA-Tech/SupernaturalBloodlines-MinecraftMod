package net.ofatech.supernaturalbloodlines.parts.celestials.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.vampire.IDrinkBloodContext;
import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;

/** A vampire's bite on a Blood Moon. Needs Vampirism. */
@PrefixGameTestTemplate(false)
public final class CelestialsVampireGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private CelestialsVampireGameTests() {
    }

    public static int feed(ServerPlayer drinker, LivingEntity source, int amount) {
        IDrinkBloodContext context = new IDrinkBloodContext() {
            @Override
            public Optional<LivingEntity> getEntity() {
                return Optional.of(source);
            }

            @Override
            public Optional<ItemStack> getStack() {
                return Optional.empty();
            }

            @Override
            public Optional<BlockState> getBlockState() {
                return Optional.empty();
            }

            @Override
            public Optional<BlockPos> getBlockPos() {
                return Optional.empty();
            }
        };
        var event = new BloodDrinkEvent.PlayerDrinkBloodEvent(VampirismAPI.vampirePlayer(drinker), amount, 0.5f, false, context);
        NeoForge.EVENT_BUS.post(event);
        return event.getAmount();
    }

    /** For the part-switch test: with a Blood Moon installed, a bite of 4 gives 5 when the part is on and 4 when it is off. */
    public static void biteHook(GameTestHelper helper, boolean partOn) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        int amount = feed(p, EntityType.VILLAGER.create(helper.getLevel()), 4);
        VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
        helper.assertTrue(amount == (partOn ? 5 : 4), "bite hook: part " + (partOn ? "on" : "off") + ", a bite of 4 gave " + amount);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBloodMoonBiteGivesMoreBlood(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        var villager = EntityType.VILLAGER.create(helper.getLevel());
        helper.assertTrue(feed(p, villager, 4) == 4, "control: a bite without a mood changed");
        CelestialsGameTests.with(new FakeEc().running("blood_moon"), () -> {
            helper.assertTrue(feed(p, villager, 4) == 5, "a Blood Moon bite of 4 gave " + feed(p, villager, 4) + ", not 5 (x1.25)");
            helper.assertTrue(feed(p, villager, 0) == 0, "an emptied bite (a vervain victim) got blood on a Blood Moon");
            helper.assertTrue(feed(p, villager, 1) == 1, "a bite of 1 grew by a quarter");
        });
        CelestialsGameTests.with(new FakeEc().running("super_blood_moon"), () -> helper.assertTrue(feed(p, villager, 8) == 10, "a Super Blood Moon bite of 8"));
        CelestialsGameTests.with(new FakeEc().running("blue_moon"), () -> helper.assertTrue(feed(p, villager, 4) == 4, "a Blue Moon changed a bite"));
        helper.assertTrue(feed(p, villager, 4) == 4, "the bonus stayed after the moon");
        VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
        helper.succeed();
    }
}
