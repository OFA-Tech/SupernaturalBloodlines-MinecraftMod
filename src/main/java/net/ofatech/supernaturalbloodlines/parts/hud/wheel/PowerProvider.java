package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;

/** One source of wheel entries. A provider lists only what the player has now; it never runs anything. */
public interface PowerProvider {
    List<WheelEntry> list(ServerPlayer p);
}
