package net.ofatech.supernaturalbloodlines.parts.species;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.ofatech.supernaturalbloodlines.SourceTree;
import org.junit.jupiter.api.Test;

/**
 * Plan 20d Task 0: Kol's warning to a brotherhood is one call inside {@code BossDirector.spawnKol}, so every way Kol is made must be one
 * that is meant (the director's roll, the debug command, a test). A new caller fails here by file name and is given a reason or removed.
 */
class KolSpawnSitesTest {
    private static final String DIRECTOR = "src/main/java/net/ofatech/supernaturalbloodlines/parts/bosses/director/BossDirector.java";
    private static final Pattern QUALIFIED = Pattern.compile("BossDirector\\s*\\.\\s*spawnKol\\s*\\(");
    private static final Pattern BARE = Pattern.compile("(?<![.\\w])spawnKol\\s*\\(");

    /** File to (reason, calls). The director's two are the declaration and the call chosen by the director. */
    private static final Map<String, Integer> ALLOWED = Map.of(
            DIRECTOR, 2, // the declaration, and the call chosen by the director
            "src/main/java/net/ofatech/supernaturalbloodlines/parts/bosses/command/BossCommand.java", 1, // debug
            "src/main/java/net/ofatech/supernaturalbloodlines/parts/bosses/gametest/BossTestRig.java", 1); // test

    private static int count(Pattern p, String text) {
        Matcher m = p.matcher(text);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    @Test
    void everyCallerOfSpawnKolIsOnTheList() {
        Map<String, Integer> found = new TreeMap<>();
        List<String> files = new ArrayList<>(SourceTree.javaFiles("main"));
        files.addAll(SourceTree.javaFiles("test"));
        for (String f : files) {
            String text = SourceTree.read(f);
            int n = f.equals(DIRECTOR) ? count(BARE, text) : count(QUALIFIED, text);
            if (n > 0) {
                found.put(f, n);
            }
        }
        assertEquals(new TreeMap<>(ALLOWED), found, "a new way to make Kol: give it a reason here, or remove it");
    }
}
