package net.ofatech.supernaturalbloodlines.parts.species;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.ReentrancyGuard;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;

/** Does what an {@link Outcome} asks of the player: the faction changes, the death of a failed transition, the refusal message. */
public final class OutcomeExecutor {
    public static final ResourceKey<DamageType> FAILED_TRANSITION = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "failed_transition"));

    private OutcomeExecutor() {
    }

    public static void run(ServerPlayer p, Outcome o) {
        ReentrancyGuard.run(p.getUUID(), "species", () -> {
            if (o.refused()) {
                p.sendSystemMessage(Component.translatable(o.refusalKey()));
                return;
            }
            // leave, then join, in that order (the rite factory's own runner); the kill comes last
            FactionRiteFactory.runFactionActions(p, o);
            if (o.actions().contains(Outcome.Action.KILL_PLAYER)) {
                p.hurt(p.damageSources().source(FAILED_TRANSITION), Float.MAX_VALUE);
            }
        });
    }
}
