package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.shared.tensura.SkillOwners;

/**
 * A werewolf's rank (Alpha, True Alpha) and a born werewolf's level 5 give Tensura skills (config rank_skills). The race
 * never changes. Only skills this class granted are taken back, tracked in {@code WolfState.grantedSkills}; a skill another
 * source (the species layer, the werewolf level grants) still wants stays, through {@link SkillOwners}. Registered only
 * when Werewolves is loaded; the faction is matched by id, so no Werewolves class is linked.
 */
public final class RankSkills {
    static final int BORN_LEVEL = 5;

    private static final String SOURCE = "wolf_rank";
    private static final String WEREWOLF_FACTION = "werewolves:werewolf";
    private static final int RECHECK_TICKS = 200;
    private static final Set<String> WARNED = new HashSet<>();

    private RankSkills() {}

    public static void register() {
        SkillOwners.register(SOURCE, (p, skill) -> wanted(p).contains(skill.toString()));
        // the listeners hear rank and lineage changes; the rest of what moves the wanted set is the level and the login
        BloodlineAccess.addListener((p, change) -> sync(p));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> syncIfServer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> syncIfServer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerFactionEvent.FactionLevelChanged e) -> syncIfServer(e.getPlayer().getPlayer()));
        // a skill another source dropped while this one wanted it only becomes ours to track on the next pass
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % RECHECK_TICKS == 0) {
                sync(p);
            }
        });
    }

    private static void syncIfServer(Player p) {
        if (p instanceof ServerPlayer sp) {
            sync(sp);
        }
    }

    /** The skills the player's rank and werewolf level ask for right now. */
    public static Set<String> wanted(Player p) {
        WolfState wolf = BloodlineAccess.get(p).wolf();
        RankSkillTable table = SpeciesTensuraConfig.rankSkills();
        Set<String> out = new LinkedHashSet<>();
        switch (wolf.rank()) {
            case ALPHA -> out.addAll(table.skillsFor(RankSkillTable.ALPHA));
            case TRUE_ALPHA -> out.addAll(table.skillsFor(RankSkillTable.TRUE_ALPHA));
            default -> {
            }
        }
        if (wolf.lineage() == WolfState.Lineage.BORN && werewolfLevel(p) >= BORN_LEVEL) {
            out.addAll(table.skillsFor(RankSkillTable.BORN_LEVEL5));
        }
        return out;
    }

    private static int werewolfLevel(Player p) {
        IFactionPlayerHandler handler = VampirismAPI.factionPlayerHandler(p);
        var faction = handler.getCurrentFaction();
        return faction != null && WEREWOLF_FACTION.equals(faction.getID().toString()) ? handler.getCurrentLevel() : 0;
    }

    /** Idempotent: makes the player's rank skills match their rank and level. */
    public static void sync(ServerPlayer p) {
        if (p.level().isClientSide) {
            return;
        }
        Set<String> wanted = wanted(p);
        Set<String> before = BloodlineAccess.get(p).wolf().grantedSkills();
        Set<String> tracked = new HashSet<>(before);
        var skills = SkillAPI.getSkillsFrom(p);
        for (String id : before) {
            if (!wanted.contains(id)) {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl != null) {
                    SkillOwners.release(p, rl, SOURCE);
                }
                tracked.remove(id);
            }
        }
        for (String id : wanted) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            ManasSkill skill = rl == null ? null : SkillAPI.getSkillRegistry().get(rl);
            if (skill == null) {
                if (WARNED.add(id)) {
                    SupernaturalBloodlines.LOGGER.warn("species_tensura: unknown skill '{}' in rank_skills, skipped", id);
                }
                continue;
            }
            if (skills.getSkill(rl).isPresent()) {
                // one this mod granted for another source is shared; one the player had before is theirs
                if (SkillOwners.isGranted(p, rl)) {
                    tracked.add(id);
                }
            } else if (SkillOwners.grant(p, skill)) {
                tracked.add(id);
            } else {
                tracked.remove(id);
            }
        }
        if (!tracked.equals(before)) {
            // a change of grantedSkills alone does not notify the listeners, so this cannot loop
            BloodlineState state = BloodlineAccess.get(p);
            BloodlineAccess.set(p, state.toBuilder().wolf(state.wolf().toBuilder().grantedSkills(tracked).build()).build());
        }
    }
}
