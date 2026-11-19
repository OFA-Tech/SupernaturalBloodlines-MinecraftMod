package dev.corgitaco.enhancedcelestials2core.api.lunarevent;

import java.util.Optional;
import net.minecraft.network.chat.TextColor;

/** A test stand-in (see EcReflectTest). */
public class LunarEvent {
    public Optional<TextColor> nameColor = Optional.empty();

    public Optional<TextColor> getNameColor() {
        return nameColor;
    }
}
