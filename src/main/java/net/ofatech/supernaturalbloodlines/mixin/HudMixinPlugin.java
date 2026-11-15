package net.ofatech.supernaturalbloodlines.mixin;

import java.util.List;
import java.util.Map;
import net.neoforged.fml.loading.LoadingModList;
import net.ofatech.supernaturalbloodlines.core.Part;

/**
 * The hud part has no mixin yet (budget 0 of 3). A mixin that a later task adds is listed in {@link #REQUIRED_MODS} by its simple class name,
 * with the mods it targets (Tensura for the next-preset one, Tensura and Vampirism for the race-feed one), and is applied only when they are all installed.
 */
public final class HudMixinPlugin extends PartMixinPlugin {
    private static final Map<String, List<String>> REQUIRED_MODS = Map.of();

    public HudMixinPlugin() {
        super(Part.HUD);
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
