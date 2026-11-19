package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.BloodMoonLayout;
import net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.theme.GrimoireLayout;
import net.ofatech.supernaturalbloodlines.parts.supernaturaltensura.client.TensuraHubLayout;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminEntries;
import net.ofatech.supernaturalbloodlines.shared.hub.HubLayout;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import net.ofatech.supernaturalbloodlines.shared.hub.kit.Flow;
import org.junit.jupiter.api.Test;

/**
 * Every row of buttons the new pages draw (a request's answers, the do-now buttons, the admin sections and actions, the settings choices) laid
 * out with {@link Flow} in the real content regions of the three themes at GUI scales 4, 3 and 2 (480 x 270, 640 x 360, 960 x 540): each button
 * inside its region, none on another, and a button never wider than the region. A label is measured at 6 GUI px a character, the widest the font draws.
 */
class PageRowsLayoutTest {
    private static final int[][] SCREENS = {{480, 270}, {640, 360}, {960, 540}};
    private static final int CHAR = 6;

    private static JsonObject lang() throws Exception {
        try (var in = new InputStreamReader(PageRowsLayoutTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json"), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(in).getAsJsonObject();
        }
    }

    private static List<List<String>> rows(JsonObject lang) {
        String h = "supernaturalbloodlines.hub.";
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of(h + "home.answer.accept", h + "home.answer.decline", h + "home.answer.dismiss"));
        rows.add(List.of(h + "home.answer.discount", h + "home.answer.follow"));
        rows.add(List.of(h + "home.call", h + "home.roar", h + "home.open_quests"));
        List<String> sections = new ArrayList<>();
        AdminEntries.SECTIONS.forEach(s -> sections.add(h + "admin.section." + s));
        sections.add(h + "admin.section.debug");
        sections.add(h + "admin.section.parts");
        rows.add(sections);
        List<String> acts = new ArrayList<>();
        for (String a : List.of("get", "show", "backfill", "reset", "tree", "set", "event", "sp", "rung", "gene_on", "gene_off")) {
            acts.add(h + "admin.act." + a);
        }
        rows.add(acts);
        rows.add(List.of(h + "admin.act.eclipse_start", h + "admin.act.eclipse_stop", h + "admin.act.npc_pack"));
        rows.add(List.of(h + "theme.grimoire", h + "theme.blood_moon", h + "theme.tensura"));
        rows.add(List.of(h + "settings.controls", h + "home.profile"));
        return rows;
    }

    @Test
    void everyButtonRowFitsEveryRegionAtEveryScale() throws Exception {
        JsonObject lang = lang();
        for (int[] s : SCREENS) {
            List<HubLayout> layouts = List.of(GrimoireLayout.of(s[0], s[1], 10, 3), BloodMoonLayout.of(s[0], s[1], 10, 3), TensuraHubLayout.of(s[0], s[1], 10, 3));
            for (HubLayout l : layouts) {
                for (String region : List.of(HubLayout.MAIN, HubLayout.SIDE)) {
                    Rect r = l.region(region);
                    for (List<String> row : rows(lang)) {
                        List<Integer> widths = new ArrayList<>();
                        for (String key : row) {
                            assertTrue(lang.has(key), "no words for " + key);
                            widths.add(lang.get(key).getAsString().length() * CHAR + 12);
                        }
                        Flow.Result flow = Flow.wrap(new Rect(r.x(), r.y(), r.w(), 0), widths, PageWidgets.H, PageWidgets.GAP);
                        String at = s[0] + "x" + s[1] + " " + region + " " + r.w() + " wide, row " + row.get(0);
                        for (int i = 0; i < flow.rects().size(); i++) {
                            Rect a = flow.rects().get(i);
                            assertTrue(a.x() >= r.x() && a.right() <= r.right(), at + ": button " + i + " leaves the region");
                            for (int j = i + 1; j < flow.rects().size(); j++) {
                                assertFalse(a.overlaps(flow.rects().get(j)), at + ": buttons " + i + " and " + j + " overlap");
                            }
                        }
                    }
                }
            }
        }
    }
}

