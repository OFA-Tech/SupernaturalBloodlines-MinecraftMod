package net.ofatech.supernaturalbloodlines.parts.species.group.clan;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** What the clan perks listen to, with or without Werewolves (the species part registers it through {@code GroupsRegistry}). */
public final class ClanRegistry {
    private static boolean registered;

    private ClanRegistry() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % 200 == 0) {
                ClanDiscipline.sweep(e.getServer());
            }
            if (e.getServer().getTickCount() % 20 == 0) {
                Contests.tick(e.getServer());
                SirelineBonuses.refresh(e.getServer());
            }
            if (e.getServer().getTickCount() % 40 == 0) {
                CharterScan.scanNow(e.getServer());
            }
        });
        CharterGuard.register();
        Contests.register();
        SharedFeeding.register();
    }
}
