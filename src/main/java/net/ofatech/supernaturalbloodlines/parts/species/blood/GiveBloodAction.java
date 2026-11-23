package net.ofatech.supernaturalbloodlines.parts.species.blood;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.CommonHooks;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Effect;
import net.ofatech.supernaturalbloodlines.core.blood.BloodKind;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSource;
import net.ofatech.supernaturalbloodlines.core.blood.ForceFeedRules;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodBottles;
import net.ofatech.supernaturalbloodlines.parts.species.line.Lines;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.parts.species.skill.ActionClock;
import net.ofatech.supernaturalbloodlines.parts.species.skill.TargetChecks;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;
import net.ofatech.supernaturalbloodlines.shared.supernatural.BiteProtection;
import org.jetbrains.annotations.Nullable;

/**
 * Force-feed my blood (R208): a vampire of any kind puts their own blood in the player they look at, as a bottle of it would (R210, {@link BloodBottles#apply}).
 * A target who sneaks accepts; an unwilling one needs PvP on and to be down or at {@code force_feed_health_limit} of their health; vervain blocks it for
 * everyone (asked after consent); a claim that would stop a hit stops it. A refusal that is not about the giver or the target being out of reach costs the
 * cooldown too. Whatever the blood does (nothing at all, for a vampire or a hunter), the giver is told the same thing and starts the same cooldown, so the
 * action does not say who is human. Blood forced on someone who did not accept it never names a sire ({@link ForceFeedRules#withoutConsent}). A Ghoul has the
 * action too, like Bottle my blood.
 */
public final class GiveBloodAction implements ExtraActions.Action {
    public static final GiveBloodAction INSTANCE = new GiveBloodAction();
    private static final String KEY = "supernaturalbloodlines.hud.action.give_blood.";
    public static final String NO_TARGET = KEY + "no_target";
    public static final String COOLDOWN = KEY + "cooldown";
    public static final String REFUSED = KEY + "refused";
    public static final String DONE = KEY + "done";
    public static final String RECEIVED = KEY + "received";
    public static final String RECEIVED_WILLING = KEY + "received_willing";
    public static final String NOT_A_PLAYER = KEY + "not_a_player";
    public static final String SELF = KEY + "self";
    public static final String VERVAIN = KEY + "vervain";
    public static final String NOT_WILLING = KEY + "not_willing";
    /** The placeholder until the art (textures/gui/hud/give_blood.png, still to be drawn) lands. */
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/hud/unknown.png");

    private GiveBloodAction() {
    }

    public static void register() {
        ExtraActions.install(INSTANCE);
        VampireBloodBuff.register();
    }

    @Override
    public ResourceLocation id() {
        return ExtraActions.GIVE_BLOOD;
    }

    @Override
    public Component name() {
        return Component.translatable("supernaturalbloodlines.hud.action.give_blood");
    }

    @Override
    public Component note() {
        return Component.translatable("supernaturalbloodlines.hud.action.give_blood.note", Math.round(SpeciesConfig.forceFeedHealthLimit() * 100), SpeciesConfig.giveBloodCooldownSeconds(),
                BloodBottles.vampireBloodMinutes());
    }

    @Override
    public ResourceLocation icon() {
        return ICON;
    }

    @Override
    public boolean available(Player p) {
        return Supernatural.isVampiric(p);
    }

    @Nullable
    @Override
    public Component trigger(ServerPlayer p) {
        return trigger(p, null);
    }

    @Nullable
    @Override
    public Component trigger(ServerPlayer p, @Nullable LivingEntity target) {
        if (!available(p)) {
            return Component.translatable(REFUSED);
        }
        if (target == null) {
            return Component.translatable(NO_TARGET);
        }
        if (target == p) {
            return Component.translatable(SELF);
        }
        if (!TargetChecks.inView(p, target)) {
            return Component.translatable(NO_TARGET);
        }
        if (!ActionClock.ready(p, id())) {
            return Component.translatable(COOLDOWN);
        }
        // from here a target is found: every refusal costs the same cooldown, so asking again and again is no way to find out who is protected by what
        ServerPlayer receiver = target instanceof ServerPlayer sp ? sp : null;
        // a creative, spectating or just-spawned player is one refusal, whatever else is true of them
        if (TargetChecks.untouchable(target)) {
            return refuse(p, REFUSED);
        }
        boolean downed = receiver != null && (Downed.isDowned(receiver) || TargetChecks.inVampirismDying(receiver));
        boolean willing = target.isShiftKeyDown();
        var refusal = ForceFeedRules.check(receiver != null, false, receiver != null && BiteProtection.victimProtected(receiver), willing,
                p.server.isPvpAllowed(), target.getHealth() / target.getMaxHealth(), downed, (float) SpeciesConfig.forceFeedHealthLimit());
        if (refusal.isPresent()) {
            if (refusal.get() == ForceFeedRules.Refusal.NOT_A_PLAYER) {
                return Component.translatable(NOT_A_PLAYER); // a misclick on a creature: nothing to hide, nothing to charge
            }
            return refuse(p, switch (refusal.get()) {
                case NOT_A_PLAYER -> NOT_A_PLAYER;
                case SELF -> SELF;
                case VERVAIN -> VERVAIN;
                case NOT_WILLING -> NOT_WILLING;
            });
        }
        if (!allowedHere(p, receiver, willing, downed)) {
            return refuse(p, REFUSED);
        }
        BloodSource source = new BloodSource(p.getUUID(), p.getGameProfile().getName(), false, BloodKind.of(BloodlineAccess.get(p).species()));
        // only an accepted drink of an Original's blood names a sire, so only then is the name the line's secret (the drink's own message); a forced one is a plain vampire's
        Component who = willing && source.kind().namesSire() ? Lines.shownName(p.server, receiver, p.getUUID()) : p.getDisplayName();
        receiver.sendSystemMessage(Component.translatable(willing ? RECEIVED_WILLING : RECEIVED, who));
        Effect effect = willing ? BloodBottles.apply(receiver, source) : BloodBottles.applyWithoutConsent(receiver, source);
        if (effect == Effect.REFUSED) {
            receiver.displayClientMessage(BloodBottles.refusal(receiver, source), true);
        }
        p.displayClientMessage(Component.translatable(DONE, receiver.getDisplayName()), true);
        ActionClock.start(p, id(), cooldownTicks(p));
        return null;
    }

    private Component refuse(ServerPlayer p, String key) {
        ActionClock.start(p, id(), cooldownTicks(p));
        return Component.translatable(key);
    }

    /**
     * What a claim, a team, a pack or a duel says, asked as the blow it would be (never as a click: our own click listeners and other mods' would act on it).
     * An unwilling target is asked all of it (PvP, friendly fire, a pack-mate, the attack event, then the damage probe). A target who sneaks accepts, so PvP, a
     * team and a pack do not count, but a claim or a duel still can: only the attack event and the damage probe are asked. A downed one is not probed: our own
     * dying rules cancel every blow, so the probe would only ever say no.
     */
    // shortcut: onPlayerAttackTarget also calls the held item's onLeftClickEntity (as BiteService does), so a held item that reacts to a left click acts on a feed; fine while nothing of ours does, revisit if one starts to
    private static boolean allowedHere(ServerPlayer giver, ServerPlayer receiver, boolean willing, boolean downed) {
        boolean claims = willing ? CommonHooks.onPlayerAttackTarget(giver, receiver) : TargetChecks.mayAttack(giver, receiver);
        return claims && (downed || TargetChecks.damageWouldLand(receiver, giver.damageSources().playerAttack(giver), TargetChecks.PROBE));
    }

    @Override
    public int cooldownTicks(ServerPlayer p) {
        return SpeciesConfig.giveBloodCooldownSeconds() * 20;
    }

    @Override
    public int cooldownLeft(ServerPlayer p) {
        return ActionClock.left(p, id());
    }
}
