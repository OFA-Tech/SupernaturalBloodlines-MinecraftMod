package net.ofatech.supernaturalbloodlines.gametest;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminCommands;

/**
 * Walks the real dispatcher: a command form of ours with no row in {@link AdminCommands#ALL} (so no hub button or admin action decided for it)
 * fails here with the form, and so does a row that no registered form has. {@code /sb} is a redirect of {@code bloodline} and {@code pack}.
 */
@GameTestHolder(SupernaturalBloodlines.MODID)
@PrefixGameTestTemplate(false)
public final class CommandCoverageGameTests {
    private static final List<String> ROOTS = List.of("bloodline", "pack", "ward", SupernaturalBloodlines.MODID);

    private CommandCoverageGameTests() {}

    private static void walk(CommandNode<CommandSourceStack> node, String path, Set<String> out) {
        if (node.getCommand() != null) {
            out.add(path);
        }
        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            walk(child, path + " " + (child instanceof ArgumentCommandNode<?, ?> ? "<>" : child.getName()), out);
        }
    }

    @GameTest(template = "empty")
    public static void everyCommandFormHasAUiRow(GameTestHelper helper) {
        var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();
        Set<String> real = new TreeSet<>();
        for (String name : ROOTS) {
            CommandNode<CommandSourceStack> node = root.getChild(name);
            if (node != null) {
                walk(node, name, real);
            }
        }
        Set<String> rows = new TreeSet<>();
        AdminCommands.ALL.forEach(r -> rows.addAll(r.shapes()));

        List<String> noRow = new ArrayList<>(real);
        noRow.removeAll(rows);
        helper.assertTrue(noRow.isEmpty(), "command forms with no row in AdminCommands.ALL (give each a hub button, an admin action or a reason): " + noRow);

        // a row whose root is registered must have its form; a root that is off (no species part, no Werewolves) has none
        List<String> noForm = new ArrayList<>();
        for (AdminCommands.Row r : AdminCommands.ALL) {
            if (root.getChild(r.root()) == null || (r.form().contains(" debug eclipse") && !has(root, r.root(), "debug", "eclipse"))
                    || (r.form().contains(" debug npcpack") && !has(root, r.root(), "debug", "npcpack"))
                    || (r.form().contains(" debug npcs ") && !has(root, r.root(), "debug", "npcs"))
                    || (r.form().contains(" boss ") && !has(root, r.root(), "boss"))
                    || (r.form().startsWith("bloodline mastery") && !has(root, "bloodline", "mastery"))) { // the evolution rites part (plan 19) registers it
                continue;
            }
            for (String shape : r.shapes()) {
                if (!real.contains(shape)) {
                    noForm.add(r.id() + " '" + shape + "'");
                }
            }
        }
        helper.assertTrue(noForm.isEmpty(), "rows in AdminCommands.ALL that no command has (renamed or removed?): " + noForm);
        helper.succeed();
    }

    private static boolean has(CommandNode<CommandSourceStack> root, String... path) {
        CommandNode<CommandSourceStack> at = root;
        for (String word : path) {
            at = at.getChild(word);
            if (at == null) {
                return false;
            }
        }
        return true;
    }
}
