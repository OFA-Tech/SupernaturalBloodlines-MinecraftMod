package net.ofatech.supernaturalbloodlines.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.network.GlowPayload;

/**
 * The Hunter's vampire sense, drawn on this client only. Client only: nothing but the payload handler in ModNetwork
 * refers to this class.
 * <p>
 * The outline is the entity's glow flag (shared flag 6), set in this client's copy of the entity. The server never
 * sets it, so no other player sees anything. The server's own flag updates can overwrite it, so the flag is set again
 * every frame before entities are drawn (after the sky, which comes first) until the entry expires. What the flag was
 * before, and what it goes back to, is {@link GlowHold}'s job.
 */
public final class HunterSenseClient {
    // the server resends every 40 ticks; a few ticks of slack stop the outline flickering when a packet is late
    private static final int HOLD_TICKS = GlowPayload.TICKS + 5;
    private static final int GLOW_FLAG = 6;

    // render thread only
    private static final GlowHold HOLD = new GlowHold();
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
            HOLD.clear(); // another world or dimension: the ids mean other entities
            levelOfEntries = level;
        }
        HOLD.hold(ids, level.getGameTime() + HOLD_TICKS);
    }

    private static void apply() {
        if (HOLD.isEmpty()) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level != levelOfEntries) {
            HOLD.clear();
            return;
        }
        long now = level.getGameTime();
        for (int id : HOLD.ids()) {
            Entity e = level.getEntity(id);
            if (e == null) {
                HOLD.missing(id, now);
            } else {
                e.setSharedFlag(GLOW_FLAG, HOLD.frame(id, e.getSharedFlag(GLOW_FLAG), now));
            }
        }
    }
}
