package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /bloodline call}: an Original or hybrid calls their sired. Online sired players get a clickable [Accept]; only
 * when they accept (within a minute) are they teleported, to wherever the caller is at that moment, whichever dimension.
 * Sired creatures that are loaded in the caller's dimension are sent walking to them. Nothing is ever force-loaded.
 */
public final class SireCall {
    public static final long COOLDOWN_TICKS = 12000;
    private static final Set<Species> MAY_CALL = EnumSet.of(Species.ORIGINAL, Species.ORIGINAL_HYBRID, Species.HYBRID, Species.TRIBRID);
    private static final String PLAYER = "minecraft:player";
    private static final String KEY = "supernaturalbloodlines.bloodline.call.";

    /** What a call came to: an error to show, or how many were called. */
    public record Result(@Nullable Component error, int players, int mobs) {
        static Result failed(String key, Object... args) {
            return new Result(Component.translatable(KEY + key, args), 0, 0);
        }
    }

    private static final CallRequests REQUESTS = new CallRequests();
    private static volatile Function<UUID, ServerPlayer> pinnedLookup;

    private SireCall() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            REQUESTS.forget(e.getEntity().getUUID());
            CallFollowers.forget(e.getEntity().getUUID());
        });
        CallFollowers.register();
    }

    public static CallRequests requests() {
        return REQUESTS;
    }

    /** For tests: finds players the player list does not have (fake players); null clears it. */
    public static void overridePlayerLookup(@Nullable Function<UUID, ServerPlayer> lookup) {
        pinnedLookup = lookup;
    }

    @Nullable
    static ServerPlayer player(MinecraftServer server, UUID id) {
        Function<UUID, ServerPlayer> pinned = pinnedLookup;
        ServerPlayer p = pinned == null ? null : pinned.apply(id);
        return p != null ? p : server.getPlayerList().getPlayer(id);
    }

    public static Result call(ServerPlayer caller) {
        if (!MAY_CALL.contains(BloodlineAccess.get(caller).species())) {
            return Result.failed("needs_species");
        }
        ServerLevel level = caller.serverLevel();
        long now = level.getGameTime();
        if (caller.hasData(SpeciesRegistries.LAST_CALL)) {
            long left = COOLDOWN_TICKS - (now - caller.getData(SpeciesRegistries.LAST_CALL));
            if (left > 0) {
                return Result.failed("cooling", (left + 19) / 20);
            }
        }
        SirelineIndex index = SirelineIndex.get(level);
        int players = 0;
        int mobs = 0;
        for (SirelineIndex.Node sired : index.childrenOf(caller.getUUID())) {
            if (PLAYER.equals(sired.entityType())) {
                ServerPlayer target = player(caller.server, sired.id());
                if (target != null && target != caller) {
                    target.sendSystemMessage(requestMessage(caller.getGameProfile().getName(), REQUESTS.open(caller.getUUID(), target.getUUID(), now)));
                    players++;
                }
            } else if (level.getEntity(sired.id()) instanceof Mob mob && mob.isAlive()) {
                CallFollowers.follow(mob, caller.getUUID(), now);
                mobs++;
            }
        }
        if (players + mobs == 0) {
            return Result.failed("nobody");
        }
        caller.setData(SpeciesRegistries.LAST_CALL, now);
        return new Result(null, players, mobs);
    }

    /** @return the caller's name when the sired answered, or an error to show */
    public static Result accept(ServerPlayer sired, String token) {
        ServerLevel at = sired.serverLevel();
        CallRequests.Request r = REQUESTS.take(token, sired.getUUID(), at.getGameTime()).orElse(null);
        if (r == null || !SirelineIndex.get(at).sireOf(sired.getUUID()).map(r.caller()::equals).orElse(false)) {
            return Result.failed("expired");
        }
        ServerPlayer caller = player(sired.server, r.caller());
        if (caller == null) {
            return Result.failed("caller_gone");
        }
        ServerLevel level = caller.serverLevel();
        Vec3 spot = spotNear(level, caller);
        sired.teleportTo(level, spot.x, spot.y, spot.z, sired.getYRot(), sired.getXRot());
        caller.sendSystemMessage(Component.translatable(KEY + "answered", sired.getDisplayName()));
        return new Result(null, 1, 0);
    }

    public static Component requestMessage(String callerName, String token) {
        Component accept = Component.translatable(KEY + "accept").withStyle(s -> s.withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bloodline accept " + token))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(KEY + "accept_hover", callerName))));
        return Component.translatable(KEY + "request", callerName).append(" ").append(accept);
    }

    /** A free spot with floor, two blocks from the caller; the caller's own spot when there is none. */
    public static Vec3 spotNear(ServerLevel level, ServerPlayer caller) {
        Vec3 base = caller.position();
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            double x = base.x + Math.cos(angle) * 2;
            double z = base.z + Math.sin(angle) * 2;
            AABB box = EntityType.PLAYER.getDimensions().makeBoundingBox(x, base.y, z);
            BlockPos below = BlockPos.containing(x, base.y - 1, z);
            if (level.noCollision(box) && !level.containsAnyLiquid(box) && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                return new Vec3(x, base.y, z);
            }
        }
        return base;
    }
}
