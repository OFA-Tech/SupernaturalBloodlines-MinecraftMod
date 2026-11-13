package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted;

import net.favouriteless.enchanted.common.enchanted.poppet.PoppetColour;
import net.favouriteless.enchanted.common.items.poppets.PlayerPoppetItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.item.SunPoppetEffect;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.werewolves.SilverPoppetEffect;

public final class PartItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SupernaturalBloodlines.MODID);

    public static final DeferredItem<BlockItem> BLOOD_STONE = ITEMS.registerSimpleBlockItem(PartBlocks.BLOOD_STONE);

    // durability 1 and stack size 1 copy Enchanted's plain protection poppets; a test pins the durability
    public static final DeferredItem<PlayerPoppetItem> SUN_POPPET = ITEMS.register("sun_poppet",
            () -> new PlayerPoppetItem(PoppetColour.FIRE, new SunPoppetEffect(), new Item.Properties().durability(1).stacksTo(1)));

    /** Null without Werewolves: the item is not registered then. */
    public static final DeferredItem<PlayerPoppetItem> SILVER_POPPET = ModList.get().isLoaded("werewolves")
            ? ITEMS.register("silver_poppet",
                    () -> new PlayerPoppetItem(PoppetColour.EARTH, new SilverPoppetEffect(), new Item.Properties().durability(1).stacksTo(1)))
            : null;

    private PartItems() {}

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
                e.accept(BLOOD_STONE);
            } else if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                e.accept(SUN_POPPET);
                if (SILVER_POPPET != null) {
                    e.accept(SILVER_POPPET);
                }
            }
        });
    }
}
