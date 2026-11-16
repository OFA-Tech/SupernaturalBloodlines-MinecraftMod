package net.ofatech.supernaturalbloodlines.parts.supernatural.skill;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.supernatural.task.TaskTracker;
import net.ofatech.supernaturalbloodlines.shared.supernatural.VampirismSkillAccess;

/** Applies a purchase: the rules decide, this stores the state and tells the player. The effects follow from the state change. */
public final class SkillService {
    private SkillService() {
    }

    /** Never throws for bad input: a refusal comes back as a message to the player and in the result. */
    public static SkillRules.BuyResult buy(ServerPlayer p, String skillId) {
        List<SupernaturalRace> races = TaskTracker.races(p);
        Optional<SupernaturalRace> magic = races.size() > 1 ? Optional.of(races.get(1)) : Optional.empty();
        SkillRules.BuyResult r = SkillRules.buy(SkillTable.current(), SupernaturalAccess.get(p), races.get(0), magic, skillId,
                spec -> VampirismSkillAccess.covers(p, spec));
        if (!r.ok()) {
            p.sendSystemMessage(Component.translatable(r.refusal().langKey()));
            return r;
        }
        SupernaturalAccess.set(p, r.state());
        p.sendSystemMessage(Component.translatable("supernaturalbloodlines.supernatural.skill.bought",
                SkillTable.current().skill(skillId).map(s -> s.name()).orElse(skillId)));
        return r;
    }
}
