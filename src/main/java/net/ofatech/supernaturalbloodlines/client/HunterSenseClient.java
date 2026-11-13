package net.ofatech.supernaturalbloodlines.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.network.GlowPayload;

/**
 * The Hunter's vampire sense, drawn on this client only. Client only: nothing but the payload handler in ModNetwork
 * refers to this class.
 * <p>
 * The outline is the entity's glow flag (shared flag 6), set in this client's copy of the entity. The server never
 * sets it, so no other player sees anything. The server's own flag updates can overwrite it, so the flag is set again
 * every frame before entities are drawn (after the sky, which comes first) until the entry expires. Expiry puts the
 * flag back to what the server would say: on only for a real Glowing effect.
 */
public final class HunterSenseClient {
    // the server resends every 40 ticks; a few ticks of slack stop the outline flickering when a packet is late
    private static final int HOLD_TICKS = GlowPayload.TICKS + 5;
    private static final int GLOW_FLAG = 6;

    // entity id -> game time it expires; render thread only
    private static final Map<Integer, Long> UNTIL = new HashMap<>();
    private static ClientLevel levelOfEntries;
    private static boolean listening;

    private HunterSenseClient() {}

    public static void show(List<Integer> ids) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> {
                if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
                    apply();
                }
            });
        }
        if (levelOfEntries != level) {
            UNTIL.clear(); // another world or dimension: the ids mean other entities
            levelOfEntries = level;
        }
        long until = level.getGameTime() + HOLD_TICKS;
        ids.forEach(id -> UNTIL.put(id, until));
    }

    private static void apply() {
        ClientLevel level = Minecraft.getInstance().level;
        if (UNTIL.isEmpty()) {
            return;
        }
        if (level == null || level != levelOfEntries) {
            UNTIL.clear();
            return;
        }
        long now = level.getGameTime();
        UNTIL.entrySet().removeIf(entry -> {
            Entity e = level.getEntity(entry.getKey());
            boolean expired = now >= entry.getValue();
            if (e == null) {
                return expired;
            }
            e.setSharedFlag(GLOW_FLAG, !expired || e instanceof LivingEntity l && l.hasEffect(MobEffects.GLOWING));
            return expired;
        });
    }
}
