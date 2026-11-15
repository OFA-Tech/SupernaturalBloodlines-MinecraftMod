package net.ofatech.supernaturalbloodlines.mixin;

import java.util.List;
import java.util.Map;
import net.neoforged.fml.loading.LoadingModList;
import net.ofatech.supernaturalbloodlines.core.Part;

/**
 * The hud part's mixins (budget 3, 1 used: the next-preset one; the race-feed one and the Controls one are reserved). Each is listed in
 * {@link #REQUIRED_MODS} by its simple class name with the mods it targets (Tensura for the next-preset one, Tensura and Vampirism for the race-feed one),
 * and is applied only when they are all installed. {@link #postApply} checks that every hook landed, since {@code require = 0} skips a renamed method silently.
 */
public final class HudMixinPlugin extends PartMixinPlugin {
    private static final Map<String, List<String>> REQUIRED_MODS = Map.of(
            "AbilityStorageNextPresetMixin", List.of("tensura"));

    public HudMixinPlugin() {
        super(Part.HUD);
    }

    @Override
    public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName,
                          org.spongepowered.asm.mixin.extensibility.IMixinInfo mixinInfo) {
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
        return REQUIRED_MODS.getOrDefault(simple, List.of()).stream().allMatch(id -> mods.getModFileById(id) != null);
    }
}
