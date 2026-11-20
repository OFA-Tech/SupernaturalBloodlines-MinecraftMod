package net.ofatech.supernaturalbloodlines.core.boss;

import net.minecraft.world.entity.Entity;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;

/** The cloak of R172 on an entity: the one-line checks of the compass and of the hunters' sense read {@link #cloaked}. Server and client see the same attachment value only on the server, which is where both checks run. */
public final class CloakAccess {
    private CloakAccess() {
    }

    public static long until(Entity e) {
        return e.hasData(ModAttachments.CLOAK_UNTIL) ? e.getData(ModAttachments.CLOAK_UNTIL) : Cloak.NONE;
    }

    public static boolean cloaked(Entity e) {
        return Cloak.active(until(e), e.level().getGameTime());
    }

    public static void set(Entity e, long until) {
        e.setData(ModAttachments.CLOAK_UNTIL, until);
    }
}
