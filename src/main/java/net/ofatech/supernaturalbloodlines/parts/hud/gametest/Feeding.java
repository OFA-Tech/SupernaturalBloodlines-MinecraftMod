package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.server.level.ServerPlayer;

/** A FakePlayer is never ticked by the server, so a test drives Vampirism's own per-tick player update (its feeding runs in it). */
final class Feeding {
    private Feeding() {
    }

    static void tick(ServerPlayer p) {
        Object vampire = VampirismAPI.vampirePlayer(p);
        try {
            MethodHandles.publicLookup().findVirtual(vampire.getClass(), "onUpdate", MethodType.methodType(void.class)).invoke(vampire);
        } catch (Throwable t) {
            throw new IllegalStateException("cannot tick Vampirism's player update", t);
        }
    }
}
