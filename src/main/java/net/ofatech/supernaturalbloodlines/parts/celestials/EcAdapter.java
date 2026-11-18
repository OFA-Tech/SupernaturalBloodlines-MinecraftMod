package net.ofatech.supernaturalbloodlines.parts.celestials;

import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** What the part needs from Enhanced Celestials 2. {@link EcReflect} talks to the real mod; the game tests use a fake. */
public interface EcAdapter {
    /** A lunar event: its id and name colour (RGB, -1 when it has none). */
    record Event(ResourceLocation id, int color) {
        /** The translation key EC's own language files use for the event's name. */
        public String nameKey() {
            return "lunar.event." + id.getNamespace() + "." + id.getPath();
        }
    }

    /** The event that is running now; empty for the normal moon. Works on both sides. */
    Optional<Event> active(Level level);

    /** The scheduled events by days from today (0 = tonight), below {@code days}. Works on both sides. */
    Map<Integer, Event> upcoming(Level level, int days);

    /** Whether Enhanced Celestials has loaded the lunar event {@code id} (our datapack file decoded). Server only. */
    boolean knows(ServerLevel level, ResourceLocation id);

    /** Starts the event {@code id} tonight. False when another event is running, or the event is not known. Server only. */
    boolean start(ServerLevel level, ResourceLocation id);

    /** Ends the running event when it is {@code id}. False when it is another one. Server only. */
    boolean stop(ServerLevel level, ResourceLocation id);
}
