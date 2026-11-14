package net.ofatech.supernaturalbloodlines.mixin;

import net.neoforged.fml.loading.LoadingModList;
import net.ofatech.supernaturalbloodlines.core.Part;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Applies the supernatural mixin only when the part is on and Enchanted is installed (the mixin targets Enchanted's {@code Rite}; the
 * part itself only needs Vampirism). Every mixin is {@code require = 0}, so {@link #postApply} checks that both hooks really landed.
 */
public final class SupernaturalMixinPlugin extends PartMixinPlugin {
    private static volatile boolean riteHooked;
    private boolean enchanted;

    public SupernaturalMixinPlugin() {
        super(Part.SUPERNATURAL);
    }

    /** Both hooks of {@code RiteCompleteMixin} really carry our call: a rite that worked is signalled. */
    public static boolean riteHooked() {
        return riteHooked;
    }

    @Override
    public void onLoad(String mixinPackage) {
        super.onLoad(mixinPackage);
        enchanted = LoadingModList.get().getModFileById("enchanted") != null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return enchanted && super.shouldApplyMixin(targetClassName, mixinClassName);
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (mixinClassName.endsWith(".RiteCompleteMixin")) {
            riteHooked = calls(targetClass, "start", "riteCompleted") && calls(targetClass, "cancel", "riteCancelled");
            log.info("RiteCompleteMixin hook {}", riteHooked ? "in place" : "MISSING (an Enchanted update?): rites will not count for ladder tasks");
        }
    }
}
