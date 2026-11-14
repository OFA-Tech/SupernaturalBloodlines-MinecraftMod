package net.ofatech.supernaturalbloodlines.parts.species.item;

import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * R101b: a bottle of blood taken from a creature or player with a silver blade ({@code SiresBlood}). It remembers whose blood it is
 * (the {@code blood_source} UUID and the {@code blood_source_name}); the Rite of Severance only accepts the blood of the sired's own sire.
 * It cannot be drunk.
 */
public final class SiresBloodItem extends Item {
    public SiresBloodItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(UUID source, String name) {
        ItemStack stack = new ItemStack(SpeciesRegistries.SIRES_BLOOD.get());
        stack.set(SpeciesRegistries.BLOOD_SOURCE_COMPONENT, source);
        stack.set(SpeciesRegistries.BLOOD_SOURCE_NAME_COMPONENT, name);
        return stack;
    }

    @Nullable
    public static UUID sourceOf(ItemStack stack) {
        return stack.get(SpeciesRegistries.BLOOD_SOURCE_COMPONENT);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        String name = stack.get(SpeciesRegistries.BLOOD_SOURCE_NAME_COMPONENT);
        lines.add(name == null ? Component.translatable("tooltip.supernaturalbloodlines.sires_blood.nobody")
                : Component.translatable("tooltip.supernaturalbloodlines.sires_blood", name));
    }
}
