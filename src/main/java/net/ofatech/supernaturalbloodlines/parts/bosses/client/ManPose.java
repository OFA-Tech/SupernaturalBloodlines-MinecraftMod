package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.ManPoseTable;
import org.slf4j.Logger;

/**
 * The man's poses for the shift (UI overhaul phase 7), one table per boss, read from {@code assets/supernaturalbloodlines/morph/man_<boss>.json} when the
 * resources load and parsed once. A file that is missing or cannot be read is logged and the boss gets a neutral table (he stands as he always did), so a bad
 * pack never breaks the shift. Client only; the format and the sampling are {@link ManPoseTable}'s, which has no Minecraft in it.
 */
public final class ManPose {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** The file name of each boss that shifts: the three men, and the Beast's Host. */
    private static final Map<BossId, String> FILES = Map.of(
            BossId.KLAUS, "klaus",
            BossId.DEUCALION, "deucalion",
            BossId.PETER_HALE, "peter",
            BossId.BEAST_OF_GEVAUDAN, "beast_host");

    private static volatile Map<BossId, ManPoseTable> tables = Map.of();

    private ManPose() {
    }

    /** The table of this boss; neutral for one that has none. */
    public static ManPoseTable table(BossId boss) {
        ManPoseTable t = tables.get(boss);
        return t != null ? t : ManPoseTable.neutral();
    }

    public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Map<BossId, ManPoseTable>>() {
            @Override
            protected Map<BossId, ManPoseTable> prepare(ResourceManager manager, ProfilerFiller profiler) {
                Map<BossId, ManPoseTable> read = new EnumMap<>(BossId.class);
                for (Map.Entry<BossId, String> e : FILES.entrySet()) {
                    ResourceLocation file = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "morph/man_" + e.getValue() + ".json");
                    try (var in = manager.getResourceOrThrow(file).open()) {
                        read.put(e.getKey(), ManPoseTable.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
                    } catch (IOException | RuntimeException ex) {
                        LOGGER.error("The man's pose table {} cannot be used, {} stands as he is: {}", file, e.getKey(), ex.toString());
                    }
                }
                return read;
            }

            @Override
            protected void apply(Map<BossId, ManPoseTable> read, ResourceManager manager, ProfilerFiller profiler) {
                tables = Map.copyOf(read);
            }
        });
    }
}
