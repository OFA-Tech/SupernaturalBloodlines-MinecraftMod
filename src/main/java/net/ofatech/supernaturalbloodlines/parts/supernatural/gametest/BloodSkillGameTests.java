package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import net.ofatech.supernaturalbloodlines.parts.supernatural.skill.BloodEffects;
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

    static IDrinkBloodContext from(LivingEntity source) {
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
    static int feed(ServerPlayer drinker, LivingEntity source, int amount) {
        var event = new BloodDrinkEvent.PlayerDrinkBloodEvent(VampirismAPI.vampirePlayer(drinker), amount, 0.5f, false, from(source));
        NeoForge.EVENT_BUS.post(event);
        return event.getAmount();
    }

    /** One feed is many bites (Vampirism fires the event for every bite of a drink): the skill adds its blood once. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void keenFangsCountsOncePerFeed(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().skills(Set.of("S-VAM-1")).build());
        var first = EntityType.VILLAGER.create(helper.getLevel());
        var second = EntityType.VILLAGER.create(helper.getLevel());
        int total = 0;
        for (int bite = 0; bite < 5; bite++) {
            total += feed(p, first, 2);
        }
        helper.assertTrue(total == 5 * 2 + 1, "five bites of one villager gave " + total + ", not 11");
        helper.assertTrue(feed(p, second, 2) == 3, "a second villager is a feed of its own");
        helper.assertTrue(feed(p, first, 2) == 2, "the chain with the first villager was still going");
        // after a pause the next bite is a new feed
        BloodEffects.forgetBitesForTest();
        helper.assertTrue(feed(p, first, 2) == 3, "a new feed after the pause got no extra");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void keenFangsAddsBlood(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "could not make a vampire");
        var villager = EntityType.VILLAGER.create(helper.getLevel());
        var cow = EntityType.COW.create(helper.getLevel());
        helper.assertTrue(feed(p, villager, 2) == 2, "a feed without the skill changed");
        SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().skills(Set.of("S-VAM-1")).build());
        int fed = feed(p, villager, 2);
        helper.assertTrue(fed == 3, "a human-like feed with Keen Fangs gave " + fed + ", not 3");
        helper.assertTrue(feed(p, cow, 2) == 2, "an animal's blood gave more");
        BloodEffects.forgetBitesForTest();
        helper.assertTrue(feed(p, villager, 0) == 0, "a feed that gave nothing (a vervain victim) gave something");
        // a skill that is dormant (the player is no vampire now) adds nothing, and nobody else gains from it
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(null, 0), "could not leave the vampires");
        BloodEffects.forgetBitesForTest();
        helper.assertTrue(feed(p, villager, 2) == 2, "a dormant Keen Fangs still adds blood");
        helper.succeed();
    }
}
