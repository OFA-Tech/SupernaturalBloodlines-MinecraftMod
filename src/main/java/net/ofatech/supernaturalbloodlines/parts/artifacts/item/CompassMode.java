package net.ofatech.supernaturalbloodlines.parts.artifacts.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** What the Vampire Compass looks for. A compass with no mode is in {@link #VAMPIRES}. */
public enum CompassMode implements StringRepresentable {
    VAMPIRES("vampires"),
    DOPPELGANGERS("doppelgangers"),
    /** The third mode (R316): somebody a witch found with their taglock or blood. Only a compass attuned to somebody has it in its cycle. */
    LOCATOR("locator");

    public static final Codec<CompassMode> CODEC = StringRepresentable.fromEnum(CompassMode::values);
    public static final StreamCodec<ByteBuf, CompassMode> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], CompassMode::ordinal);

    private final String id;

    CompassMode(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public CompassMode toggled() {
        return toggled(false);
    }

    /** The next mode of the cycle; {@code attuned} puts the locator after the doppelgängers. */
    public CompassMode toggled(boolean attuned) {
        return switch (this) {
            case VAMPIRES -> DOPPELGANGERS;
            case DOPPELGANGERS -> attuned ? LOCATOR : VAMPIRES;
            case LOCATOR -> VAMPIRES;
        };
    }

    public String langKey() {
        return "supernaturalbloodlines.compass.mode." + id;
    }
}
