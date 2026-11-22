package net.ofatech.supernaturalbloodlines.mixin;

import net.neoforged.fml.loading.LoadingModList;
import net.ofatech.supernaturalbloodlines.core.Part;
import net.ofatech.supernaturalbloodlines.core.hud.HudMixinRules;

/**
 * The hud part's mixins (budget 3, 1 used: the next-preset one; the race-feed one and the Controls one are reserved). Each is listed in
 * {@link HudMixinRules#REQUIRED_MODS} by its simple class name with the mods it targets (Tensura for the next-preset one, Tensura and Vampirism for the race-feed one),
 * and is applied only when they are all installed (and, for the next-preset one, Tensura Slots is not: see {@link HudMixinRules#SUPERSEDED_BY}). {@link #postApply} checks that every hook landed, since {@code require = 0} skips a renamed method silently.
 */
public final class HudMixinPlugin extends PartMixinPlugin {
    public HudMixinPlugin() {
        super(Part.HUD);
    }

    @Override
    public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName,
                          org.spongepowered.asm.mixin.extensibility.IMixinInfo mixinInfo) {
        if (mixinClassName.endsWith(".VampireRaceFeedMixin")) {
            boolean hooked = targetClass.methods.stream().anyMatch(m -> m.name.equals("onActivateAbility")
                    && java.util.stream.StreamSupport.stream(m.instructions.spliterator(), false)
                    .anyMatch(i -> i instanceof org.objectweb.asm.tree.MethodInsnNode call && call.name.contains("supernaturalbloodlines$needsFood")));
            if (hooked) {
                log.info("VampireRaceFeedMixin: the needsFood hook in VampireRace.onActivateAbility is in place");
            } else {
                log.warn("VampireRaceFeedMixin: the needsFood call was not found in VampireRace.onActivateAbility (a Tensura update?): a thirsty healthy vampire gets no race feed");
            }
        }
        if (mixinClassName.endsWith(".AbilityStorageNextPresetMixin")) {
            long hooks = targetClass.methods.stream().filter(m -> m.name.equals("isAbilityInActivePreset")
                    && java.util.stream.StreamSupport.stream(m.instructions.spliterator(), false)
                    .anyMatch(i -> i instanceof org.objectweb.asm.tree.MethodInsnNode call && call.name.contains("supernaturalbloodlines$next"))).count();
            if (hooks == 4) {
                log.info("AbilityStorageNextPresetMixin: the four isAbilityInActivePreset hooks are in place");
            } else {
                log.warn("AbilityStorageNextPresetMixin: only {} of the 4 isAbilityInActivePreset hooks landed (a Tensura update?): skills in the HUD's right column may not fire", hooks);
            }
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!super.shouldApplyMixin(targetClassName, mixinClassName)) {
            return false;
        }
        String simple = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        LoadingModList mods = LoadingModList.get();
        boolean applies = HudMixinRules.applies(simple, id -> mods.getModFileById(id) != null);
        if (!applies && mixinClassName.endsWith(".AbilityStorageNextPresetMixin") && mods.getModFileById("tensura") != null) {
            log.info("AbilityStorageNextPresetMixin: Tensura Slots hooks the same four isAbilityInActivePreset overloads, so ours stays out");
        }
        return applies;
    }
}
