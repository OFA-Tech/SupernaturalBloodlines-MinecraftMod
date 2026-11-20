package net.ofatech.supernaturalbloodlines.parts.bosses.deal;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.DealRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossClock;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.ElijahEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.NpcOriginalBoss;
import net.ofatech.supernaturalbloodlines.parts.species.item.SiresBloodItem;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import org.jetbrains.annotations.Nullable;

/** The server side of Elijah's deal (R167): the offer, the answer, and the oath that is broken by a blow. */
public final class Deals {
    /** How near the player must be to the one who made the offer when they answer (blocks). */
    public static final double REACH = 8.0;
    /** How long Elijah stands still waiting for the answer (ticks, 60 seconds). */
    public static final int LISTEN_TICKS = 1200;
    /** What the client sends for "no deal". */
    public static final String NO_DEAL = "";

    private Deals() {
    }

    /** "1 in-game day", "7 in-game days". */
    public static Component days(long n) {
        return Component.translatable("supernaturalbloodlines.boss.days." + (n == 1 ? "one" : "other"), n);
    }

    /** The ids the offer lists, in order: the same for every player. */
    public static List<String> ids() {
        return DealRules.options().stream().map(NpcOriginal::id).toList();
    }

    /** The player spoke to Elijah: the offer (and he waits for the answer) if they have no deal, else what he says. Returns the offer, if one was sent. */
    @Nullable
    public static DealOfferPayload talk(ElijahEntity elijah, ServerPlayer p) {
        BossIndex index = BossIndex.get(p.server);
        long today = BossClock.today(p.server.overworld());
        long until = index.oathOf(p.getUUID());
        if (elijah.hostileForGood()) {
            return null;
        }
        if (DealRules.offers(until, false, elijah.isDesiccated())) {
            DealOfferPayload offer = new DealOfferPayload(elijah.getId(), BossesConfig.elijahOathDays(), ids());
            PacketDistributor.sendToPlayer(p, offer);
            elijah.listenFor(LISTEN_TICKS, p.getUUID());
            elijah.getLookControl().setLookAt(p);
            return offer;
        } else if (DealRules.binding(until, today)) {
            long left = until - today;
            p.displayClientMessage(Component.translatable("supernaturalbloodlines.boss.elijah.keep", days(left)), true);
        } else {
            p.displayClientMessage(Component.translatable("supernaturalbloodlines.boss.elijah.done"), true);
        }
        return null;
    }

    private static void say(ServerPlayer p, String key) {
        p.displayClientMessage(Component.translatable("supernaturalbloodlines.boss.elijah." + key), true);
    }

    /**
     * The player answered with the id of the Original they chose, or {@link #NO_DEAL}: the server checks it against the list it would offer, and every refusal
     * says so on the action bar. A deal gives the vial, swears the oath and writes the record to disk at once.
     */
    public static void choose(ServerPlayer p, int entityId, String answer) {
        Entity e = p.level().getEntity(entityId);
        if (!(e instanceof ElijahEntity elijah) || !elijah.isAlive()) {
            say(p, "gone");
            return;
        }
        if (p.distanceToSqr(elijah) > REACH * REACH) {
            say(p, "gone");
            return;
        }
        BossIndex index = BossIndex.get(p.server);
        if (!DealRules.offers(index.oathOf(p.getUUID()), elijah.hostileForGood(), elijah.isDesiccated())) {
            say(p, "done");
            return;
        }
        // they were within reach and could have been offered: this is an answer, whatever it says (and only theirs frees him)
        elijah.stopListening(p.getUUID());
        if (answer.equals(NO_DEAL)) {
            say(p, "declined");
            return;
        }
        Optional<NpcOriginal> chosen = DealRules.choose(answer);
        if (chosen.isEmpty()) {
            say(p, "refused");
            return;
        }
        long today = BossClock.today(p.server.overworld());
        index.setOath(p.getUUID(), DealRules.sworn(today, BossesConfig.elijahOathDays()));
        BossIndex.flush(p.server);
        NpcOriginal o = chosen.get();
        String name = o.id().substring(0, 1).toUpperCase(Locale.ROOT) + o.id().substring(1);
        p.getInventory().placeItemBackInInventory(SiresBloodItem.of(o.uuid(), name));
        p.sendSystemMessage(Component.translatable("supernaturalbloodlines.boss.elijah.sworn", Component.translatable(o.translationKey()), days(BossesConfig.elijahOathDays())));
    }

    /** Who is behind a blow: the attacker, or the owner of a tamed animal, or the lord of a Vampirism minion, when that is a player. */
    @Nullable
    public static ServerPlayer playerBehind(net.minecraft.server.MinecraftServer server, @Nullable Entity attacker) {
        Entity who = attacker;
        for (int hops = 0; hops < 3 && who != null && !(who instanceof ServerPlayer); hops++) {
            if (who instanceof OwnableEntity ownable) {
                // by uuid, server-wide: the owner may be in another dimension, where getOwner() would not find them
                java.util.UUID id = ownable.getOwnerUUID();
                who = id == null ? null : SireCall.player(server, id);
            } else if (who instanceof de.teamlapen.vampirism.api.entity.minion.IMinionEntity minion) {
                who = minion.getLordOpt().map(de.teamlapen.vampirism.api.entity.player.ILordPlayer::getPlayer).orElse(null);
            } else {
                return null;
            }
        }
        return who instanceof ServerPlayer p ? p : null;
    }

    /** Whether this blow, struck by {@code striker} for {@code owner}, is held against the owner (see {@link DealRules#counts}). */
    public static boolean counts(ServerPlayer owner, @Nullable Entity striker, NpcOriginalBoss victim) {
        return DealRules.counts(striker == owner, victim.getTarget() == owner || victim.getLastHurtByMob() == owner);
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
            elijah.stopListening(null);
            elijah.setTarget(attacker);
        }
    }
}
