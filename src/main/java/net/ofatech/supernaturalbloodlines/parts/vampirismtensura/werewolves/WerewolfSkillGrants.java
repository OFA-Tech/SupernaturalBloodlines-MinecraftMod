package net.ofatech.supernaturalbloodlines.parts.vampirismtensura.werewolves;

import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.FactionRaceRules;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.SkillGrantSpec;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.VampireSyncConfig;
import org.jetbrains.annotations.Nullable;

/**
 * Werewolf levels grant Tensura skills (config werewolf_skill_grants). Registered by the part only when Werewolves is
 * loaded; the faction is matched by id, so this class links no Werewolves class itself.
 */
public final class WerewolfSkillGrants {
    private static final String NO_MAGICULE_COST = "NoMagiculeCost";
    private static final String GRANTED = "supernaturalbloodlines:granted";

    private WerewolfSkillGrants() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(WerewolfSkillGrants::onFactionLevelChanged);
    }

    private static boolean isWerewolf(@Nullable IPlayableFaction<?> faction) {
        return faction != null && FactionRaceRules.WEREWOLF_FACTION_ID.equals(faction.getID());
    }

    private static void onFactionLevelChanged(PlayerFactionEvent.FactionLevelChanged event) {
        IPlayableFaction<?> now = event.getCurrentFaction();
        boolean wasWolf = isWerewolf(event.getOldFaction());
        boolean isWolf = isWerewolf(now);
        if (!(event.getPlayer().getPlayer() instanceof ServerPlayer p) || p.level().isClientSide || !(wasWolf || isWolf)) {
            return;
        }
        var skills = SkillAPI.getSkillsFrom(p);
        // the old level only counts while the old faction was the werewolves, the new one only while the new faction is
        int oldLevel = wasWolf ? event.getOldLevel() : 0;
        int newLevel = isWolf ? event.getNewLevel() : 0;
        int oldMax = wasWolf ? event.getOldFaction().getHighestReachableLevel() : 0;
        int newMax = isWolf ? now.getHighestReachableLevel() : 0;
        for (SkillGrantSpec grant : VampireSyncConfig.werewolfSkillGrants()) {
            ResourceLocation id = ResourceLocation.tryParse(grant.skill());
            ManasSkill skill = id == null ? null : SkillAPI.getSkillRegistry().get(id);
            if (skill == null) {
                SupernaturalBloodlines.LOGGER.warn("werewolf_skill_grants: unknown skill '{}'", grant.skill());
                continue;
            }
            if (grant.reachedAt(newLevel, newMax)) {
                if (skills.getSkill(id).isEmpty()) {
                    // Tensura charges max magicule for a learned skill and vetoes it without enough; this tag is its own "free" switch
                    ManasSkillInstance instance = skill.createDefaultInstance();
                    // (Tensura removes that tag itself while it unlocks, so it is one-shot; the grant mark below stays)
                    instance.getOrCreateTag().putBoolean(NO_MAGICULE_COST, true);
                    instance.getOrCreateTag().putBoolean(GRANTED, true);
                    skills.learnSkill(instance);
                }
            } else if (grant.reachedAt(oldLevel, oldMax)) {
                // only a skill this class granted is taken back, never one the player had from somewhere else
                skills.getSkill(id).filter(held -> held.getTag() != null && held.getTag().getBoolean(GRANTED))
                        .ifPresent(skills::forgetSkill);
            }
        }
    }
}
