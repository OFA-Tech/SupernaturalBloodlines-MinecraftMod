package net.ofatech.supernaturalbloodlines.parts.hud.bite;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.hud.BiteAction;
import net.ofatech.supernaturalbloodlines.core.hud.BiteContext;
import net.ofatech.supernaturalbloodlines.core.hud.BiteRules;
import net.ofatech.supernaturalbloodlines.parts.hud.HudConfig;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HybridFeed;

/**
 * The server side of the Bite action. {@link BiteRules} picks what the bite does from the player's form, then this class resolves the
 * target (same level, within {@code bite_reach}, in sight, alive, not the player) and does it. Every refusal is an action bar line with a
 * {@code supernaturalbloodlines.hud.bite.*} key. One bite per {@link #COOLDOWN_TICKS}.
 */
public final class BiteService {
    public static final String KEY = "supernaturalbloodlines.hud.bite.";
    public static final int COOLDOWN_TICKS = 20;
    public static final float HYBRID_BITE_DAMAGE = 2f;

    /** @param refusal the lang key that explains why nothing happened, null when the bite happened */
    public record Result(BiteAction action, String refusal) {
        public boolean ok() {
            return refusal == null;
        }
    }

    private static final Map<UUID, Long> LAST_BITE = new ConcurrentHashMap<>();

    private BiteService() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> LAST_BITE.remove(e.getEntity().getUUID()));
    }

    public static boolean usable() {
        return ModList.get().isLoaded("vampirism");
    }

    public static BiteContext context(ServerPlayer p) {
        ResourceLocation faction = VampirismBite.factionId(p);
        boolean werewolf = VampirismBite.WEREWOLF.equals(faction);
        boolean wolfForm = werewolf && ModList.get().isLoaded("werewolves") && WerewolfBite.wolfForm(p);
        boolean vampire = VampirismBite.VAMPIRE.equals(faction);
        return new BiteContext(werewolf, wolfForm, vampire, HybridFeed.isHybrid(p), false, false);
    }

    /** What a press would do now. NONE without Vampirism: every row needs it. */
    public static BiteAction actionFor(ServerPlayer p) {
        return usable() ? BiteRules.decide(context(p)) : BiteAction.NONE;
    }

    /** Ticks until the next bite, 0 when ready. */
    public static int cooldownLeft(ServerPlayer p) {
        Long last = LAST_BITE.get(p.getUUID());
        return last == null ? 0 : (int) Math.max(0, last + COOLDOWN_TICKS - p.serverLevel().getGameTime());
    }

    public static Result press(ServerPlayer p, int targetId) {
        Result r = doPress(p, targetId);
        if (!r.ok()) {
            p.displayClientMessage(Component.translatable(r.refusal()), true);
        } else {
            LAST_BITE.put(p.getUUID(), p.serverLevel().getGameTime());
        }
        return r;
    }

    /** The key was released: Vampirism's feeding ends. */
    public static void release(ServerPlayer p) {
        if (actionFor(p) == BiteAction.VAMPIRISM_DRINK) {
            VampirismBite.end(p);
        }
    }

    private static Result doPress(ServerPlayer p, int targetId) {
        BiteAction action = actionFor(p);
        if (action == BiteAction.NONE) {
            return new Result(action, KEY + (usable() ? "none" : "needs_vampirism"));
        }
        if (cooldownLeft(p) > 0) {
            return new Result(action, KEY + "cooldown");
        }
        Entity found = targetId < 0 ? null : p.level().getEntity(targetId);
        if (!(found instanceof LivingEntity target) || !target.isAlive() || target == p) {
            return new Result(action, KEY + "no_target");
        }
        if (p.distanceTo(target) > HudConfig.biteReach()) {
            return new Result(action, KEY + "out_of_reach");
        }
        if (!p.hasLineOfSight(target)) {
            return new Result(action, KEY + "no_line_of_sight");
        }
        String refusal = switch (action) {
            case WEREWOLF_BITE -> ModList.get().isLoaded("werewolves") ? WerewolfBite.bite(p, target) : KEY + "none";
            case VAMPIRISM_DRINK -> VampirismBite.start(p, target);
            case HYBRID_FEED -> hybridFeed(p, target);
            default -> KEY + "none";
        };
        return new Result(action, refusal);
    }

    /** Our own feeding for a hybrid type in human form: the victim bleeds 2, so Venom (R42) poisons a vampire by itself, then +5 thirst. */
    private static String hybridFeed(ServerPlayer p, LivingEntity target) {
        if (BloodlineAccess.get(p).thirst() >= HybridFeed.MAX_THIRST) {
            return KEY + "thirst_full";
        }
        if (!VampirismBite.hasBlood(target)) {
            return KEY + "no_blood";
        }
        if (!target.hurt(p.damageSources().playerAttack(p), HYBRID_BITE_DAMAGE)) {
            return KEY + "cannot_bite_target";
        }
        HybridFeed.feed(p);
        return null;
    }
}
