package net.ofatech.supernaturalbloodlines.parts.hud.tensura;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.ofatech.supernaturalbloodlines.core.hud.TensuraFeedRules;
import net.ofatech.supernaturalbloodlines.parts.hud.HudConfig;

/**
 * The answer {@code VampireRaceFeedMixin} gives for Tensura's {@code FoodData.needsFood()} inside the vampire race ability: the player of the call is
 * remembered for the length of the call (server thread only). Linked to Vampirism: the mixin that reaches it is applied only with Tensura and Vampirism.
 */
public final class FeedContext {
    private static final ThreadLocal<LivingEntity> CURRENT = new ThreadLocal<>();

    private FeedContext() {
    }

    public static void enter(LivingEntity entity) {
        CURRENT.set(entity);
    }

    public static void exit() {
        CURRENT.remove();
    }

    /** @param original Tensura's own answer, which is also the answer for everyone who is not a Vampirism vampire or when the switch is off */
    public static boolean needsFood(boolean original) {
        LivingEntity e = CURRENT.get();
        if (!HudConfig.tensuraFeedFillsBlood() || !(e instanceof ServerPlayer p)) {
            return original;
        }
        boolean vampire = VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == VReference.VAMPIRE_FACTION;
        return TensuraFeedRules.needsFood(original, vampire, vampire && !VampirismAPI.vampirePlayer(p).getBloodStats().needsBlood());
    }
}
