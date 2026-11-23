package net.ofatech.supernaturalbloodlines.parts.bosses.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.boss.BossBarForms;
import org.junit.jupiter.api.Test;

class FormMarkTest {
    @Test
    void theMarkIsTheFirstLetterInCapitalsAndNothingForNoWord() {
        assertEquals("H", FormMark.of("HUMAN"));
        assertEquals("S", FormMark.of("Shifting"));
        assertEquals("B", FormMark.of("  beast"));
        assertEquals("\u00d1", FormMark.of("\u00f1u"));
        assertEquals("", FormMark.of(""));
        assertEquals("", FormMark.of("   "));
        assertEquals("", FormMark.of(null));
        assertEquals("\ud83d\ude00", FormMark.of("\ud83d\ude00 smile"), "a letter outside the basic plane is kept whole");
    }

    @Test
    void theMarksOfTheFormsOfEveryRowDifferInEnglishSoNoFormIsToldByColourAlone() throws Exception {
        JsonObject lang;
        try (InputStream in = FormMarkTest.class.getResourceAsStream("/assets/supernaturalbloodlines/lang/en_us.json")) {
            lang = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        for (BossBarForms.Set set : new BossBarForms.Set[] {BossBarForms.Set.SHIFTER, BossBarForms.Set.HOST}) {
            Set<String> marks = new HashSet<>();
            for (int i = 0; i < set.chips(); i++) {
                String word = lang.get(set.chipKey(i)).getAsString();
                assertTrue(marks.add(FormMark.of(word)), set + ": the mark of " + word + " is already used by another form of the row");
            }
            assertEquals(3, marks.size());
        }
        assertNotEquals(FormMark.of(lang.get(BossBarForms.Set.SHIFTER.chipKey(1)).getAsString()), FormMark.of(lang.get(BossBarForms.Set.SHIFTER.chipKey(2)).getAsString()));
    }
}
