package net.ofatech.supernaturalbloodlines.mixin;

import net.ofatech.supernaturalbloodlines.core.Part;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * The species part's mixins target Enchanted ({@code AltarBlockEntity}, {@code GoldChalkBlockEntity}, {@code Rite}) and every hook is
 * {@code require = 0}, which skips a renamed target silently. {@link #postApply} proves the rite cost hook by finding its handlers in the method
 * (13d M9) and logs a warning when they are missing, so a witch's discount that quietly stopped working is in the log.
 */
public final class SpeciesMixinPlugin extends PartMixinPlugin {
    private static volatile boolean riteCostHooked;

    public SpeciesMixinPlugin() {
        super(Part.SPECIES);
    }

    /** All four hooks of {@code RiteCostMixin} really carry our call: the witch discount and the siphoned power apply. */
    public static boolean riteCostHooked() {
        return riteCostHooked;
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (mixinClassName.endsWith(".RiteCostMixin")) {
            riteCostHooked = calls(targetClass, "serverTick", "startCost") && calls(targetClass, "serverTick", "started")
                    && calls(targetClass, "serverTick", "startDone") && calls(targetClass, "tryConsumePower", "tickCost");
            if (riteCostHooked) {
                log.info("RiteCostMixin hooks in place");
            } else {
                log.warn("RiteCostMixin hooks MISSING (an Enchanted update?): the witch rite discount and paying rites with siphoned power do not work");
            }
        }
    }
}
