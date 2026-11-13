package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class PartBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SupernaturalBloodlines.MODID);

    /** Its altar power comes from data (enchanted/altar/block/blood_stone.json). */
    public static final DeferredBlock<Block> BLOOD_STONE = BLOCKS.registerSimpleBlock("blood_stone",
            BlockBehaviour.Properties.of().mapColor(MapColor.CRIMSON_NYLIUM).strength(3.0F, 6.0F).sound(SoundType.NETHER_BRICKS));

    private PartBlocks() {}

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
