package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossRegistries;

/** Client only: the renderer of every boss. Added to the mod bus by the part only when the game is a client. */
public final class BossRenderers {
    private BossRenderers() {
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BossRegistries.MIKAEL.get(), context -> new HumanoidBossRenderer<>(context, false));
        event.registerEntityRenderer(BossRegistries.KLAUS.get(), context -> new ShiftRenderer<>(context, "klaus_hybrid", "klaus_hybrid_geo", 0.6F));
        event.registerEntityRenderer(BossRegistries.DEUCALION.get(), context -> new ShiftRenderer<>(context, "deucalion_demon_wolf", "deucalion_demon_wolf_geo", 0.9F));
        event.registerEntityRenderer(BossRegistries.ELIJAH.get(), context -> new HumanoidBossRenderer<>(context, false));
        event.registerEntityRenderer(BossRegistries.REBEKAH.get(), context -> new HumanoidBossRenderer<>(context, true));
        event.registerEntityRenderer(BossRegistries.KOL.get(), context -> new HumanoidBossRenderer<>(context, false));
        event.registerEntityRenderer(BossRegistries.FINN.get(), context -> new HumanoidBossRenderer<>(context, false));
    }
}
