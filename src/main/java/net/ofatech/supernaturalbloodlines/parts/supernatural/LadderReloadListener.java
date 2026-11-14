package net.ofatech.supernaturalbloodlines.parts.supernatural;

import net.ofatech.supernaturalbloodlines.parts.supernatural.net.LadderTablePayload;
import net.ofatech.supernaturalbloodlines.core.supernatural.PayloadCaps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;

/**
 * Loads {@code data/<ns>/supernatural/ladder/<race>.json} on every data reload. This listener only exists while the
 * {@code supernatural} part is on, so the files carry no {@code part_enabled} condition. Tasks are dropped when their mod is
 * missing or the part that fires their signal is off.
 */
public final class LadderReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    public LadderReloadListener() {
        super(GSON, "supernatural/ladder");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, JsonElement> byRace = new HashMap<>();
        PayloadCaps caps = new PayloadCaps();
        new java.util.TreeMap<>(files).forEach((id, json) -> {
            if (id.getNamespace().equals(SupernaturalBloodlines.MODID)) {
                // the same limits as LadderTablePayload: a file over them would disconnect every player when the table is sent
                String tooBig = caps.check(id.getPath(), json, LadderTablePayload.NAME_BYTES, LadderTablePayload.BODY_BYTES);
                if (tooBig != null) {
                    SupernaturalBloodlines.LOGGER.error("Ladder {} skipped: {}", id, tooBig);
                } else {
                    byRace.put(id.getPath(), json);
                }
            } else {
                SupernaturalBloodlines.LOGGER.warn("Ladder {} ignored: only the {} namespace holds ladders", id, SupernaturalBloodlines.MODID);
            }
        });
        LadderTable.Parsed parsed = LadderTable.parse(byRace, ModList.get()::isLoaded, EnabledParts::isEnabled);
        parsed.rejected().forEach(r -> SupernaturalBloodlines.LOGGER.error("Ladder rejected: {}", r));
        parsed.warnings().forEach(w -> SupernaturalBloodlines.LOGGER.warn("Ladder: {}", w));
        LadderTable.setCurrent(parsed.table());
        SupernaturalBloodlines.LOGGER.info("Loaded {} supernatural ladders ({} rejected)", byRace.size() - parsed.rejected().size(), parsed.rejected().size());
    }
}
