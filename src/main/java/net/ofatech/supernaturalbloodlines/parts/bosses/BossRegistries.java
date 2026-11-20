package net.ofatech.supernaturalbloodlines.parts.bosses;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.ElijahEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.FinnEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.KolEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.KlausEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.RebekahEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.MikaelEntity;

/** The bosses part's entity types (the first of the mod). Server and client both load this; nothing client-side is named here. */
public final class BossRegistries {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, SupernaturalBloodlines.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<MikaelEntity>> MIKAEL = ENTITY_TYPES.register("mikael",
            () -> EntityType.Builder.of(MikaelEntity::new, MobCategory.MISC).sized(0.6F, 1.95F).clientTrackingRange(10).fireImmune().build("mikael"));

    public static final DeferredHolder<EntityType<?>, EntityType<KlausEntity>> KLAUS = ENTITY_TYPES.register("klaus",
            () -> EntityType.Builder.of(KlausEntity::new, MobCategory.MISC).sized(0.6F, 1.95F).clientTrackingRange(10).fireImmune().build("klaus"));

    public static final DeferredHolder<EntityType<?>, EntityType<ElijahEntity>> ELIJAH = ENTITY_TYPES.register("elijah",
            () -> EntityType.Builder.of(ElijahEntity::new, MobCategory.MISC).sized(0.6F, 1.95F).clientTrackingRange(10).fireImmune().build("elijah"));

    public static final DeferredHolder<EntityType<?>, EntityType<RebekahEntity>> REBEKAH = ENTITY_TYPES.register("rebekah",
            () -> EntityType.Builder.of(RebekahEntity::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(10).fireImmune().build("rebekah"));

    public static final DeferredHolder<EntityType<?>, EntityType<KolEntity>> KOL = ENTITY_TYPES.register("kol",
            () -> EntityType.Builder.of(KolEntity::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(10).fireImmune().build("kol"));

    public static final DeferredHolder<EntityType<?>, EntityType<FinnEntity>> FINN = ENTITY_TYPES.register("finn",
            () -> EntityType.Builder.of(FinnEntity::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(10).fireImmune().build("finn"));

    public static final DeferredHolder<EntityType<?>, EntityType<net.ofatech.supernaturalbloodlines.parts.bosses.entity.DeucalionEntity>> DEUCALION = ENTITY_TYPES.register("deucalion",
            () -> EntityType.Builder.of(net.ofatech.supernaturalbloodlines.parts.bosses.entity.DeucalionEntity::new, MobCategory.MISC).sized(0.6F, 1.95F).clientTrackingRange(10).fireImmune().build("deucalion"));

    private BossRegistries() {
    }

    static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(MIKAEL.get(), MikaelEntity.attributes().build());
            e.put(KLAUS.get(), KlausEntity.attributes().build());
            e.put(ELIJAH.get(), ElijahEntity.attributes().build());
            e.put(REBEKAH.get(), RebekahEntity.attributes().build());
            e.put(KOL.get(), KolEntity.attributes().build());
            e.put(FINN.get(), FinnEntity.attributes().build());
            e.put(DEUCALION.get(), net.ofatech.supernaturalbloodlines.parts.bosses.entity.DeucalionEntity.attributes().build());
        });
    }
}
