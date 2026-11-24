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
    private static final String STEVE = "123e4567-e89b-42d3-a456-426614174000";
    private static final String ALEX = "0f8fad5b-d9cb-469f-a165-70867728950e";

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
    void aNumberArgumentMatchesWhatBrigadierReads() {
        AdminCommands.Arg n = new AdminCommands.Arg("n", false);
        for (String ok : new String[] {"5", "-5", "0", "-0"}) {
            assertTrue(n.accepts(ok), ok);
        }
        for (String bad : new String[] {"+5", " 5", "5 ", "5.0", "0x5", "", "-", "99999999999"}) {
            assertFalse(n.accepts(bad), "[" + bad + "]");
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
    void rowsPerRootAreThirteenTwentyFiveNineteenAndFive() {
        assertEquals(13, AdminCommands.ALL.stream().filter(r -> r.root().equals("bloodline")).count());
        assertEquals(25, AdminCommands.ALL.stream().filter(r -> r.root().equals("pack")).count());
        assertEquals(19, AdminCommands.ALL.stream().filter(r -> r.root().equals("supernaturalbloodlines")).count());
        assertEquals(5, AdminCommands.ALL.stream().filter(r -> r.root().equals("ward")).count());
        assertEquals(62, AdminCommands.ALL.size());
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
        assertEquals(List.of("pack protect on"), row("P16").shapes());
        assertEquals(List.of("pack protect off confirm"), row("P17").shapes());
    }

    @Test
    void theCommandIsRebuiltFromTheRowAndItsArguments() {
        assertEquals(Optional.of("pack kick " + STEVE), AdminCommands.command(row("P4"), List.of(STEVE)));
        assertEquals(Optional.of("pack emissary"), AdminCommands.command(row("P13"), List.of()));
        assertEquals(Optional.of("pack emissary " + ALEX), AdminCommands.command(row("P13"), List.of(ALEX)));
        assertEquals(Optional.of("bloodline compel ab12cd34 follow"), AdminCommands.command(row("B6"), List.of("ab12cd34")));
        assertEquals(Optional.of("supernaturalbloodlines debug supernatural " + STEVE + " rung vampire 3"),
                AdminCommands.command(row("D8"), List.of(STEVE, "vampire", "3")));
        assertEquals(Optional.of("pack accept"), AdminCommands.command(row("P2"), List.of()));
    }

    @Test
    void anArgumentThatCouldSmuggleAnotherCommandIsRefused() {
        assertTrue(AdminCommands.command(row("P4"), List.of(STEVE + " ; op Steve")).isEmpty(), "a space");
        assertTrue(AdminCommands.command(row("P4"), List.of("@a")).isEmpty(), "a selector");
        assertTrue(AdminCommands.command(row("P4"), List.of("Steve")).isEmpty(), "a name: people are named by UUID");
        assertTrue(AdminCommands.command(row("P4"), List.of("")).isEmpty(), "empty");
        assertTrue(AdminCommands.command(row("B4"), List.of("not-a-token")).isEmpty(), "a token is 8 hex digits");
        assertTrue(AdminCommands.command(row("D7"), List.of(STEVE, "ten")).isEmpty(), "n is a number");
        assertTrue(AdminCommands.command(row("B7"), List.of(STEVE, "maybe")).isEmpty(), "value is true or false");
    }

    @Test
    void theNumberOfArgumentsMustFit() {
        assertTrue(AdminCommands.command(row("P4"), List.of()).isEmpty(), "missing");
        assertTrue(AdminCommands.command(row("P4"), List.of(STEVE, ALEX)).isEmpty(), "extra");
        assertTrue(AdminCommands.command(row("P3"), List.of("a")).isEmpty(), "a form without arguments takes none");
    }

    @Test
    void anOnlinePersonIsSwappedForTheNameTheCommandNeeds() {
        // EntityArgument.player refuses a UUID, so the server looks the online player up
        assertEquals(Optional.of("pack invite Steve"), AdminCommands.command(row("P1"), List.of(STEVE), uuid -> "Steve"));
        assertEquals(Optional.of("pack invite " + STEVE), AdminCommands.command(row("P1"), List.of(STEVE), uuid -> uuid), "offline: the UUID stays");
        assertEquals(Optional.of("supernaturalbloodlines debug supernatural Steve sp 5"), AdminCommands.command(row("D7"), List.of(STEVE, "5"), uuid -> "Steve"));
        // only people are looked up, and what the lookup returns is checked again
        assertEquals(Optional.of("bloodline compel ab12cd34 follow"), AdminCommands.command(row("B6"), List.of("ab12cd34"), uuid -> "never called"));
        assertTrue(AdminCommands.command(row("P1"), List.of(STEVE), uuid -> "Steve extra").isEmpty());
        assertTrue(AdminCommands.command(row("P1"), List.of(STEVE), uuid -> "@a").isEmpty());
        assertTrue(AdminCommands.command(row("P1"), List.of(STEVE), uuid -> null).isEmpty());
        assertTrue(AdminCommands.command(row("P1"), List.of("Steve"), uuid -> "Steve").isEmpty(), "the wire still carries a UUID, never a name");
    }
}
