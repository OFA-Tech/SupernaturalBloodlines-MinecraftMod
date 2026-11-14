package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.species.item.SiresBloodItem;

/**
 * R101b: hitting a player or a creature with a silver blade (the {@code supernaturalbloodlines:silver_blades} item tag) while a glass
 * bottle is in the off hand takes a bottle of their blood: the bottle is spent and Sire's Blood, bound to the one hit, is handed over.
 * Only a hit that does damage counts, and only a melee one.
 */
public final class SiresBlood {
    public static final TagKey<Item> SILVER_BLADES =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "silver_blades"));

    private SiresBlood() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SiresBlood::onDamage);
    }

    private static void onDamage(LivingDamageEvent.Post e) {
        if (e.getEntity().level().isClientSide || e.getNewDamage() <= 0 || !(e.getSource().getEntity() instanceof ServerPlayer taker)
                || e.getSource().getDirectEntity() != taker) {
            return;
        }
        take(taker, e.getEntity());
    }

    /** @return whether the player took blood: a silver blade in the main hand, a glass bottle in the off hand. */
    public static boolean take(ServerPlayer taker, LivingEntity hit) {
        ItemStack bottle = taker.getItemInHand(InteractionHand.OFF_HAND);
        if (hit == taker || !taker.getMainHandItem().is(SILVER_BLADES) || !bottle.is(Items.GLASS_BOTTLE)) {
            return false;
        }
        if (!taker.getAbilities().instabuild) {
            bottle.shrink(1);
        }
        ItemStack blood = SiresBloodItem.of(hit.getUUID(), nameOf(hit));
        if (!taker.getInventory().add(blood)) {
            taker.drop(blood, false);
        }
        return true;
    }

    private static String nameOf(LivingEntity e) {
        return e instanceof Player p ? p.getGameProfile().getName() : e.getName().getString();
    }
}
