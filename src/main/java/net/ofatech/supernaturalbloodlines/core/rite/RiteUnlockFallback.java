package net.ofatech.supernaturalbloodlines.core.rite;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;

/**
 * With the {@code species} part off nothing teaches the rites and nothing performs the species rites, so a player would see a book of "???": at login every
 * learnable rite is known instead (the rites of the other parts stay in the book as they are). Always registered; does nothing while the part is on.
 */
public final class RiteUnlockFallback {
    private RiteUnlockFallback() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && !EnabledParts.isEnabled("species")) {
                knowAll(p);
            }
        });
    }

    /** Makes every learnable rite known (and none sealed). */
    public static void knowAll(ServerPlayer p) {
        for (String rite : RiteUnlockTable.GATED.keySet()) {
            AdvancementHolder known = p.server.getAdvancements().get(ResourceLocation.parse(RiteAdvancements.known(rite)));
            AdvancementHolder sealed = p.server.getAdvancements().get(ResourceLocation.parse(RiteAdvancements.sealed(rite)));
            if (known != null) {
                p.getAdvancements().award(known, RiteAdvancements.CRITERION);
            }
            if (sealed != null) {
                p.getAdvancements().revoke(sealed, RiteAdvancements.CRITERION);
            }
        }
    }
}
