package net.ofatech.supernaturalbloodlines.parts.species.rite;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CovenPotency;

/**
 * The Fete des Benedictions (R264, tier 1): everybody near the chalk, the coven and outsiders alike, is blessed with Luck I and Regeneration I. The reach and
 * the length grow with the circle's potency; both are capped (48 blocks) and every number is an Admin row.
 */
public record FeteRiteFactory(int minCircle) implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "fete");
    public static final String DONE = "supernaturalbloodlines.rite.grand.fete_done";
    public static final int MAX_RADIUS = 48;
    public static final MapCodec<FeteRiteFactory> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("min_circle", 0).forGetter(FeteRiteFactory::minCircle)).apply(i, FeteRiteFactory::new));

    /** The reach of a grand rite: the config radius times the circle's potency, never over {@link #MAX_RADIUS}. */
    public static int radius() {
        return (int) Math.min(MAX_RADIUS, Math.round(CovenPotency.scale(GroupsConfig.grandRiteRadius())));
    }

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new FeteRite(base, params, minCircle);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of();
    }

    private static final class FeteRite extends Rite {
        private final int minCircle;

        FeteRite(BaseRiteParams base, RiteParams params, int minCircle) {
            super(base, params);
            this.minCircle = minCircle;
        }

        @Override
        protected boolean onStart(RiteParams params) {
            String id = GrandGate.idOf(level, type);
            Optional<String> refused = GrandGate.check(level, params.caster, id, minCircle);
            if (refused.isPresent()) {
                if (level.getPlayerByUUID(params.caster) instanceof ServerPlayer caster) {
                    caster.displayClientMessage(Component.translatable(refused.get()), true);
                }
                return cancel();
            }
            int radius = radius();
            int ticks = (int) Math.round(CovenPotency.scale(GroupsConfig.feteMinutes()) * 60 * 20);
            double r2 = (double) radius * radius;
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(radius, radius, radius), pl -> pl.isAlive() && !pl.isSpectator())) {
                double dx = p.getX() - (pos.getX() + 0.5);
                double dz = p.getZ() - (pos.getZ() + 0.5);
                if (dx * dx + dz * dz <= r2) {
                    p.addEffect(new MobEffectInstance(MobEffects.LUCK, ticks, 0));
                    p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 0));
                    p.displayClientMessage(Component.translatable(DONE), true);
                }
            }
            GrandGate.stamp(level, params.caster, id);
            return false;
        }
    }
}