package net.ofatech.supernaturalbloodlines.parts.artifacts;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.artifacts.item.BoundRingItem;

/** Everything the artifacts part registers. */
public final class ArtifactItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SupernaturalBloodlines.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SupernaturalBloodlines.MODID);

    /** The player a ring belongs to. A ring without it is unbound and does nothing. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> BOUND_TO =
            COMPONENTS.registerComponentType("bound_to", b -> b.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    public static final DeferredItem<BoundRingItem> SUN_RING = ITEMS.register("sun_ring", BoundRingItem::new);

    /** Null without Werewolves: the item is not registered then. */
    public static final DeferredItem<BoundRingItem> MOON_RING = ModList.get().isLoaded("werewolves")
            ? ITEMS.register("moon_ring", BoundRingItem::new)
            : null;

    private ArtifactItems() {
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                e.accept(SUN_RING);
                if (MOON_RING != null) {
                    e.accept(MOON_RING);
                }
            }
        });
    }

    /** Is a copy of {@code item} bound to this player worn (Curios slot or inventory)? Another player's ring does not hide their own. */
    public static boolean isBoundWorn(Player p, Item item) {
        return ArtifactSlots.findWorn(p, item, s -> isBoundTo(s, p.getUUID())).isPresent();
    }

    public static boolean isBoundTo(ItemStack stack, UUID id) {
        return id.equals(stack.get(BOUND_TO.get()));
    }
}
