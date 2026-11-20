package net.ofatech.supernaturalbloodlines.parts.bosses;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.Unbreakable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** What the bosses part registers as items. Mikael's Stake is in {@code #supernaturalbloodlines:white_oak}, so it kills an open Original like the White Oak Stake. */
public final class BossItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SupernaturalBloodlines.MODID);

    /** Mikael's reward and weapon: white oak, and it never breaks. */
    public static final DeferredItem<SwordItem> MIKAELS_STAKE = ITEMS.register("mikaels_stake",
            () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true)).fireResistant()));

    private BossItems() {
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.COMBAT) {
                e.accept(MIKAELS_STAKE);
            }
        });
    }
}
