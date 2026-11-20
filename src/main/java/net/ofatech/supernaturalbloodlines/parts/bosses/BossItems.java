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
import net.ofatech.supernaturalbloodlines.parts.bosses.item.EstherGrimoireItem;
import net.ofatech.supernaturalbloodlines.parts.bosses.item.KlausBloodItem;
import net.ofatech.supernaturalbloodlines.parts.bosses.item.KolGrimoireItem;
import net.ofatech.supernaturalbloodlines.parts.bosses.item.MemoryClawsItem;
import net.ofatech.supernaturalbloodlines.parts.bosses.item.TheElixirItem;
import net.ofatech.supernaturalbloodlines.parts.bosses.item.TheTrueCureItem;

/** What the bosses part registers as items. Mikael's Stake is in {@code #supernaturalbloodlines:white_oak}, so it kills an open Original like the White Oak Stake. */
public final class BossItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SupernaturalBloodlines.MODID);

    /** Mikael's reward and weapon: white oak, and it never breaks. */
    public static final DeferredItem<SwordItem> MIKAELS_STAKE = ITEMS.register("mikaels_stake",
            () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true)).fireResistant()));

    /** Klaus's reward: Original hybrid blood that also cures werewolf venom (R164). */
    public static final DeferredItem<KlausBloodItem> KLAUS_BLOOD = ITEMS.register("klaus_blood",
            () -> new KlausBloodItem(new Item.Properties().stacksTo(16).fireResistant()));

    /** Elijah's reward: one life back from any death but a line death or /kill (R168). */
    public static final DeferredItem<TheElixirItem> THE_ELIXIR = ITEMS.register("the_elixir", () -> new TheElixirItem(new Item.Properties()));

    /** Rebekah's reward: the cure an Original can drink without being desiccated (R171). */
    public static final DeferredItem<TheTrueCureItem> THE_TRUE_CURE = ITEMS.register("the_true_cure", () -> new TheTrueCureItem(new Item.Properties()));

    /** Kol's reward: skill points once, and the key to the Rite of Cloaking (R174). */
    public static final DeferredItem<KolGrimoireItem> KOL_GRIMOIRE = ITEMS.register("kol_grimoire", () -> new KolGrimoireItem(new Item.Properties()));

    /** Finn's reward: the key to the Linking Spell (R177). */
    public static final DeferredItem<EstherGrimoireItem> ESTHER_GRIMOIRE = ITEMS.register("esther_grimoire", () -> new EstherGrimoireItem(new Item.Properties()));

    /** Peter's reward: the sire and the genes of a player who agrees to it, read on their Profile page (R182). */
    public static final DeferredItem<MemoryClawsItem> MEMORY_CLAWS = ITEMS.register("memory_claws", () -> new MemoryClawsItem(new Item.Properties()));

    private BossItems() {
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == CreativeModeTabs.COMBAT) {
                e.accept(MIKAELS_STAKE);
            }
            if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) {
                e.accept(KLAUS_BLOOD);
                e.accept(THE_ELIXIR);
                e.accept(THE_TRUE_CURE);
                e.accept(KOL_GRIMOIRE);
                e.accept(ESTHER_GRIMOIRE);
                e.accept(MEMORY_CLAWS);
            }
        });
    }
}
