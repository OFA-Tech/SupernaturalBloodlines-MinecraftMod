package net.ofatech.supernaturalbloodlines.parts.supernatural.skill;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.VampirismRegistries;
import de.teamlapen.vampirism.api.entity.player.skills.ISkill;
import de.teamlapen.vampirism.api.entity.player.skills.ISkillHandler;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillRules;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillSpec;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillTable;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalRace;
import net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalState;
import net.ofatech.supernaturalbloodlines.parts.supernatural.task.TaskTracker;
import net.ofatech.supernaturalbloodlines.shared.supernatural.GrantedSkills;
import net.ofatech.supernaturalbloodlines.shared.supernatural.SkillGrants;

/**
 * What the owned skills do to the player: the ATTR modifiers (our ids, on while the skill is active and its {@code when} holds, off
 * otherwise) and the TENSURA skills (through {@link SkillGrants}). RULE and BLOOD skills are not applied here: the rule that has the
 * number asks {@code LadderQuery.value}. {@link #sync} is idempotent. It runs on login, respawn, a change of race or of skills, and
 * every 200 ticks (the sky changes the night and moon conditions); nothing scans the world.
 */
public final class SkillEffects {
    private static final int RECHECK_TICKS = 200;
    private static final Set<String> WARNED = new HashSet<>();

    private SkillEffects() {
    }

    public static ResourceLocation modifierId(String skillId) {
        return ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "skill/" + skillId.toLowerCase(Locale.ROOT));
    }

    public static void register() {
        BloodlineAccess.addListener((p, change) -> sync(p));
        SupernaturalAccess.addListener((p, change) -> {
            if (!change.before().skills().equals(change.after().skills()) || change.before().schema() != change.after().schema()) {
                sync(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> syncIfServer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> syncIfServer(e.getEntity()));
        // joining or leaving a faction changes the race, so which trees count
        NeoForge.EVENT_BUS.addListener((PlayerFactionEvent.FactionLevelChanged e) -> syncIfServer(e.getPlayer().getPlayer()));
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

    public static void sync(ServerPlayer p) {
        SkillTable table = SkillTable.current();
        // a data reload that lost the whole catalogue must not take Tensura skills back
        if (p.level().isClientSide || !TaskTracker.tracked(p) || table.skills().isEmpty()) {
            return;
        }
        SupernaturalState s = SupernaturalAccess.get(p);
        List<SupernaturalRace> races = TaskTracker.races(p);
        Optional<SupernaturalRace> magic = races.size() > 1 ? Optional.of(races.get(1)) : Optional.empty();
        Set<String> active = SkillRules.active(table, s, races.get(0), magic, id -> GrantedSkills.holds(p, id));
        Level level = p.level();
        boolean fixed = level.dimensionType().hasFixedTime();
        for (SkillSpec spec : table.skills()) {
            if (spec.attr() != null) {
                apply(p, spec, active.contains(spec.id()) && spec.attr().when().holds(fixed, level.getMoonPhase(), level.getDayTime()));
            }
        }
        enableVampirismSkills(p, table, active);
        Set<String> granted = SkillGrants.sync(p, tensuraSkillsOf(table, active), s.granted());
        if (!granted.equals(s.granted())) {
            SupernaturalAccess.set(p, SupernaturalAccess.get(p).toBuilder().granted(granted).build());
        }
    }

    /**
     * The Vampirism skills the active VAMPIRISM skills stand for (S-VAM-7: {@code vampirism:less_sundamage}) are enabled on the player's Vampirism skill
     * handler, so Vampirism's own effect and its own skill screen carry them. Nothing is taken back: a skill the player already has is left alone, and
     * a Vampirism skill reset is answered by the next sync (every 200 ticks) while the ladder skill is owned. It is not paid for with Vampirism skill
     * points here (checked in a GameTest, SkillRuleGameTests.eldersSkinEnablesVampirismsLessSundamageSkill): the ladder skill's own cost is what the player paid.
     */
    private static void enableVampirismSkills(ServerPlayer p, SkillTable table, Set<String> active) {
        for (String id : active) {
            table.skill(id).filter(s -> s.vampirismSkill() != null).ifPresent(spec -> {
                ResourceLocation rl = ResourceLocation.tryParse(spec.vampirismSkill());
                var skill = rl == null ? null : VampirismRegistries.SKILL.get().get(rl);
                var handler = VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().map(fp -> fp.getSkillHandler());
                if (skill == null || handler.isEmpty()) {
                    if (skill == null && WARNED.add(spec.id())) {
                        SupernaturalBloodlines.LOGGER.warn("Skill {}: unknown Vampirism skill '{}', skipped", spec.id(), spec.vampirismSkill());
                    }
                    return;
                }
                @SuppressWarnings({"unchecked", "rawtypes"})
                ISkillHandler<?> h = (ISkillHandler) handler.get();
                if (!h.isSkillEnabled((ISkill<?>) skill) && skill.getFaction().map(f -> f == VampirismAPI.factionPlayerHandler(p).getCurrentFaction()).orElse(false)) {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    ISkillHandler raw = h;
                    raw.enableSkill((ISkill) skill, true);
                }
            });
        }
    }

    /** The Tensura skills the active TENSURA skills grant. */
    public static Set<String> tensuraSkillsOf(SkillTable table, Set<String> active) {
        Set<String> out = new HashSet<>();
        for (String id : active) {
            table.skill(id).filter(s -> s.tensuraSkill() != null).ifPresent(s -> out.add(s.tensuraSkill()));
        }
        return out;
    }

    private static void apply(ServerPlayer p, SkillSpec spec, boolean on) {
        SkillSpec.Attr a = spec.attr();
        ResourceLocation attributeId = ResourceLocation.tryParse(a.attribute());
        Holder<Attribute> holder = attributeId == null ? null : BuiltInRegistries.ATTRIBUTE.getHolder(attributeId).orElse(null);
        if (holder == null) {
            if (WARNED.add(spec.id())) {
                SupernaturalBloodlines.LOGGER.warn("Skill {}: unknown attribute '{}', skipped", spec.id(), a.attribute());
            }
            return;
        }
        AttributeInstance instance = p.getAttribute(holder);
        if (instance == null) {
            return;
        }
        ResourceLocation id = modifierId(spec.id());
        AttributeModifier current = instance.getModifier(id);
        if (!on) {
            if (current != null) {
                instance.removeModifier(id);
            }
            return;
        }
        AttributeModifier.Operation operation = switch (a.operation()) {
            case "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
            default -> AttributeModifier.Operation.ADD_VALUE;
        };
        if (current == null || current.amount() != a.amount() || current.operation() != operation) {
            instance.addOrReplacePermanentModifier(new AttributeModifier(id, a.amount(), operation));
        }
    }
}
