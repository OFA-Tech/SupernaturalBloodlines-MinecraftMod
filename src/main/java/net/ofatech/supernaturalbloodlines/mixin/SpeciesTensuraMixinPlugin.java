package net.ofatech.supernaturalbloodlines.mixin;

import java.util.List;
import java.util.Set;
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
 * Applies the species_tensura mixins only when every mod the part requires is installed and its switch is on.
 * The switch is read from the toml file (EarlySwitches), the same way the part loader does, since mixins are
 * applied long before COMMON configs load.
 */
public final class SpeciesTensuraMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpeciesTensuraMixinPlugin.class);
    private static final Part PART = Part.SPECIES_TENSURA;

    private static volatile boolean raceUtilsHooked;

    private boolean apply;

    /** Both RaceUtils checks really carry our call (set when the mixin has been applied to Tensura's class). */
    public static boolean raceUtilsHooked() {
        return raceUtilsHooked;
    }

    @Override
    public void onLoad(String mixinPackage) {
        LoadingModList mods = LoadingModList.get();
        // the same rule the part loader uses, so a part whose required parts are off gets no mixins either
        apply = PartLoader.enabledParts(id -> mods.getModFileById(id) != null, EarlySwitches.enabled()::contains).contains(PART);
        LOGGER.info("{} mixins {} (part loader: {})", PART.configKey(), apply ? "applied" : "NOT applied",
                PartLoader.status(PART, id -> mods.getModFileById(id) != null, EarlySwitches.enabled()::contains));
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
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // require = 0: a renamed Tensura method skips the injection silently, so look for our call in the target methods
        if (mixinClassName.endsWith(".RaceUtilsMixin")) {
            raceUtilsHooked = calls(targetClass, "isUndead", "layerIsUndead")
                    && calls(targetClass, "isAffectedByHolyExposure", "layerIsAffectedByHoly");
            LOGGER.info("RaceUtilsMixin hooks {}", raceUtilsHooked ? "in place" : "MISSING (a Tensura update?): undead treatment is off");
        }
    }

    private static boolean calls(ClassNode target, String method, String handlerPart) {
        return target.methods.stream().filter(m -> m.name.equals(method))
                .anyMatch(m -> java.util.stream.StreamSupport.stream(m.instructions.spliterator(), false)
                        .anyMatch(i -> i instanceof MethodInsnNode call && call.name.contains(handlerPart)));
    }
}
