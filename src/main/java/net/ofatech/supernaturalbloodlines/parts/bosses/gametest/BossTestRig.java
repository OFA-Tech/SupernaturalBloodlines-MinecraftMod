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
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BeastDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBars;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossClock;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.KlausEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.MikaelEntity;
import net.ofatech.supernaturalbloodlines.parts.species.item.WhiteOak;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineWarning;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/** A boss with fixed facts for the GameTests (plan 17, "BossTestRig"): the index starts clean, the world is put back at the end. */
final class BossTestRig implements AutoCloseable {
    static final UUID MIKAEL = NpcOriginal.MIKAEL.uuid();
    static final UUID KLAUS = NpcOriginal.KLAUS.uuid();

    final GameTestHelper helper;
    final MinecraftServer server;
    final BossIndex index;
    final long dayTime;
    MikaelEntity mikael;
    KlausEntity klaus;

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
        var oldKlaus = BossDirector.bossEntity(server, BossId.KLAUS);
        if (oldKlaus != null) {
            oldKlaus.discard();
        }
        index.forget(BossId.KLAUS);
        LineIndex.get(server).unmarkDead(KLAUS);
        var oldDeucalion = BossDirector.anyBossBody(server, BossId.DEUCALION);
        if (oldDeucalion instanceof net.ofatech.supernaturalbloodlines.parts.bosses.entity.DeucalionEntity d) {
            d.dismissPack();
        }
        if (oldDeucalion != null) {
            oldDeucalion.discard();
        }
        index.forget(BossId.DEUCALION);
        BossesConfig.override("deucalion_pack_alphas", null);
        var oldPeter = BossDirector.anyBossBody(server, BossId.PETER_HALE);
        if (oldPeter != null) {
            oldPeter.discard();
        }
        index.forget(BossId.PETER_HALE);
        BeastDirector.clear(server);
        var oldBeast = BossDirector.anyBossBody(server, BossId.BEAST_OF_GEVAUDAN);
        if (oldBeast != null) {
            oldBeast.discard();
        }
        index.forget(BossId.BEAST_OF_GEVAUDAN);
        for (String key : new String[] {"beast_of_gevaudan_enabled", "beast_host_chance", "beast_respects_ash", "beast_returns_once", "beast_spirit_seconds", "beast_roar_range", "beast_roar_seconds", "beast_roar_cooldown_seconds", "peter_hale_enabled", "peter_respawn", "peter_respawn_days", "peter_feral_full_moon", "peter_house_enabled", "peter_house_chance"}) {
            BossesConfig.override(key, null);
        }
        for (BossId other : new BossId[] {BossId.ELIJAH, BossId.REBEKAH, BossId.KOL, BossId.FINN}) {
            var e = BossDirector.bossEntity(server, other);
            if (e != null) {
                e.discard();
            }
            index.forget(other);
            LineIndex.get(server).unmarkDead(other.npcOriginal().orElseThrow().uuid());
        }
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

    /** Klaus, made the way the director makes him, standing 2 blocks into the test. */
    KlausEntity spawnKlaus() {
        klaus = BossDirector.spawnKlaus(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
        return klaus;
    }

    /** Elijah, made the way the director makes him, standing 2 blocks into the test. */
    net.ofatech.supernaturalbloodlines.parts.bosses.entity.ElijahEntity spawnElijah() {
        return BossDirector.spawnElijah(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
    }

    /** Kol, made the way the director makes him (no prey), standing 2 blocks into the test. */
    net.ofatech.supernaturalbloodlines.parts.bosses.entity.KolEntity spawnKol() {
        return BossDirector.spawnKol(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
    }

    /** Finn, made the way the director makes him (no target), standing 2 blocks into the test, at the start of his ritual. */
    net.ofatech.supernaturalbloodlines.parts.bosses.entity.FinnEntity spawnFinn() {
        return BossDirector.spawnFinn(helper.getLevel(), null, helper.absolutePos(new BlockPos(2, 1, 2))).orElseThrow();
    }

    /** Rebekah, made the way the director makes her (no prey), standing near the middle of the test. */
    net.ofatech.supernaturalbloodlines.parts.bosses.entity.RebekahEntity spawnRebekah() {
        return BossDirector.spawnRebekah(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), null, null).orElseThrow();
    }

    /** The item entities of this kind lying near this entity. */
    long dropsNear(net.minecraft.world.entity.Entity e, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, e.getBoundingBox().inflate(6.0), i -> i.getItem().is(item)).size();
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
        BossDirector.useOnGround(null);
        BossDirector.useGround(null);
        BossDirector.useHouses(null);
        BossDirector.useMods(null);
        BossDirector.useMoon(null);
        net.ofatech.supernaturalbloodlines.parts.bosses.item.MantleEvents.useShifted(null);
        BossDirector.useEclipse(null);
        BossDirector.useVillagers(null);
        SireCall.overridePlayerLookup(null);
        LineWarning.overridePlayers(null);
        reset();
    }
}
