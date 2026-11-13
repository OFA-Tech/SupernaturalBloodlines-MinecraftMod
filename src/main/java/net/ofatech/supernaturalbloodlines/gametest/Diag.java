package net.ofatech.supernaturalbloodlines.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Failure messages for GameTests that expect damage or items: what the target looked like, what happened to each hit
 * on the way (raw amount, cancelled, final amount) and which items lay around. {@link #watch} before the action.
 */
public final class Diag {
    private static final Map<LivingEntity, List<String>> LOG = new WeakHashMap<>();
    private static boolean registered;

    private Diag() {
    }

    public static synchronized void watch(LivingEntity e) {
        if (!registered) {
            registered = true;
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, (LivingIncomingDamageEvent ev) -> note(ev.getEntity(),
                    "incoming " + ev.getAmount() + " from " + ev.getSource().getMsgId() + " (invulnerableTime " + ev.getEntity().invulnerableTime + ")"));
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, (LivingIncomingDamageEvent ev) -> note(ev.getEntity(),
                    "incoming done: " + (ev.isCanceled() ? "CANCELLED" : "kept") + ", amount " + ev.getAmount()));
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, (LivingDamageEvent.Pre ev) -> note(ev.getEntity(),
                    "damage pre: " + ev.getNewDamage()));
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, (LivingDamageEvent.Post ev) -> note(ev.getEntity(),
                    "damage post: " + ev.getNewDamage()));
        }
        synchronized (LOG) {
            LOG.put(e, new ArrayList<>());
        }
    }

    private static void note(LivingEntity e, String line) {
        synchronized (LOG) {
            List<String> l = LOG.get(e);
            if (l != null) {
                l.add(line);
            }
        }
    }

    public static String describe(LivingEntity e) {
        List<String> events;
        synchronized (LOG) {
            events = LOG.getOrDefault(e, List.of());
        }
        return e.getName().getString() + "{health " + e.getHealth() + "/" + e.getMaxHealth() + ", absorption " + e.getAbsorptionAmount()
                + ", invulnerableTime " + e.invulnerableTime + ", alive " + e.isAlive() + ", removed " + e.isRemoved()
                + ", lastDamage " + e.getLastDamageSource() + ", pos " + e.blockPosition() + ", effects " + e.getActiveEffects().size()
                + ", events " + events + "}";
    }

    /** Where a position stands with the chunk loader: the cell's blocks outside the forced chunk are the usual suspect. */
    public static String chunk(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(pos);
        var chunk = level.getChunkSource().getChunkNow(cp.x, cp.z);
        return pos + " in chunk " + cp + " " + (chunk == null ? "unloaded" : chunk.getFullStatus())
                + ", entityTicking " + level.isPositionEntityTicking(pos) + ", forced " + level.getForcedChunks().contains(cp.toLong())
                + ", gameTime " + level.getGameTime();
    }

    /** Every item entity within {@code radius} of the entity's block, with position and count. */
    public static String items(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos centre, double radius) {
        List<String> out = new ArrayList<>();
        for (ItemEntity i : level.getEntitiesOfClass(ItemEntity.class, new AABB(centre).inflate(radius))) {
            out.add(i.getItem() + " @" + i.blockPosition() + (i.isRemoved() ? " removed" : ""));
        }
        return out.toString();
    }
}
