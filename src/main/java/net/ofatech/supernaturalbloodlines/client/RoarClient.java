package net.ofatech.supernaturalbloodlines.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;

/** Who is roaring right now (the server says so), so {@link EyeColorClient} makes their eyes glow. Client only. */
public final class RoarClient {
    private static final Map<Integer, Long> UNTIL = new HashMap<>();
    private static boolean hooked;

    private RoarClient() {
    }

    public static void show(int playerId, int ticks) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        UNTIL.put(playerId, Minecraft.getInstance().level.getGameTime() + ticks);
        if (!hooked) {
            hooked = true;
            EyeColorClient.addGlowCondition(p -> UNTIL.getOrDefault(p.getId(), 0L) > p.level().getGameTime());
        }
    }
}
