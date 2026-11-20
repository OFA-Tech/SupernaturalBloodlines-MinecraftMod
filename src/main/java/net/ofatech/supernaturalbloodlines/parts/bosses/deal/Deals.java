package net.ofatech.supernaturalbloodlines.parts.bosses.deal;

import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.DealRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossClock;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.ElijahEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.NpcOriginalBoss;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.item.SiresBloodItem;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;

/** The server side of Elijah's deal (R167): the offer, the answer, and the oath that is broken by a blow. */
public final class Deals {
    /** How near the player must be to the one who made the offer when they answer (blocks). */
    public static final double REACH = 8.0;

    private Deals() {
    }

    /** What Elijah may give this player, in order (the list the client sees, and the one the answer indexes). */
    public static List<NpcOriginal> options(ServerPlayer p) {
        return DealRules.options(LineIndex.get(p.server).rootOf(p.getUUID()).orElse(null), BloodlineAccess.get(p).knownLine(), SpeciesConfig.originalVisibilityAlways());
    }

    /** The player spoke to Elijah: the offer if they have no deal, else what he says. */
    @org.jetbrains.annotations.Nullable
    public static DealOfferPayload talk(ElijahEntity elijah, ServerPlayer p) {
        BossIndex index = BossIndex.get(p.server);
        long today = BossClock.today(p.server.overworld());
        long until = index.oathOf(p.getUUID());
        if (elijah.hostileForGood()) {
            return null;
        }
        if (DealRules.offers(until, false, elijah.isDesiccated())) {
            List<String> ids = options(p).stream().map(NpcOriginal::id).toList();
            DealOfferPayload offer = new DealOfferPayload(elijah.getId(), BossesConfig.elijahOathDays(), ids);
            PacketDistributor.sendToPlayer(p, offer);
            return offer;
        } else if (DealRules.binding(until, today)) {
            p.displayClientMessage(Component.translatable("supernaturalbloodlines.boss.elijah.keep", until - today), true);
        } else {
            p.displayClientMessage(Component.translatable("supernaturalbloodlines.boss.elijah.done"), true);
        }
        return null;
    }

    /** The player answered: -1 is no deal (and they may be offered again); else the vial, the oath, and the record written to disk at once. */
    public static void choose(ServerPlayer p, int entityId, int answer) {
        Entity e = p.level().getEntity(entityId);
        if (!(e instanceof ElijahEntity elijah) || !elijah.isAlive() || p.distanceToSqr(elijah) > REACH * REACH) {
            return;
        }
        BossIndex index = BossIndex.get(p.server);
        if (!DealRules.offers(index.oathOf(p.getUUID()), elijah.hostileForGood(), elijah.isDesiccated())) {
            return;
        }
        List<NpcOriginal> options = options(p);
        if (answer < 0) {
            p.displayClientMessage(Component.translatable("supernaturalbloodlines.boss.elijah.declined"), true);
            return;
        }
        if (answer >= options.size()) {
            return;
        }
        NpcOriginal chosen = options.get(answer);
        long today = BossClock.today(p.server.overworld());
        index.setOath(p.getUUID(), DealRules.sworn(today, BossesConfig.elijahOathDays()));
        BossIndex.flush(p.server);
        String name = chosen.id().substring(0, 1).toUpperCase(Locale.ROOT) + chosen.id().substring(1);
        p.getInventory().placeItemBackInInventory(SiresBloodItem.of(chosen.uuid(), name));
        p.sendSystemMessage(Component.translatable("supernaturalbloodlines.boss.elijah.sworn", Component.translatable(chosen.translationKey()), BossesConfig.elijahOathDays()));
    }

    /** A player struck an NPC Original: the oath breaks if it binds them and the victim is one it protects (R167). Elijah is hostile for good from then on. */
    public static void struck(ServerPlayer attacker, NpcOriginalBoss victim) {
        BossIndex index = BossIndex.get(attacker.server);
        long today = BossClock.today(attacker.server.overworld());
        if (!DealRules.broken(index.oathOf(attacker.getUUID()), today, victim.boss())) {
            return;
        }
        index.setOath(attacker.getUUID(), DealRules.BROKEN);
        index.of(BossId.ELIJAH).hostile = true;
        index.setDirty();
        BossIndex.flush(attacker.server);
        attacker.sendSystemMessage(Component.translatable("supernaturalbloodlines.boss.elijah.broken"));
        if (BossDirector.bossEntity(attacker.server, BossId.ELIJAH) instanceof ElijahEntity elijah) {
            elijah.setTarget(attacker);
        }
    }
}