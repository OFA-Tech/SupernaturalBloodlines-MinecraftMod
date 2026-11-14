package net.ofatech.supernaturalbloodlines.mixin;

import net.ofatech.supernaturalbloodlines.core.Part;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Applies the species_tensura mixins and, since every mixin is {@code require = 0}, checks in {@link #postApply} that each hook
 * really landed in Tensura's or ManasCore's class (a renamed method would skip it silently).
 */
public final class SpeciesTensuraMixinPlugin extends PartMixinPlugin {
    private static volatile boolean raceUtilsHooked;
    private static volatile boolean apexHooked;
    private static volatile boolean sireNamingHooked;

    public SpeciesTensuraMixinPlugin() {
        super(Part.SPECIES_TENSURA);
    }

    /** Both RaceUtils checks really carry our call (set when the mixin has been applied to Tensura's class). */
    public static boolean raceUtilsHooked() {
        return raceUtilsHooked;
    }

    /** {@code ManasRaceInstance.getNextEvolutions} really carries our call: the apex race can be offered. */
    public static boolean apexHooked() {
        return apexHooked;
    }

    /** {@code RequestNamingKeyPacket.isNotNameable} really carries our wrap of the playerNaming game rule read. */
    public static boolean sireNamingHooked() {
        return sireNamingHooked;
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (mixinClassName.endsWith(".RaceUtilsMixin")) {
            raceUtilsHooked = calls(targetClass, "isUndead", "layerIsUndead")
                    && calls(targetClass, "isAffectedByHolyExposure", "layerIsAffectedByHoly");
            log.info("RaceUtilsMixin hooks {}", raceUtilsHooked ? "in place" : "MISSING (a Tensura update?): undead treatment is off");
        } else if (mixinClassName.endsWith(".RaceInstanceMixin")) {
            apexHooked = calls(targetClass, "getNextEvolutions", "offerApex");
            if (apexHooked) {
                log.info("RaceInstanceMixin hook in place");
            } else {
                log.warn("RaceInstanceMixin.getNextEvolutions hook MISSING (a ManasCore update?): no apex evolution is offered");
            }
        } else if (mixinClassName.endsWith(".SireNamingMixin")) {
            // a WrapOperation is rewritten after postApply, so the proof is the call the handler's @At names being in isNotNameable
            sireNamingHooked = wrapTargetPresent(targetClass, "isNotNameable", mixinInfo, "sireNamesSired");
            if (sireNamingHooked) {
                log.info("SireNamingMixin hook in place");
            } else {
                log.warn("SireNamingMixin hook MISSING (a Tensura update?): a sire cannot name their sired player while playerNaming is off");
            }
        }
    }
}
