package net.ofatech.supernaturalbloodlines.core.bloodline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.SourceTree;
import org.junit.jupiter.api.Test;

/** Plan 20d Task 0: events are only ever named, so a new one can be appended; this pins that nothing reads their position. */
class BloodlineEventAppendTest {
    @Test
    void noFileThatNamesAnEventReadsAnOrdinal() {
        List<String> offenders = new ArrayList<>();
        List<String> files = new ArrayList<>(SourceTree.javaFiles("main"));
        files.addAll(SourceTree.javaFiles("test"));
        for (String f : files) {
            if (f.endsWith("BloodlineEventAppendTest.java")) {
                continue;
            }
            String text = SourceTree.read(f);
            if (text.contains("BloodlineEvent") && (text.contains(".ordinal()") || text.contains("BloodlineEvent.values()[") || text.contains("EnumCodec") || text.contains("Enum.valueOf(BloodlineEvent"))) {
                offenders.add(f);
            }
        }
        assertEquals(List.of(), offenders, "an event is read by its position or by a codec: it can no longer be appended freely");
    }

    @Test
    void theDebugCommandListsEventsByName() {
        String debug = SourceTree.read("src/main/java/net/ofatech/supernaturalbloodlines/core/command/DebugCommand.java");
        assertTrue(debug.contains("Arrays.stream(BloodlineEvent.values()).map(Enum::name)"));
    }

    @Test
    void theRiteCodecWritesTheName() {
        String factory = SourceTree.read("src/main/java/net/ofatech/supernaturalbloodlines/parts/vampirismenchanted/rite/FactionRiteFactory.java");
        assertTrue(factory.contains("e.name().toLowerCase(Locale.ROOT)") && factory.contains("Enum.valueOf(type, name.toUpperCase(Locale.ROOT))"));
    }
}
