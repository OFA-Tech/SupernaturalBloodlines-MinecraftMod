package net.ofatech.supernaturalbloodlines.parts.species.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.turning.TurningHandler;

/**
 * What a bottle of blood says about itself, read from the synced label only (R210): a common vampire's bottle names its source, every other kind is
 * "Strange blood", and no kind word or name of a secret ever reaches a tooltip because none is sent to the client.
 */
public final class BloodTooltip {
    public static final String COMMON = "tooltip.supernaturalbloodlines.blood.common";
    public static final String FROM = "tooltip.supernaturalbloodlines.blood.from";
    public static final String STRANGE = "tooltip.supernaturalbloodlines.blood.strange";

    private BloodTooltip() {
    }

    public static void register() {
        if (FMLEnvironment.dist.isClient()) {
            NeoForge.EVENT_BUS.addListener((ItemTooltipEvent e) -> append(e.getItemStack(), e.getToolTip()));
        }
    }

    /** Adds the blood lines of a stamped bottle, or of an unstamped vampire blood bottle (common vampire blood). Anything else is untouched. */
    public static void append(ItemStack stack, List<Component> lines) {
        String label = stack.get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get());
        if (label == null) {
            if (stack.is(TurningHandler.VAMPIRE_BLOOD)) {
                lines.add(Component.translatable(COMMON).withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        if (BloodStamp.SECRET_LABEL.equals(label)) {
            lines.add(Component.translatable(STRANGE).withStyle(ChatFormatting.GRAY));
            return;
        }
        lines.add(Component.translatable(COMMON).withStyle(ChatFormatting.GRAY));
        if (!label.isEmpty()) {
            lines.add(Component.translatable(FROM, label).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
