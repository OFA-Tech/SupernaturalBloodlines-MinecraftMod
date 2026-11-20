package net.ofatech.supernaturalbloodlines.parts.bosses.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.boss.PeterRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;

/**
 * The burned Hale house (plan 17g, R181): a one-piece jigsaw (the piece is {@code structure/hale_house.nbt}), sunk three layers into the ground so the
 * cellar is under it, in forests and taigas, far apart. This type is the jigsaw with one question asked first: do the settings let a house be made here? The
 * switch ({@code peter_house_enabled}) and the chance ({@code peter_house_chance}) are read for each house, so a server can change them without a restart (houses
 * already in the world stay). Many houses can generate; there is one Peter ({@code BossDirector}). Registered with the bosses part, and the JSON that uses it carries
 * the load condition for the part and for Werewolves, so nothing here is loaded without both.
 */
public final class HaleHouse extends Structure {
    public static final MapCodec<HaleHouse> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(h -> h.pool)).apply(i, HaleHouse::new));

    private static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, SupernaturalBloodlines.MODID);
    public static final DeferredHolder<StructureType<?>, StructureType<HaleHouse>> TYPE = TYPES.register("hale_house", () -> () -> CODEC);

    /** The template starts three layers below the ground: the cellar is under the floor ({@link PeterRules#FLOOR_LAYER}). */
    public static final int SINK = -PeterRules.FLOOR_LAYER;

    private final Holder<StructureTemplatePool> pool;
    private final JigsawStructure delegate;

    public HaleHouse(StructureSettings settings, Holder<StructureTemplatePool> pool) {
        super(settings);
        this.pool = pool;
        this.delegate = new JigsawStructure(settings, pool, 1, ConstantHeight.of(VerticalAnchor.absolute(SINK)), false, Heightmap.Types.WORLD_SURFACE_WG);
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!PeterRules.houseGenerates(BossesConfig.peterHouseEnabled(), BossesConfig.peterHouseChance(), context.random().nextDouble())) {
            return Optional.empty();
        }
        return delegate.findGenerationPoint(context);
    }

    @Override
    public StructureType<?> type() {
        return TYPE.get();
    }
}