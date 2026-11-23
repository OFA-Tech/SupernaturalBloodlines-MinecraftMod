package net.ofatech.supernaturalbloodlines.parts.evolutionrites.client;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.evolution.ChosenMark;
import net.ofatech.supernaturalbloodlines.core.evolution.Tier;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoSkills;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvolutionRitesConfig;
import net.ofatech.supernaturalbloodlines.shared.hub.MasteryAccess;

/**
 * The client's half of the Rite of Mastery picker: the player's own skills, as the hub's Mastery tab lists them, by tier and then name, with what the client
 * can already tell is refused greyed (mastered, lent, the refused list of this client's own config: on a dedicated server that is the default list, the
 * server decides). The chosen mark follows the server's answers in chat ({@link ChosenMark}). Installed only on the client.
 */
public final class MasteryClient {
    private static volatile String chosen;

    private MasteryClient() {
    }

    public static void install() {
        MasteryAccess.install(new MasteryAccess.Provider() {
            @Override
            public List<MasteryAccess.Entry> entries() {
                return MasteryClient.entries();
            }

            @Override
            public String chosen() {
                return chosen;
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientChatReceivedEvent.System e) -> {
            if (e.getMessage().getContents() instanceof TranslatableContents t) {
                Object first = t.getArgs().length > 0 ? t.getArgs()[0] : null;
                chosen = ChosenMark.after(chosen, t.getKey(), first == null ? null : String.valueOf(first));
            }
        });
    }

    private static List<MasteryAccess.Entry> entries() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return List.of();
        }
        return SkillAPI.getSkillsFrom(player).getLearnedSkills().stream().map(inst -> entry(player, inst))
                .sorted(Comparator.comparingInt(MasteryAccess.Entry::tierOrder).thenComparing(e -> e.name().getString())).toList();
    }

    private static MasteryAccess.Entry entry(net.minecraft.world.entity.player.Player player, ManasSkillInstance inst) {
        Tier tier = EvoSkills.tierOf(inst.getSkill());
        Component grey = inst.isTemporarySkill() ? Component.translatable("supernaturalbloodlines.refusal.mastery.temporary")
                : isDone(player, inst) ? Component.translatable("supernaturalbloodlines.refusal.mastery.already")
                : EvolutionRitesConfig.masteryRefusedSkills().contains(inst.getSkillId().toString()) ? Component.translatable("supernaturalbloodlines.refusal.mastery.system") : null;
        return new MasteryAccess.Entry(inst.getSkillId().toString(), inst.getSkill().getName(), Component.translatable("supernaturalbloodlines.mastery.tier." + tier.id()),
                tier.ordinal(), grey);
    }

    private static boolean isDone(net.minecraft.world.entity.player.Player player, ManasSkillInstance inst) {
        return EvoSkills.isResistance(inst.getSkill()) ? inst.getMastery() >= 0 : inst.isMastered(player);
    }
}