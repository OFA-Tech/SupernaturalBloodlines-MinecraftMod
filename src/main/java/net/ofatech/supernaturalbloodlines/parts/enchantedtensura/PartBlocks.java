package net.ofatech.supernaturalbloodlines.parts.enchantedtensura;

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

    /** Its altar power comes from data (enchanted/altar/block/condensed_magicule.json). */
    public static final DeferredBlock<Block> CONDENSED_MAGICULE = BLOCKS.registerSimpleBlock("condensed_magicule",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(3.0F, 6.0F)
                    .sound(SoundType.AMETHYST).lightLevel(s -> 5));

    private PartBlocks() {}

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
