package net.ofatech.supernaturalbloodlines.parts.species.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.core.blood.BloodKind;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSource;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.skill.ActionClock;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;
import org.jetbrains.annotations.Nullable;

/**
 * Bottle my blood (R211): a vampire of any kind but an Original or an Original hybrid fills a glass bottle with their own blood, for some of their health
 * (never all of it) and a cooldown. The bottle is stamped with their kind, so the hybrid's blood stays a secret to every client. The Rite of Original's
 * Blood stays the only way to bottle an Original, and an Original who asks gets the same line as someone with no bottle, so it never says what they are.
 */
public final class BottleBloodAction implements ExtraActions.Action {
    public static final BottleBloodAction INSTANCE = new BottleBloodAction();
    public static final String NOTHING = "supernaturalbloodlines.bottle.nothing";
    public static final String COOLDOWN = "supernaturalbloodlines.bottle.cooldown";
    static final ResourceLocation BLOOD_BOTTLE = ResourceLocation.parse("vampirism:vampire_blood_bottle");
    /** The placeholder until the art (textures/gui/hud/bottle_blood.png) lands. */
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/hud/unknown.png");

    private BottleBloodAction() {
    }

    @Override
    public ResourceLocation id() {
        return ExtraActions.BOTTLE_BLOOD;
    }

    @Override
    public Component name() {
        return Component.translatable("supernaturalbloodlines.hud.action.bottle_blood");
    }

    @Override
    public Component note() {
        return Component.translatable("supernaturalbloodlines.hud.action.bottle_blood.note");
    }

    @Override
    public ResourceLocation icon() {
        return ICON;
    }

    @Override
    public boolean available(Player p) {
        return Supernatural.isVampiric(p) && !isOriginal(p);
    }

    private static boolean isOriginal(Player p) {
        return BloodKind.of(BloodlineAccess.get(p).species()).namesSire();
    }

    @Nullable
    @Override
    public Component trigger(ServerPlayer p) {
        Item bottle = BuiltInRegistries.ITEM.get(BLOOD_BOTTLE);
        if (!Supernatural.isVampiric(p) || isOriginal(p) || bottle == Items.AIR || !hasGlassBottle(p)) {
            return Component.translatable(NOTHING);
        }
        if (!ActionClock.ready(p, id())) {
            return Component.translatable(COOLDOWN);
        }
        if (!p.isCreative()) {
            useUpGlassBottle(p);
        }
        p.setHealth(Math.max(1f, p.getHealth() - (float) SpeciesConfig.bottleBloodHealthCost()));
        ItemStack blood = new ItemStack(bottle);
        BloodStamp.write(blood, new BloodSource(p.getUUID(), p.getGameProfile().getName(), false, BloodKind.of(BloodlineAccess.get(p).species())));
        if (!p.getInventory().add(blood)) {
            p.drop(blood, false);
        }
        ActionClock.start(p, id(), cooldownTicks(p));
        return null;
    }

    private static boolean hasGlassBottle(ServerPlayer p) {
        return p.isCreative() || p.getInventory().contains(s -> s.is(Items.GLASS_BOTTLE));
    }

    private static void useUpGlassBottle(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(Items.GLASS_BOTTLE)) {
                s.shrink(1);
                return;
            }
        }
    }

    @Override
    public int cooldownTicks(ServerPlayer p) {
        return SpeciesConfig.bottleBloodCooldownSeconds() * 20;
    }

    @Override
    public int cooldownLeft(ServerPlayer p) {
        return ActionClock.left(p, id());
    }
}
