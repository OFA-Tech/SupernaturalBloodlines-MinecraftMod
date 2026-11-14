package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.entity.player.skills.SkillPointProviders;
import de.teamlapen.vampirism.api.entity.player.vampire.IDrinkBloodContext;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.turning.GhoulGates;

/** Ghouls (R20e, R20f): what a finished Sanguinare leaves, and what a Ghoul cannot do. Needs no Werewolves. */
@PrefixGameTestTemplate(false)
public final class GhoulGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String KEY = "supernaturalbloodlines.turning.";
    private static final ResourceLocation PROBE = ResourceLocation.fromNamespaceAndPath(NS, "test_negative_skill_points");
    private static final UUID PROBE_PLAYER = UUID.randomUUID();
    private static boolean probeRegistered;

    private GhoulGameTests() {
    }

    private static IFactionPlayerHandler handler(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p);
    }

    private static void vampireAt(GameTestHelper helper, ServerPlayer p, int level) {
        helper.assertTrue(handler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, level), "could not make a level " + level + " vampire");
    }

    private static void makeGhoul(GameTestHelper helper, ServerPlayer p) {
        vampireAt(helper, p, 1);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().ghoul(true).build());
    }

    private static void reset(ServerPlayer... players) {
        for (ServerPlayer p : players) {
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
    }

    private static Holder<MobEffect> sanguinare() {
        return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("vampirism:sanguinare")).orElseThrow();
    }

    /** The last tick of a Sanguinare: Vampirism's own effect code calls {@code onSanguinareFinished}. */
    private static void finishSanguinare(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(sanguinare(), 100));
        sanguinare().value().applyEffectTick(p, 0);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    private static PlayerInteractEvent.RightClickBlock clickAltar(GameTestHelper helper, ServerPlayer p, String altar) {
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(abs, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(altar)).defaultBlockState());
        var click = new PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, abs,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        NeoForge.EVENT_BUS.post(click);
        return click;
    }

    private static void drinkFrom(ServerPlayer drinker, LivingEntity source, int amount) {
        VampirismAPI.vampirePlayer(drinker).drinkBlood(amount, 0.1f, new IDrinkBloodContext() {
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
        });
    }

    // ---- making a Ghoul ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ghoulMadeBySanguinare(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            finishSanguinare(p);
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(s.ghoul() && s.species() == Species.NONE, "not a Ghoul: " + s);
            helper.assertTrue(handler(p).getCurrentFaction() == VReference.VAMPIRE_FACTION && handler(p).getCurrentLevel() == 1,
                    "not a level 1 vampire: " + handler(p).getCurrentFaction() + " " + handler(p).getCurrentLevel());
            helper.assertTrue(GhoulGates.isGhoul(p), "isGhoul is false");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void existingVampireUnaffected(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        vampireAt(helper, p, 3);
        try {
            finishSanguinare(p);
            helper.assertTrue(!BloodlineAccess.get(p).ghoul() && !GhoulGates.isGhoul(p), "an existing vampire became a Ghoul");
            helper.assertTrue(handler(p).setFactionLevel(VReference.VAMPIRE_FACTION, 4) && handler(p).getCurrentLevel() == 4,
                    "an existing vampire cannot level up: " + handler(p).getCurrentLevel());
            helper.assertTrue(!clickAltar(helper, p, "vampirism:altar_infusion").isCanceled(), "an altar click was refused for a vampire");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ourOwnTurningIsNotASanguinareJoin(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().turningTicksLeft(9000).build());
            p.addEffect(new MobEffectInstance(sanguinare(), 1000)); // a Sanguinare is running while the turning completes
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("vampirism:blood_bottle")));
            NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND));
            helper.assertTrue(handler(p).getCurrentFaction() == VReference.VAMPIRE_FACTION, "the turning did not complete");
            BloodlineState s = BloodlineAccess.get(p);
            helper.assertTrue(!s.ghoul() && !s.inTurning(), "the completed turning left a Ghoul: " + s);
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void leavingTheVampiresEndsTheGhoul(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        try {
            finishSanguinare(p);
            helper.assertTrue(BloodlineAccess.get(p).ghoul(), "setup: not a Ghoul");
            handler(p).leaveFaction(true);
            helper.assertTrue(!BloodlineAccess.get(p).ghoul(), "a cured Ghoul is still flagged");
            vampireAt(helper, p, 2);
            helper.assertTrue(!GhoulGates.isGhoul(p) && handler(p).getCurrentLevel() == 2, "the old flag gates a new vampire");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    // ---- gates ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ghoulCannotLevelUp(GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        try {
            makeGhoul(helper, p);
            helper.assertTrue(!handler(p).setFactionLevel(VReference.VAMPIRE_FACTION, 2), "setFactionLevel(2) was allowed");
            helper.assertTrue(!handler(p).setFactionAndLevel(VReference.VAMPIRE_FACTION, 3), "setFactionAndLevel(3) was allowed");
            helper.assertTrue(handler(p).getCurrentLevel() == 1, "level is " + handler(p).getCurrentLevel());
            helper.assertTrue(p.received(KEY + "ghoul_no_level"), "the player was not told why");
            helper.assertTrue(handler(p).setFactionLevel(VReference.VAMPIRE_FACTION, 1), "staying at level 1 was refused");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ghoulCannotStartAltarRitual(GameTestHelper helper) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        try {
            makeGhoul(helper, p);
            for (String altar : new String[] {"vampirism:altar_infusion", "vampirism:altar_inspiration"}) {
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND, 5));
                var click = clickAltar(helper, p, altar);
                helper.assertTrue(click.isCanceled() && !click.getCancellationResult().consumesAction(), altar + " was not refused");
                helper.assertTrue(p.getMainHandItem().is(Items.DIAMOND) && p.getMainHandItem().getCount() == 5, "the items changed");
            }
            helper.assertTrue(p.received(KEY + "ghoul_no_altar"), "the player was not told why");
        } finally {
            reset(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ghoulGetsNoSkillPoints(GameTestHelper helper) {
        // Vampirism sums every provider's answer floored at 0 (SkillHandler$SkillPoints), so no provider can take points away.
        // The proof: a provider answering -100 for a probe player changes nothing. What keeps a Ghoul at the level 1 points is the cap.
        if (!probeRegistered) {
            SkillPointProviders.register(PROBE, f -> f.getRepresentingPlayer().getUUID().equals(PROBE_PLAYER) ? -100 : 0);
            probeRegistered = true;
        }
        ServerPlayer plain = TestPlayers.createMortal(helper);
        ServerPlayer higher = TestPlayers.createMortal(helper);
        ServerPlayer ghoul = TestPlayers.createMortal(helper);
        try {
            vampireAt(helper, plain, 1);
            vampireAt(helper, higher, 5);
            makeGhoul(helper, ghoul);
            int levelOne = VampirismAPI.vampirePlayer(plain).getSkillHandler().getLeftSkillPoints();
            int level5 = VampirismAPI.vampirePlayer(higher).getSkillHandler().getLeftSkillPoints();
            int ghoulPoints = VampirismAPI.vampirePlayer(ghoul).getSkillHandler().getLeftSkillPoints();
            SupernaturalBloodlines.LOGGER.info("ghoul skill points: level 1 = {}, level 5 = {}, ghoul = {}", levelOne, level5, ghoulPoints);
            helper.assertTrue(ghoulPoints == levelOne, "a Ghoul has " + ghoulPoints + " points, a level 1 vampire " + levelOne);
            helper.assertTrue(level5 > levelOne, "level 5 has no more points than level 1 (" + level5 + " vs " + levelOne + "): the cap would mean nothing");
        } finally {
            reset(plain, higher, ghoul);
        }
        helper.succeed();
    }

    // ---- biting a vampire ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bitingVampireGivesNoBloodAndCosts(GameTestHelper helper) {
        ServerPlayer biter = TestPlayers.createMortal(helper);
        ServerPlayer source = TestPlayers.createMortal(helper);
        try {
            vampireAt(helper, biter, 1);
            vampireAt(helper, source, 1);
            VampirismAPI.vampirePlayer(biter).useBlood(10, true);
            int start = VampirismAPI.vampirePlayer(biter).getBloodStats().getBloodLevel();
            helper.assertTrue(start <= 10, "setup: blood level is " + start);
            // control: a villager's blood does fill
            drinkFrom(biter, EntityType.VILLAGER.create(helper.getLevel()), 2);
            int afterVillager = VampirismAPI.vampirePlayer(biter).getBloodStats().getBloodLevel();
            helper.assertTrue(afterVillager > start, "a villager gave no blood (" + start + " -> " + afterVillager + ")");
            drinkFrom(biter, source, 4);
            int afterVampire = VampirismAPI.vampirePlayer(biter).getBloodStats().getBloodLevel();
            helper.assertTrue(afterVampire == afterVillager - 1, "blood went " + afterVillager + " -> " + afterVampire + ", not down by bite_vampire_cost");
        } finally {
            reset(biter, source);
        }
        helper.succeed();
    }
}
