package net.ofatech.supernaturalbloodlines.parts.supernatural.client;

import java.util.UUID;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.ProfilePayload;

/** Client only: the newest profile the server sent. The Profile page reads it; nothing here draws. */
public final class ProfileClient {
    private static volatile ProfilePayload latest;

    private ProfileClient() {}

    public static void receive(ProfilePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> latest = payload);
    }

    private static volatile java.util.Map<UUID, ProfilePayload> demo = java.util.Map.of();

    /** Development only (the dev screenshots): fixed profiles for made-up people, so every knowledge state can be photographed without a server. */
    public static void demo(java.util.Map<UUID, ProfilePayload> profiles) {
        demo = java.util.Map.copyOf(profiles);
    }

    /** The profile of {@code subject} if it is the one last received, else null. */
    public static ProfilePayload of(UUID subject) {
        ProfilePayload d = demo.get(subject);
        if (d != null) {
            return d;
        }
        ProfilePayload p = latest;
        return p != null && p.subject().equals(subject) ? p : null;
    }

    public static void clear() {
        latest = null;
    }
}

