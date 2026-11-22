package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Rules about how the page code is written that a unit test cannot reach through the screen: read from the sources, like {@code AdminCoverageTest}. */
class SourceRulesTest {
    private static Path root() {
        Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path at = p; at != null; at = at.getParent()) {
            if (Files.isDirectory(at.resolve("src/main/java"))) {
                return at.resolve("src/main/java");
            }
        }
        throw new IllegalStateException("no src/main/java above " + p);
    }

    @Test
    void anEmptyStateEndsItsRegionAtItsOwnBottomSoItsLastButtonIsNeverCut() throws IOException {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root())) {
            for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (f.getFileName().toString().equals("EmptyState.java")) {
                    continue;
                }
                for (String line : Files.readAllLines(f)) {
                    String s = line.strip();
                    if (s.startsWith("EmptyState.render(")) {
                        bad.add(f.getFileName() + ": " + s);
                    }
                }
            }
        }
        assertTrue(bad.isEmpty(), "EmptyState.render returns the y under its last button: use it (f.end(...) or a variable), do not drop it: " + bad);
    }

    @Test
    void theBloodMoonStageTitlesGoThroughTheThemesTitleFont() throws IOException {
        String bookKit = Files.readString(root().resolve("net/ofatech/supernaturalbloodlines/parts/supernatural/client/hub/page/BookKit.java"));
        int head = bookKit.indexOf("static void moonHead(");
        assertTrue(head > 0 && bookKit.indexOf("PageKit.themedTitle(", head) > head, "moonHead (every Blood Moon page title) must try the theme's title font first");
    }
}