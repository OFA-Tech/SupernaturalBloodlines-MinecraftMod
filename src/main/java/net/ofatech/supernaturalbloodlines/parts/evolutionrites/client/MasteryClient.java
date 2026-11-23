package net.ofatech.supernaturalbloodlines.parts.evolutionrites.client;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.ofatech.supernaturalbloodlines.core.evolution.Tier;
import net.ofatech.supernaturalbloodlines.parts.evolutionrites.EvoSkills;
import net.ofatech.supernaturalbloodlines.shared.hub.MasteryAccess;

/** The client's half of the Rite of Mastery picker: the player's own skills, as the hub's Mastery tab lists them. Installed only on the client. */
public final class MasteryClient {
    private MasteryClient() {
    }

    public static void install() {
        MasteryAccess.install(MasteryClient::entries);
    }

    private static List<MasteryAccess.Entry> entries() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return List.of();
        }
        return SkillAPI.getSkillsFrom(player).getLearnedSkills().stream().map(inst -> entry(player, inst))
                .sorted(Comparator.comparing((MasteryAccess.Entry e) -> e.tier().getString()).thenComparing(e -> e.name().getString())).toList();
    }

    private static MasteryAccess.Entry entry(net.minecraft.world.entity.player.Player player, ManasSkillInstance inst) {
        Tier tier = EvoSkills.tierOf(inst.getSkill());
        Component grey = inst.isTemporarySkill() ? Component.translatable("supernaturalbloodlines.refusal.mastery.temporary")
                : isDone(player, inst) ? Component.translatable("supernaturalbloodlines.refusal.mastery.already") : null;
        return new MasteryAccess.Entry(inst.getSkillId().toString(), inst.getSkill().getName(), Component.translatable("supernaturalbloodlines.mastery.tier." + tier.id()), grey);
    }

    private static boolean isDone(net.minecraft.world.entity.player.Player player, ManasSkillInstance inst) {
        return EvoSkills.isResistance(inst.getSkill()) ? inst.getMastery() >= 0 : inst.isMastered(player);
    }
}
