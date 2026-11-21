package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcMark;
import net.ofatech.supernaturalbloodlines.core.boss.BeastRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAccess;

/**
 * The Beast's Roar (R186), the moment the wind-up ends: everyone in range who is not an Alpha or a True Alpha is shaken (Weakness II and Darkness for
 * {@code beast_roar_seconds}), a hostile mob that belongs to no pack runs from it, and the Alphas who stand against it are told they did. Creative and spectating players,
 * the bosses and the Spirit are left alone. The chat line is the notice: nobody is hit by something they were not told about.
 */
public final class BeastRoar {
    public static final String SHAKEN = "supernaturalbloodlines.boss.beast.roar";
    public static final String STOOD = "supernaturalbloodlines.boss.beast.roar_stood";

    private BeastRoar() {
    }

    /** Lets it out; returns how many were shaken. */
    public static int let(ServerLevel level, BeastEntity beast) {
        double range = BossesConfig.beastRoarRange();
        int ticks = BeastRules.fearTicks(BossesConfig.beastRoarSeconds());
        level.playSound(null, beast.getX(), beast.getY(), beast.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 4.0F, 0.5F);
        int shaken = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, beast.getBoundingBox().inflate(range), x -> x != beast && x.isAlive())) {
            if (!BeastRules.roarInRange(e.distanceToSqr(beast), range)) {
                continue;
            }
            boolean exempt = e instanceof BossNpc || e instanceof GevaudanSpiritEntity || e instanceof Player p && (p.isCreative() || p.isSpectator());
            if (e instanceof Player p) {
                boolean alpha = PackAccess.isAlpha(PackAccess.wolf(p).rank());
                if (BeastRules.roarAffects(BeastRules.Kind.PLAYER, alpha, exempt)) {
                    shake(e, ticks);
                    shaken++;
                    if (p instanceof ServerPlayer sp) {
                        sp.sendSystemMessage(Component.translatable(SHAKEN));
                    }
                } else if (alpha && !exempt && p instanceof ServerPlayer sp) {
                    sp.displayClientMessage(Component.translatable(STOOD), true);
                }
            } else if (e.hasData(SpeciesRegistries.NPC_MARK)) {
                boolean alpha = e.getData(SpeciesRegistries.NPC_MARK).kind() == NpcMark.Kind.BOSS_PACK;
                if (BeastRules.roarAffects(BeastRules.Kind.PACK_MEMBER, alpha, exempt)) {
                    shake(e, ticks);
                    shaken++;
                }
            } else if (e instanceof Monster m && BeastRules.roarAffects(BeastRules.Kind.MOB, false, exempt)) {
                flee(m, beast);
                shaken++;
            }
        }
        return shaken;
    }

    private static void shake(LivingEntity e, int ticks) {
        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, ticks, 0));
    }

    /** A mob that belongs to no pack runs from the Beast for the length of the fear: it drops its target and goes the other way. */
    private static void flee(Mob mob, BeastEntity beast) {
        mob.setTarget(null);
        Vec3 away = mob.position().subtract(beast.position());
        Vec3 dir = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
        Vec3 to = mob.position().add(dir.scale(14.0));
        mob.getNavigation().moveTo(to.x, to.y, to.z, 1.4);
    }
}
