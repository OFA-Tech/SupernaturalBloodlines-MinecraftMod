package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.core.wheel.ReplacedPowers;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudClientConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.net.ClientCatalogue;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;

/**
 * The list the wheel draws: what the server sent, minus the powers one of ours replaces (only ours shows), plus Great Sage's (it runs on the client). Rebuilt when the
 * catalogue changes and on each game tick for Great Sage's on/off state; between ticks the same list is returned. Client only.
 */
public final class WheelList {
    private static List<WheelEntry> rawSeen;
    private static boolean leapSeen;
    private static List<WheelEntry> kept = List.of();
    private static long builtAt = Long.MIN_VALUE;
    private static List<WheelEntry> cached = List.of();

    private WheelList() {
    }

    /** The sources whose mod is installed; a power of one that is not is never listed. */
    public static boolean installed(PowerSource s) {
        if (DevWheel.active) {
            return true;
        }
        return switch (s) {
            case BLOODLINE -> true;
            case GREAT_SAGE -> GreatSageProvider.available();
            default -> ModList.get().isLoaded(s.modId());
        };
    }

    public static List<WheelEntry> current() {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        List<WheelEntry> raw = ClientCatalogue.entries();
        boolean leap = HudClientConfig.sprintLeap();
        if (raw == rawSeen && leap == leapSeen && now == builtAt) {
            return cached;
        }
        if (raw != rawSeen || leap != leapSeen) {
            rawSeen = raw;
            leapSeen = leap;
            List<WheelEntry> out = new ArrayList<>();
            for (WheelEntry e : raw) {
                if (!ReplacedPowers.hidden(e.ref(), leap)) {
                    out.add(e);
                }
            }
            kept = out;
        }
        builtAt = now;
        LocalPlayer p = mc.player;
        List<WheelEntry> gs = p == null ? List.of() : GreatSageProvider.entries(p);
        if (gs.isEmpty()) {
            cached = kept;
        } else {
            List<WheelEntry> all = new ArrayList<>(kept);
            all.addAll(gs);
            cached = all;
        }
        return cached;
    }

    public static void reset() {
        rawSeen = null;
        builtAt = Long.MIN_VALUE;
        cached = List.of();
        kept = List.of();
    }
}
