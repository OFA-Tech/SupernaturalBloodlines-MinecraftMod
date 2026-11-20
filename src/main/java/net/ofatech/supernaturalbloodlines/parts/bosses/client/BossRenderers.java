package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossRegistries;

/** Client only: the renderer of every boss. Added to the mod bus by the part only when the game is a client. */
public final class BossRenderers {
    private BossRenderers() {
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(BossRegistries.MIKAEL.get(), context -> new HumanoidBossRenderer<>(context, false));
        event.registerEntityRenderer(BossRegistries.KLAUS.get(), ShiftRenderer::new);
        event.registerEntityRenderer(BossRegistries.ELIJAH.get(), context -> new HumanoidBossRenderer<>(context, false));
        event.registerEntityRenderer(BossRegistries.REBEKAH.get(), context -> new HumanoidBossRenderer<>(context, true));
    }
}
