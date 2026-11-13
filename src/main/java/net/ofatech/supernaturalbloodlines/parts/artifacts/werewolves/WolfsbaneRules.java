package net.ofatech.supernaturalbloodlines.parts.artifacts.werewolves;

import de.teamlapen.vampirism.api.event.ActionEvent;
import de.teamlapen.werewolves.api.entities.werewolf.WerewolfForm;
import de.teamlapen.werewolves.entities.player.werewolf.actions.WerewolfFormAction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.ofatech.supernaturalbloodlines.parts.artifacts.werewolves.WolfsbaneStrains.Coating;
import net.ofatech.supernaturalbloodlines.parts.artifacts.werewolves.WolfsbaneStrains.Strain;

/**
 * How the strains reach a werewolf type (eating, a coated weapon, a potion) and how they are cured, plus what the
 * lichen does. Links Werewolves ({@code WerewolfFormAction}): call {@link #register} only when it is loaded.
 */
public final class WolfsbaneRules {
    private WolfsbaneRules() {
    }

    public static void register(IEventBus modBus) {
        // the purple plants are no food in their own mods: make them edible so that eating is a way to take the strain
        modBus.addListener((ModifyDefaultComponentsEvent e) -> {
            for (var id : WolfsbaneStrains.COMMON_PLANTS) {
                BuiltInRegistries.ITEM.getOptional(id).filter(i -> !i.components().has(DataComponents.FOOD))
                        .ifPresent(i -> e.modify(i, b -> b.set(DataComponents.FOOD, WolfsbaneStrains.PLANT_FOOD)));
            }
        });
        NeoForge.EVENT_BUS.addListener(WolfsbaneRules::onEaten);
        NeoForge.EVENT_BUS.addListener(WolfsbaneRules::onUseAsh);
        NeoForge.EVENT_BUS.addListener(WolfsbaneRules::onAnvil);
        NeoForge.EVENT_BUS.addListener(WolfsbaneRules::onHit);
        NeoForge.EVENT_BUS.addListener(WolfsbaneRules::onHeal);
        NeoForge.EVENT_BUS.addListener(WolfsbaneRules::onForm);
        NeoForge.EVENT_BUS.addListener(WolfsbaneRules::onApplicable);
        NeoForge.EVENT_BUS.addListener((RegisterBrewingRecipesEvent e) -> {
            e.getBuilder().addMix(Potions.AWKWARD, WolfsbaneStrains.YELLOW_PLANT.get(), WolfsbaneStrains.YELLOW_POTION);
            e.getBuilder().addMix(Potions.AWKWARD, WolfsbaneStrains.NORTHERN_BLUE_PLANT.get(), WolfsbaneStrains.NORTHERN_BLUE_POTION);
            e.getBuilder().addMix(Potions.AWKWARD, WolfsbaneStrains.LICHEN.get(), WolfsbaneStrains.LICHEN_POTION);
        });
    }

    /** Eating a plant gives the strain, eating its ash cures it (only the same strain's). */
    private static void onEaten(LivingEntityUseItemEvent.Finish e) {
        if (e.getEntity().level().isClientSide) {
            return;
        }
        Strain plant = WolfsbaneStrains.strainOfPlant(e.getItem());
        if (plant != null) {
            WolfsbaneStrains.apply(e.getEntity(), plant);
        }
        Strain ash = WolfsbaneStrains.strainOfAsh(e.getItem());
        if (ash != null) {
            WolfsbaneStrains.cure(e.getEntity(), ash);
        }
    }

    /** Burning the ash on yourself: use it while sneaking, and only when you have that strain. */
    private static void onUseAsh(PlayerInteractEvent.RightClickItem e) {
        Player p = e.getEntity();
        ItemStack stack = e.getItemStack();
        Strain strain = WolfsbaneStrains.strainOfAsh(stack);
        if (strain == null || !p.isShiftKeyDown() || !p.hasEffect(strain.effect())) {
            return;
        }
        if (!p.level().isClientSide) {
            WolfsbaneStrains.cure(p, strain);
            if (!p.hasInfiniteMaterials()) {
                stack.shrink(1);
            }
        }
        e.setCancellationResult(InteractionResult.SUCCESS);
        e.setCanceled(true);
    }

    /** Anvil: a sword or an axe plus a plant is the same weapon with {@code wolfsbane_coating} (8 hits). */
    private static void onAnvil(AnvilUpdateEvent e) {
        Strain strain = WolfsbaneStrains.strainOfPlant(e.getRight());
        ItemStack weapon = e.getLeft();
        if (strain == null || !(weapon.is(ItemTags.SWORDS) || weapon.is(ItemTags.AXES))) {
            return;
        }
        ItemStack out = weapon.copy();
        out.set(WolfsbaneStrains.COATING.get(), new Coating(strain, WolfsbaneStrains.COATING_HITS));
        // setting an output skips vanilla's rename handling, so keep it: a changed name field renames, for one more level
        long cost = 1;
        String name = e.getName();
        if (name != null && !name.isBlank() && !name.equals(out.getHoverName().getString())) {
            out.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            cost++;
        }
        e.setOutput(out);
        e.setCost(cost);
        e.setMaterialCost(1);
    }

    /** A melee hit with a coated weapon on a werewolf type; only such a hit uses a charge. */
    private static void onHit(LivingDamageEvent.Post e) {
        DamageSource source = e.getSource();
        boolean melee = source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
        if (e.getNewDamage() <= 0 || !melee || !(source.getDirectEntity() instanceof LivingEntity wielder) || wielder != source.getEntity()) {
            return;
        }
        ItemStack weapon = wielder.getMainHandItem();
        Coating coating = weapon.get(WolfsbaneStrains.COATING.get());
        if (coating == null || !WolfsbaneStrains.apply(e.getEntity(), coating.strain())) {
            return;
        }
        if (coating.hits() <= 1) {
            weapon.remove(WolfsbaneStrains.COATING.get());
        } else {
            weapon.set(WolfsbaneStrains.COATING.get(), new Coating(coating.strain(), coating.hits() - 1));
        }
    }

    /**
     * Lichen: no natural regeneration. The vanilla one is {@code FoodData.tick} calling {@code heal}; the event does not
     * say who heals, so the stack is read (only for a lichen victim). Other healing (potions, food, Werewolves' own
     * regeneration) is untouched; a heal that a mod routes through {@code FoodData.tick} would be cancelled too.
     */
    private static void onHeal(LivingHealEvent e) {
        if (e.getEntity().hasEffect(WolfsbaneStrains.LICHEN_EFFECT)
                && StackWalker.getInstance().walk(s -> s.anyMatch(f -> f.getClassName().equals(FoodData.class.getName())))) {
            e.setCanceled(true);
        }
    }

    /** Lichen: no transforming, forced or at will. Going back to human form stays possible. */
    private static void onForm(ActionEvent.ActionActivatedEvent e) {
        if (e.getAction() instanceof WerewolfFormAction form && form.getForm() != WerewolfForm.HUMAN
                && e.getFactionPlayer().asEntity().hasEffect(WolfsbaneStrains.LICHEN_EFFECT)) {
            e.setCanceled(true);
        }
    }

    /** A potion or any other source goes through addEffect: the strain effects only take hold on werewolf types. */
    private static void onApplicable(MobEffectEvent.Applicable e) {
        if (e.getEffectInstance().getEffect().value() instanceof WolfsbaneStrains.WolfsbaneEffect && !WolfsbaneStrains.isWerewolfType(e.getEntity())) {
            e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
