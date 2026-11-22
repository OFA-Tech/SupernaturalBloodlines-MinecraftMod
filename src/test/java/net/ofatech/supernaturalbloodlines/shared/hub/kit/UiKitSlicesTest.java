package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Every sprite the code draws exists in the three kits, and the nine-slice of each is valid for its picture. */
class UiKitSlicesTest {
    private static final String DIR = "/assets/supernaturalbloodlines/textures/gui/sprites/";

    static BufferedImage png(String sprite) throws Exception {
        try (InputStream in = UiKitSlicesTest.class.getResourceAsStream(DIR + sprite + ".png")) {
            assertNotNull(in, sprite + ".png is missing");
            return ImageIO.read(in);
        }
    }

    private static JsonObject mcmeta(String sprite) throws Exception {
        try (InputStream in = UiKitSlicesTest.class.getResourceAsStream(DIR + sprite + ".png.mcmeta")) {
            return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void everyPartExistsInEveryKit() throws Exception {
        List<String> missing = new ArrayList<>();
        for (String kit : KitSprites.KITS) {
            for (KitSprites.Part part : KitSprites.Part.values()) {
                String sprite = KitSprites.of(kit, part.sprite());
                if (UiKitSlicesTest.class.getResource(DIR + sprite + ".png") == null) {
                    missing.add(sprite);
                }
            }
        }
        assertTrue(missing.isEmpty(), "missing sprites: " + missing);
    }

    @Test
    void everyNineSliceFitsItsPicture() throws Exception {
        List<String> bad = new ArrayList<>();
        for (String kit : KitSprites.KITS) {
            for (KitSprites.Part part : KitSprites.Part.values()) {
                String sprite = KitSprites.of(kit, part.sprite());
                JsonObject meta = mcmeta(sprite);
                if (meta == null) {
                    continue;
                }
                JsonObject scaling = meta.getAsJsonObject("gui").getAsJsonObject("scaling");
                BufferedImage img = png(sprite);
                String type = scaling.get("type").getAsString();
                if (type.equals("nine_slice")) {
                    JsonObject b = scaling.getAsJsonObject("border");
                    int l = b.get("left").getAsInt();
                    int t = b.get("top").getAsInt();
                    int r = b.get("right").getAsInt();
                    int bo = b.get("bottom").getAsInt();
                    if (scaling.get("width").getAsInt() != img.getWidth() || scaling.get("height").getAsInt() != img.getHeight()) {
                        bad.add(sprite + " says " + scaling.get("width") + "x" + scaling.get("height") + " but the picture is " + img.getWidth() + "x" + img.getHeight());
                    }
                    if (l + r >= img.getWidth() || t + bo >= img.getHeight()) {
                        bad.add(sprite + " borders " + l + "," + t + "," + r + "," + bo + " leave no middle in " + img.getWidth() + "x" + img.getHeight());
                    }
                } else if (type.equals("tile")) {
                    if (scaling.get("width").getAsInt() != img.getWidth() || scaling.get("height").getAsInt() != img.getHeight()) {
                        bad.add(sprite + " tile size differs from the picture");
                    }
                }
            }
        }
        assertTrue(bad.isEmpty(), "bad slices: " + bad);
    }

    @Test
    void everyIconExistsAtBothSizesAtItsOwnSize() throws Exception {
        List<String> bad = new ArrayList<>();
        for (Icons icon : Icons.values()) {
            for (int size : new int[] {16, 32}) {
                String sprite = KitSprites.icon(size, icon.id());
                BufferedImage img = UiKitSlicesTest.class.getResource(DIR + sprite + ".png") == null ? null : png(sprite);
                if (img == null || img.getWidth() != size || img.getHeight() != size) {
                    bad.add(sprite);
                }
            }
        }
        assertTrue(bad.isEmpty(), "icons missing or the wrong size: " + bad);
    }

    @Test
    void everySkillWeShipHasItsOwnIconAtBothSizes() throws Exception {
        Path dir = Path.of(UiKitSlicesTest.class.getResource("/data/supernaturalbloodlines/supernatural/skill/s-vam-1.json").toURI()).getParent();
        List<String> ids;
        try (Stream<Path> files = Files.list(dir)) {
            ids = files.map(p -> p.getFileName().toString().replace(".json", "")).toList();
        }
        assertTrue(ids.size() >= 30, "the skill folder was not found");
        List<String> bad = new ArrayList<>();
        for (String id : ids) {
            for (int size : new int[] {16, 32}) {
                if (UiKitSlicesTest.class.getResource(DIR + KitSprites.icon(size, id) + ".png") == null) {
                    bad.add(id + " at " + size);
                }
            }
        }
        assertTrue(bad.isEmpty(), "skills without their own icon: " + bad);
    }

    @Test
    void anIconIsOnlyDrawnAtAWholeMultipleOrHalfOfItsOwnSize() {
        assertEquals(new KitSprites.IconPlan(16, 8), KitSprites.iconPlan(8));
        assertEquals(new KitSprites.IconPlan(16, 8), KitSprites.iconPlan(11));
        assertEquals(new KitSprites.IconPlan(16, 16), KitSprites.iconPlan(12));
        assertEquals(new KitSprites.IconPlan(16, 16), KitSprites.iconPlan(31));
        assertEquals(new KitSprites.IconPlan(32, 32), KitSprites.iconPlan(32));
        assertEquals(new KitSprites.IconPlan(32, 64), KitSprites.iconPlan(64));
        for (int box = 1; box <= 200; box++) {
            KitSprites.IconPlan p = KitSprites.iconPlan(box);
            assertTrue(p.draw() == p.sprite() / 2 || p.draw() == p.sprite() || p.draw() == p.sprite() * 2, "box " + box + " draws " + p);
        }
    }
}
