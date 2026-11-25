package net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/** What the brotherhood perks listen to, with or without Werewolves (the species part registers it through {@code GroupsRegistry}). */
public final class BrotherhoodRegistry {
    private static final int SWEEP = 200;
    private static boolean registered;

    private BrotherhoodRegistry() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        // only a change of the mark or of the kind can move the lead or the recorded mark: this is a hot path
        BloodlineAccess.addListener((p, change) -> {
            if (change.before().markKills() != change.after().markKills() || change.before().species() != change.after().species()) {
                onMarkChanged(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                onMarkChanged(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % SWEEP == 0) {
                sweep(e.getServer());
            }
            if (e.getServer().getTickCount() % 20 == 0) {
                BrotherhoodArms.refresh(e.getServer());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                BrotherhoodArms.remove(p);
            }
        });
    }

    public static synchronized boolean registered() {
        return registered;
    }

    /** Records the player's mark and lets the lead move. */
    public static void onMarkChanged(ServerPlayer p) {
        BrotherhoodMarks.record(p);
        BrotherhoodLead.check(p.server, p.getUUID());
    }

    /** Every online member's recorded mark is refreshed, and every brotherhood's lead is checked. */
    public static void sweep(MinecraftServer server) {
        PackIndex packs = PackIndex.get(server.overworld());
        for (UUID leader : List.copyOf(packs.leaders())) {
            if (BrotherhoodScope.isBrotherhood(server.overworld(), leader)) {
                for (UUID id : packs.pack(leader)) {
                    ServerPlayer p = SireCall.player(server, id);
                    if (p != null) {
                        BrotherhoodMarks.record(p);
                    }
                }
                BrotherhoodLead.check(server, leader);
            }
        }
    }
}