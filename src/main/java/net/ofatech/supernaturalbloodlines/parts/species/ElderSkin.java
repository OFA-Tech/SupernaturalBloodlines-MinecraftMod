package net.ofatech.supernaturalbloodlines.parts.species;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;

/**
 * Elder's Skin (S-VAM-7): Vampirism's sun damage hurts a vampire who owns it 25% less. A bound Sun Ring cancels the damage before this
 * (it listens at the default priority and cancels; a cancelled event is not seen here), so the two do not stack.
 */
public final class ElderSkin {
    private static final ResourceKey<DamageType> SUN_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("vampirism:sun_damage"));

    private ElderSkin() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ElderSkin::onIncomingDamage);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (e.getEntity() instanceof Player p && e.getSource().is(SUN_DAMAGE)) {
            e.setAmount((float) (e.getAmount() * LadderQuery.value(p, "S-VAM-7", "sun_damage_factor", 1.0)));
        }
    }
}