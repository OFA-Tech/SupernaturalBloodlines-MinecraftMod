package net.ofatech.supernaturalbloodlines.parts.supernaturaltensura;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;
import net.ofatech.supernaturalbloodlines.shared.supernatural.SkillGrants;
import net.ofatech.supernaturalbloodlines.shared.tensura.SkillOwners;

/**
 * The TENSURA skills of the skill trees: granted through {@link SkillOwners} (source {@code supernatural_skill}) and tracked in the
 * {@code granted} set of the supernatural state. Only a skill this mod granted, or shares with another of its sources, is ever taken
 * back; a skill the player had before is theirs.
 */
public final class TensuraSkillEffects {
    static final String SOURCE = "supernatural_skill";
    private static final Set<String> WARNED = new HashSet<>();

    private TensuraSkillEffects() {
    }

    public static void register() {
        SkillOwners.register(SOURCE, (p, skill) -> LadderQuery.tensuraSkills(p).contains(skill.toString()));
        SkillGrants.set(TensuraSkillEffects::sync);
    }

    static Set<String> sync(ServerPlayer p, Set<String> wanted, Set<String> granted) {
        var skills = SkillAPI.getSkillsFrom(p);
        Set<String> out = new HashSet<>(granted);
        for (String id : granted) {
            if (!wanted.contains(id)) {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl != null) {
                    // kept when another source (the layer, the werewolf grants) still wants it; the last one to let go forgets it
                    SkillOwners.release(p, rl, SOURCE);
                }
                out.remove(id);
            }
        }
        for (String id : wanted) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            ManasSkill skill = rl == null ? null : SkillAPI.getSkillRegistry().get(rl);
            if (skill == null) {
                if (WARNED.add(id)) {
                    SupernaturalBloodlines.LOGGER.warn("supernatural skill: unknown Tensura skill '{}', skipped", id);
                }
                continue;
            }
            if (skills.getSkill(rl).isPresent()) {
                // one this mod granted for another source is shared; one the player had before is theirs and is never tracked
                if (SkillOwners.isGranted(p, rl)) {
                    out.add(id);
                }
                continue;
            }
            if (SkillOwners.grant(p, skill)) {
                out.add(id);
            } else {
                out.remove(id);
            }
        }
        return out;
    }
}
