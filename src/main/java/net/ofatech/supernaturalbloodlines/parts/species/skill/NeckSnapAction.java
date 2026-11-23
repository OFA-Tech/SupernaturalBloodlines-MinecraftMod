package net.ofatech.supernaturalbloodlines.parts.species.skill;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.hud.NeckSnapRules;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;
import net.ofatech.supernaturalbloodlines.shared.supernatural.TaskSignals;
import org.jetbrains.annotations.Nullable;

/**
 * Neck Snap (R214, skill S-VAM-9): a vampire-like breaks a neck. A vampire-type lies as if dead for a while and wakes on its own (all kinds for the same time by
 * default, so the time is not a test of what the target is), a weak mortal dies, a healthy one staggers, a boss is not moved. The user is given the same line and
 * the same cooldown for every one of those; the only other answers are no target, cooldown, and one refusal.
 */
public final class NeckSnapAction implements ExtraActions.Action {
    public static final NeckSnapAction INSTANCE = new NeckSnapAction();
    public static final String SKILL = "S-VAM-9";
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "neck_snap"));
    private static final String KEY = "supernaturalbloodlines.hud.action.neck_snap.";
    public static final String NO_TARGET = KEY + "no_target";
    public static final String COOLDOWN = KEY + "cooldown";
    public static final String REFUSED = KEY + "refused";
    public static final String DONE = KEY + "done";
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/hud/neck_snap.png");

    private NeckSnapAction() {
    }

    public static void register() {
        ExtraActions.install(INSTANCE);
    }

    @Override
    public ResourceLocation id() {
        return ExtraActions.NECK_SNAP;
    }

    @Override
    public Component name() {
        return Component.translatable("supernaturalbloodlines.hud.action.neck_snap");
    }

    @Override
    public Component note() {
        return Component.translatable("supernaturalbloodlines.hud.action.neck_snap.note", SpeciesConfig.snapSeconds().vampire(), RipHeartAction.hearts(SpeciesConfig.neckSnapKillHealth()), effectiveCooldownSeconds());
    }

    @Override
    public ResourceLocation icon() {
        return ICON;
    }

    @Override
    public boolean available(Player p) {
        return LadderQuery.owns(p, SKILL);
    }

    @Nullable
    @Override
    public Component trigger(ServerPlayer p) {
        return trigger(p, null);
    }

    /** What a snapped neck is, for this target (a player by their state, a creature by its faction, its Original root and its tag). */
    public static NeckSnapRules.Kind kindOf(LivingEntity target) {
        boolean boss = TargetChecks.isBoss(target);
        if (target instanceof Player pl) {
            BloodlineState s = BloodlineAccess.get(pl);
            var faction = VampirismAPI.factionPlayerHandler(pl).getCurrentFaction();
            return NeckSnapRules.kindOf(s.species(), faction == VReference.VAMPIRE_FACTION, s.ghoul(), s.inTurning() || s.inTransition(), boss);
        }
        if (TargetChecks.isOriginal(target)) {
            return boss ? NeckSnapRules.Kind.BOSS : NeckSnapRules.Kind.ORIGINAL;
        }
        boolean vampire = VampirismAPI.factionRegistry().getFaction(target) == VReference.VAMPIRE_FACTION;
        return NeckSnapRules.kindOf(Species.NONE, vampire, false, false, boss);
    }

    @Nullable
    @Override
    public Component trigger(ServerPlayer p, @Nullable LivingEntity target) {
        if (target == null || !TargetChecks.inView(p, target)) {
            return Component.translatable(NO_TARGET);
        }
        if (!ActionClock.ready(p, id())) {
            return Component.translatable(COOLDOWN);
        }
        // one refusal for everything that is not the snapper's to ask about: nothing here depends on what the target is
        if (TargetChecks.untouchable(target) || TargetChecks.alreadyDown(target) || !TargetChecks.mayAttack(p, target)) {
            return Component.translatable(REFUSED);
        }
        NeckSnapRules.Kind kind = kindOf(target);
        NeckSnapRules.Result result = NeckSnapRules.decide(kind, target.getHealth(), (float) SpeciesConfig.neckSnapKillHealth());
        DamageSource source = new DamageSource(p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE), p);
        if (result == NeckSnapRules.Result.KILL) {
            // a normal death (the savers and the turning apply): the hit is as hard as the target is healthy
            if (!target.hurt(source, target.getHealth() + target.getAbsorptionAmount() + 1.0F)) {
                return Component.translatable(REFUSED);
            }
        } else if (!TargetChecks.damageWouldLand(target, source, TargetChecks.PROBE)) { // a boss too: the answer must not depend on what the target is
            return Component.translatable(REFUSED);
        }
        switch (result) {
            case SNAP -> snap(target, kind);
            case STAGGER -> {
                int ticks = SpeciesConfig.neckSnapStaggerTicks();
                if (ticks > 0) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0));
                }
            }
            default -> { }
        }
        // the same for every result
        p.level().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 0.5f);
        p.displayClientMessage(Component.translatable(DONE, target.getDisplayName()), true);
        ActionClock.start(p, id(), cooldownTicks(p));
        TaskSignals.fire(p, "neck_snap", 1);
        return null;
    }

    private static void snap(LivingEntity target, NeckSnapRules.Kind kind) {
        SpeciesConfig.SnapSeconds s = SpeciesConfig.snapSeconds();
        int ticks = NeckSnapRules.ticks(kind, s.vampire(), s.augustine(), s.hybrid(), s.original());
        if (target instanceof ServerPlayer sp) {
            Downed.startSnapped(sp, ticks);
        } else {
            target.addEffect(new MobEffectInstance(SpeciesRegistries.SNAPPED, ticks, 0, false, true));
        }
    }

    /** The configured cooldown, but never shorter than the longest wake time (a snap cannot be renewed while it lasts). */
    static int effectiveCooldownSeconds() {
        SpeciesConfig.SnapSeconds s = SpeciesConfig.snapSeconds();
        return NeckSnapRules.cooldownSeconds(SpeciesConfig.neckSnapCooldownSeconds(), s.vampire(), s.augustine(), s.hybrid(), s.original());
    }

    @Override
    public int cooldownTicks(ServerPlayer p) {
        return effectiveCooldownSeconds() * 20;
    }

    @Override
    public int cooldownLeft(ServerPlayer p) {
        return ActionClock.left(p, id());
    }
}
