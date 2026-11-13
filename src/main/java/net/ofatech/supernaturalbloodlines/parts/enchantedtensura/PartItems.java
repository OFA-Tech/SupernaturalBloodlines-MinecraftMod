package net.ofatech.supernaturalbloodlines.parts.enchantedtensura;

import net.favouriteless.enchanted.common.enchanted.poppet.PoppetColour;
import net.favouriteless.enchanted.common.items.poppets.PlayerPoppetItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.item.SpiritPoppetEffect;

public final class PartItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SupernaturalBloodlines.MODID);

    public static final DeferredItem<BlockItem> CONDENSED_MAGICULE = ITEMS.registerSimpleBlockItem(PartBlocks.CONDENSED_MAGICULE);

    // colour and durability copy Enchanted's magic poppet (durability 1, stacks to 1); the test pins both
    public static final DeferredItem<PlayerPoppetItem> SPIRIT_POPPET = ITEMS.register("spirit_poppet",
            () -> new PlayerPoppetItem(PoppetColour.MAGIC, new SpiritPoppetEffect(), new Item.Properties().durability(1).stacksTo(1)));

    private PartItems() {}

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
                e.accept(CONDENSED_MAGICULE);
            } else if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                e.accept(SPIRIT_POPPET);
            }
        });
    }
}
