package net.ofatech.supernaturalbloodlines.parts.species.blood;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.Supernatural;
import net.ofatech.supernaturalbloodlines.core.blood.BloodBottleRules.Effect;
import net.ofatech.supernaturalbloodlines.core.blood.BloodKind;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSource;
import net.ofatech.supernaturalbloodlines.core.blood.ForceFeedRules;
import net.ofatech.supernaturalbloodlines.core.death.DamageProbe;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.item.BloodBottles;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.parts.species.skill.ActionClock;
import net.ofatech.supernaturalbloodlines.parts.species.skill.TargetChecks;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;
import net.ofatech.supernaturalbloodlines.shared.supernatural.BiteProtection;
import org.jetbrains.annotations.Nullable;

/**
 * Force-feed my blood (R208): a vampire of any kind puts their own blood in the player they look at, as a bottle of it would (R210, {@link BloodBottles#apply}).
 * A target who sneaks accepts; an unwilling one needs PvP on and to be down or at {@code force_feed_health_limit} of their health; vervain blocks it for
 * everyone; a claim that would stop a hit (or, for a willing target, a click) stops it. Whatever the blood does (nothing at all, for a vampire or a
 * hunter), the giver is told the same thing and starts the same cooldown, so the action does not say who is human.
 */
public final class GiveBloodAction implements ExtraActions.Action {
    public static final GiveBloodAction INSTANCE = new GiveBloodAction();
    private static final String KEY = "supernaturalbloodlines.hud.action.give_blood.";
    public static final String NO_TARGET = KEY + "no_target";
    public static final String COOLDOWN = KEY + "cooldown";
    public static final String REFUSED = KEY + "refused";
    public static final String DONE = KEY + "done";
    public static final String RECEIVED = KEY + "received";
    public static final String NOT_A_PLAYER = KEY + "not_a_player";
    public static final String SELF = KEY + "self";
    public static final String VERVAIN = KEY + "vervain";
    public static final String NOT_WILLING = KEY + "not_willing";
    /** The placeholder until the art (textures/gui/hud/give_blood.png) lands. */
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
        if (target == null || target != p && !TargetChecks.inView(p, target)) {
            return Component.translatable(NO_TARGET);
        }
        if (!ActionClock.ready(p, id())) {
            return Component.translatable(COOLDOWN);
        }
        ServerPlayer receiver = target instanceof ServerPlayer sp ? sp : null;
        // a creative, spectating or just-spawned player is one refusal, whatever else is true of them
        if (target != p && TargetChecks.untouchable(target)) {
            return Component.translatable(REFUSED);
        }
        boolean downed = receiver != null && (Downed.isDowned(receiver) || TargetChecks.inVampirismDying(receiver));
        boolean willing = target.isShiftKeyDown();
        var refusal = ForceFeedRules.check(receiver != null, target == p, receiver != null && BiteProtection.victimProtected(receiver), willing,
                p.server.isPvpAllowed(), target.getHealth() / target.getMaxHealth(), downed, (float) SpeciesConfig.forceFeedHealthLimit());
        if (refusal.isPresent()) {
            return Component.translatable(switch (refusal.get()) {
                case NOT_A_PLAYER -> NOT_A_PLAYER;
                case SELF -> SELF;
                case VERVAIN -> VERVAIN;
                case NOT_WILLING -> NOT_WILLING;
            });
        }
        if (!allowedHere(p, receiver, willing, downed)) {
            return Component.translatable(REFUSED);
        }
        BloodSource source = new BloodSource(p.getUUID(), p.getGameProfile().getName(), false, BloodKind.of(BloodlineAccess.get(p).species()));
        receiver.sendSystemMessage(Component.translatable(RECEIVED, p.getDisplayName()));
        Effect effect = BloodBottles.apply(receiver, source);
        if (effect == Effect.REFUSED) {
            receiver.displayClientMessage(BloodBottles.refusal(receiver, source), true);
        }
        p.displayClientMessage(Component.translatable(DONE, receiver.getDisplayName()), true);
        ActionClock.start(p, id(), cooldownTicks(p));
        return null;
    }

    /**
     * What a claim, a team, a pack or a duel says. A willing target is asked as a click (a protected chest-and-door claim may hold a click back); an unwilling one
     * as the blow it is, with the damage probe too, except for a downed one (our own dying rules cancel every blow, so the probe would only ever say no).
     */
    private static boolean allowedHere(ServerPlayer giver, ServerPlayer receiver, boolean willing, boolean downed) {
        if (willing) {
            return !DamageProbe.run(() -> NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(giver, InteractionHand.MAIN_HAND, receiver)).isCanceled());
        }
        if (!TargetChecks.mayAttack(giver, receiver)) {
            return false;
        }
        return downed || TargetChecks.damageWouldLand(receiver, giver.damageSources().playerAttack(giver), TargetChecks.PROBE);
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
