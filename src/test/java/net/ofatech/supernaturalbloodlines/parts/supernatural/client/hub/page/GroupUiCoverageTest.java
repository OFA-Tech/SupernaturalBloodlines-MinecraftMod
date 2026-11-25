package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupCommand;
import org.junit.jupiter.api.Test;

/** The UI-first rule for groups: every literal of the /group tree has a control on a hub page, and the page draws it. */
class GroupUiCoverageTest {
    private static void walk(CommandNode<CommandSourceStack> node, String path, Set<String> out) {
        if (node.getCommand() != null && !path.isEmpty()) {
            out.add(path);
        }
        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            boolean argument = child instanceof com.mojang.brigadier.tree.ArgumentCommandNode<?, ?>;
            walk(child, argument ? path : (path.isEmpty() ? child.getName() : path + " " + child.getName()), out);
        }
    }

    /** The tree's literal paths: argument words are not literals, so {@code invite <player>} is {@code invite}. */
    static Set<String> literals(CommandDispatcher<CommandSourceStack> d, String root) {
        Set<String> out = new TreeSet<>();
        walk(d.getRoot().getChild(root), "", out);
        return out;
    }

    private static CommandDispatcher<CommandSourceStack> tree() {
        CommandDispatcher<CommandSourceStack> d = new CommandDispatcher<>();
        d.register(GroupCommand.tree("group"));
        return d;
    }

    private static String pageSources() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isDirectory(root.resolve("src/main/java"))) {
            root = root.getParent();
        }
        Path base = root.resolve("src/main/java/net/ofatech/supernaturalbloodlines/parts/supernatural/client/hub");
        try (Stream<Path> files = Files.walk(base)) {
            StringBuilder all = new StringBuilder();
            for (Path p : files.filter(f -> f.toString().endsWith(".java") && !f.getFileName().toString().startsWith("DevDemo")).collect(Collectors.toList())) {
                all.append(Files.readString(p).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "")).append('\n');
            }
            return all.toString();
        }
    }

    @Test
    void everyLiteralOfTheTreeHasAControl() {
        List<String> missing = new ArrayList<>();
        for (String path : literals(tree(), "group")) {
            if (!GroupButtons.IDS.containsKey(path) && !GroupButtons.NO_BUTTON.contains(path)) {
                missing.add(path);
            }
        }
        assertTrue(missing.isEmpty(), "commands of /group with no control on the hub: " + missing);
    }

    @Test
    void everyControlIsOfACommandThatExists() {
        Set<String> real = literals(tree(), "group");
        for (String path : GroupButtons.IDS.keySet()) {
            assertTrue(real.contains(path), path + " is not a command of /group");
        }
    }

    @Test
    void noControlIsUsedTwiceAndEveryOneHasAWord() throws Exception {
        Set<String> seen = new HashSet<>();
        JsonObject lang = net.ofatech.supernaturalbloodlines.parts.species.group.GroupTextTest.lang();
        for (var e : GroupButtons.IDS.entrySet()) {
            assertTrue(seen.add(e.getValue()), "the control " + e.getValue() + " is the answer for two commands");
            assertTrue(GroupButtons.LABELS.containsKey(e.getValue()), e.getValue() + " has no label");
            assertTrue(lang.has(GroupButtons.LABELS.get(e.getValue())), GroupButtons.LABELS.get(e.getValue()) + " is not in the lang file");
        }
        assertEquals(GroupButtons.IDS.size(), GroupButtons.LABELS.size());
    }

    @Test
    void noControlIsExemptFromBeingDrawnAnyMore() {
        assertTrue(GroupButtons.NOT_DRAWN_YET.isEmpty(), "plan 20b Task 12 draws every coven control: " + GroupButtons.NOT_DRAWN_YET);
    }

    @Test
    void thePagesReallyDrawEveryControl() throws Exception {
        String source = pageSources();
        List<String> missing = new ArrayList<>();
        for (String id : GroupButtons.IDS.values()) {
            if (!GroupButtons.NOT_DRAWN_YET.contains(id) && !source.contains("\"" + id + "\"")) {
                missing.add(id);
            }
        }
        assertTrue(missing.isEmpty(), "controls no hub page draws: " + missing);
    }
}
