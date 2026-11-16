package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/** Plan 13d: {@code /pack} and {@code /bloodline} keep working, and {@code /sb pack} and {@code /sb bloodline} are the same trees under our own prefix. */
@PrefixGameTestTemplate(false)
public final class CommandAliasGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private CommandAliasGameTests() {
    }

    private static boolean parses(GameTestHelper helper, String command) {
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack().withPermission(4);
        ParseResults<CommandSourceStack> parsed = dispatcher.parse(command, source);
        return !parsed.getReader().canRead() && parsed.getExceptions().isEmpty() && parsed.getContext().getLastChild().getCommand() != null;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void sbAliasesParseLikeTheOriginals(GameTestHelper helper) {
        boolean packs = net.neoforged.fml.ModList.get().isLoaded("werewolves");
        for (String original : packs ? new String[] {"pack info", "pack leave", "bloodline tree", "bloodline call"} : new String[] {"bloodline tree", "bloodline call"}) {
            helper.assertTrue(parses(helper, original), "/" + original + " no longer parses");
            helper.assertTrue(parses(helper, "sb " + original), "/sb " + original + " does not parse");
        }
        helper.assertTrue(!parses(helper, "sb bloodline nonsense"), "/sb bloodline accepted a word that is not part of the tree");
        helper.succeed();
    }
}
