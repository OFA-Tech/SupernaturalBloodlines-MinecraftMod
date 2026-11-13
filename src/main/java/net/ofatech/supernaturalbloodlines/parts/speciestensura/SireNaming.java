package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import dev.architectury.event.EventResult;
import java.util.Arrays;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.tensura.event.TensuraEntityEvents;
import io.github.manasmods.tensura.network.c2s.RequestNamingKeyPacket;
import io.github.manasmods.tensura.network.c2s.RequestNamingMenuPacket.NamingType;
import io.github.manasmods.tensura.storage.TensuraStorages;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;

/**
 * R101: a sire may name their own sired through Tensura's naming menu, a player too, with the playerNaming game rule off. The one
 * refusal that is lifted is that game rule ({@code mixin.speciestensura.SireNamingMixin}); every other check of Tensura stays (alive,
 * the sired's own nameable toggle, not named yet, the owner, magicule cost, health and EP limits). Tensura's rule against naming
 * one's own owner is a different pair and needs no change: once the sire is the sired's owner, the sire passes it.
 */
public final class SireNaming {
    private SireNaming() {}

    public static void register() {
        TensuraEntityEvents.NAMING_EVENT.register(SireNaming::onNaming);
    }

    /**
     * The game rule read in {@code RequestNamingKeyPacket.isNotNameable} is really wrapped: false when it was not (a Tensura update, or
     * the part is off), since {@code require = 0} skips a changed method silently. MixinExtras adds a method named after our handler
     * to the target class only when it wrapped the call; the class literal loads the class, which is what applies the mixin.
     */
    public static boolean hookApplied() {
        return Arrays.stream(RequestNamingKeyPacket.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().startsWith("wrapOperation$") && m.getName().contains("sireNamesSired"));
    }

    /** Called from the mixin; true when {@code namer} is the recorded sire of {@code target} and the target has no name yet. */
    public static boolean isOwnSired(LivingEntity target, Player namer) {
        if (!SpeciesTensuraConfig.sireNaming() || target == namer || target.level().isClientSide
                || TensuraStorages.getExistenceFrom(target).getName() != null) {
            return false;
        }
        return SirelineIndex.get((ServerLevel) target.level()).sireOf(target.getUUID()).map(namer.getUUID()::equals).orElse(false);
    }

    /**
     * Tensura's naming reward evolves the race of a named MEDIUM or HIGH target (the harvest festival evolution). A sired player is
     * never evolved by their sire (R100), so the type drops to LOW: the EP and stat reward stays, the evolution and the HIGH skill
     * learn points do not. A creature keeps Tensura's reward.
     */
    private static EventResult onNaming(LivingEntity target, Player namer, Changeable<Double> cost, Changeable<Double> epGain,
                                        Changeable<NamingType> type, Changeable<String> name) {
        if (target instanceof Player && type.get() != NamingType.LOW && isOwnSired(target, namer)) {
            type.set(NamingType.LOW);
        }
        return EventResult.pass();
    }
}
