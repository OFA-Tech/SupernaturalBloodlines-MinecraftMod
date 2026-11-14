package net.ofatech.supernaturalbloodlines.mixin;

import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import net.neoforged.fml.loading.LoadingModList;
import net.ofatech.supernaturalbloodlines.core.EarlySwitches;
import net.ofatech.supernaturalbloodlines.core.Part;
import net.ofatech.supernaturalbloodlines.core.PartLoader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Applies a part's mixins only when every mod the part requires is installed and its switch is on. The switch is read from
 * the toml file (EarlySwitches), the same way the part loader does, since mixins are applied long before COMMON configs load.
 * A subclass names its part and may override {@link #postApply} to check that its hooks really landed.
 */
public abstract class PartMixinPlugin implements IMixinConfigPlugin {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final Part part;
    private boolean apply;

    protected PartMixinPlugin(Part part) {
        this.part = part;
    }

    @Override
    public void onLoad(String mixinPackage) {
        LoadingModList mods = LoadingModList.get();
        // the same rule the part loader uses, so a part whose required parts are off gets no mixins either
        apply = PartLoader.enabledParts(id -> mods.getModFileById(id) != null, EarlySwitches.enabled()::contains).contains(part);
        log.info("{} mixins {} (part loader: {})", part.configKey(), apply ? "applied" : "NOT applied",
                PartLoader.status(part, id -> mods.getModFileById(id) != null, EarlySwitches.enabled()::contains));
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

    /** {@code require = 0} skips a renamed target method silently, so a hook is proven by finding the handler's call in the method. */
    protected static boolean calls(ClassNode target, String method, String handlerPart) {
        return target.methods.stream().filter(m -> m.name.equals(method))
                .anyMatch(m -> StreamSupport.stream(m.instructions.spliterator(), false)
                        .anyMatch(i -> i instanceof MethodInsnNode call && call.name.contains(handlerPart)));
    }
}
