package net.ofatech.supernaturalbloodlines.parts.species.line;

import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * R124: the line is warned once each time an Original becomes open to white oak (desiccated, in Vampirism's dying state, or downed).
 * Every online member hears it; a member who has not discovered the Original hears "the Original of your bloodline" and no name.
 * A root stays latched while it is open; recovering re-arms it. NPC Original bosses call {@link #opened} and {@link #closed} themselves.
 * Nothing is sent when {@code original_line_death} is off.
 */
public final class LineWarning {
    private static final int PERIOD = 20;
    private static final ResourceLocation DESICCATED = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "desiccated");
    private static final Set<UUID> OPEN = new HashSet<>();
    private static volatile Function<MinecraftServer, Collection<ServerPlayer>> players = s -> s.getPlayerList().getPlayers();

    private LineWarning() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % PERIOD == 0) {
                tick(e.getServer());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                onLogin(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> OPEN.clear());
    }

    /** Scans the online Originals: one who is open to white oak warns their line, one who has recovered re-arms it. */
    public static void tick(MinecraftServer server) {
        for (ServerPlayer p : players.apply(server)) {
            if (OriginalRules.isOriginal(BloodlineAccess.get(p).species())) {
                if (isOpen(p)) {
                    opened(server, p.getUUID());
                } else {
                    closed(p.getUUID());
                }
            }
        }
    }

    private static boolean isOpen(ServerPlayer p) {
        Holder<MobEffect> desiccated = BuiltInRegistries.MOB_EFFECT.getHolder(DESICCATED).orElse(null);
        return Downed.isDowned(p) || VampirismAPI.vampirePlayer(p).isDBNO() || (desiccated != null && p.hasEffect(desiccated));
    }

    /** The Original of this root has become open to white oak: warns every online member, once until {@link #closed}. */
    public static void opened(MinecraftServer server, UUID root) {
        if (!SpeciesConfig.originalLineDeath() || !OPEN.add(root)) {
            return;
        }
        for (LineIndex.Member m : LineIndex.get(server).membersOf(root)) {
            ServerPlayer member = m.player() ? SireCall.player(server, m.id()) : null;
            if (member != null) {
                warn(server, member, root);
            }
        }
    }

    public static void closed(UUID root) {
        OPEN.remove(root);
    }

    /** A member who logs in while their Original is open hears it once. */
    public static void onLogin(ServerPlayer p) {
        if (!SpeciesConfig.originalLineDeath()) {
            return;
        }
        LineIndex.get(p.server).rootOf(p.getUUID()).filter(root -> OPEN.contains(root) && !root.equals(p.getUUID())).ifPresent(root -> warn(p.server, p, root));
    }

    private static void warn(MinecraftServer server, ServerPlayer member, UUID root) {
        boolean named = LineRules.showsRoot(BloodlineAccess.get(member).knownLine(), root, SpeciesConfig.originalVisibilityAlways());
        member.sendSystemMessage(named
                ? Component.translatable("supernaturalbloodlines.line.warning_named", Lines.rootName(server, root))
                : Component.translatable("supernaturalbloodlines.line.warning_hidden"));
    }

    /** For tests: who counts as online (fake players are not in the player list); null resets. */
    @VisibleForTesting
    public static void overridePlayers(@Nullable Function<MinecraftServer, Collection<ServerPlayer>> lookup) {
        players = lookup != null ? lookup : s -> s.getPlayerList().getPlayers();
    }

    /** For tests, which share one process: forgets every latch. */
    @VisibleForTesting
    public static void reset() {
        OPEN.clear();
    }
}
