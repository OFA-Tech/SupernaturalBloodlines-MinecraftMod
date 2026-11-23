package net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest;

import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** {@code /bloodline mastery} and {@code /sb bloodline mastery}: the second is a redirect of the species part's, which shows only its target's children. */
@PrefixGameTestTemplate(false)
public final class EvolutionCommandGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private EvolutionCommandGameTests() {
    }

    private static boolean parses(GameTestHelper helper, String command) {
        var server = helper.getLevel().getServer();
        CommandSourceStack source = server.createCommandSourceStack();
        ParseResults<CommandSourceStack> parse = server.getCommands().getDispatcher().parse(command, source);
        return !parse.getReader().canRead() && parse.getExceptions().isEmpty() && parse.getContext().build(command).getCommand() != null;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bothPrefixesReachTheMasteryCommands(GameTestHelper helper) {
        if (EvoTestSupport.skipped(helper, "mastery_common")) {
            return;
        }
        for (String prefix : new String[] {"bloodline", "sb bloodline"}) {
            helper.assertTrue(parses(helper, prefix + " mastery"), "'" + prefix + " mastery' does not parse");
            helper.assertTrue(parses(helper, prefix + " mastery clear"), "'" + prefix + " mastery clear' does not parse");
            helper.assertTrue(parses(helper, prefix + " mastery choose tensura:anti_skill"), "'" + prefix + " mastery choose <skill>' does not parse");
        }
        helper.succeed();
    }
}