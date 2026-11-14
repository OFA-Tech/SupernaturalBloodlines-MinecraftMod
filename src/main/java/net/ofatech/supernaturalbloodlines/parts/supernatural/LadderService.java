package net.ofatech.supernaturalbloodlines.parts.supernatural;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.LadderTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.parts.supernatural.task.TaskTracker;
import net.ofatech.supernaturalbloodlines.shared.supernatural.EpReward;

/** Applies a claim: the rules decide, this pays SP (in the state), EP (through {@link EpReward}) and tells the player. */
public final class LadderService {
    private static final String KEY = "supernaturalbloodlines.supernatural.claim.";

    private LadderService() {
    }

    /** Never throws for bad input: a refusal comes back as a message to the player and in the result. */
    public static LadderRules.ClaimResult claim(ServerPlayer p, String taskId) {
        List<SupernaturalRace> races = TaskTracker.races(p);
        Optional<SupernaturalRace> magic = races.size() > 1 ? Optional.of(races.get(1)) : Optional.empty();
        LadderRules.ClaimResult r = LadderRules.claim(LadderTable.current(), SupernaturalAccess.get(p), races.get(0), magic, taskId);
        if (!r.ok()) {
            p.sendSystemMessage(Component.translatable(r.refusal().langKey()));
            return r;
        }
        SupernaturalAccess.set(p, r.state());
        pay(p, r.sp(), r.ep(), r.race(), r.rungReached());
        return r;
    }

    /** Tells the player what a claim or an automatic rung paid and pays the EP. */
    public static void pay(ServerPlayer p, int sp, long ep, SupernaturalRace race, int rungReached) {
        EpReward.pay(p, ep);
        if (sp > 0 || ep > 0) {
            p.sendSystemMessage(Component.translatable(KEY + "paid", sp, ep));
        }
        if (rungReached >= 0 && race != null) {
            p.sendSystemMessage(Component.translatable(KEY + "rung_reached", Component.translatable("supernaturalbloodlines.supernatural.race." + race.id()),
                    rungReached));
        }
    }
}
