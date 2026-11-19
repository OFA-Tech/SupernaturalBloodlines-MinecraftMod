package net.ofatech.supernaturalbloodlines.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Every item this mod registers (whatever parts and linked mods are on in this run) has a {@code models/item} file and an English name.
 * The other direction, a model with no item, is {@code ItemModelCoverageTest}: it cannot run here because a part that is off registers fewer items.
 */
@GameTestHolder(SupernaturalBloodlines.MODID)
@PrefixGameTestTemplate(false)
public final class ItemModelCoverageGameTests {
    private ItemModelCoverageGameTests() {}

    @GameTest(template = "empty")
    public static void everyRegisteredItemHasAModelAndAName(GameTestHelper helper) throws Exception {
        JsonObject lang;
        try (var in = new InputStreamReader(ItemModelCoverageGameTests.class.getResourceAsStream("/assets/" + SupernaturalBloodlines.MODID + "/lang/en_us.json"),
                StandardCharsets.UTF_8)) {
            lang = JsonParser.parseReader(in).getAsJsonObject();
        }
        List<String> noModel = new ArrayList<>();
        List<String> noName = new ArrayList<>();
        int seen = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            var key = BuiltInRegistries.ITEM.getKey(item);
            if (!key.getNamespace().equals(SupernaturalBloodlines.MODID)) {
                continue;
            }
            seen++;
            if (ItemModelCoverageGameTests.class.getResource("/assets/" + SupernaturalBloodlines.MODID + "/models/item/" + key.getPath() + ".json") == null) {
                noModel.add(key.getPath());
            }
            if (!lang.has(item.getDescriptionId())) {
                noName.add(item.getDescriptionId());
            }
        }
        helper.assertTrue(seen > 0, "no item of ours is registered, so the check proved nothing");
        helper.assertTrue(noModel.isEmpty(), "registered items with no models/item file: " + noModel);
        helper.assertTrue(noName.isEmpty(), "registered items with no lang key: " + noName);
        helper.succeed();
    }
}
