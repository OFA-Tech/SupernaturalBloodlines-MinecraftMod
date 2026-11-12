package net.ofatech.supernaturalbloodlines.parts.vampirismtensura;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.vampire.IVampire;
import io.github.manasmods.tensura.damage.TensuraDamageHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Tensura holy damage hurts a Vampirism vampire like holy water does. Vampirism's own holy water already counts
 * as Tensura holy damage (TensuraDamageHelper.isHoly matches its "holy_water" message id), so it is left alone.
 */
public final class HolyDamageBridge {
    private static final ResourceKey<DamageType> HOLY_WATER =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("vampirism:holy_water"));

    private HolyDamageBridge() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener(HolyDamageBridge::onIncomingDamage);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        float factor = factor(event.getEntity(), event.getSource());
        if (factor != 1.0F) {
            event.setAmount(event.getAmount() * factor);
        }
    }

    public static float factor(LivingEntity target, DamageSource source) {
        if (target.level().isClientSide || source.is(HOLY_WATER) || !isVampire(target) || !TensuraDamageHelper.isHoly(source)) {
            return 1.0F;
        }
        return (float) VampireSyncConfig.holyVsVampire();
    }

    private static boolean isVampire(LivingEntity entity) {
        return entity instanceof Player p
                ? VampirismAPI.factionPlayerHandler(p).isInFaction(VReference.VAMPIRE_FACTION)
                : entity instanceof IVampire;
    }
}
