package net.ofatech.supernaturalbloodlines.mixin.hud;

import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.ofatech.supernaturalbloodlines.parts.hud.tensura.FeedContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reserved mixin (2) of the hud part (13c Task 2). Tensura's vampire race ability only feeds when the player is hurt or {@code FoodData.needsFood()} is
 * true, and a Vampirism vampire's food is never short, so a healthy thirsty vampire got no feed at all (shown by {@code TensuraFeedGameTests}, red without
 * this). The one call is redirected: for a Vampirism vampire "needs food" means "blood is not full" ({@link FeedContext}); for anyone else it is
 * Tensura's own answer. The credit itself needs no mixin ({@code TensuraFeed}, on events). The entity is remembered by two injections around the method,
 * because a redirect of {@code needsFood()} only sees the {@code FoodData}. {@code @Pseudo}, string target, {@code require = 0}, switch
 * {@code tensura_feed_fills_blood}; applied only with Tensura and Vampirism installed ({@code HudMixinPlugin}).
 */
@Pseudo
@Mixin(targets = "io.github.manasmods.tensura.race.vampire.VampireRace", remap = false)
public abstract class VampireRaceFeedMixin {
    private static final String METHOD = "onActivateAbility(Lio/github/manasmods/manascore/race/api/ManasRaceInstance;Lnet/minecraft/world/entity/LivingEntity;)V";

    @Inject(method = METHOD, at = @At("HEAD"), require = 0)
    private void supernaturalbloodlines$enter(ManasRaceInstance instance, LivingEntity entity, CallbackInfo ci) {
        FeedContext.enter(entity);
    }

    @Inject(method = METHOD, at = @At("RETURN"), require = 0)
    private void supernaturalbloodlines$exit(ManasRaceInstance instance, LivingEntity entity, CallbackInfo ci) {
        FeedContext.exit();
    }

    @Redirect(method = METHOD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;needsFood()Z"), require = 0)
    private boolean supernaturalbloodlines$needsFood(FoodData food) {
        return FeedContext.needsFood(food.needsFood());
    }
}
