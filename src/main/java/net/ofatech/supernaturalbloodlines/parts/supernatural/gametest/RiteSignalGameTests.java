package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import java.util.List;
import net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.favouriteless.enchanted.common.init.EData;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig;

/**
 * Real rites started on a circle: the mixin on Enchanted's {@code Rite.start()} signals a rite that worked, once, and never a rite that
 * cancelled itself. Needs Enchanted, so the part registers it only then.
 */
@PrefixGameTestTemplate(false)
public final class RiteSignalGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private RiteSignalGameTests() {}

    private static RiteType type(GameTestHelper helper, String path) {
        return helper.getLevel().registryAccess().registryOrThrow(EData.RITE_TYPES_REGISTRY)
                .get(ResourceLocation.fromNamespaceAndPath(NS, path));
    }

    private static BlockPos centre(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(1, 1, 1));
    }

    /** FakePlayers are not in the level, which is where a rite looks for the caster: put one there on the circle. */
    private static ServerPlayer witch(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        p.setPos(Vec3.atBottomCenterOf(centre(helper)));
        helper.getLevel().addNewPlayer(p);
        return p;
    }

    private static void leave(ServerLevel level, ServerPlayer p) {
        level.removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    private static void run(GameTestHelper helper, String rite, ServerPlayer caster) {
        Rite r = type(helper, rite).create(helper.getLevel(), centre(helper), caster.getUUID(), List.of());
        r.start();
    }

    private static long anyRites(ServerPlayer p) {
        return SupernaturalAccess.get(p).counters().getOrDefault("L-WIT-1a", 0L);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anEnchantedTypeCountsOnce(GameTestHelper helper) {
        ServerPlayer p = witch(helper);
        try {
            SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().rung(SupernaturalRace.WITCH, 1).build());
            run(helper, "ward_of_return", p); // an enchanted:create_item rite, no factory of ours in it
            SupernaturalState s = SupernaturalAccess.get(p);
            helper.assertTrue(s.done().contains("L-WIT-2a.3"), "the Ward of Return did not complete L-WIT-2a: " + s.done());
        } finally {
            leave(helper.getLevel(), p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anyRiteIsCountedExactlyOnce(GameTestHelper helper) {
        ServerPlayer p = witch(helper);
        try {
            run(helper, "ward_of_return", p);
            helper.assertTrue(anyRites(p) == 1, "one rite counted " + anyRites(p) + " times: " + SupernaturalAccess.get(p).counters());
            run(helper, "ward_of_return", p);
            helper.assertTrue(anyRites(p) == 2, "two rites counted " + anyRites(p));
        } finally {
            leave(helper.getLevel(), p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRiteOfOursCountsOnceToo(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        p.setPos(Vec3.atBottomCenterOf(centre(helper)));
        helper.getLevel().addNewPlayer(p);
        try {
            run(helper, "ancestral_calling", p); // a human becomes a witch, and has performed one rite
            helper.assertTrue(BloodlineAccess.get(p).species() == Species.WITCH || BloodlineAccess.get(p).species() == Species.SIPHONER,
                    "the rite did not run: " + BloodlineAccess.get(p).species());
            if (BloodlineAccess.get(p).species() == Species.WITCH) {
                helper.assertTrue(anyRites(p) == 1, "the Ancestral Calling counted " + anyRites(p) + " times");
            }
        } finally {
            leave(helper.getLevel(), p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCancelledRiteCountsForNothing(GameTestHelper helper) {
        ServerPlayer p = witch(helper);
        try {
            run(helper, "hybrid_blood", p); // the player is not a hybrid: the rite cancels itself
            helper.assertTrue(anyRites(p) == 0, "a cancelled rite was counted: " + SupernaturalAccess.get(p).counters());
            run(helper, "original_blood", p);
            helper.assertTrue(anyRites(p) == 0, "a second cancelled rite was counted");
        } finally {
            leave(helper.getLevel(), p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSwitchTurnsTheSignalOff(GameTestHelper helper) {
        ServerPlayer p = witch(helper);
        try {
            SupernaturalConfig.overrideRiteCompleteSignal(false);
            run(helper, "ward_of_return", p);
            helper.assertTrue(anyRites(p) == 0, "the rite counted with the switch off");
        } finally {
            SupernaturalConfig.overrideRiteCompleteSignal(null);
            leave(helper.getLevel(), p);
        }
        helper.succeed();
    }
}
