package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.ofatech.supernaturalbloodlines.core.boss.BossRules;
import net.ofatech.supernaturalbloodlines.core.boss.KlausRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.ai.PreyGoal;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import org.jetbrains.annotations.Nullable;

/** The hunt the two shifting bosses share: who the prey is, how long he has been without it or without a hit, and when he leaves (R162 and R178). */
final class HuntState {
    enum Verdict { NONE, DAWN, LOST }

    @Nullable
    private UUID prey;
    private int strandedTicks;
    private int sinceHit;

    @Nullable
    UUID prey() {
        return prey;
    }

    void hunt(@Nullable UUID prey) {
        this.prey = prey;
    }

    int sinceHit() {
        return sinceHit;
    }

    /** Every half second: counts, and says whether he should leave. {@code active} is false while he is not hunting (a fallen Original): nothing counts. */
    Verdict period(Mob boss, boolean active) {
        if (prey == null || !active || boss.level().getServer() == null) {
            strandedTicks = 0;
            sinceHit = 0;
            return Verdict.NONE;
        }
        strandedTicks = KlausRules.stranded(PreyGoal.find(boss, prey) != null, boss.getTarget() != null) ? strandedTicks + 10 : 0;
        sinceHit += 10;
        if (KlausRules.dawn(boss.level().getDayTime())) {
            return Verdict.DAWN;
        }
        return KlausRules.givesUp(sinceHit) || KlausRules.withdraws(strandedTicks) ? Verdict.LOST : Verdict.NONE;
    }

    /** A blow landed: only one on his prey starts the count again. */
    void landed(Entity target) {
        if (prey != null && prey.equals(target.getUUID())) {
            sinceHit = 0;
        }
    }

    /** He goes: the one he hunted (if here) is told why, and the index says lost so the director may send him again. The caller discards the entity. */
    void leave(BossNpc boss, @Nullable String say) {
        var server = boss.level().getServer();
        if (server == null) {
            return;
        }
        if (say != null && prey != null && SireCall.player(server, prey) instanceof ServerPlayer p) {
            p.displayClientMessage(Component.translatable(say), true);
        }
        BossIndex.get(server).apply(boss.boss(), BossRules.Event.LOST);
    }

    void save(CompoundTag tag) {
        if (prey != null) {
            tag.putUUID("prey", prey);
        }
        tag.putInt("stranded", strandedTicks);
        tag.putInt("since_hit", sinceHit);
    }

    void load(CompoundTag tag) {
        prey = tag.hasUUID("prey") ? tag.getUUID("prey") : null;
        strandedTicks = tag.getInt("stranded");
        sinceHit = tag.getInt("since_hit");
    }
}
