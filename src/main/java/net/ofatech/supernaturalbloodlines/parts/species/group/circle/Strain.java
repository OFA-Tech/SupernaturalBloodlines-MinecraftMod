package net.ofatech.supernaturalbloodlines.parts.species.group.circle;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.ofatech.supernaturalbloodlines.core.group.circle.ChannelMeter;
import net.ofatech.supernaturalbloodlines.core.group.circle.CirclePlan;
import org.joml.Vector3f;

/** What it costs a member to give more than their channel meter holds (R257): a nosebleed, and past half the capacity pain that never takes the last heart. */
public final class Strain {
    private static final int NAUSEA_TICKS = 10 * 20;
    private static final int WEAKNESS_TICKS = 30 * 20;

    private Strain() {
    }

    public static void apply(ServerPlayer member, CirclePlan.Share share, int cap) {
        if (share.zone() == ChannelMeter.Zone.CLEAN || share.zone() == ChannelMeter.Zone.REFUSED) {
            return;
        }
        member.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_TICKS, 0));
        member.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 0));
        if (member.level() instanceof ServerLevel level) {
            level.sendParticles(new DustParticleOptions(new Vector3f(0.8f, 0.04f, 0.04f), 1.0f), member.getX(), member.getEyeY() - 0.2, member.getZ(), 12, 0.15, 0.1, 0.15, 0.0);
        }
        if (share.zone() == ChannelMeter.Zone.DAMAGE) {
            Pain.hurt(member, ChannelMeter.damageHp(share.overdraw(), cap));
        }
    }
}