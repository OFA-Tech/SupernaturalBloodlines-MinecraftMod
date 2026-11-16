package net.ofatech.supernaturalbloodlines.shared.supernatural;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.VampirismRegistries;
import de.teamlapen.vampirism.api.entity.player.skills.ISkill;
import de.teamlapen.vampirism.api.entity.player.skills.ISkillHandler;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillSpec;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.core.supernatural.VampirismSkillPlan;

/**
 * The one place that enables and takes back the Vampirism skills our ladder skills stand for (S-VAM-7), on both sides of the supernatural part
 * (its sync, and the login clean-up when it is off). Only a skill this mod enabled is recorded in {@code vampirismGranted}, so one the player had or
 * bought is never touched. Needs Vampirism: call it only when Vampirism is loaded.
 */
public final class VampirismSkillAccess {
    private VampirismSkillAccess() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Optional<ISkillHandler> handler(Player p) {
        return VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().map(fp -> (ISkillHandler) fp.getSkillHandler());
    }

    private static ISkill<?> find(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : VampirismRegistries.SKILL.get().get(rl);
    }

    /** Whether the player has this Vampirism skill now (client or server); false for an unknown skill or a player outside its faction. */
    public static boolean has(Player p, String skillId) {
        ISkill<?> skill = find(skillId);
        return skill != null && handler(p).map(h -> h.isSkillEnabled(skill)).orElse(false);
    }

    /** Whether the player already has what this ladder skill stands for (S-VAM-7: Vampirism's own skill), so buying it would pay for nothing. */
    public static boolean covers(Player p, SkillSpec spec) {
        return p != null && spec.vampirismSkill() != null && has(p, spec.vampirismSkill());
    }

    /** Whether the Vampirism skill exists in this Vampirism version. */
    public static boolean exists(String skillId) {
        return find(skillId) != null;
    }

    /**
     * Makes the player's Vampirism skills agree with {@code wanted}: enables what is missing (and records it), takes back what this mod enabled and
     * is not wanted any more. An empty {@code wanted} takes everything back.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void apply(ServerPlayer p, Set<String> wanted) {
        SupernaturalState s = SupernaturalAccess.get(p);
        Optional<ISkillHandler> handler = handler(p);
        if (handler.isEmpty()) {
            // outside the vampire faction Vampirism has already reset its skills, so there is nothing to take back, only the record to forget
            if (!s.vampirismGranted().isEmpty()) {
                SupernaturalAccess.set(p, s.toBuilder().vampirismGranted(Set.of()).build());
            }
            return;
        }
        Set<String> usable = new HashSet<>();
        for (String id : wanted) {
            ISkill<?> skill = find(id);
            if (skill != null && skill.getFaction().map(f -> f == VampirismAPI.factionPlayerHandler(p).getCurrentFaction()).orElse(false)) {
                usable.add(id);
            }
        }
        VampirismSkillPlan.Result r = VampirismSkillPlan.plan(usable, s.vampirismGranted(), id -> has(p, id));
        r.enable().forEach(id -> handler.get().enableSkill((ISkill) find(id), true));
        r.disable().forEach(id -> handler.get().disableSkill((ISkill) find(id)));
        if (!r.tracked().equals(s.vampirismGranted())) {
            SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().vampirismGranted(r.tracked()).build());
        }
    }
}
