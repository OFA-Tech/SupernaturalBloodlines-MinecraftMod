package net.ofatech.supernaturalbloodlines.parts.species.pack;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackMoonRules;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift;
import net.ofatech.supernaturalbloodlines.shared.werewolves.MoonControl;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * R96, the pack gift (The Originals): while the current leader of a werewolf's pack is hybrid-like, the werewolf has moon control, as with a Moon
 * Ring. Nothing is stored for the member: {@link #has} reads the pack index each time, so leaving, a kick, a dissolved pack or a change of leader
 * ends it at once. A member who loses it mid-shift keeps the form (like a Moon Ring taken off); {@code ForcedShift} reads {@link MoonControl} on the
 * next form action. Only the in-memory last answer is kept, to say once when it changes. Links no Werewolves class.
 */
public final class PackMoonGift {
    public static final String GAINED = "supernaturalbloodlines.pack.moon_gift.gained";
    public static final String LOST = "supernaturalbloodlines.pack.moon_gift.lost";
    private static final int CHECK_TICKS = 20;
    private static final Map<UUID, Boolean> HELD = new ConcurrentHashMap<>();

    private PackMoonGift() {
    }

    public static void register() {
        ForcedShift.register();
        MoonControl.addSource(PackMoonGift::has);
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % CHECK_TICKS == 0) {
                check(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> HELD.remove(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> HELD.clear());
    }

    /** Whether the pack of {@code p} gives them moon control now. */
    public static boolean has(ServerPlayer p) {
        PackIndex index = PackIndex.get(p.serverLevel());
        Optional<UUID> leader = index.alphaOf(p.getUUID());
        return PackMoonRules.gift(SpeciesConfig.hybridPackMoonControl(), PackAccess.wolf(p), VampirismSupernatural.isWerewolf(p),
                leader.isPresent(), leader.isPresent() && index.isHybridLike(leader.get()));
    }

    /** One line when {@code p} gains or loses the gift since the last check (the key sent, else null); the first check of a session only records it. */
    @Nullable
    public static String check(ServerPlayer p) {
        boolean now = has(p);
        Boolean before = HELD.put(p.getUUID(), now);
        if (before == null || before == now) {
            return null;
        }
        String key = now ? GAINED : LOST;
        p.sendSystemMessage(Component.translatable(key));
        return key;
    }

    @VisibleForTesting
    public static void reset() {
        HELD.clear();
    }
}
