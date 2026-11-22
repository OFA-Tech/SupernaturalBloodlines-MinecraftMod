package net.ofatech.supernaturalbloodlines.parts.species.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.turning.TurningHandler;

/**
 * What a bottle of blood says about itself, read from the synced label only (R210): a common vampire's bottle names its source, every other kind is
 * "Strange blood" and warns that drinking it may harm or change you, and no kind word or name of a secret ever reaches a tooltip because none is sent to the
 * client. A werewolf in the hybrid transition is also told that only the right blood will hold it, on every blood bottle, so the line tells nothing about the kind.
 */
public final class BloodTooltip {
    public static final String COMMON = "tooltip.supernaturalbloodlines.blood.common";
    public static final String FROM = "tooltip.supernaturalbloodlines.blood.from";
    public static final String STRANGE = "tooltip.supernaturalbloodlines.blood.strange";
    public static final String WARNING = "tooltip.supernaturalbloodlines.blood.warning";
    public static final String TRANSITION = "tooltip.supernaturalbloodlines.blood.transition";

    private BloodTooltip() {
    }

    public static void register() {
        if (FMLEnvironment.dist.isClient()) {
            Client.register();
        }
    }

    /** Client only: its own class, so the server never resolves the client classes it names. */
    private static final class Client {
        static void register() {
            NeoForge.EVENT_BUS.addListener((ItemTooltipEvent e) -> {
                var player = Minecraft.getInstance().player;
                append(e.getItemStack(), e.getToolTip(), player != null && BloodlineAccess.get(player).inTransition());
            });
        }
    }

    public static void append(ItemStack stack, List<Component> lines) {
        append(stack, lines, false);
    }

    /**
     * Adds the blood lines of a stamped bottle, or of an unstamped vampire blood bottle (common vampire blood). Anything else is untouched.
     *
     * @param inTransition the viewer is a werewolf in the hybrid transition (their own state, which their client knows)
     */
    public static void append(ItemStack stack, List<Component> lines, boolean inTransition) {
        String label = stack.get(SpeciesRegistries.BLOOD_LABEL_COMPONENT.get());
        if (label == null && !stack.is(TurningHandler.VAMPIRE_BLOOD)) {
            return;
        }
        if (label != null && BloodStamp.SECRET_LABEL.equals(label)) {
            lines.add(Component.translatable(STRANGE).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable(WARNING).withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable(COMMON).withStyle(ChatFormatting.GRAY));
            if (label != null && !label.isEmpty()) {
                lines.add(Component.translatable(FROM, label).withStyle(ChatFormatting.GRAY));
            }
        }
        if (inTransition) {
            lines.add(Component.translatable(TRANSITION).withStyle(ChatFormatting.GRAY));
        }
    }
}
