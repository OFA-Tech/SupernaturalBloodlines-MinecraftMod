package net.ofatech.supernaturalbloodlines.mixin;

import net.ofatech.supernaturalbloodlines.core.DevShotsGate;

/**
 * Development only. Applies the dev mixin config only when the JVM has {@code -Dsupernaturalbloodlines.devshots.hidden=true}, so a normal
 * run (and a release jar, which carries the config but never the property) applies nothing. Not a part: no switch, not in the mixin budget.
 * The mixin is {@code require = 1}: with the property on, a missed hook must stop the start, never show a window.
 */
public final class DevShotsMixinPlugin extends PartMixinPlugin {
    public DevShotsMixinPlugin() {
        super(null);
    }

    @Override
    public void onLoad(String mixinPackage) {
        if (DevShotsGate.hidden()) {
            log.warn("DEVSHOTS hidden window mixin applied (development only)");
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return DevShotsGate.hidden();
    }
}
