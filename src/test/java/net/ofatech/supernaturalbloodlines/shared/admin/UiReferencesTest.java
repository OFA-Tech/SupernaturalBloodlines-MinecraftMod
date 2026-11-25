package net.ofatech.supernaturalbloodlines.shared.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.ofatech.supernaturalbloodlines.shared.hub.HubPage;
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

    /** Page code with comments and the dev demo removed: a row mentioned only there is not a control. */
    private static String hubSource() throws Exception {
        StringBuilder all = new StringBuilder();
        for (Path dir : List.of(base().resolve("parts/supernatural/client/hub"), base().resolve("shared/hub"))) {
            try (Stream<Path> files = Files.walk(dir)) {
                for (Path p : files.filter(f -> f.toString().endsWith(".java") && !f.getFileName().toString().startsWith("DevDemo")).collect(Collectors.toList())) {
                    all.append(Files.readString(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "")).append('\n');
                }
            }
        }
        return all.toString();
    }

    /** The places a page really runs a row: a command call, or the row id handed to an action, answer or act record. */
    private static boolean runsRow(String source, String id) {
        return Pattern.compile("(runCommand\\(\\s*|new (?:Action|Answer|Act|Button)\\(\"[a-z_:]+\",\\s*(?:Kind\\.\\w+,\\s*)?|[?:]\\s*)\"" + id + "\"").matcher(source).find();
    }

    /** The only rows that are a view, and the page each one must name; a new view-only row has to be added here on purpose. */
    private static final Map<String, String> PAGE_ROWS = Map.of("B1", "BLOODLINE", "P12", "PACK");

    @Test
    void everyHubButtonAndAdminActionIsReferencedByAPage() throws Exception {
        String source = hubSource();
        List<String> missing = new ArrayList<>();
        for (AdminCommands.Row r : AdminCommands.ALL) {
            if ((r.ui() == AdminCommands.Ui.HUB_BUTTON || r.ui() == AdminCommands.Ui.ADMIN_ACTION) && !runsRow(source, r.id())) {
                missing.add(r.id() + " (" + r.form() + ")");
            }
        }
        assertTrue(missing.isEmpty(), "rows no hub page runs: " + missing);
    }

    @Test
    void theReferenceCheckIgnoresCommentsTheDemoAndAMereMention() {
        assertTrue(runsRow("HubClient.runCommand(\"B3\", List.of())", "B3"));
        assertTrue(runsRow("new Action(\"kick\", Kind.COMMAND, \"P4\", true)", "P4"));
        assertTrue(runsRow("new Answer(\"accept\", \"P2\", true)", "P2"));
        assertTrue(runsRow("buttons.add(new Button(\"clan:charter_forge\", \"C16\", \"charter_forge\"", "C16"), "a plan button runs its row");
        assertTrue(!runsRow("String label = \"B3\";", "B3"), "a bare string is not a call site");
        assertTrue(!runsRow("", "B3"));
    }

    @Test
    void everyViewOnlyRowIsOnTheClosedListAndItsPageExistsWithAMarker() throws Exception {
        Map<String, String> found = new TreeMap<>();
        for (AdminCommands.Row r : AdminCommands.ALL) {
            if (r.ui() == AdminCommands.Ui.PAGE) {
                found.put(r.id(), r.page());
            }
        }
        assertEquals(new TreeMap<>(PAGE_ROWS), found, "a view-only row that is not on the closed list, or on it with another page");
        String pages = Files.readString(base().resolve("parts/supernatural/client/hub/HubPages.java"));
        for (var e : PAGE_ROWS.entrySet()) {
            HubPage.valueOf(e.getValue());
            assertTrue(pages.contains("HubPage." + e.getValue() + ", new "), e.getKey() + ": no page is registered for " + e.getValue());
        }
    }
    @Test
    void everySymbolButtonIsAStepperNamedByWhatItChanges() throws Exception {
        List<String> bad = new ArrayList<>();
        for (String line : hubSource().split("\\R")) {
            boolean symbol = line.contains("Component.literal(\"+\")") || line.contains("Component.literal(\"-\")");
            if (symbol && (line.contains("button(") || line.contains("described(")) && !line.contains("PageWidgets.stepper(")) {
                bad.add(line.trim());
            }
        }
        assertTrue(bad.isEmpty(), "a bare + or - button that is not a stepper (it would be narrated as a symbol): " + bad);
        assertTrue(hubSource().contains("PageWidgets.stepper("), "the Admin steppers are gone");
    }

    @Test
    void aViewOnlyRowIsMarkedAsAPageNotAButton() {
        assertTrue(AdminCommands.byId("B1").orElseThrow().ui() == AdminCommands.Ui.PAGE);
        assertTrue(AdminCommands.byId("P12").orElseThrow().ui() == AdminCommands.Ui.PAGE);
    }

    @Test
    void turningTheProtectionOffIsAlwaysAskedFirstWhereverItIsOffered() {
        // the Profile and Settings both take their action from ProfileActions.protectAction, so one check covers both pages
        var off = net.ofatech.supernaturalbloodlines.shared.hub.profile.ProfileActions.protectAction(true);
        var on = net.ofatech.supernaturalbloodlines.shared.hub.profile.ProfileActions.protectAction(false);
        assertEquals("P17", off.rowId());
        assertTrue(off.confirm(), "turning the protection off runs with no question");
        assertEquals("P16", on.rowId());
        assertTrue(!on.confirm(), "turning it on asks, which is only noise");
        var self = new net.ofatech.supernaturalbloodlines.shared.hub.profile.ProfileActions.Subject(java.util.UUID.randomUUID(), "Me", true, net.ofatech.supernaturalbloodlines.shared.hub.profile.ProfileActions.Role.NONE, false, false);
        var profile = net.ofatech.supernaturalbloodlines.shared.hub.profile.ProfileActions.of(new net.ofatech.supernaturalbloodlines.shared.hub.profile.ProfileActions.Viewer(false, false, false, false, false, true, true, true), self, false);
        assertTrue(profile.contains(off), "the Profile does not offer the confirmed action while the path is open and protected");
        var closed = net.ofatech.supernaturalbloodlines.shared.hub.profile.ProfileActions.of(new net.ofatech.supernaturalbloodlines.shared.hub.profile.ProfileActions.Viewer(true, true, false, true, false, true, false, true), self, false);
        assertTrue(closed.stream().noneMatch(a -> a.rowId().equals("P17") || a.rowId().equals("P16")), "an Alpha (a closed path) is offered the protection switch");
    }
}
