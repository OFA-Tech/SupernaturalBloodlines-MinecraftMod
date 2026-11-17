package net.ofatech.supernaturalbloodlines.mixin.dev;

import com.mojang.blaze3d.platform.Window;
import net.ofatech.supernaturalbloodlines.core.DevShotsGate;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Development only (see docs/mixins.md, "Dev tooling"). Applied by {@code DevShotsMixinPlugin} only when
 * {@code -Dsupernaturalbloodlines.devshots.hidden=true}. Vanilla calls {@code glfwDefaultWindowHints()} and sets its hints, then hands over to
 * {@code ImmediateWindowHandler.setupMinecraftWindow}; with {@code earlyWindowControl=false} that ends in {@code GLFW.glfwCreateWindow} inside
 * NeoForge's NoVizFallback and uses the hints set at that moment, so these go in right before it.
 */
@Mixin(Window.class)
public abstract class DevShotsWindowMixin {
    @Inject(method = "<init>", require = 1, at = @At(value = "INVOKE",
            target = "Lnet/neoforged/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J"))
    private void supernaturalbloodlines$hiddenWindow(CallbackInfo ci) {
        for (int[] hint : DevShotsGate.windowHints(DevShotsGate.hidden())) {
            GLFW.glfwWindowHint(hint[0], hint[1]);
        }
    }
}
