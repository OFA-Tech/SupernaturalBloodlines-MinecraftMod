package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService;

/**
 * The server side of the Profile page. The player-facing request is answered for anyone, with what their relation to the subject allows;
 * the OP view has its own payload and is answered only when {@link ProfileService#ofForOp} sees permission level 2, read from the op list.
 */
public final class ProfileNet {
    private ProfileNet() {}

    public static void handleRequest(ServerPlayer p, UUID subject, Consumer<ProfilePayload> reply) {
        reply.accept(ProfilePayload.of(ProfileService.of(p, subject)));
    }

    /** @return whether an OP view was sent: false, and silence, for a sender below level 2 */
    public static boolean handleAdminRequest(ServerPlayer p, int permissionLevel, UUID subject, Consumer<ProfilePayload> reply) {
        var profile = ProfileService.ofForOp(p, permissionLevel, subject);
        if (profile.isEmpty()) {
            SupernaturalBloodlines.LOGGER.warn("{} (permission {}) asked for the OP view of a profile and was refused", p.getGameProfile().getName(), permissionLevel);
            return false;
        }
        reply.accept(ProfilePayload.of(profile.get()));
        return true;
    }

    static void onRequest(ProfileRequestPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer p && AdminNet.allowedRate(p)) {
                handleRequest(p, payload.subject(), r -> PacketDistributor.sendToPlayer(p, r));
            }
        });
    }

    static void onAdminRequest(AdminProfileRequestPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer p && AdminNet.allowedRate(p)) {
                handleAdminRequest(p, AdminNet.levelOf(p), payload.subject(), r -> PacketDistributor.sendToPlayer(p, r));
            }
        });
    }
}

