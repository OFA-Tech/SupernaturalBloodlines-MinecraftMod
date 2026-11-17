package net.ofatech.supernaturalbloodlines.shared.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AdminCommandsTest {
    private static AdminCommands.Row row(String id) {
        return AdminCommands.byId(id).orElseThrow();
    }

    @Test
    void everyOpFormIsAnAdminAction() {
        for (AdminCommands.Row r : AdminCommands.ALL) {
            if (r.level() >= 2) {
                assertEquals(AdminCommands.Ui.ADMIN_ACTION, r.ui(), r.id() + " needs permission " + r.level());
            } else {
                assertFalse(r.ui() == AdminCommands.Ui.ADMIN_ACTION, r.id() + " is for everyone");
            }
        }
    }

    @Test
    void aChatOnlyRowSaysWhy() {
        for (AdminCommands.Row r : AdminCommands.ALL) {
            if (r.ui() == AdminCommands.Ui.CHAT_ONLY) {
                assertNotNull(r.reason(), r.id());
                assertFalse(r.reason().isBlank(), r.id());
            }
        }
    }

    @Test
    void rowsPerRootAreSevenFifteenAndTen() {
        assertEquals(7, AdminCommands.ALL.stream().filter(r -> r.root().equals("bloodline")).count());
        assertEquals(15, AdminCommands.ALL.stream().filter(r -> r.root().equals("pack")).count());
        assertEquals(10, AdminCommands.ALL.stream().filter(r -> r.root().equals("supernaturalbloodlines")).count());
        assertEquals(32, AdminCommands.ALL.size());
    }

    @Test
    void idsAndFormsAreUnique() {
        Set<String> ids = new HashSet<>();
        Set<String> shapes = new HashSet<>();
        for (AdminCommands.Row r : AdminCommands.ALL) {
            assertTrue(ids.add(r.id()), "duplicate id " + r.id());
            for (String s : r.shapes()) {
                assertTrue(shapes.add(s), "two rows have the shape '" + s + "'");
            }
        }
    }

    @Test
    void anOptionalArgumentGivesTwoShapes() {
        assertEquals(List.of("pack emissary <>", "pack emissary"), row("P13").shapes());
        assertEquals(List.of("pack challenge <>"), row("P8").shapes());
    }

    @Test
    void theCommandIsRebuiltFromTheRowAndItsArguments() {
        assertEquals(Optional.of("pack kick Steve"), AdminCommands.command(row("P4"), List.of("Steve")));
        assertEquals(Optional.of("pack emissary"), AdminCommands.command(row("P13"), List.of()));
        assertEquals(Optional.of("pack emissary Alex"), AdminCommands.command(row("P13"), List.of("Alex")));
        assertEquals(Optional.of("bloodline compel ab12cd34 follow"), AdminCommands.command(row("B6"), List.of("ab12cd34")));
        assertEquals(Optional.of("supernaturalbloodlines debug supernatural Steve rung vampire 3"),
                AdminCommands.command(row("D8"), List.of("Steve", "vampire", "3")));
        assertEquals(Optional.of("pack accept"), AdminCommands.command(row("P2"), List.of()));
    }

    @Test
    void anArgumentThatCouldSmuggleAnotherCommandIsRefused() {
        assertTrue(AdminCommands.command(row("P4"), List.of("Steve ; op Steve")).isEmpty(), "a space");
        assertTrue(AdminCommands.command(row("P4"), List.of("@a")).isEmpty(), "a selector");
        assertTrue(AdminCommands.command(row("P4"), List.of("")).isEmpty(), "empty");
        assertTrue(AdminCommands.command(row("B4"), List.of("not-a-token")).isEmpty(), "a token is 8 hex digits");
        assertTrue(AdminCommands.command(row("D7"), List.of("Steve", "ten")).isEmpty(), "n is a number");
        assertTrue(AdminCommands.command(row("B7"), List.of("Steve", "maybe")).isEmpty(), "value is true or false");
    }

    @Test
    void theNumberOfArgumentsMustFit() {
        assertTrue(AdminCommands.command(row("P4"), List.of()).isEmpty(), "missing");
        assertTrue(AdminCommands.command(row("P4"), List.of("a", "b")).isEmpty(), "extra");
        assertTrue(AdminCommands.command(row("P3"), List.of("a")).isEmpty(), "a form without arguments takes none");
    }
}
