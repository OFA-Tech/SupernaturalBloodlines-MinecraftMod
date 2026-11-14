package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.apex.EvolutionBoost;
import net.ofatech.supernaturalbloodlines.shared.tensura.SkillOwners;

/**
 * A species is a layer on the player's Tensura race (R100): skills, max magicule and aura, and (through
 * {@link UndeadTreatment}) the undead checks. The race is never written here. Only skills this class granted are
 * taken back; they are tracked in {@code BloodlineState.layerSkills}.
 */
public final class SpeciesLayer {
    public static final ResourceLocation MAGICULE_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "layer_magicule");
    public static final ResourceLocation AURA_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "layer_aura");

    private static final String SOURCE = "species_layer";
    private static final int RECHECK_TICKS = 200;
    private static final Set<String> WARNED = new HashSet<>();

    private SpeciesLayer() {}

    public static void register() {
        SkillOwners.register(SOURCE, (p, skill) -> spec(p).skills().contains(skill.toString()));
        BloodlineAccess.addListener((p, change) -> sync(p));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> syncIfServer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> syncIfServer(e.getEntity()));
        // a plain Vampirism vampire has a layer too (R20), so a faction change can change it
        NeoForge.EVENT_BUS.addListener((PlayerFactionEvent.FactionLevelChanged e) -> syncIfServer(e.getPlayer().getPlayer()));
        // a Tensura evolution may rewrite skills or modifiers; the sync is idempotent
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

    public static LayerSpec spec(Player p) {
        Species species = BloodlineAccess.get(p).species();
        boolean plainVampire = species == Species.NONE && VampirismAPI.factionPlayerHandler(p).isInFaction(VReference.VAMPIRE_FACTION);
        return SpeciesTensuraConfig.table().specFor(species, plainVampire);
    }

    /** What the hunters and werewolves rules and the holy bridge are told: the layer says undead, the switch is on and the hook is in place. */
    public static boolean treatedAsUndeadForBridge(Player p) {
        return UndeadTreatment.mixinApplied() && treatAsUndead(p);
    }

    /** The layer says undead and the switch is on: Tensura's undead and holy-exposure checks answer true (through the mixin). */
    public static boolean treatAsUndead(Player p) {
        return SpeciesTensuraConfig.undeadTreatment() && spec(p).treatAsUndead();
    }

    /** R103 (see {@link ApexRule}): an apex race of ours on a vampire-type species. */
    public static boolean isApexVampireType(Player p, ResourceLocation raceId) {
        return ApexRule.isApexVampire(BloodlineAccess.get(p).species(), raceId.getNamespace());
    }

    /** Idempotent: makes the player's skills and attributes match their layer. */
    public static void sync(ServerPlayer p) {
        if (p.level().isClientSide) {
            return;
        }
        LayerSpec spec = spec(p);
        Set<String> before = BloodlineAccess.get(p).layerSkills();
        Set<String> tracked = syncSkills(p, before, spec.skills());
        if (!tracked.equals(before)) {
            // a change of layerSkills alone does not notify the listeners, so this cannot loop
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().layerSkills(tracked).build());
        }
        applyBonus(p, TensuraAttributes.MAX_MAGICULE, MAGICULE_ID, spec.maxMagiculeBonus());
        applyBonus(p, TensuraAttributes.MAX_AURA, AURA_ID, spec.maxAuraBonus());
        EvolutionBoost.sync(p);
    }

    private static Set<String> syncSkills(ServerPlayer p, Set<String> tracked, List<String> wanted) {
        var skills = SkillAPI.getSkillsFrom(p);
        Set<String> out = new HashSet<>(tracked);
        for (String id : tracked) {
            if (!wanted.contains(id)) {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl != null) {
                    // kept when another source (the werewolf grants) still wants it; the last one to let go forgets it
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
                    SupernaturalBloodlines.LOGGER.warn("species_tensura: unknown skill '{}' in the layers config, skipped", id);
                }
                continue;
            }
            if (skills.getSkill(rl).isPresent()) {
                // a tracked skill stays tracked; one this mod granted for another source is shared; one the player had before is theirs
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
    private static void applyBonus(ServerPlayer p, Holder<Attribute> attribute, ResourceLocation id, double bonus) {
        AttributeInstance a = p.getAttribute(attribute);
        if (a == null) {
            return;
        }
        if (bonus <= 0) {
            a.removeModifier(id);
            return;
        }
        AttributeModifier current = a.getModifier(id);
        if (current == null || Math.abs(current.amount() - bonus) > 1e-12) {
            a.addOrReplacePermanentModifier(new AttributeModifier(id, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }
}
