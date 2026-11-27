package net.ofatech.supernaturalbloodlines.parts.species.rite;

import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import net.ofatech.supernaturalbloodlines.shared.rite.RiteTell;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.species.group.circle.CircleStage;

/**
 * Night Watch (R265, tier 2): a rite that stays until dawn. Every two seconds the monsters around the chalk are outlined (Glowing) and slowed (Slowness I)
 * for five seconds. The reach is fixed when it starts, from the circle's potency, and is saved with the rite. While it runs the anchor's death still
 * counts (the live link of {@code LinkWindow}).
 */
public record NightWatchRiteFactory(int minCircle) implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "night_watch");
    public static final String STARTED = "supernaturalbloodlines.rite.grand.night_watch_started";
    public static final String DAY = "supernaturalbloodlines.rite.grand.night_watch_day";
    public static final MapCodec<NightWatchRiteFactory> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("min_circle", 0).forGetter(NightWatchRiteFactory::minCircle)).apply(i, NightWatchRiteFactory::new));
    private static final int PERIOD = 40;

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new NightWatchRite(base, params, minCircle);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of();
    }

    public static final class NightWatchRite extends Rite {
        private final int minCircle;
        private int radius;
        private long dawn;

        NightWatchRite(BaseRiteParams base, RiteParams params, int minCircle) {
            super(base, params);
            this.minCircle = minCircle;
        }

        public int radius() {
            return radius;
        }

        @Override
        protected boolean onStart(RiteParams params) {
            long now = level.getDayTime();
            if (!MoonRule.isNight(level.dimensionType().hasFixedTime(), now)) {
                // nothing is paid and no cooldown is stamped: the rite cancels itself
                if (level.getPlayerByUUID(params.caster) instanceof ServerPlayer caster) {
                    RiteTell.refuse(caster, DAY);
                }
                return cancel();
            }
            String id = GrandGate.idOf(level, type);
            Optional<String> refused = GrandGate.check(level, params.caster, id, minCircle);
            if (refused.isPresent()) {
                if (level.getPlayerByUUID(params.caster) instanceof ServerPlayer caster) {
                    RiteTell.refuse(caster, refused.get());
                }
                return cancel();
            }
            radius = FeteRiteFactory.radius();
            dawn = now - Math.floorMod(now, 24_000L) + MoonRule.NIGHT_END;
            GrandGate.stamp(level, params.caster, id);
            if (level.getPlayerByUUID(params.caster) instanceof ServerPlayer caster) {
                RiteTell.say(caster, STARTED);
            }
            return true;
        }

        @Override
        public boolean tick() {
            if (level.getDayTime() >= dawn) {
                // Enchanted skips unloaded rites, and stop() would load the chalk to detach it.
                return level.isLoaded(pos) && stop();
            }
            return super.tick();
        }

        @Override
        protected boolean onTick(RiteParams params) {
            if (!MoonRule.isNight(level.dimensionType().hasFixedTime(), level.getDayTime())) {
                return false;
            }
            if (params.ticks() % PERIOD == 0) {
                for (Monster m : level.getEntitiesOfClass(Monster.class, new AABB(pos).inflate(radius, 16, radius), mob -> mob.isAlive()
                        && Math.pow(mob.getX() - (pos.getX() + 0.5), 2) + Math.pow(mob.getZ() - (pos.getZ() + 0.5), 2) <= (double) radius * radius)) {
                    m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                    m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
                }
            }
            return true;
        }

        @Override
        protected void saveAdditional(CompoundTag tag, ServerLevel level) {
            tag.putInt("radius", radius);
            tag.putLong("dawn", dawn);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, ServerLevel level) {
            radius = tag.getInt("radius");
            long now = level.getDayTime();
            dawn = tag.contains("dawn") ? tag.getLong("dawn") : MoonRule.isNight(false, now) ? now - Math.floorMod(now, 24_000L) + MoonRule.NIGHT_END : now;
        }
    }
}
