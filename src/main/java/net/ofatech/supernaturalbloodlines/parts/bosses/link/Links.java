package net.ofatech.supernaturalbloodlines.parts.bosses.link;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.core.boss.LinkRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;

/** The Linking Spell at work (R177): damage to one of two linked creatures is shared with the other, once, and the link ends with a death, a dimension change or its time. */
public final class Links {
    /** How deep the damage being dealt is in shares: a share dealt to the partner is depth 1 and is never shared again. */
    private static int depth;

    private Links() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, LivingDamageEvent.Post.class, Links::onDamaged);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, LivingDeathEvent.class, Links::onDeath);
        NeoForge.EVENT_BUS.addListener(Links::onTravel);
        NeoForge.EVENT_BUS.addListener(Links::onJoin);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % 100 == 0) {
                LinkIndex.get(e.getServer()).expire(e.getServer().overworld().getGameTime());
            }
        });
    }

    /** Links two creatures for {@code seconds}. */
    public static void link(MinecraftServer server, UUID a, UUID b, int seconds) {
        LinkIndex.get(server).add(a, b, server.overworld().getGameTime() + seconds * 20L);
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
            index.remove(link);
            return;
        }
        float share = LinkRules.share(e.getNewDamage(), depth, BossesConfig.linkDamageShare());
        if (share <= 0F) {
            return;
        }
        Entity other = find(server, link.other(hurt.getUUID()));
        if (other == null) {
            index.owe(link.other(hurt.getUUID()), share); // not loaded: it takes it when it comes back
            return;
        }
        if (!(other instanceof LivingEntity partner) || !partner.isAlive() || partner.level().dimension() != hurt.level().dimension()) {
            index.remove(link);
            return;
        }
        depth++;
        try {
            partner.invulnerableTime = 0;
            partner.hurt(partner.damageSources().magic(), share);
        } finally {
            depth--;
        }
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!e.getEntity().level().isClientSide && e.getEntity().level().getServer() != null) {
            LinkIndex index = LinkIndex.get(e.getEntity().level().getServer());
            index.of(e.getEntity().getUUID()).ifPresent(index::remove);
        }
    }

    private static void onTravel(EntityTravelToDimensionEvent e) {
        if (!e.getEntity().level().isClientSide && e.getEntity().level().getServer() != null) {
            LinkIndex index = LinkIndex.get(e.getEntity().level().getServer());
            index.of(e.getEntity().getUUID()).ifPresent(index::remove);
        }
    }

    private static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide || !(e.getEntity() instanceof LivingEntity living) || e.getLevel().getServer() == null) {
            return;
        }
        MinecraftServer server = e.getLevel().getServer();
        float owed = LinkIndex.get(server).settle(living.getUUID());
        if (owed > 0F) {
            // not inside the join itself: the entity is not in the level yet
            server.execute(() -> {
                if (living.isAlive() && !living.isRemoved()) {
                    depth++;
                    try {
                        living.hurt(living.damageSources().magic(), owed);
                    } finally {
                        depth--;
                    }
                }
            });
        }
    }
}
