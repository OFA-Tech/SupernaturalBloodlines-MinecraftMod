package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.hud.RateLimiter;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminAccess;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminEntries;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminRows;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminSettings;
import net.ofatech.supernaturalbloodlines.shared.admin.SettingEntry;

/**
 * The server side of the Admin page. The permission is read from the server's own op list for every payload; the allow-list, the value
 * check, the write and the save are {@link AdminSettings#apply}. A sender below level 2 gets no answer at all, only a line in the log.
 */
public final class AdminNet {
    /** Edits and page requests one player may send in a second. */
    public static final int PER_SECOND = 5;
    private static final RateLimiter LIMITER = new RateLimiter();

    private AdminNet() {}

    /** The permission level of a player as the server's op list says it (the same number {@code hasPermissions} compares). */
    public static int levelOf(ServerPlayer p) {
        return p.server.getProfilePermissions(p.getGameProfile());
    }

    public static AdminSnapshotPayload snapshot(Optional<AdminSnapshotPayload.Outcome> outcome) {
        return new AdminSnapshotPayload(AdminRows.of(AdminEntries.ALL), outcome);
    }

    /** The whole of an edit, for the real handler and for a test that gives its own level, writer and reply. */
    public static AdminSettings.Result handleSet(ServerPlayer p, int level, AdminSetPayload payload, AdminSettings.Writer writer, Consumer<AdminSnapshotPayload> reply) {
        AdminSettings.Result result = AdminSettings.apply(level, payload.id(), payload.value(), writer);
        switch (result.status()) {
            case NOT_OP -> SupernaturalBloodlines.LOGGER.warn("{} (permission {}) tried to change admin setting {} and was refused", p.getGameProfile().getName(), level, payload.id());
            case OK -> {
                SupernaturalBloodlines.LOGGER.info("{} set {} = {}", p.getGameProfile().getName(), payload.id(), payload.value());
                if (AdminEntries.find(AdminEntries.ALL, AdminEntries.RULES, payload.id()).map(SettingEntry::clientCopy).orElse(false)) {
                    AdminSettings.clientCopySaved(p.server);
                }
                reply.accept(snapshot(Optional.of(AdminSnapshotPayload.Outcome.of(payload.id(), result))));
            }
            default -> {
                SupernaturalBloodlines.LOGGER.info("{} could not set {}: {} {}", p.getGameProfile().getName(), payload.id(), result.status(), result.refusalKey());
                reply.accept(snapshot(Optional.of(AdminSnapshotPayload.Outcome.of(payload.id(), result))));
            }
        }
        return result;
    }

    static void onSet(AdminSetPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer p && allowedRate(p)) {
                handleSet(p, levelOf(p), payload, new Real(p.server), s -> PacketDistributor.sendToPlayer(p, s));
            }
        });
    }

    static void onRequest(AdminRequestPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer p && allowedRate(p) && AdminAccess.allowed(levelOf(p))) {
                PacketDistributor.sendToPlayer(p, snapshot(Optional.empty()));
            }
        });
    }

    /** A player left: their place in the rate limiter goes with them. */
    public static void forget(java.util.UUID player) {
        LIMITER.forget(player);
    }

    static boolean allowedRate(ServerPlayer p) {
        return LIMITER.tryAcquire(p.getUUID(), p.serverLevel().getGameTime(), PER_SECOND);
    }

    /** The real writer: the config in memory and on disk, and a game rule on the running server. */
    public record Real(MinecraftServer server) implements AdminSettings.Writer {
        @Override
        @SuppressWarnings("unchecked")
        public void set(ModConfigSpec.ConfigValue<?> value, Object boxed) {
            ((ModConfigSpec.ConfigValue<Object>) value).set(boxed);
        }

        @Override
        public void save(ModConfigSpec spec) {
            spec.save();
        }

        @Override
        public void setRule(SettingEntry rule, Object boxed) {
            GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
                @Override
                public void visitBoolean(GameRules.Key<GameRules.BooleanValue> key, GameRules.Type<GameRules.BooleanValue> type) {
                    if (key.getId().equals(rule.dottedPath()) && boxed instanceof Boolean b) {
                        server.getGameRules().getRule(key).set(b, server);
                    }
                }

                @Override
                public void visitInteger(GameRules.Key<GameRules.IntegerValue> key, GameRules.Type<GameRules.IntegerValue> type) {
                    if (key.getId().equals(rule.dottedPath()) && boxed instanceof Integer i) {
                        server.getGameRules().getRule(key).set(i, server);
                    }
                }
            });
        }
    }
}
