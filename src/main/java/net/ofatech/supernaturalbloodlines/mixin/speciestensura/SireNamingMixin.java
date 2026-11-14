package net.ofatech.supernaturalbloodlines.mixin.speciestensura;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.manasmods.tensura.network.c2s.RequestNamingKeyPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.SireNaming;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RequestNamingKeyPacket.class)
public abstract class SireNamingMixin {
    /** The playerNaming game rule is read once, for a player target, in isNotNameable(target, namer): a sire naming their own sired passes it. */
    @WrapOperation(method = "isNotNameable", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"), require = 0)
    private static boolean supernaturalbloodlines$sireNamesSired(GameRules rules, GameRules.Key<GameRules.BooleanValue> key, Operation<Boolean> original,
                                                                  @Local(argsOnly = true) LivingEntity target, @Local(argsOnly = true) Player namer) {
        return SireNaming.isOwnSired(target, namer) || original.call(rules, key);
    }
}
