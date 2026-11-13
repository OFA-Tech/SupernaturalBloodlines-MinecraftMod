package net.ofatech.supernaturalbloodlines.parts.artifacts;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * No sun damage for the vampire a Sun Ring is bound to. Vampirism has no sun event, so two things work together:
 * <ul>
 * <li>{@code vampirism:sun_damage} is cancelled in {@code LivingIncomingDamageEvent} for the bound wearer;</li>
 * <li>Vampirism's own sun code ({@code VampirePlayer.handleSunDamage}) also kills outright after 100 ticks in the sun
 * and adds Nausea without any damage event. It reads the {@code vampirism:sunscreen} effect and caps its sun counter
 * at 50 for amplifier 0 to 3 (when the balance option {@code vpSunscreenBuff} is on, the default), which stops both. So
 * the wearer keeps a short, hidden sunscreen effect. Weakness (every 150 ticks in the sun) is not stopped.</li>
 * </ul>
 * Links no Vampirism class beyond its API: the damage type and the effect are looked up by id.
 */
public final class SunRing {
    private static final ResourceKey<DamageType> SUN_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("vampirism:sun_damage"));
    private static final ResourceLocation SUNSCREEN = ResourceLocation.parse("vampirism:sunscreen");
    private static final int SUNSCREEN_TICKS = 40;
    private static final int REFRESH_PERIOD = 20;

    private SunRing() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SunRing::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % REFRESH_PERIOD == 0) {
                refreshSunscreen(p);
            }
        });
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (e.getEntity() instanceof Player p && e.getSource().is(SUN_DAMAGE) && ArtifactItems.isBoundWorn(p, ArtifactItems.SUN_RING.get())) {
            e.setCanceled(true);
        }
    }

    /**
     * Keeps the sunscreen effect on a vampire who wears their own ring (twice as long as the refresh period). A real
     * sunscreen (a potion or lotion: visible, or a higher level) is left alone, or the hidden one would replace it.
     */
    public static void refreshSunscreen(ServerPlayer p) {
        if (VampirismAPI.factionPlayerHandler(p).getCurrentFaction() == VReference.VAMPIRE_FACTION
                && ArtifactItems.isBoundWorn(p, ArtifactItems.SUN_RING.get())) {
            BuiltInRegistries.MOB_EFFECT.getHolder(SUNSCREEN).ifPresent(effect -> {
                MobEffectInstance current = p.getEffect(effect);
                if (current == null || (!current.isVisible() && current.getAmplifier() == 0)) {
                    p.addEffect(new MobEffectInstance(effect, SUNSCREEN_TICKS, 0, true, false, false));
                }
            });
        }
    }
}
