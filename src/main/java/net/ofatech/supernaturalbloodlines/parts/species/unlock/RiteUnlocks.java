package net.ofatech.supernaturalbloodlines.parts.species.unlock;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.List;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.rite.RiteAdvancements;
import net.ofatech.supernaturalbloodlines.core.rite.RiteKind;
import net.ofatech.supernaturalbloodlines.core.rite.RiteKinds;
import net.ofatech.supernaturalbloodlines.core.rite.RiteUnlockTable;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import net.ofatech.supernaturalbloodlines.shared.species.RiteLearned;

/**
 * What a player knows of the rites (plan {@code rites-unlock}). The state is two displayless advancements per gated rite, {@code known} and
 * {@code sealed} ({@link RiteAdvancements}), kept so that exactly one is done: Enchanted's book (Modopedia) reads them on the client to show the
 * rite's entry or its "???" one. Knowledge is only added by being a kind, reading lore or performing the rite, and only removed by an admin.
 */
public final class RiteUnlocks {
    private static final int POLL_TICKS = 100;

    private RiteUnlocks() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                reconcile(p);
            }
        });
        BloodlineAccess.addListener((p, change) -> reconcile(p));
        // a faction (Vampirism's) is not in our state, so it is looked at now and then
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % POLL_TICKS == 17) {
                reconcile(p);
            }
        });
        // /advancement grant or revoke (or anything else) must not leave a rite both known and sealed, or neither
        NeoForge.EVENT_BUS.addListener((AdvancementEvent.AdvancementProgressEvent e) -> {
            String rite = RiteAdvancements.riteOf(e.getAdvancement().id().toString());
            if (rite != null && e.getEntity() instanceof ServerPlayer p) {
                fix(p, rite);
            }
        });
        RiteLearned.set((p, riteId) -> {
            if (p instanceof ServerPlayer sp) {
                learn(sp, riteId.substring(riteId.indexOf(':') + 1), true);
            }
        });
    }

    public static Set<RiteKind> kindsOf(ServerPlayer p) {
        var faction = VampirismAPI.factionPlayerHandler(p).getCurrentFaction();
        boolean wolfLineage = BloodlineAccess.get(p).wolf().lineage() != WolfState.Lineage.NONE;
        return RiteKinds.of(BloodlineAccess.get(p).species(), faction == VReference.VAMPIRE_FACTION,
                VampirismSupernatural.isWerewolf(p) || wolfLineage, faction == VReference.HUNTER_FACTION);
    }

    private static AdvancementHolder holder(ServerPlayer p, String id) {
        return p.server.getAdvancements().get(ResourceLocation.parse(id));
    }

    /** Whether an advancement of the unlock (by id) is done for the player: what the book reads on the client. */
    public static boolean done(ServerPlayer p, String id) {
        AdvancementHolder h = holder(p, id);
        return h != null && p.getAdvancements().getOrStartProgress(h).isDone();
    }

    private static void award(ServerPlayer p, String id) {
        AdvancementHolder h = holder(p, id);
        if (h != null && !done(p, id)) {
            p.getAdvancements().award(h, RiteAdvancements.CRITERION);
        }
    }

    private static void revoke(ServerPlayer p, String id) {
        AdvancementHolder h = holder(p, id);
        if (h != null && done(p, id)) {
            p.getAdvancements().revoke(h, RiteAdvancements.CRITERION);
        }
    }

    /** Whether the player knows the rite (an open rite always); a rite that has no advancement in the loaded datapacks counts as known, so nothing is sealed by a missing file. */
    public static boolean isKnown(ServerPlayer p, String rite) {
        if (!RiteUnlockTable.isGated(rite)) {
            return true;
        }
        return holder(p, RiteAdvancements.known(rite)) == null || done(p, RiteAdvancements.known(rite));
    }

    /** The invariant for one rite: known and sealed are never both done, and never both undone. */
    static void fix(ServerPlayer p, String rite) {
        if (!RiteUnlockTable.isGated(rite)) {
            return;
        }
        if (done(p, RiteAdvancements.known(rite))) {
            revoke(p, RiteAdvancements.sealed(rite));
        } else {
            award(p, RiteAdvancements.sealed(rite));
        }
    }

    /** Teaches the rites of the player's kinds and puts the sealed side right for every gated rite. Cheap: it runs at login, on a change and every five seconds. */
    public static void reconcile(ServerPlayer p) {
        Set<String> byKind = RiteUnlockTable.knownByKinds(kindsOf(p));
        for (String rite : RiteUnlockTable.GATED.keySet()) {
            if (byKind.contains(rite)) {
                award(p, RiteAdvancements.known(rite));
            }
            fix(p, rite);
        }
    }

    /** Teaches one rite. Returns whether it was new. An open rite or an unknown one teaches nothing. */
    public static boolean learn(ServerPlayer p, String rite, boolean say) {
        if (!RiteUnlockTable.isGated(rite) || done(p, RiteAdvancements.known(rite))) {
            return false;
        }
        award(p, RiteAdvancements.known(rite));
        fix(p, rite);
        if (say) {
            p.displayClientMessage(Component.translatable("supernaturalbloodlines.rites.learned", Component.translatable("rite.supernaturalbloodlines." + rite)), true);
        }
        return true;
    }

    /** What a lore source teaches (a grimoire read, a page): returns how many rites were new. */
    public static int learnLore(ServerPlayer p, String lore) {
        int fresh = 0;
        for (String rite : RiteUnlockTable.taughtBy(lore)) {
            if (learn(p, rite, true)) {
                fresh++;
            }
        }
        return fresh;
    }

    /** Takes a rite away (an admin); the rites of the player's kinds come back at the next check, so this is for lore. Returns whether it was known. */
    public static boolean forget(ServerPlayer p, String rite) {
        if (!RiteUnlockTable.isGated(rite) || !done(p, RiteAdvancements.known(rite))) {
            return false;
        }
        revoke(p, RiteAdvancements.known(rite));
        fix(p, rite);
        return true;
    }

    /** The gated rites the player knows, in table order. */
    public static List<String> known(ServerPlayer p) {
        return RiteUnlockTable.GATED.keySet().stream().filter(r -> done(p, RiteAdvancements.known(r))).toList();
    }
}
