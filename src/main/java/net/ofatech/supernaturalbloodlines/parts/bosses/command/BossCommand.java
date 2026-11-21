package net.ofatech.supernaturalbloodlines.parts.bosses.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.boss.BossFacts;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossRules;
import net.ofatech.supernaturalbloodlines.core.boss.BossState;
import net.ofatech.supernaturalbloodlines.core.boss.DesiccationRules;
import net.ofatech.supernaturalbloodlines.core.boss.SpawnRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BeastDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.NpcOriginalBoss;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineWarning;

/**
 * {@code /supernaturalbloodlines boss <boss> spawn|state|desiccate|return|change|reset} (permission 2): the operator's hands on the director. Every form has an
 * Admin button (rows X1 to X6 of {@code AdminCommands}). {@code reset} is the Admin page's "Forget".
 */
public final class BossCommand {
    private BossCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal(SupernaturalBloodlines.MODID).requires(s -> s.hasPermission(2))
                .then(Commands.literal("boss").then(Commands.argument("boss", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(BossId.values()).filter(BossId::landed).map(BossId::id), b))
                        .then(Commands.literal("spawn").executes(c -> spawn(c)))
                        .then(Commands.literal("state").executes(c -> state(c)))
                        .then(Commands.literal("desiccate").executes(c -> desiccate(c)))
                        .then(Commands.literal("return").executes(c -> giveBack(c)))
                        .then(Commands.literal("change").executes(c -> change(c)))
                        .then(Commands.literal("reset").executes(c -> reset(c))))));
    }

    private static Optional<BossId> boss(CommandContext<CommandSourceStack> c) {
        Optional<BossId> id = BossId.byId(StringArgumentType.getString(c, "boss")).filter(BossId::landed);
        if (id.isEmpty()) {
            c.getSource().sendFailure(Component.translatable("supernaturalbloodlines.boss.cmd.unknown", StringArgumentType.getString(c, "boss")));
        }
        return id;
    }

    public static Component name(BossId boss) {
        return Component.translatable("supernaturalbloodlines.boss.name." + boss.id());
    }

    private static int spawn(CommandContext<CommandSourceStack> c) {
        Optional<BossId> id = boss(c);
        if (id.isEmpty()) {
            return 0;
        }
        BossId boss = id.get();
        if (!(c.getSource().getEntity() instanceof ServerPlayer p)) {
            c.getSource().sendFailure(Component.translatable("supernaturalbloodlines.boss.cmd.player_only"));
            return 0;
        }
        ServerLevel level = p.serverLevel();
        BossIndex index = BossIndex.get(level.getServer());
        Optional<String> refusal = SpawnRules.refusal(new BossFacts(BossesConfig.enabled(boss), BossDirector.modsPresent(boss), index.state(boss), false));
        if (refusal.isPresent()) {
            c.getSource().sendFailure(Component.translatable("supernaturalbloodlines.boss.cmd.refused", name(boss), Component.translatable("supernaturalbloodlines.boss.refusal." + refusal.get())));
            return 0;
        }
        var spot = BossDirector.findGround(level, p.blockPosition(), 4, 8);
        boolean made = spot.isPresent() && switch (boss) {
            case KLAUS -> BossDirector.spawnKlaus(level, null, spot.get()).isPresent();
            case DEUCALION -> BossDirector.spawnDeucalion(level, null, spot.get()).isPresent();
            case PETER_HALE -> BossDirector.spawnPeterAt(level, spot.get()).isPresent();
            case ELIJAH -> BossDirector.spawnElijah(level, null, spot.get()).isPresent();
            case REBEKAH -> BossDirector.spawnRebekah(level, p.blockPosition(), null, null).isPresent();
            case KOL -> BossDirector.spawnKol(level, null, spot.get()).isPresent();
            case FINN -> BossDirector.spawnFinn(level, null, spot.get()).isPresent();
            case BEAST_OF_GEVAUDAN -> BeastDirector.spawnHost(level, null, spot.get()).isPresent();
            default -> BossDirector.spawnMikael(level, null, spot.get()).isPresent();
        };
        if (!made) {
            c.getSource().sendFailure(Component.translatable("supernaturalbloodlines.boss.cmd.no_ground"));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.translatable("supernaturalbloodlines.boss.cmd.spawned", name(boss)), true);
        return 1;
    }

    private static int state(CommandContext<CommandSourceStack> c) {
        Optional<BossId> id = boss(c);
        if (id.isEmpty()) {
            return 0;
        }
        BossId boss = id.get();
        ServerLevel overworld = c.getSource().getServer().overworld();
        BossIndex.Entry e = BossIndex.get(c.getSource().getServer()).of(boss);
        long left = e.state == BossState.OPEN ? DesiccationRules.daysLeft(net.ofatech.supernaturalbloodlines.parts.bosses.director.BossClock.today(overworld), e.returnDay) : 0;
        c.getSource().sendSuccess(() -> Component.translatable("supernaturalbloodlines.boss.cmd.state", name(boss),
                Component.translatable("supernaturalbloodlines.boss.state." + e.state.name().toLowerCase(java.util.Locale.ROOT)), left, e.rewarded, e.appearances), false);
        if (boss == BossId.BEAST_OF_GEVAUDAN) {
            c.getSource().sendSuccess(() -> Component.translatable("supernaturalbloodlines.boss.cmd.beast_phase", e.phase.name().toLowerCase(java.util.Locale.ROOT), e.hostsMade, e.returned), false);
        }
        return 1;
    }

    private static int desiccate(CommandContext<CommandSourceStack> c) {
        return withEntity(c, b -> b.desiccate(), "desiccated");
    }

    private static int giveBack(CommandContext<CommandSourceStack> c) {
        return withEntity(c, NpcOriginalBoss::recover, "returned");
    }

    private static int withEntity(CommandContext<CommandSourceStack> c, java.util.function.Consumer<NpcOriginalBoss> action, String done) {
        Optional<BossId> id = boss(c);
        if (id.isEmpty()) {
            return 0;
        }
        NpcOriginalBoss b = BossDirector.bossEntity(c.getSource().getServer(), id.get());
        if (b == null) {
            c.getSource().sendFailure(Component.translatable("supernaturalbloodlines.boss.cmd.not_loaded", name(id.get())));
            return 0;
        }
        action.accept(b);
        c.getSource().sendSuccess(() -> Component.translatable("supernaturalbloodlines.boss.cmd." + done, name(id.get())), true);
        return 1;
    }

    /** {@code boss beast_of_gevaudan change}: the waiting Host becomes the Beast now (the Admin page's button). */
    private static int change(CommandContext<CommandSourceStack> c) {
        Optional<BossId> id = boss(c);
        if (id.isEmpty()) {
            return 0;
        }
        if (id.get() != BossId.BEAST_OF_GEVAUDAN || !BeastDirector.change(c.getSource().getServer().overworld())) {
            c.getSource().sendFailure(Component.translatable("supernaturalbloodlines.boss.cmd.cannot_change", name(id.get())));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.translatable("supernaturalbloodlines.boss.cmd.changed", name(id.get())), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> c) {
        Optional<BossId> id = boss(c);
        if (id.isEmpty()) {
            return 0;
        }
        BossId boss = id.get();
        var server = c.getSource().getServer();
        var b = BossDirector.anyBossBody(server, boss);
        if (b instanceof net.ofatech.supernaturalbloodlines.parts.bosses.entity.DeucalionEntity d) {
            d.dismissPack();
        }
        if (b != null) {
            b.discard();
        }
        if (boss == BossId.BEAST_OF_GEVAUDAN) {
            BeastDirector.clear(server);
        }
        BossIndex.get(server).forget(boss);
        boss.npcOriginal().ifPresent(o -> {
            LineWarning.closed(o.uuid());
            LineIndex.get(server).unmarkDead(o.uuid());
        });
        c.getSource().sendSuccess(() -> Component.translatable("supernaturalbloodlines.boss.cmd.reset", name(boss)), true);
        return 1;
    }
}
