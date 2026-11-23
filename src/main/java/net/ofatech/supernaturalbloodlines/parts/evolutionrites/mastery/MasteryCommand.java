package net.ofatech.supernaturalbloodlines.parts.evolutionrites.mastery;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.evolution.MasteryRules;

/**
 * {@code /bloodline mastery [choose <skill>|clear]} (everyone, for themselves): the same choice the hub's Skills page makes, so a player on a client
 * without the hub, or a server console player, can make it. The server validates the choice here exactly as it does for the hub.
 */
public final class MasteryCommand {
    private MasteryCommand() {
    }

    /**
     * {@code /bloodline mastery ...} and, because {@code /sb bloodline} redirects to the same node, {@code /sb bloodline mastery ...}. The species part registers
     * {@code bloodline} first and its redirect points at the node it got back; Brigadier merges a second {@code bloodline} into the first node, so ours must be
     * registered AFTER the species part's (see the LOWEST priority in the part): registered first, ours would be the tree node and the species part's redirect
     * would point at a node that never gets {@code mastery}.
     */
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("bloodline").then(node()));
    }

    static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> node() {
        return (Commands.literal("mastery")
                .executes(c -> show(c.getSource().getPlayerOrException(), c.getSource()))
                .then(Commands.literal("clear").executes(c -> clear(c.getSource().getPlayerOrException(), c.getSource())))
                .then(Commands.literal("choose").then(Commands.argument("skill", StringArgumentType.greedyString())
                        .suggests((c, b) -> c.getSource().getPlayer() == null ? b.buildFuture()
                                : SharedSuggestionProvider.suggest(MasteryChoice.options(c.getSource().getPlayerOrException()).stream()
                                        .filter(o -> o.refusal().isEmpty()).map(o -> o.skill().toString()), b))
                        .executes(c -> choose(c.getSource().getPlayerOrException(), c.getSource(), StringArgumentType.getString(c, "skill"))))));
    }

    static int show(ServerPlayer p, CommandSourceStack source) {
        Optional<ResourceLocation> chosen = MasteryChoice.get(p);
        source.sendSuccess(() -> chosen.<Component>map(id -> Component.translatable("supernaturalbloodlines.mastery.chosen_is", id.toString()))
                .orElseGet(() -> Component.translatable("supernaturalbloodlines.mastery.none")), false);
        for (MasteryChoice.Option o : MasteryChoice.options(p)) {
            source.sendSuccess(() -> Component.translatable("supernaturalbloodlines.mastery.option", o.skill().toString(),
                    Component.translatable("supernaturalbloodlines.mastery.tier." + o.tier().id()),
                    o.refusal().map(r -> (Component) Component.translatable(MasteryRules.keyOf(r, o.tier()))).orElse(Component.translatable("supernaturalbloodlines.mastery.ok"))), false);
        }
        return 1;
    }

    static int clear(ServerPlayer p, CommandSourceStack source) {
        MasteryChoice.clear(p);
        source.sendSuccess(() -> Component.translatable("supernaturalbloodlines.mastery.cleared"), false);
        return 1;
    }

    static int choose(ServerPlayer p, CommandSourceStack source, String text) {
        ResourceLocation id = ResourceLocation.tryParse(text.trim());
        if (id == null) {
            source.sendFailure(Component.translatable("supernaturalbloodlines.mastery.bad_id"));
            return 0;
        }
        Optional<MasteryRules.Reason> why = MasteryChoice.choose(p, id);
        if (why.isPresent()) {
            source.sendFailure(Component.translatable(MasteryRules.keyOf(why.get(), net.ofatech.supernaturalbloodlines.core.evolution.Tier.UNIQUE)));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("supernaturalbloodlines.mastery.chosen", id.toString()), false);
        return 1;
    }
}
