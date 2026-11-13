package net.ofatech.supernaturalbloodlines.parts.enchantedtensura;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

public final class PartItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SupernaturalBloodlines.MODID);

    public static final DeferredItem<BlockItem> CONDENSED_MAGICULE = ITEMS.registerSimpleBlockItem(PartBlocks.CONDENSED_MAGICULE);

    private PartItems() {}

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
                e.accept(CONDENSED_MAGICULE);
            }
        });
    }
}
