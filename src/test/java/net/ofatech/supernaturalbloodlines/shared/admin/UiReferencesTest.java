package net.ofatech.supernaturalbloodlines.shared.admin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * A command row that promises a hub button or an admin action must be wired in the hub's page code: a row whose id no page mentions is a command with
 * no way to run it from the UI. A source scan, because the pages are client classes that cannot be built here.
 */
class UiReferencesTest {
    /** The project folder, found from the working directory upwards (Gradle runs the tests from a folder inside the project). */
    private static Path base() throws Exception {
        Path at = Path.of("").toAbsolutePath();
        while (at != null && !Files.isDirectory(at.resolve("src/main/java"))) {
            at = at.getParent();
        }
        return at.resolve("src/main/java/net/ofatech/supernaturalbloodlines");
    }

    private static String hubSource() throws Exception {
        StringBuilder all = new StringBuilder();
        for (Path dir : List.of(base().resolve("parts/supernatural/client/hub"), base().resolve("shared/hub"))) {
            try (Stream<Path> files = Files.walk(dir)) {
                for (Path p : files.filter(f -> f.toString().endsWith(".java")).collect(Collectors.toList())) {
                    all.append(Files.readString(p));
                }
            }
        }
        return all.toString();
    }

    @Test
    void everyHubButtonAndAdminActionIsReferencedByAPage() throws Exception {
        String source = hubSource();
        List<String> missing = new ArrayList<>();
        for (AdminCommands.Row r : AdminCommands.ALL) {
            if ((r.ui() == AdminCommands.Ui.HUB_BUTTON || r.ui() == AdminCommands.Ui.ADMIN_ACTION) && !source.contains("\"" + r.id() + "\"")) {
                missing.add(r.id() + " (" + r.form() + ")");
            }
        }
        assertTrue(missing.isEmpty(), "rows no hub page runs: " + missing);
    }

    @Test
    void aViewOnlyRowIsMarkedAsAPageNotAButton() {
        assertTrue(AdminCommands.byId("B1").orElseThrow().ui() == AdminCommands.Ui.PAGE);
        assertTrue(AdminCommands.byId("P12").orElseThrow().ui() == AdminCommands.Ui.PAGE);
    }
}

