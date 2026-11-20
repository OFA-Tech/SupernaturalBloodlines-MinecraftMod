package net.ofatech.supernaturalbloodlines.parts.bosses.link;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.core.boss.LinkRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;

/**
 * The Linking Spell at work (R177): damage to one of two linked creatures is shared with the other, once, and the link ends with a death, a dimension change
 * or its time. A linked player is told when it begins and when it ends.
 */
public final class Links {
    public static final String LINKED = "supernaturalbloodlines.rite.link_linked";
    public static final String ENDED = "supernaturalbloodlines.rite.link_ended";

    /** The creatures that are taking a share right now: a share dealt to one of them is never shared again, whatever else happens inside the hit. */
    private static final Set<UUID> SHARING = new HashSet<>();

    private Links() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, LivingDamageEvent.Post.class, Links::onDamaged);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, LivingDeathEvent.class, Links::onDeath);
        NeoForge.EVENT_BUS.addListener(Links::onTravel);
        NeoForge.EVENT_BUS.addListener(Links::onJoin);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % 100 == 0) {
                expire(e.getServer(), e.getServer().overworld().getGameTime());
            }
        });
    }

    /** Ends every link that ran out by {@code now}, telling the players in it. */
    public static void expire(MinecraftServer server, long now) {
        LinkIndex index = LinkIndex.get(server);
        for (LinkIndex.Link link : index.expiredAt(now)) {
            end(server, index, link);
        }
    }

    /** Links two creatures for {@code seconds}; a player on either side is told. */
    public static void link(MinecraftServer server, Entity a, Entity b, int seconds) {
        LinkIndex.get(server).add(a.getUUID(), b.getUUID(), server.overworld().getGameTime() + seconds * 20L, a instanceof Player, b instanceof Player);
        tell(a, b, seconds);
        tell(b, a, seconds);
    }

    /** Links two creatures by id (they need not be loaded): nobody is told. */
    public static void link(MinecraftServer server, UUID a, UUID b, int seconds) {
        LinkIndex.get(server).add(a, b, server.overworld().getGameTime() + seconds * 20L);
    }

    private static void tell(Entity who, Entity with, int seconds) {
        if (who instanceof ServerPlayer p) {
            p.sendSystemMessage(Component.translatable(LINKED, with.getDisplayName(), seconds, Math.round(BossesConfig.linkDamageShare() * 100)));
        }
    }

    private static Entity find(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    /** The link is over: whoever of the two is a player and here is told, and it is taken out of the list. */
    static void end(MinecraftServer server, LinkIndex index, LinkIndex.Link link) {
        for (UUID id : new UUID[] {link.a(), link.b()}) {
            if (link.isPlayer(id) && find(server, id) instanceof ServerPlayer p) {
                p.sendSystemMessage(Component.translatable(ENDED));
            }
        }
        index.remove(link);
    }

    private static void onDamaged(LivingDamageEvent.Post e) {
        LivingEntity hurt = e.getEntity();
        if (hurt.level().isClientSide || hurt.level().getServer() == null) {
            return;
        }
        MinecraftServer server = hurt.level().getServer();
        LinkIndex index = LinkIndex.get(server);
        Optional<LinkIndex.Link> found = index.of(hurt.getUUID());
        if (found.isEmpty()) {
            return;
        }
        LinkIndex.Link link = found.get();
        long now = hurt.level().getGameTime();
        if (!LinkRules.active(link.until(), now)) {
            end(server, index, link);
            return;
        }
        float share = LinkRules.share(e.getNewDamage(), SHARING.contains(hurt.getUUID()) ? 1 : 0, BossesConfig.linkDamageShare());
        if (share <= 0F) {
            return;
        }
        UUID otherId = link.other(hurt.getUUID());
        Entity other = find(server, otherId);
        if (other == null) {
            if (!link.isPlayer(otherId)) {
                index.owe(otherId, share); // not loaded: a creature takes it when it comes back; a player never does
            }
            return;
        }
        if (!(other instanceof LivingEntity partner) || !partner.isAlive() || partner.level().dimension() != hurt.level().dimension()) {
            end(server, index, link);
            return;
        }
        deal(partner, share);
    }

    /** A share, dealt once: the partner is marked while it takes it, so the hit it makes cannot come back. Never skips a player's own hit cooldown. */
    private static void deal(LivingEntity partner, float share) {
        boolean added = SHARING.add(partner.getUUID());
        try {
            if (!(partner instanceof Player)) {
                partner.invulnerableTime = 0;
            }
            partner.hurt(partner.damageSources().magic(), share);
        } finally {
            if (added) {
                SHARING.remove(partner.getUUID());
            }
        }
    }

    private static void onDeath(LivingDeathEvent e) {
        endOf(e.getEntity());
    }

    private static void onTravel(EntityTravelToDimensionEvent e) {
        endOf(e.getEntity());
    }

    private static void endOf(Entity who) {
        if (!who.level().isClientSide && who.level().getServer() != null) {
            LinkIndex index = LinkIndex.get(who.level().getServer());
            index.of(who.getUUID()).ifPresent(link -> end(who.level().getServer(), index, link));
        }
    }

    private static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide || !(e.getEntity() instanceof LivingEntity living) || e.getLevel().getServer() == null) {
            return;
        }
        MinecraftServer server = e.getLevel().getServer();
        float owed = LinkRules.settleCap(LinkIndex.get(server).settle(living.getUUID()), living.getMaxHealth());
        if (owed > 0F) {
            // not inside the join itself: the entity is not in the level yet
            server.execute(() -> {
                if (living.isAlive() && !living.isRemoved()) {
                    deal(living, owed);
                }
            });
        }
    }
}
