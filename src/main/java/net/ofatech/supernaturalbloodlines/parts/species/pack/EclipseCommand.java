package net.ofatech.supernaturalbloodlines.parts.species.pack;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * {@code /supernaturalbloodlines debug eclipse start|stop} (permission 2). The same root and {@code debug} literals as the
 * foundation's {@code DebugCommand}: Brigadier merges them, so this part adds its own branch without core knowing it.
 */
public final class EclipseCommand {
    private EclipseCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal(SupernaturalBloodlines.MODID)
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("debug")
                        .then(Commands.literal("eclipse")
                                .then(Commands.literal("start").executes(c -> run(c, true)))
                                .then(Commands.literal("stop").executes(c -> run(c, false))))));
    }

    private static int run(CommandContext<CommandSourceStack> c, boolean start) {
        var server = c.getSource().getServer();
        if (start ? LunarEclipse.start(server) : LunarEclipse.stop(server)) {
            c.getSource().sendSuccess(() -> Component.literal(start ? "The lunar eclipse starts and lasts until dawn." : "The lunar eclipse ends."), true);
            return 1;
        }
        c.getSource().sendFailure(Component.literal(start ? "An eclipse is already running." : "No eclipse is running."));
        return 0;
    }
}
