package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBars;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossClock;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.MikaelEntity;
import net.ofatech.supernaturalbloodlines.parts.species.item.WhiteOak;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineWarning;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/** A boss with fixed facts for the GameTests (plan 17, "BossTestRig"): the index starts clean, the world is put back at the end. */
final class BossTestRig implements AutoCloseable {
    static final UUID MIKAEL = NpcOriginal.MIKAEL.uuid();

    final GameTestHelper helper;
    final MinecraftServer server;
    final BossIndex index;
    final long dayTime;
    MikaelEntity mikael;

    BossTestRig(GameTestHelper helper) {
        this.helper = helper;
        this.server = helper.getLevel().getServer();
        this.index = BossIndex.get(server);
        this.dayTime = helper.getLevel().getDayTime();
        reset();
    }

    private void reset() {
        var old = BossDirector.bossEntity(server, BossId.MIKAEL);
        if (old != null) {
            old.discard();
        }
        index.forget(BossId.MIKAEL);
        LineIndex.get(server).unmarkDead(MIKAEL);
        LineWarning.reset();
        BossBars.reset();
        BossesConfig.override("original_return_days", null);
    }

    /** Makes it day number {@code n} for the desiccation clock (game time), without touching the world's time. */
    void day(long n) {
        BossClock.offsetForTests(n * 24000L + 100 - helper.getLevel().getGameTime());
    }

    /** The director puts him a few blocks from his target, not wherever the neighbouring tests left ground. */
    static void fixedGround() {
        BossDirector.useGround((level, from, min, max) -> java.util.Optional.of(from.offset(3, 0, 3)));
    }

    /** Mikael, made the way the director makes him, standing 2 blocks into the test. */
    MikaelEntity spawn() {
        mikael = BossDirector.spawnMikael(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
        return mikael;
    }

    /** A player holding a white oak stake, and the blow it makes. */
    DamageSource whiteOakBlow() {
        ServerPlayer killer = TestPlayers.create(helper);
        killer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(WhiteOak.STAKE.get()));
        return killer.damageSources().playerAttack(killer);
    }

    DamageSource blowWith(Item item) {
        ServerPlayer killer = TestPlayers.create(helper);
        killer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        return killer.damageSources().playerAttack(killer);
    }

    DamageSource plainBlow() {
        return helper.getLevel().damageSources().magic();
    }

    /** The item entities of this kind lying near the boss. */
    long dropsOf(Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, mikael.getBoundingBox().inflate(6.0), e -> e.getItem().is(item)).size();
    }

    List<ItemEntity> drops() {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, mikael.getBoundingBox().inflate(6.0));
    }

    long bloodBottles() {
        return drops().stream().filter(e -> MIKAEL.equals(net.ofatech.supernaturalbloodlines.parts.species.item.SiresBloodItem.sourceOf(e.getItem()))).count();
    }

    @Override
    public void close() {
        BossClock.offsetForTests(0);
        helper.getLevel().setDayTime(dayTime);
        if (mikael != null) {
            drops().forEach(net.minecraft.world.entity.Entity::discard);
        }
        BossDirector.useRoll(null);
        BossDirector.useWorld(null, null);
        BossDirector.useRingCheck(null);
        BossDirector.useDifficulty(null);
        BossDirector.useGround(null);
        SireCall.overridePlayerLookup(null);
        LineWarning.overridePlayers(null);
        reset();
    }
}
