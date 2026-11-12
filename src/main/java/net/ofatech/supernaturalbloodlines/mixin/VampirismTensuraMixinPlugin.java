package net.ofatech.supernaturalbloodlines.mixin;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import net.ofatech.supernaturalbloodlines.core.EarlySwitches;
import net.ofatech.supernaturalbloodlines.core.Part;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Applies the vampirism_tensura mixins only when every mod the part requires is installed and its switch is on.
 * The switch is read from the toml file (EarlySwitches), the same way the part loader does, since mixins are
 * applied long before COMMON configs load.
 */
public final class VampirismTensuraMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(VampirismTensuraMixinPlugin.class);
    private static final Part PART = Part.VAMPIRISM_TENSURA;

    private boolean apply;

    @Override
    public void onLoad(String mixinPackage) {
        LoadingModList mods = LoadingModList.get();
        boolean modsPresent = PART.requiredMods().stream().allMatch(id -> mods.getModFileById(id) != null);
        boolean switchOn = EarlySwitches.enabled().contains(PART);
        apply = modsPresent && switchOn;
        LOGGER.info("{} mixins {} (required mods present: {}, part switch on: {})",
                PART.configKey(), apply ? "applied" : "NOT applied", modsPresent, switchOn);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return apply;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
