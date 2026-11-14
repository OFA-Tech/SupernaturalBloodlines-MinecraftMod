package net.ofatech.supernaturalbloodlines.parts.supernatural.skill;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.VampirismRegistries;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.skills.ISkill;
import de.teamlapen.vampirism.api.entity.player.skills.ISkillHandler;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.shared.vampirism.Ghouls;

/**
 * Unlocking a Vampirism or Werewolves skill from the hub: no rules of ours, only the mod's own. The skill is looked up in Vampirism's
 * skill registry (Werewolves registers its skills there too), must belong to the player's current faction, and the faction player's
 * {@link ISkillHandler} decides: {@code canSkillBeEnabled} (points, parents, exclusions, locks) and then {@code enableSkill}, which
 * spends that mod's points. The handler marks itself dirty and Vampirism syncs it to the client. A Ghoul unlocks nothing.
 */
public final class ModSkillService {
    /** @param reason the lower-case reason of a refusal (it is the last part of the lang key), null when unlocked */
    public record Outcome(String reason) {
        public boolean ok() {
            return reason == null;
        }
    }

    private ModSkillService() {
    }

    /** Never throws for bad input: a refusal comes back as a message to the player and in the result. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Outcome unlock(ServerPlayer p, String skillId) {
        Outcome refused = check(p, skillId);
        if (refused != null) {
            return refused(p, refused.reason());
        }
        ISkill skill = VampirismRegistries.SKILL.get().get(ResourceLocation.parse(skillId));
        IFactionPlayer<?> fp = VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow();
        ISkillHandler handler = fp.getSkillHandler();
        ISkillHandler.Result result = handler.canSkillBeEnabled(skill);
        if (result != ISkillHandler.Result.OK) {
            return refused(p, result.name().toLowerCase(Locale.ROOT));
        }
        handler.enableSkill(skill);
        p.sendSystemMessage(Component.translatable("supernaturalbloodlines.supernatural.modskill.unlocked", skill.getName()));
        return new Outcome(null);
    }

    /** Everything that is ours to refuse; null when the mod's handler should be asked. */
    private static Outcome check(ServerPlayer p, String skillId) {
        if (Ghouls.isGhoul(p)) {
            return new Outcome("ghoul");
        }
        ResourceLocation id = ResourceLocation.tryParse(skillId);
        ISkill<?> skill = id == null ? null : VampirismRegistries.SKILL.get().getOptional(id).orElse(null);
        if (skill == null) {
            return new Outcome("unknown_skill");
        }
        IFactionPlayer<?> fp = VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElse(null);
        if (fp == null) {
            return new Outcome("no_faction");
        }
        if (skill.getFaction().filter(f -> f != fp.getFaction()).isPresent()) {
            return new Outcome("wrong_faction");
        }
        return null;
    }

    private static Outcome refused(ServerPlayer p, String reason) {
        p.sendSystemMessage(Component.translatable("supernaturalbloodlines.supernatural.modskill.refuse." + reason));
        return new Outcome(reason);
    }
}