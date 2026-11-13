package net.ofatech.supernaturalbloodlines.parts.enchantedtensura.rite;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.manasmods.tensura.race.RaceUtils;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** On start, holy damage to every undead-race (Tensura) or {@code minecraft:undead} creature within 8 blocks of the circle. */
public record PurificationRiteFactory(float damage) implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "purification");
    // the id contains "holy", which is how Tensura's TensuraDamageHelper.isHoly recognises it
    public static final ResourceKey<DamageType> HOLY_PURIFICATION = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "holy_purification"));
    public static final MapCodec<PurificationRiteFactory> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("damage", 8.0F).forGetter(PurificationRiteFactory::damage)
    ).apply(i, PurificationRiteFactory::new));

    private static final double RADIUS = 8;

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new PurificationRite(base, params, damage);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    private static final class PurificationRite extends Rite {
        private final float damage;

        PurificationRite(BaseRiteParams base, RiteParams params, float damage) {
            super(base, params);
            this.damage = damage;
        }

        @Override
        protected boolean onStart(RiteParams params) {
            DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(HOLY_PURIFICATION));
            Vec3 centre = Vec3.atCenterOf(pos);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(RADIUS))) {
                if (e.distanceToSqr(centre) <= RADIUS * RADIUS
                        && (RaceUtils.isUndead(e) || e.getType().is(EntityTypeTags.UNDEAD))) {
                    e.hurt(source, damage);
                }
            }
            return false;
        }
    }
}
