package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import de.teamlapen.vampirism.api.entity.player.vampire.IDrinkBloodContext;
import java.util.Optional;
import java.util.Set;
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
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;

/** The BLOOD skills (phase 12c Task 3): what a feed gives. */
@PrefixGameTestTemplate(false)
public final class BloodSkillGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private BloodSkillGameTests() {}

    private static IDrinkBloodContext from(LivingEntity source) {
        return new IDrinkBloodContext() {
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
    }

    /** Posts the event Vampirism fires when {@code drinker} feeds, and returns the amount the vampire ends up with. */
    private static int feed(ServerPlayer drinker, LivingEntity source, int amount) {
        var event = new BloodDrinkEvent.PlayerDrinkBloodEvent(VampirismAPI.vampirePlayer(drinker), amount, 0.5f, false, from(source));
        NeoForge.EVENT_BUS.post(event);
        return event.getAmount();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void keenFangsAddsBlood(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        var villager = EntityType.VILLAGER.create(helper.getLevel());
        var cow = EntityType.COW.create(helper.getLevel());
        helper.assertTrue(feed(p, villager, 2) == 2, "a feed without the skill changed");
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().skills(Set.of("S-VAM-1")).build());
        helper.assertTrue(feed(p, villager, 2) == 3, "a human-like feed with Keen Fangs gave " + feed(p, villager, 2) + ", not 3");
        helper.assertTrue(feed(p, cow, 2) == 2, "an animal's blood gave more");
        helper.assertTrue(feed(p, villager, 0) == 0, "a feed that gave nothing (a vervain victim) gave something");
        // a skill that is dormant (the player is no vampire now) adds nothing, and nobody else gains from it
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(null, 0), "could not leave the vampires");
        helper.assertTrue(feed(p, villager, 2) == 2, "a dormant Keen Fangs still adds blood");
        helper.succeed();
    }
}
