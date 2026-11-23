package net.ofatech.supernaturalbloodlines.parts.species.skill;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.death.FinalDeath;
import net.ofatech.supernaturalbloodlines.core.hud.RipHeartRules;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.original.Downed;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;
import net.ofatech.supernaturalbloodlines.shared.supernatural.TaskSignals;
import org.jetbrains.annotations.Nullable;

/**
 * Rip Heart (R204, skill S-VAM-8): a vampire-like tears the heart out of a creature at or under {@code rip_heart_health}. It is final (the Elixir, the Resurrection
 * Ring and a Totem do not take it back) and reaches a downed or snapped target, because that is what it is for. An Original never dies of it, and the user is told the
 * same thing and pays the same cooldown for an Original as for anyone else: only the missing death differs. Bosses are treated like Originals.
 */
public final class RipHeartAction implements ExtraActions.Action {
    public static final RipHeartAction INSTANCE = new RipHeartAction();
    public static final String SKILL = "S-VAM-8";
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "rip_heart"));
    private static final String KEY = "supernaturalbloodlines.hud.action.rip_heart.";
    public static final String NO_TARGET = KEY + "no_target";
    public static final String COOLDOWN = KEY + "cooldown";
    public static final String REFUSED = KEY + "refused";
    public static final String TOO_STRONG = KEY + "too_strong";
    public static final String DONE = KEY + "done";
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/hud/rip_heart.png");

    private RipHeartAction() {
    }

    public static void register() {
        ExtraActions.install(INSTANCE);
        // the vanilla Totem is used inside the hit, before any death event: a heart-rip is final, so the Totem is not used
        NeoForge.EVENT_BUS.addListener((LivingUseTotemEvent e) -> {
            if (FinalDeath.is(e.getEntity())) {
                e.setCanceled(true);
            }
        });
    }

    @Override
    public ResourceLocation id() {
        return ExtraActions.RIP_HEART;
    }

    @Override
    public Component name() {
        return Component.translatable("supernaturalbloodlines.hud.action.rip_heart");
    }

    @Override
    public Component note() {
        return Component.translatable("supernaturalbloodlines.hud.action.rip_heart.note", hearts(), SpeciesConfig.ripHeartCooldownSeconds());
    }

    static String hearts() {
        return hearts(SpeciesConfig.ripHeartHealth());
    }

    /** A health in hearts (two health to a heart), as the texts show it. */
    static String hearts(double health) {
        double h = health / 2.0;
        return h == Math.rint(h) ? Long.toString((long) h) : Double.toString(h);
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

    @Nullable
    @Override
    public Component trigger(ServerPlayer p, @Nullable LivingEntity target) {
        if (target == null || !TargetChecks.inView(p, target)) {
            return Component.translatable(NO_TARGET);
        }
        if (!ActionClock.ready(p, id())) {
            return Component.translatable(COOLDOWN);
        }
        if (TargetChecks.untouchable(target)) {
            return Component.translatable(REFUSED);
        }
        // from here on the answer for an Original (or a boss) and for anyone else is the same line, the same cooldown and the same checks
        RipHeartRules.Result result = RipHeartRules.decide(target.getHealth(), (float) SpeciesConfig.ripHeartHealth(), TargetChecks.isOriginal(target) || TargetChecks.isBoss(target));
        if (result == RipHeartRules.Result.TOO_STRONG) {
            return Component.translatable(TOO_STRONG); // the same for everyone, an Original at full health too
        }
        if (!TargetChecks.mayAttack(p, target)) {
            return Component.translatable(REFUSED);
        }
        DamageSource source = new DamageSource(p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE), p);
        float blow = target.getHealth() + target.getAbsorptionAmount() + 1.0F;
        boolean inDying = TargetChecks.inVampirismDying(target);
        if (result == RipHeartRules.Result.NO_EFFECT) {
            // nothing is done, but the damage is asked about all the same (a duel, an invulnerability, a claim), with a probe that deals nothing and counts nothing:
            // the answer must not say what the target is
            if (!inDying && !TargetChecks.damageWouldLand(target, source, TargetChecks.PROBE)) {
                return Component.translatable(REFUSED);
            }
        } else if (!tear(p, target, source, blow, inDying)) {
            return Component.translatable(REFUSED);
        }
        p.level().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8f, 0.5f);
        p.displayClientMessage(Component.translatable(DONE, target.getDisplayName()), true);
        ActionClock.start(p, id(), cooldownTicks(p));
        if (result == RipHeartRules.Result.FINISH && !target.isAlive()) {
            TaskSignals.fire(p, "rip_heart", 1);
        }
        return null;
    }

    /**
     * The blow itself: past the Original rules (a downed or snapped target is reached), final, and through Vampirism's dying state: a vampire who is already in
     * it is killed there, and one whose death Vampirism catches only now (its hook starts the dying state instead of letting the blow kill) is finished at once.
     */
    private static boolean tear(ServerPlayer p, LivingEntity target, DamageSource source, float blow, boolean inDying) {
        boolean[] hit = new boolean[1];
        DamageSource kill = p.damageSources().source(DamageTypes.GENERIC_KILL, p);
        FinalDeath.run(target, () -> {
            if (inDying) {
                hit[0] = OriginalRules.killThroughDying((ServerPlayer) target, kill);
                return;
            }
            hit[0] = target.hurt(source, blow);
            if (hit[0] && target instanceof ServerPlayer sp && sp.isAlive() && TargetChecks.inVampirismDying(sp)) {
                OriginalRules.killThroughDying(sp, kill);
            }
        });
        return hit[0];
    }
    @Override
    public int cooldownTicks(ServerPlayer p) {
        return SpeciesConfig.ripHeartCooldownSeconds() * 20;
    }

    @Override
    public int cooldownLeft(ServerPlayer p) {
        return ActionClock.left(p, id());
    }
}
