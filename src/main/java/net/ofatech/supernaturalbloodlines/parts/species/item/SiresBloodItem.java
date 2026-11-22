package net.ofatech.supernaturalbloodlines.parts.species.item;

import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.ofatech.supernaturalbloodlines.core.blood.BloodKind;
import net.ofatech.supernaturalbloodlines.core.blood.BloodSource;
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
        // sealed like every bottle (R210); its own public name stays: the taker took it from their own sire
        BloodStamp.writeSeal(stack, new BloodSource(source, name, false, BloodKind.COMMON_VAMPIRE));
        stack.set(SpeciesRegistries.BLOOD_SOURCE_NAME_COMPONENT, name);
        return stack;
    }

    @Nullable
    public static UUID sourceOf(ItemStack stack) {
        BloodStamp.migrate(stack); // an old Sire's Blood is sealed the moment a rite reads it
        return BloodStamp.read(stack).map(BloodSource::id).orElse(null);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        String name = stack.get(SpeciesRegistries.BLOOD_SOURCE_NAME_COMPONENT);
        lines.add(name == null ? Component.translatable("tooltip.supernaturalbloodlines.sires_blood.nobody")
                : Component.translatable("tooltip.supernaturalbloodlines.sires_blood", name));
    }
}
