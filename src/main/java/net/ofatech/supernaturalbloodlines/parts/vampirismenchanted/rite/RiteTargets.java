package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite;

import java.util.Comparator;
import java.util.Optional;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RiteTargets {
    private RiteTargets() {
    }

    /** What the caster and the one on the circle are told for a refusal that is about that one; {@code target} is null when nobody else is told. */
    public record Told(String caster, String target) {
    }

    /**
     * A refusal that says something about the target (their gene) goes to the target only: a caster who is not the target is told the rite would change
     * nothing, so a refusal cannot be used to ask a stranger on the circle what they are. The Moon rite uses this (it cannot ask for consent: it works on
     * what is already in the target); the blood rites ask for consent first, see {@link #bloodAnswer}.
     */
    public static Told refusalAbout(boolean casterIsTarget, String key) {
        return casterIsTarget ? new Told(key, null) : new Told(FactionRiteFactory.NO_EFFECT, key);
    }

    /** What the one on the centre is told when a caster who is not them asks for their blood and they are not sneaking. */
    public static final String SNEAK_TO_GIVE_BLOOD = "supernaturalbloodlines.rite.sneak_to_give_blood";

    /**
     * The answer of a rite that takes the target's blood (Original's Blood, Hybrid's Blood), or null when it goes ahead. Both of those would otherwise
     * answer "is that player an Original / a hybrid?" for anyone standing on the circle, by a bottle or none (and the bottle names the species). So when the
     * target is not the caster they must agree, by sneaking on the centre: without it the caster gets one answer ({@code NO_EFFECT}, the same cost, every
     * ingredient back) whatever the target is, and the target is asked. With it the rite works as before, and a target of the wrong kind is told by their
     * own words ({@code targetKey}), the caster by the neutral one. A caster who is the target is told plainly ({@code selfKey}).
     *
     * @param right whether the target is of the kind the rite needs
     */
    public static Told bloodAnswer(boolean casterIsTarget, boolean sneaking, boolean right, String selfKey, String targetKey) {
        if (casterIsTarget) {
            return right ? null : new Told(selfKey, null);
        }
        if (!sneaking) {
            return new Told(FactionRiteFactory.NO_EFFECT, SNEAK_TO_GIVE_BLOOD);
        }
        return right ? null : new Told(FactionRiteFactory.NO_EFFECT, targetKey);
    }

    /** What the one on the centre is told when a caster who is not them works a rite that changes them (the Rite of Lineage: Awaken) and they are not sneaking. */
    public static final String SNEAK_TO_ACCEPT = "supernaturalbloodlines.rite.sneak_to_accept";

    /** What the one on the centre is told when a caster who is not them works the Rite of Augustine on them and they are not sneaking. */
    public static final String SNEAK_TO_BE_MADE_AUGUSTINE = "supernaturalbloodlines.rite.sneak_to_be_made_augustine";

    /**
     * The consent of a rite that changes the target (the Rite of Augustine), or null when it goes ahead: a target who is not the caster must sneak on the centre.
     * Without it the caster gets one answer ({@code NO_EFFECT}) whatever the target is, and the target is asked. After it, a refusal goes through
     * {@link #refusalAbout} (the caster hears nothing about the target).
     */
    public static Told consentAnswer(boolean casterIsTarget, boolean sneaking, String askKey) {
        return casterIsTarget || sneaking ? null : new Told(FactionRiteFactory.NO_EFFECT, askKey);
    }

    /** Applies {@link #bloodAnswer} to the two players: true when the rite goes ahead, else both have been told and the rite should be cancelled. */
    public static boolean askForBlood(ServerPlayer caster, ServerPlayer target, boolean right, String selfKey, String targetKey) {
        Told told = bloodAnswer(caster == target, target.isShiftKeyDown(), right, selfKey, targetKey);
        if (told == null) {
            return true;
        }
        caster.displayClientMessage(net.minecraft.network.chat.Component.translatable(told.caster()), true);
        if (told.target() != null) {
            target.displayClientMessage(net.minecraft.network.chat.Component.translatable(told.target()), true);
        }
        return false;
    }

    public static void refuseAbout(net.minecraft.server.level.ServerPlayer caster, net.minecraft.server.level.ServerPlayer target, String key) {
        Told told = refusalAbout(caster == target, key);
        caster.displayClientMessage(net.minecraft.network.chat.Component.translatable(told.caster()), true);
        if (told.target() != null) {
            target.displayClientMessage(net.minecraft.network.chat.Component.translatable(told.target()), true);
        }
    }

    /**
     * The performer of a rite ({@link net.ofatech.supernaturalbloodlines.core.rite.PerformerRule}): the player Enchanted found for the caster id, else the
     * target when the target IS the caster, else nobody, and the caller must {@code cancel()} (which gives the ingredients back).
     */
    public static Optional<ServerPlayer> performer(@org.jetbrains.annotations.Nullable net.minecraft.world.entity.Entity found, java.util.UUID casterId, ServerPlayer target) {
        return switch (net.ofatech.supernaturalbloodlines.core.rite.PerformerRule.pick(found instanceof ServerPlayer, target.getUUID().equals(casterId))) {
            case FOUND -> Optional.of((ServerPlayer) found);
            case TARGET -> Optional.of(target);
            case NONE -> Optional.empty();
        };
    }

    /**
     * The player standing on the circle's centre block, else the caster (if online). {@code params} is needed because
     * Enchanted keeps the caster id private to the rite and only hands it to {@code onStart}.
     */
    public static Optional<ServerPlayer> centrePlayerOrCaster(Rite rite, Rite.RiteParams params, ServerLevel level) {
        Vec3 centre = Vec3.atBottomCenterOf(rite.getPos());
        return level.getEntitiesOfClass(ServerPlayer.class, new AABB(rite.getPos()).expandTowards(0, 1, 0),
                        p -> p.isAlive() && !p.isSpectator()).stream()
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(centre)))
                .or(() -> Optional.ofNullable(level.getPlayerByUUID(params.caster)).filter(ServerPlayer.class::isInstance)
                        .map(ServerPlayer.class::cast));
    }
}
