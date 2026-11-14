package net.ofatech.supernaturalbloodlines.parts.supernatural;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;

/** Loads {@code data/<ns>/supernatural/ladder/<race>.json} on every data reload. */
public final class LadderReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    public LadderReloadListener() {
        super(GSON, "supernatural/ladder");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, JsonElement> byRace = new HashMap<>();
        files.forEach((id, json) -> {
            if (!id.getNamespace().equals(SupernaturalBloodlines.MODID)) {
                SupernaturalBloodlines.LOGGER.warn("Ladder {} ignored: only the {} namespace holds ladders", id, SupernaturalBloodlines.MODID);
            } else if (ICondition.conditionsMatched(JsonOps.INSTANCE, json)) {
                byRace.put(id.getPath(), json);
            }
        });
        LadderTable.Parsed parsed = LadderTable.parse(byRace, ModList.get()::isLoaded);
        parsed.rejected().forEach(r -> SupernaturalBloodlines.LOGGER.error("Ladder rejected: {}", r));
        LadderTable.setCurrent(parsed.table());
        SupernaturalBloodlines.LOGGER.info("Loaded {} supernatural ladders ({} rejected)", byRace.size() - parsed.rejected().size(), parsed.rejected().size());
    }
}
