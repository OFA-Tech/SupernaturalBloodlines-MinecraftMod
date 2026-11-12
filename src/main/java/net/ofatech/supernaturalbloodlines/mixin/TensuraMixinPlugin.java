package net.ofatech.supernaturalbloodlines.mixin;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import net.ofatech.supernaturalbloodlines.core.Config;
import net.ofatech.supernaturalbloodlines.core.Part;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Applies the Tensura mixins only when Tensura and Vampirism are installed and the vampirismTensura switch is on.
 * The switch is read from the toml file (Config.enabledSwitchesEarly), the same way the part loader does, since
 * mixins are applied long before COMMON configs load.
 */
public final class TensuraMixinPlugin implements IMixinConfigPlugin {
    private boolean apply;

    @Override
    public void onLoad(String mixinPackage) {
        LoadingModList mods = LoadingModList.get();
        apply = mods.getModFileById("tensura") != null
                && mods.getModFileById("vampirism") != null
                && Config.enabledSwitchesEarly().contains(Part.VAMPIRISM_TENSURA);
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
