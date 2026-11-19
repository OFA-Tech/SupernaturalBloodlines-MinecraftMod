package net.ofatech.supernaturalbloodlines.parts.species.npc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcSpawnRules;

/**
 * {@code /supernaturalbloodlines debug npcpack} (permission 2): a pack of three wolves and an Alpha 8 blocks from the operator, whatever the night
 * is, for looking at them and for testing the hunt and the weaker spark. The same root and {@code debug} literals as the eclipse command.
 */
public final class NpcPackCommand {
    private NpcPackCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal(SupernaturalBloodlines.MODID)
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("debug")
                        .then(Commands.literal("npcpack").executes(NpcPackCommand::run))
                        .then(Commands.literal("npcs")
                                .then(Commands.literal("list").executes(c -> {
                                    var rows = NpcCommands.listAll(c.getSource().getServer());
                                    if (rows.isEmpty()) {
                                        c.getSource().sendSuccess(() -> Component.translatable("supernaturalbloodlines.npc.list_none"), false);
                                    }
                                    rows.forEach(r -> c.getSource().sendSuccess(() -> r, false));
                                    return Math.max(1, rows.size());
                                }))
                                .then(Commands.literal("release").then(Commands.argument("player", net.minecraft.commands.arguments.GameProfileArgument.gameProfile()).executes(c -> {
                                    // a player who is not online (or never logged in since the profile cache forgot them) is named by profile, so their wolves can be freed too
                                    var profiles = net.minecraft.commands.arguments.GameProfileArgument.getGameProfiles(c, "player");
                                    int total = 0;
                                    for (com.mojang.authlib.GameProfile who : profiles) {
                                        int n = NpcCommands.releaseAllOf(c.getSource().getServer(), who.getId());
                                        total += n;
                                        c.getSource().sendSuccess(() -> Component.translatable("supernaturalbloodlines.npc.released_all", n, who.getName()), true);
                                    }
                                    return Math.max(1, total);
                                }))))));
    }

    private static int run(CommandContext<CommandSourceStack> c) {
        if (!(c.getSource().getEntity() instanceof ServerPlayer p)) {
            c.getSource().sendFailure(Component.literal("Only a player can say where the pack goes."));
            return 0;
        }
        var level = p.serverLevel();
        // 8 blocks ahead, then 4, then where the operator stands: the first with ground at their height
        BlockPos spot = null;
        for (int ahead : new int[] {8, 4, 0}) {
            spot = NpcPackSpawner.groundNear(level, p.blockPosition().relative(p.getDirection(), ahead));
            if (spot != null) {
                break;
            }
        }
        List<Mob> pack = spot == null ? List.of() : NpcPackSpawner.spawnPackAt(level, spot, new NpcSpawnRules.Plan(true, true, 3, false), NpcSpawnRules.nightOf(level.getDayTime()));
        if (pack.isEmpty()) {
            c.getSource().sendFailure(Component.literal(spot == null ? "No ground for a pack near you." : "The pack could not be placed (peaceful difficulty or no room)."));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal("An NPC werewolf pack of " + pack.size() + " stands near you."), true);
        return pack.size();
    }
}
