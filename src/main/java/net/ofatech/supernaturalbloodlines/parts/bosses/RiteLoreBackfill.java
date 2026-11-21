package net.ofatech.supernaturalbloodlines.parts.bosses;

import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.rite.RiteUnlockTable;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.species.unlock.RiteUnlocks;

/**
 * A world that already paid a boss's reward before the rites became learnable (plan {@code rites-unlock}) loses nothing: the first time a player logs
 * in, they learn the lore of every boss that has been rewarded in this world. That is permanent and world-wide, not tied to who was rewarded: it covers
 * everyone who joins later too, so a boss rewarded once teaches its lore to every new player on their first login (accepted). Once per player; what is
 * forgotten later stays forgotten.
 */
public final class RiteLoreBackfill {
    private static final String FLAG = "supernaturalbloodlines_rites_lore_backfilled";

    /** The lore each boss's reward carries: Kol his grimoire, Finn Esther's Grimoire and his notes, Elijah his letter, the Beast the Valet's Notes. */
    static final Map<BossId, List<String>> LORE = Map.of(
            BossId.KOL, List.of(RiteUnlockTable.KOL),
            BossId.FINN, List.of(RiteUnlockTable.ESTHER, RiteUnlockTable.FINN),
            BossId.ELIJAH, List.of(RiteUnlockTable.ELIJAH),
            BossId.BEAST_OF_GEVAUDAN, List.of(RiteUnlockTable.BEAST));

    private RiteLoreBackfill() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                backfill(p);
            }
        });
    }

    /** Teaches the lore of the rewarded bosses, once per player. Returns how many rites were new. */
    public static int backfill(ServerPlayer p) {
        if (!EnabledParts.isEnabled("species") || RiteUnlocks.flags(p).getBoolean(FLAG)) {
            return 0;
        }
        RiteUnlocks.flags(p).putBoolean(FLAG, true);
        BossIndex index = BossIndex.get(p.server);
        java.util.List<String> rites = new java.util.ArrayList<>();
        LORE.forEach((boss, lore) -> {
            if (index.of(boss).rewarded) {
                lore.forEach(l -> RiteUnlockTable.taughtBy(l).stream().filter(r -> !rites.contains(r)).forEach(rites::add));
            }
        });
        return RiteUnlocks.learnAll(p, rites).size();
    }
}
