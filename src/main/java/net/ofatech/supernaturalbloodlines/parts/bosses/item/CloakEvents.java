package net.ofatech.supernaturalbloodlines.parts.bosses.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.core.boss.Cloak;
import net.ofatech.supernaturalbloodlines.core.boss.CloakAccess;

/** The one moment of the Rite of Cloaking nobody caused: it runs out, and the player is told once (R172). */
public final class CloakEvents {
    public static final String FADED = "supernaturalbloodlines.rite.cloak_faded";

    private CloakEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % 20 == 0) {
                expire(e.getServer().getPlayerList().getPlayers(), e.getServer().overworld().getGameTime());
            }
        });
    }

    /** Tells every player whose cloak ran out, and forgets it so they are told once. */
    public static void expire(Iterable<? extends ServerPlayer> players, long now) {
        for (ServerPlayer p : players) {
            if (Cloak.expired(CloakAccess.until(p), now)) {
                CloakAccess.set(p, Cloak.NONE);
                p.displayClientMessage(Component.translatable(FADED), true);
            }
        }
    }
}
