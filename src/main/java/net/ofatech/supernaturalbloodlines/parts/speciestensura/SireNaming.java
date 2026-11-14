package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import dev.architectury.event.EventResult;
import net.ofatech.supernaturalbloodlines.mixin.SpeciesTensuraMixinPlugin;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.tensura.config.entity.PlayerConfig;
import io.github.manasmods.tensura.menu.NamingMenu;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.Alignment;
import io.github.manasmods.tensura.storage.ep.IExistence;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
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
     * the part is off), since {@code require = 0} skips a changed method silently. The mixin plugin finds our handler's call in
     * {@code isNotNameable} when the mixin is applied; loading the class here is what applies it.
     */
    public static boolean hookApplied() {
        Class<?> packet = RequestNamingKeyPacket.class;
        return packet != null && SpeciesTensuraMixinPlugin.sireNamingHooked();
    }

    /** Called from the mixin; true when {@code namer} is the recorded sire of {@code target} and the target has no name yet. */
    public static boolean isOwnSired(LivingEntity target, Player namer) {
        if (!SpeciesTensuraConfig.sireNaming() || target == namer || target.level().isClientSide
                || TensuraStorages.getExistenceFrom(target).getName() != null) {
            return false;
        }
        // a player who is no longer part of the sireline (a cured one) has only a stale sire in the index
        if (target instanceof Player && !Supernatural.is(target)) {
            return false;
        }
        return SirelineIndex.get((ServerLevel) target.level()).sireOf(target.getUUID()).map(namer.getUUID()::equals).orElse(false);
    }

    /**
     * Tensura's naming reward evolves the race of a named MEDIUM or HIGH target (the harvest festival evolution). A sired player is
     * never evolved by their sire (R100), so the type drops to LOW: the EP and stat reward stays, the evolution and the HIGH skill
     * learn points do not. A creature keeps Tensura's reward.
     * <p>
     * Tensura rolls the namer's loss of max magicule from the type that is final after this event ({@code shouldConsumeMax}: HIGH
     * {@code endowLostChance}, MEDIUM {@code evolveLostChance}, LOW {@code subdueLostChance}, default 50, 20 and 0). Lowering the type would
     * turn that into the LOW roll, so the roll of the type the namer chose is made here and the same loss applied, as Tensura does it.
     * Tensura's own LOW roll still follows (it only matters when {@code subdueLostChance} is raised).
     */
    private static EventResult onNaming(LivingEntity target, Player namer, Changeable<Double> cost, Changeable<Double> epGain,
                                        Changeable<NamingType> type, Changeable<String> name) {
        NamingType chosen = type.get();
        if (target instanceof Player && chosen != NamingType.LOW && isOwnSired(target, namer)) {
            type.set(NamingType.LOW);
            if (!namer.hasInfiniteMaterials() && shouldConsumeMax(chosen, namer)) {
                loseMax(namer, cost.get());
            }
        }
        return EventResult.pass();
    }

    /** Tensura's {@code RequestNamingMenuPacket.shouldConsumeMax}: a percent roll against the config chance of the type. */
    private static boolean shouldConsumeMax(NamingType type, Player namer) {
        float roll = namer.getRandom().nextFloat() * 100.0f;
        PlayerConfig.Naming config = NamingMenu.CONFIG;
        return switch (type) {
            case HIGH -> roll <= config.endowLostChance;
            case MEDIUM -> roll <= config.evolveLostChance;
            default -> roll <= config.subdueLostChance;
        };
    }

    /**
     * Tensura's effect when the roll hits: the cost comes off the base max magicule; with the default alignment it is paid from the
     * current magicule first and the rest from the max aura, as the cost itself is. Values stop at the attribute's minimum.
     */
    private static void loseMax(Player namer, double cost) {
        IExistence existence = TensuraStorages.getExistenceFrom(namer);
        double fromMagicule = cost;
        double fromAura = 0;
        if (Alignment.DEFAULT.equals(existence.getAlignment())) {
            fromMagicule = Math.min(existence.getMagicule(), cost);
            fromAura = cost - fromMagicule;
        }
        reduceBase(namer, TensuraAttributes.MAX_MAGICULE, fromMagicule);
        if (fromAura > 0) {
            reduceBase(namer, TensuraAttributes.MAX_AURA, fromAura);
        }
    }

    private static void reduceBase(Player namer, Holder<Attribute> attribute, double amount) {
        AttributeInstance instance = namer.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(Math.max(((RangedAttribute) attribute.value()).getMinValue(), instance.getBaseValue() - amount));
        }
    }
}