package net.ofatech.supernaturalbloodlines.mixin;

import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import net.neoforged.fml.loading.LoadingModList;
import net.ofatech.supernaturalbloodlines.core.EarlySwitches;
import net.ofatech.supernaturalbloodlines.core.Part;
import net.ofatech.supernaturalbloodlines.core.PartLoader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
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

    /**
     * MixinExtras rewrites a {@code @WrapOperation} call site only after {@link #postApply}, so {@link #calls} cannot see the handler's
     * call there. The proof is the other half: the call named by the handler's own {@code @At(target = ...)} is in the target method.
     * Reading the target from the mixin class means a mutated or outdated {@code @At} fails this check too.
     */
    protected static boolean wrapTargetPresent(ClassNode target, String method, IMixinInfo info, String handlerPart) {
        for (MethodNode handler : info.getClassNode(0).methods) {
            if (!handler.name.contains(handlerPart)) {
                continue;
            }
            for (AnnotationNode found : java.util.stream.Stream.of(handler.visibleAnnotations, handler.invisibleAnnotations).filter(java.util.Objects::nonNull).flatMap(List::stream).toList()) {
                // a handler with @Local sugar is wrapped by MixinExtras: the real annotation is kept inside it
                AnnotationNode wrap = found.desc.endsWith("/SugarWrapper;") && annotationValue(found, "original") instanceof AnnotationNode inner ? inner : found;
                if (wrap.desc.endsWith("/WrapOperation;") && atTargets(wrap).anyMatch(t -> callsTarget(target, method, t))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static java.util.stream.Stream<String> atTargets(AnnotationNode wrap) {
        Object at = annotationValue(wrap, "at");
        if (!(at instanceof List<?> ats)) {
            return java.util.stream.Stream.empty();
        }
        return ats.stream().filter(AnnotationNode.class::isInstance).map(a -> annotationValue((AnnotationNode) a, "target"))
                .filter(String.class::isInstance).map(String.class::cast);
    }

    private static Object annotationValue(AnnotationNode node, String name) {
        for (int i = 0; node.values != null && i + 1 < node.values.size(); i += 2) {
            if (name.equals(node.values.get(i))) {
                return node.values.get(i + 1);
            }
        }
        return null;
    }

    /** {@code target} is "Lowner;name(desc)ret". */
    private static boolean callsTarget(ClassNode target, String method, String atTarget) {
        int semi = atTarget.indexOf(';');
        int paren = atTarget.indexOf('(');
        if (!atTarget.startsWith("L") || semi < 0 || paren < semi) {
            return false;
        }
        String owner = atTarget.substring(1, semi);
        String name = atTarget.substring(semi + 1, paren);
        String desc = atTarget.substring(paren);
        return target.methods.stream().filter(m -> m.name.equals(method))
                .anyMatch(m -> StreamSupport.stream(m.instructions.spliterator(), false)
                        .anyMatch(i -> i instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(desc)));
    }

    /** {@code require = 0} skips a renamed target method silently, so a hook is proven by finding the handler's call in the method. */
    protected static boolean calls(ClassNode target, String method, String handlerPart) {
        return target.methods.stream().filter(m -> m.name.equals(method))
                .anyMatch(m -> StreamSupport.stream(m.instructions.spliterator(), false)
                        .anyMatch(i -> i instanceof MethodInsnNode call && call.name.contains(handlerPart)));
    }
}
