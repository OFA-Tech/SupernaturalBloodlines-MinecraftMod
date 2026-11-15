package net.ofatech.supernaturalbloodlines.parts.hud.bite;

import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HumanLikeBlood;
import net.ofatech.supernaturalbloodlines.shared.supernatural.TurningFeed;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.CommonHooks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
    public static final int HYBRID_BITE_BLOOD = 2;
    public static final float TURNING_BITE_DAMAGE = 1f;
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "turning_bite"));

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

    /** Private: it links Vampirism, so only {@link #actionFor}, which checks that Vampirism is installed, may call it. */
    private static BiteContext context(Player p) {
        ResourceLocation faction = VampirismBite.factionId(p);
        boolean werewolf = VampirismBite.WEREWOLF.equals(faction);
        boolean wolfForm = werewolf && ModList.get().isLoaded("werewolves") && WerewolfBite.wolfForm(p);
        boolean vampire = VampirismBite.VAMPIRE.equals(faction);
        return new BiteContext(werewolf, wolfForm, vampire, HybridFeed.isHybrid(p), BloodlineAccess.get(p).inTurning(), p instanceof ServerPlayer sp ? TurningFeed.canFeed(sp) : BloodlineAccess.get(p).inTurning() && !werewolf);
    }

    /** What a press would do now. NONE without Vampirism: every row needs it. */
    public static BiteAction actionFor(Player p) {
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
        // ends whatever feeding this press started, even if the form (and so actionFor) has changed since: the feeding is Vampirism's own and must always end
        if (usable()) {
            VampirismBite.end(p);
        }
    }

    private static Result doPress(ServerPlayer p, int targetId) {
        if (p.isSpectator() || !p.isAlive() || p.isSleeping()) {
            return new Result(BiteAction.NONE, KEY + "cannot_bite");
        }
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
        // Vampirism's own biteEntity silently ignores a target beyond the player's interaction range, which would look like a bite that worked
        double reach = HudConfig.biteReach();
        if (action == BiteAction.VAMPIRISM_DRINK) {
            reach = Math.min(reach, p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE));
        }
        if (p.distanceTo(target) > reach) {
            return new Result(action, KEY + "out_of_reach");
        }
        if (!p.hasLineOfSight(target)) {
            return new Result(action, KEY + "no_line_of_sight");
        }
        String refusal = switch (action) {
            case WEREWOLF_BITE -> ModList.get().isLoaded("werewolves") ? WerewolfBite.bite(p, target) : KEY + "none";
            case VAMPIRISM_DRINK -> VampirismBite.start(p, target);
            case HYBRID_FEED -> hybridFeed(p, target);
            case TURNING_BITE -> turningBite(p, target);
            default -> KEY + "none";
        };
        return new Result(action, refusal);
    }

    /** A human in the turning bites a human-like creature: it bleeds 1 (of a bite damage source, so the player is the attacker), and the turning is fed. */
    private static String turningBite(ServerPlayer p, LivingEntity target) {
        if (!HumanLikeBlood.test(target)) {
            return KEY + "not_human";
        }
        if (!CommonHooks.onPlayerAttackTarget(p, target)) {
            return KEY + "cannot_bite_target";
        }
        if (!target.hurt(new DamageSource(p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_TYPE), p), TURNING_BITE_DAMAGE)) {
            return KEY + "cannot_bite_target";
        }
        p.level().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8f, 0.6f);
        TurningFeed.feed(p);
        return null;
    }

    /**
     * Our own feeding for a hybrid type in human form: the victim bleeds 2 and loses {@link #HYBRID_BITE_BLOOD} of its Vampirism blood (a drained
     * victim gives nothing, so a hybrid cannot farm one creature), so Venom (R42) poisons a vampire by itself, then +5 thirst.
     */
    private static String hybridFeed(ServerPlayer p, LivingEntity target) {
        if (BloodlineAccess.get(p).thirst() >= HybridFeed.MAX_THIRST) {
            return KEY + "thirst_full";
        }
        if (VampirismBite.blood(target) <= 0) {
            return KEY + "no_blood";
        }
        // claim and PvP mods answer to the attack event; a victim they protect is not bitten
        if (!CommonHooks.onPlayerAttackTarget(p, target) || !target.hurt(p.damageSources().playerAttack(p), HYBRID_BITE_DAMAGE)) {
            return KEY + "cannot_bite_target";
        }
        VampirismBite.drain(target, HYBRID_BITE_BLOOD);
        HybridFeed.feed(p);
        return null;
    }
}
