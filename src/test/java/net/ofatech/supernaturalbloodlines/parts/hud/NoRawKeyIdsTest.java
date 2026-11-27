package net.ofatech.supernaturalbloodlines.parts.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.ofatech.supernaturalbloodlines.SourceTree;
import net.ofatech.supernaturalbloodlines.core.hud.CoveredKeyIds;
import net.ofatech.supernaturalbloodlines.core.hud.HudKeyDefaults;
import org.junit.jupiter.api.Test;

/**
 * Every key id we cover or plan for is a real id of a mod: one of the pack's lang ids (plan 23 Task 0), or a listed id of a mod the
 * pack does not ship. A typo such as {@code tensura.keybinding.naming} fails here by name.
 */
class NoRawKeyIdsTest {
    /** Ids of mods the owner's pack does not ship, with the reason they are still listed. */
    private static final Map<String, String> NOT_IN_PACK = Map.of(
            "tensuraslots.keybinding.secondary_ability.slot_1", "Tensura Slots, removed from the pack on release; covered for players who still have it",
            "tensuraslots.keybinding.secondary_ability.slot_2", "Tensura Slots, removed from the pack on release; covered for players who still have it",
            "tensuraslots.keybinding.secondary_ability.slot_3", "Tensura Slots, removed from the pack on release; covered for players who still have it");

    /** A whole key id, or the prefix of ids built in a loop ({@code "...slot_" + i}). */
    /** Physical key names (InputConstants) the HUD code and its key test use: not key bindings, so not in any lang list. */
    private static final Set<String> PHYSICAL_KEYS = Set.of("key.keyboard.unknown", "key.keyboard.b", "key.keyboard.g", "key.keyboard.h");

    private static final Pattern KEY_LITERAL = Pattern.compile("\"((?:keys?\\.[a-z0-9_.]*|[a-z0-9_]+\\.keybinding\\.[a-z0-9_.]*)[a-z0-9_])\"");
    private static final Path HUD_CORE = SourceTree.root().resolve("src/main/java/net/ofatech/supernaturalbloodlines/core/hud");
    /** The test that lists the keys left alone: its ids must be real too, or the rows guard nothing. */
    private static final Path COVERED_TEST = SourceTree.root().resolve("src/test/java/net/ofatech/supernaturalbloodlines/core/hud/CoveredKeyIdsTest.java");

    private static Set<String> packIds() throws IOException {
        Set<String> ids = new TreeSet<>();
        try (InputStream in = NoRawKeyIdsTest.class.getResourceAsStream("/plan23/pack-key-ids.txt")) {
            assertTrue(in != null, "the pack key list is missing");
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (!line.isBlank() && !line.startsWith("#")) {
                    ids.add(line.split(" ")[0]);
                }
            }
        }
        return ids;
    }

    /** A literal ending in "_" is the prefix of ids built in a loop: some known id must start with it. */
    private static boolean knownLiteral(Set<String> pack, String literal) {
        if (!literal.endsWith("_")) {
            return known(pack, literal);
        }
        return pack.stream().anyMatch(id -> id.startsWith(literal)) || NOT_IN_PACK.keySet().stream().anyMatch(id -> id.startsWith(literal));
    }

    private static boolean known(Set<String> pack, String id) {
        return pack.contains(id) || NOT_IN_PACK.containsKey(id) || PHYSICAL_KEYS.contains(id);
    }

    @Test
    void thePackListHasTheKeysThePlanNames() throws IOException {
        Set<String> pack = packIds();
        for (String id : List.of("tensura.keybinding.name", "tensura.keybinding.next_mode", "keys.vampirism.select_ammo", "key.tensurahorizons.journal",
                "key.ascension.codex", "key.tensura_magic_growth.toggle_magicule_debug_hud", "key.stextras.config_command_three")) {
            assertTrue(pack.contains(id), id + " is not in the pack list");
        }
        assertTrue(!pack.contains("tensura.keybinding.naming"), "the list carries the old typo");
    }

    @Test
    void everyCoveredIdIsARealKey() throws IOException {
        Set<String> pack = packIds();
        List<String> unknown = new ArrayList<>();
        for (String id : CoveredKeyIds.all(true, true, true, true)) {
            if (!known(pack, id)) {
                unknown.add(id);
            }
        }
        assertEquals(List.of(), unknown, "covered key ids that no mod has");
    }

    @Test
    void everyKeyOfOursIsInTheLangList() throws IOException {
        Set<String> pack = packIds();
        List<String> unknown = new ArrayList<>();
        for (HudKeyDefaults.Key key : HudKeyDefaults.ALL) {
            if (!known(pack, key.id())) {
                unknown.add(key.id());
            }
        }
        assertTrue(HudKeyDefaults.ALL.containsAll(HudKeyDefaults.SLOTS), "the slots are not in the full list");
        assertEquals(List.of(), unknown, "our own key ids that are not in the lang list");
    }

    @Test
    void everyKeyLiteralInTheHudRulesIsARealKey() throws IOException {
        Set<String> pack = packIds();
        List<String> unknown = new ArrayList<>();
        int seen = 0;
        List<Path> sources = new ArrayList<>();
        try (Stream<Path> files = Files.list(HUD_CORE)) {
            files.filter(p -> p.toString().endsWith(".java")).sorted().forEach(sources::add);
        }
        sources.add(COVERED_TEST);
        for (Path f : sources) {
            Matcher m = KEY_LITERAL.matcher(Files.readString(f));
            while (m.find()) {
                seen++;
                if (!knownLiteral(pack, m.group(1))) {
                    unknown.add(f.getFileName() + ": " + m.group(1));
                }
            }
        }
        assertTrue(seen > 0, "the scan found no key literal at all");
        assertEquals(List.of(), unknown, "key literals that no mod has");
    }

    @Test
    void theScanCatchesATypo() {
        Matcher m = KEY_LITERAL.matcher("String a = \"tensura.keybinding.naming\";");
        assertTrue(m.find() && "tensura.keybinding.naming".equals(m.group(1)), "the pattern does not read a key literal");
    }
}
