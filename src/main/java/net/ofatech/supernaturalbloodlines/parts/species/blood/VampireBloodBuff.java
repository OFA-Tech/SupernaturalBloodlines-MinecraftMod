package net.ofatech.supernaturalbloodlines.parts.species.blood;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.skill.ActionClock;

/**
 * What vampire blood in the system does (R207), for anyone the blood marks however it got there (a bottle, a bite, a forced drink). Regeneration II lasts only
 * {@link #REGEN_TICKS} after the drink and is given once per {@code vampire_blood_regen_cooldown_seconds}, so drinking more (or being fed more) does not keep it
 * going; the hunger ease lasts the whole window.
 */
public final class VampireBloodBuff {
    public static final int REGEN_TICKS = 90 * 20;
    static final float EXHAUSTION_EASED = 0.15f;
    /** The saved clock of the pulse (one per player, kept through death with the other action clocks). */
    public static final ResourceLocation REGEN_CLOCK = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "vampire_blood_regen");

    private VampireBloodBuff() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 0) {
                tick(p);
            }
        });
    }

    /** Vampire blood just went into this player ({@code TurningHandler.drankVampireBlood}): the one Regeneration pulse, if the player is due one. */
    public static void drank(ServerPlayer p) {
        if (!SpeciesConfig.vampireBloodBuffEnabled() || !ActionClock.ready(p, REGEN_CLOCK)) {
            return;
        }
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_TICKS, 1, true, false, true));
        ActionClock.startSilent(p, REGEN_CLOCK, SpeciesConfig.vampireBloodRegenCooldownSeconds() * 20);
    }

    /** One pass for one player (every 20 ticks of their own): the hunger ease, while the blood lasts. */
    public static void tick(ServerPlayer p) {
        if (!SpeciesConfig.vampireBloodBuffEnabled() || !p.isAlive() || BloodlineAccess.get(p).vampireBloodUntil() <= p.level().getGameTime()) {
            return;
        }
        FoodData food = p.getFoodData();
        food.setExhaustion(Math.max(0f, food.getExhaustionLevel() - EXHAUSTION_EASED));
    }
}
