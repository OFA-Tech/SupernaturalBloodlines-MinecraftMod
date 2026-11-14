package net.ofatech.supernaturalbloodlines.parts.supernatural;

import net.ofatech.supernaturalbloodlines.core.EnabledParts;
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
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillTable;

/**
 * Loads {@code data/supernaturalbloodlines/supernatural/skill/<id>.json} and {@code .../tree/<race>.json} and {@code .../quest/<id>.json} on every data reload. Like the
 * ladders it only exists while the {@code supernatural} part is on, so the files carry no {@code part_enabled} condition. It reads the
 * whole {@code supernatural} folder and keeps the two subfolders it owns (the ladders have their own listener).
 */
public final class SkillReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    public SkillReloadListener() {
        super(GSON, "supernatural");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, JsonElement> skills = new HashMap<>();
        Map<String, JsonElement> trees = new HashMap<>();
        Map<String, JsonElement> quests = new HashMap<>();
        files.forEach((id, json) -> {
            String path = id.getPath();
            boolean skill = path.startsWith("skill/");
            boolean quest = path.startsWith("quest/");
            if (!skill && !quest && !path.startsWith("tree/")) {
                return;
            }
            if (!id.getNamespace().equals(SupernaturalBloodlines.MODID)) {
                SupernaturalBloodlines.LOGGER.warn("Skill file {} ignored: only the {} namespace holds skills", id, SupernaturalBloodlines.MODID);
            } else {
                (skill ? skills : quest ? quests : trees).put(path.substring(path.indexOf('/') + 1), json);
            }
        });
        SkillTable.Parsed parsed = SkillTable.parse(skills, trees, quests, ModList.get()::isLoaded, EnabledParts::isEnabled);
        parsed.rejected().forEach(r -> SupernaturalBloodlines.LOGGER.error("Skill data rejected: {}", r));
        SkillTable.setCurrent(parsed.table());
        SupernaturalBloodlines.LOGGER.info("Loaded {} supernatural skills and {} quests ({} files rejected)", parsed.table().skills().size(), parsed.table().quests().size(), parsed.rejected().size());
    }
}
