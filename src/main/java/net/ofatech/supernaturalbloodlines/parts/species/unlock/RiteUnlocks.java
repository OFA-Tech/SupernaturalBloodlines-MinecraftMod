package net.ofatech.supernaturalbloodlines.parts.species.unlock;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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
    /** A rite learned by being a kind within this many ticks of joining is not announced (it is the login catching up, not news). */
    static final int QUIET_TICKS = 200;
    private static final String FLAG = "supernaturalbloodlines_rites_moved";

    private RiteUnlocks() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                reconcile(p, false);
                firstLogin(p);
            }
        });
        BloodlineAccess.addListener((p, change) -> reconcile(p, p.tickCount > QUIET_TICKS));
        // a faction (Vampirism's) is not in our state, so it is looked at now and then
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % POLL_TICKS == 17) {
                reconcile(p, p.tickCount > QUIET_TICKS);
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
                // said nothing: the player has just worked the rite, a line about having learned it would be noise (and a refusal test hears every line)
                learn(sp, riteId.substring(riteId.indexOf(':') + 1), false);
            }
        });
    }

    /**
     * The player's once-flags: under {@link Player#PERSISTED_NBT_TAG}, the part of the persistent data that survives death (a flag at the root is
     * dropped at the respawn, and the player would be told and taught all over again).
     */
    public static CompoundTag flags(ServerPlayer p) {
        CompoundTag data = p.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }

    /** Whether this player has been told, once, that the rites moved into the book. Sets the flag. */
    public static boolean firstTime(ServerPlayer p) {
        CompoundTag flags = flags(p);
        if (flags.getBoolean(FLAG)) {
            return false;
        }
        flags.putBoolean(FLAG, true);
        return true;
    }

    private static void firstLogin(ServerPlayer p) {
        if (firstTime(p)) {
            p.sendSystemMessage(Component.translatable("supernaturalbloodlines.rites.moved"));
        }
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

    /**
     * Teaches the rites of the player's kinds and puts the sealed side right for every gated rite. Cheap: it runs at login, on a change and every five seconds.
     *
     * @param notify say, on the action bar, when this taught something new
     * @return how many rites this taught
     */
    public static int reconcile(ServerPlayer p, boolean notify) {
        Set<String> byKind = RiteUnlockTable.knownByKinds(kindsOf(p));
        int fresh = 0;
        for (String rite : RiteUnlockTable.GATED.keySet()) {
            if (byKind.contains(rite) && !done(p, RiteAdvancements.known(rite))) {
                award(p, RiteAdvancements.known(rite));
                fresh++;
            }
            fix(p, rite);
        }
        if (notify && fresh > 0) {
            p.displayClientMessage(Component.translatable("supernaturalbloodlines.rites.new_kind"), true);
        }
        return fresh;
    }

    public static int reconcile(ServerPlayer p) {
        return reconcile(p, false);
    }

    /** Teaches one rite. Returns whether it was new. An open rite or an unknown one teaches nothing. Says nothing: {@link #learnAll} says it once. */
    public static boolean learn(ServerPlayer p, String rite, boolean say) {
        if (!RiteUnlockTable.isGated(rite) || done(p, RiteAdvancements.known(rite))) {
            return false;
        }
        award(p, RiteAdvancements.known(rite));
        fix(p, rite);
        if (say) {
            tell(p, List.of(rite));
        }
        return true;
    }

    /** One chat line naming every rite in {@code rites}. */
    static void tell(ServerPlayer p, List<String> rites) {
        List<Component> names = rites.stream().map(r -> (Component) Component.translatable("rite.supernaturalbloodlines." + r)).toList();
        Component list = Component.empty();
        for (int i = 0; i < names.size(); i++) {
            list = ((net.minecraft.network.chat.MutableComponent) list).append(i == 0 ? Component.empty() : Component.literal(", ")).append(names.get(i));
        }
        p.sendSystemMessage(Component.translatable("supernaturalbloodlines.rites.learned", list));
    }

    /** Teaches each of these rites that is new, and says so once. Returns the rites that were new. */
    public static List<String> learnAll(ServerPlayer p, List<String> rites) {
        List<String> fresh = new ArrayList<>();
        for (String rite : rites) {
            if (learn(p, rite, false)) {
                fresh.add(rite);
            }
        }
        if (!fresh.isEmpty()) {
            tell(p, fresh);
        }
        return fresh;
    }

    /** What a lore source teaches (a grimoire read, a page): returns how many rites were new, and says them in one line. */
    public static int learnLore(ServerPlayer p, String lore) {
        return learnAll(p, RiteUnlockTable.taughtBy(lore)).size();
    }

    /** Takes a rite away (an admin); the rites of the player's kind come back at the next check, so this is for lore. Returns whether it was known. */
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
