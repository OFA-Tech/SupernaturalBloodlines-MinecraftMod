package net.ofatech.supernaturalbloodlines.mixin.hud;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.storage.ability.AbilityPreset;
import java.util.List;
import net.ofatech.supernaturalbloodlines.parts.hud.HudConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.tensura.NextPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reserved mixin (1) of the hud part. Tensura's server answers a skill activation with {@code isAbilityInActivePreset(slot, skill, mode)} and refuses
 * a skill that is not in the active preset, so a skill in the HUD's right column (preset N+1) would never fire (shown by {@code TensuraPresetGameTests},
 * red without this). At RETURN of the four overloads, a {@code false} answer becomes {@code true} when the skill is in the next preset at that slot and mode.
 * Same hooks as Tensura Slots' {@code IsAbilityInActivePresetMixin}. Packs may keep Slots (the Holy Packs one does): then this mixin is not applied at all,
 * since Slots already answers true for the next preset ({@code HudMixinRules}). Applied only with Tensura installed and without Slots ({@code HudMixinPlugin});
 * {@code require = 0}; switch {@code next_preset_active}.
 */
@Pseudo
@Mixin(targets = "io.github.manasmods.tensura.storage.ability.AbilityStorage", remap = false)
public abstract class AbilityStorageNextPresetMixin {
    @Shadow
    private int activePreset;
    @Shadow
    private List<AbilityPreset> presets;

    @Inject(method = "isAbilityInActivePreset(Lio/github/manasmods/manascore/skill/api/ManasSkill;)Z", at = @At("RETURN"), cancellable = true, require = 0)
    private void supernaturalbloodlines$nextPassive(ManasSkill skill, CallbackInfoReturnable<Boolean> cir) {
        supernaturalbloodlines$check(skill, NextPreset.ANY, NextPreset.ANY, cir);
    }

    @Inject(method = "isAbilityInActivePreset(Lio/github/manasmods/manascore/skill/api/ManasSkill;I)Z", at = @At("RETURN"), cancellable = true, require = 0)
    private void supernaturalbloodlines$nextPassiveMode(ManasSkill skill, int mode, CallbackInfoReturnable<Boolean> cir) {
        supernaturalbloodlines$check(skill, NextPreset.ANY, mode, cir);
    }

    @Inject(method = "isAbilityInActivePreset(ILio/github/manasmods/manascore/skill/api/ManasSkill;)Z", at = @At("RETURN"), cancellable = true, require = 0)
    private void supernaturalbloodlines$nextSlot(int slot, ManasSkill skill, CallbackInfoReturnable<Boolean> cir) {
        supernaturalbloodlines$check(skill, slot, NextPreset.ANY, cir);
    }

    @Inject(method = "isAbilityInActivePreset(ILio/github/manasmods/manascore/skill/api/ManasSkill;I)Z", at = @At("RETURN"), cancellable = true, require = 0)
    private void supernaturalbloodlines$nextSlotMode(int slot, ManasSkill skill, int mode, CallbackInfoReturnable<Boolean> cir) {
        supernaturalbloodlines$check(skill, slot, mode, cir);
    }

    private void supernaturalbloodlines$check(ManasSkill skill, int slot, int mode, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && HudConfig.nextPresetActive() && NextPreset.matches(presets, activePreset, skill, slot, mode)) {
            cir.setReturnValue(true);
        }
    }
}
