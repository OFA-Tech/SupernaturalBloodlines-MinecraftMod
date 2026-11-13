package net.ofatech.supernaturalbloodlines.parts.species.pack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;

/**
 * The pack bonus (R94). Every 100 ticks an online Alpha counts their Betas within 64 blocks in the same dimension. With
 * 3 or more of them (raw count) the Alpha gets +5% attack for each, bitten Betas counting double, up to +25%, and those
 * Betas get Regeneration I. Below 3, or with the Alpha away, offline or no longer a werewolf Alpha, the bonus is gone:
 * the Betas keep the pack either way (the index does not care who is online). The modifier is permanent, so it is listed
 * in {@code StalePartModifiers}, and it is removed at logout.
 */
public final class PackBonus {
    public static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "pack_bonus");
    public static final int PERIOD = 100;
    public static final double RANGE = 64;
    public static final int MIN_BETAS = 3;
    public static final double STEP = 0.05;
    public static final int CAP = 5;
    public static final int REGEN_TICKS = 120;

    private PackBonus() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % PERIOD == 0) {
                tick(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                set(p, 0);
            }
        });
    }

    /** The attack bonus: nothing below {@link #MIN_BETAS} raw Betas, else 5% per counted Beta (bitten twice) up to 25%. */
    public static double bonusFor(int rawBetas, int doubledBetas) {
        return rawBetas < MIN_BETAS ? 0 : round(STEP * Math.min(doubledBetas, CAP));
    }

    /** One pass for one player: a non-Alpha is only cleaned of a bonus they should not have. */
    public static void tick(ServerPlayer alpha) {
        if (!PackAccess.isAlpha(PackAccess.wolf(alpha).rank()) || !VampirismSupernatural.isWerewolf(alpha)) {
            set(alpha, 0);
            return;
        }
        List<ServerPlayer> near = new ArrayList<>();
        int doubled = 0;
        for (UUID id : PackIndex.get(alpha.serverLevel()).betasOf(alpha.getUUID())) {
            ServerPlayer beta = SireCall.player(alpha.server, id);
            if (beta != null && beta.isAlive() && beta.level() == alpha.level() && alpha.distanceToSqr(beta) <= RANGE * RANGE) {
                near.add(beta);
                doubled += PackAccess.wolf(beta).lineage() == Lineage.BITTEN ? 2 : 1;
            }
        }
        double bonus = bonusFor(near.size(), doubled);
        set(alpha, bonus);
        if (bonus > 0) {
            for (ServerPlayer beta : near) {
                beta.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_TICKS, 0, true, true));
            }
        }
    }

    private static void set(ServerPlayer p, double amount) {
        AttributeInstance a = p.getAttribute(Attributes.ATTACK_DAMAGE);
        if (a == null) {
            return;
        }
        AttributeModifier current = a.getModifier(MODIFIER_ID);
        if (amount == 0) {
            if (current != null) {
                a.removeModifier(MODIFIER_ID);
            }
        } else if (current == null || current.amount() != amount) {
            a.removeModifier(MODIFIER_ID);
            a.addPermanentModifier(new AttributeModifier(MODIFIER_ID, amount, Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }
}
