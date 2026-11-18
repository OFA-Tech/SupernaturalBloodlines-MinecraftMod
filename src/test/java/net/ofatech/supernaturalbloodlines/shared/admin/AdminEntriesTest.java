package net.ofatech.supernaturalbloodlines.shared.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.ofatech.supernaturalbloodlines.core.Part;
import org.junit.jupiter.api.Test;

class AdminEntriesTest {
    @Test
    void sixtyFiveKeysWithUniqueIds() {
        assertEquals(65, AdminEntries.ALL.size());
        Set<String> seen = new HashSet<>();
        for (SettingEntry e : AdminEntries.ALL) {
            assertTrue(seen.add(e.id()), "duplicate id " + e.id());
            assertEquals(e.file() + ":" + e.dottedPath(), e.id());
            assertTrue(AdminEntries.SECTIONS.contains(e.section()), "unknown section " + e.section() + " of " + e.id());
        }
    }

    @Test
    void everyPartSwitchNeedsARestartAndNothingElseDoes() {
        Set<String> restart = AdminEntries.ALL.stream().filter(e -> e.restart() == SettingEntry.Restart.SERVER_RESTART).map(SettingEntry::id)
                .collect(Collectors.toSet());
        Set<String> switches = new HashSet<>();
        for (Part p : Part.values()) {
            switches.add(AdminEntries.CORE_FILE + ":parts." + p.configKey());
        }
        assertEquals(switches, restart);
    }

    @Test
    void theClientCopiesAreTheThreeTensuraKeys() {
        Set<String> copies = AdminEntries.ALL.stream().filter(SettingEntry::clientCopy).map(SettingEntry::dottedPath).collect(Collectors.toSet());
        assertEquals(Set.of("layers", "undead_treatment_enabled", "apex_evolutions_enabled"), copies);
        assertTrue(AdminEntries.ALL.stream().filter(SettingEntry::clientCopy).allMatch(e -> e.file().equals(AdminEntries.SPECIES_TENSURA_FILE)));
    }

    @Test
    void onlyTheReservedMixinSwitchIsHidden() {
        Set<String> hidden = AdminEntries.ALL.stream().filter(SettingEntry::isHidden).map(SettingEntry::id).collect(Collectors.toSet());
        assertEquals(Set.of(AdminEntries.HUD_FILE + ":mixins.hide_in_controls_mixin"), hidden);
    }

    @Test
    void sectionsAreGroupedInPageOrder() {
        List<String> order = AdminEntries.ALL.stream().map(SettingEntry::section).distinct().toList();
        assertEquals(AdminEntries.SECTIONS, order);
    }

    @Test
    void thereAreNoGameRulesYet() {
        assertTrue(AdminEntries.RULES.isEmpty());
    }

    @Test
    void gameRuleEntryResolvesById() {
        SettingEntry rule = SettingEntry.rule("someRule", SettingKind.GAMERULE_BOOL);
        assertEquals("gamerule:someRule", rule.id());
        assertEquals(rule, AdminEntries.find(AdminEntries.ALL, List.of(rule), "gamerule:someRule").orElseThrow());
        assertTrue(AdminEntries.find("gamerule:someRule").isEmpty(), "a rule that is not registered is not found");
    }
}
