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

    /** The profile of {@code subject} if it is the one last received, else null. */
    public static ProfilePayload of(UUID subject) {
        ProfilePayload p = latest;
        return p != null && p.subject().equals(subject) ? p : null;
    }

    public static void clear() {
        latest = null;
    }
}

