package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.util.List;
import java.util.Locale;
import net.ofatech.supernaturalbloodlines.SourceTree;
import org.junit.jupiter.api.Test;

class BrotherhoodTextTest {
    @Test
    void brotherhoodWordsExistAndDoNotBorrowOtherGroupsOrNames() {
        var lang = JsonParser.parseString(SourceTree.read("src/main/resources/assets/supernaturalbloodlines/lang/en_us.json")).getAsJsonObject();
        String root = "supernaturalbloodlines.";
        for (String key : List.of("lead_now", "lead_lost", "lead_you", "kol_warning", "code_broken", "code_expelled", "broke_code",
                "code_leader", "err.exiled_still", "err.exiled_still.one", "err.exiled_brings")) {
            assertTrue(lang.has(root + "brotherhood." + key), key);
        }
        for (String key : List.of("since", "since.one", "loading", "not_fit", "off", "arms", "sight", "map.title", "map.own",
                "map.complete", "code.title", "code.motto", "code.cost", "code.off", "strikes", "breaker")) {
            assertTrue(lang.has(root + "hub.brotherhood." + key), key);
        }
        assertTrue(lang.has(root + "hub.group.member.brotherhood.potential"));
        lang.entrySet().stream().filter(e -> e.getKey().startsWith(root + "brotherhood.") || e.getKey().startsWith(root + "hub.brotherhood."))
                .forEach(e -> {
                    String text = (e.getKey() + " " + e.getValue().getAsString()).toLowerCase(Locale.ROOT);
                    assertFalse(text.matches(".*\\b(pack|clan|coven|amara|qetsiyah|silas|alexander|argent|mikael|klaus|elena|jeremy|vampire|werewolf|tribrid|siphoner|witch)\\b.*"), e.getKey());
                });
    }
}
