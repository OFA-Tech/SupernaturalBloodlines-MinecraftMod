package net.ofatech.supernaturalbloodlines.mixin.tensura;

import de.teamlapen.vampirism.api.entity.player.vampire.IVampirePlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.FactionRaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vampirism's own class is not on the compile classpath (api jar only), hence the string target. */
@Mixin(targets = "de.teamlapen.vampirism.entity.player.vampire.VampirePlayer")
public abstract class VampirePlayerBiteMixin {
    /** The bite starts from a network packet, with no event to cancel; BITE_TYPE.NONE is the "cannot bite" answer. */
    @Inject(method = "determineBiteType", at = @At("HEAD"), cancellable = true, require = 0)
    private void supernaturalbloodlines$bloodlessVictim(LivingEntity victim, CallbackInfoReturnable<IVampirePlayer.BITE_TYPE> cir) {
        if (victim instanceof Player p && FactionRaceRules.isBloodless(p)) {
            cir.setReturnValue(IVampirePlayer.BITE_TYPE.NONE);
        }
    }
}
