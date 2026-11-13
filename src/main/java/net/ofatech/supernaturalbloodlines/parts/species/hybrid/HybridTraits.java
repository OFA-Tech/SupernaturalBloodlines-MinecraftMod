package net.ofatech.supernaturalbloodlines.parts.species.hybrid;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

/**
 * What being a hybrid type (HYBRID, ORIGINAL_HYBRID, TRIBRID) means for the body (R42): +0.02 movement speed and +2
 * attack damage, both scaled by age, {@code min(1, (gameTime - turnedAt) / 1_200_000)}. The modifier is permanent
 * (saved), so its id is listed in {@code core/StalePartModifiers}. Links no Werewolves class.
 */
public final class HybridTraits {
    public static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "hybrid");
    public static final long AGE_TICKS = 1_200_000L;

    private static final double SPEED = 0.02;
    private static final double ATTACK = 2.0;
    private static final int RECOMPUTE_TICKS = 1200;

    private HybridTraits() {
    }

    public static void register() {
        BloodlineAccess.addListener((p, change) -> sync(p));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> syncIfServer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> syncIfServer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> syncIfServer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % RECOMPUTE_TICKS == 0) {
                sync(p);
            }
        });
    }

    public static boolean isHybrid(Species s) {
        return s == Species.HYBRID || s == Species.ORIGINAL_HYBRID || s == Species.TRIBRID;
    }

    /** 0 to 1. A {@code turnedAt} of 0 means "unset" and counts as fully grown. */
    static double age(long turnedAt, long gameTime) {
        return turnedAt <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, (gameTime - turnedAt) / (double) AGE_TICKS));
    }

    /** Idempotent: makes the player's attributes match their species and age. */
    public static void sync(ServerPlayer p) {
        sync(p, p.level().getGameTime());
    }

    public static void sync(ServerPlayer p, long gameTime) {
        var state = BloodlineAccess.get(p);
        boolean on = isHybrid(state.species());
        double age = on ? age(state.turnedAt(), gameTime) : 0.0;
        apply(p, Attributes.MOVEMENT_SPEED, SPEED * age, on);
        apply(p, Attributes.ATTACK_DAMAGE, ATTACK * age, on);
    }

    private static void syncIfServer(net.minecraft.world.entity.player.Player p) {
        if (p instanceof ServerPlayer sp) {
            sync(sp);
        }
    }

    private static void apply(ServerPlayer p, Holder<Attribute> attribute, double amount, boolean on) {
        AttributeInstance a = p.getAttribute(attribute);
        if (a == null) {
            return;
        }
        if (!on) {
            a.removeModifier(MODIFIER_ID);
            return;
        }
        AttributeModifier current = a.getModifier(MODIFIER_ID);
        if (current == null || Math.abs(current.amount() - amount) > 1e-12) {
            a.addOrReplacePermanentModifier(new AttributeModifier(MODIFIER_ID, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
