package net.ofatech.supernaturalbloodlines.parts.species.turning;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BaseFaction;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;
import net.ofatech.supernaturalbloodlines.shared.vampirism.Ghouls;

/**
 * Ghouls (plan 11, R20e, R20f). Any Sanguinare that finishes leaves a Ghoul: a vampire at level 1 who cannot level up until the
 * turning is done ({@link TurningHandler}). Nothing here adds a mixin.
 *
 * <ul>
 * <li><b>Making one.</b> {@code onSanguinareFinished} calls {@code joinFaction}, which ends in
 * {@code setFactionAndLevel}, which posts {@link PlayerFactionEvent.FactionLevelChangePre}. That event is where the join is still
 * ahead (the player is in no faction, so the rule can tell a new vampire from an existing one). It says nothing about the cause, so
 * the cause is the Sanguinare effect, still on the player while its last tick runs, and the level (Sanguinare always joins at 1). Our own turning (and the rites) join through
 * {@code setFactionAndLevel} too: the turning marks itself with {@link #completing}, and the rules leave a vampire species alone.
 * {@code CanJoinFaction} was not used: it also fires for a mere {@code canJoin} query, and not at all for {@code setFactionAndLevel}.</li>
 * <li><b>Levels.</b> The same event is cancellable, and cancelling it makes {@code setFactionAndLevel} return false. That is the funnel
 * of {@code setFactionLevel}, the altars and the commands. The altars are also refused at the click, because the Altar of Infusion
 * may use up items before it tries the level.</li>
 * <li><b>Skill points.</b> {@code SkillPointProviders} cannot take points away (each provider's answer is floored at 0 and summed),
 * so the level 1 cap is what keeps a Ghoul at the level 1 points.</li>
 * <li><b>Biting a vampire</b> never feeds the biter and costs {@code bite_vampire_cost} of their own blood. Plan 12's skill will lift it.</li>
 * </ul>
 */
public final class GhoulGates {
    private static final ResourceLocation SANGUINARE = ResourceLocation.parse("vampirism:sanguinare");
    private static final Set<ResourceLocation> ALTARS = Set.of(ResourceLocation.parse("vampirism:altar_infusion"),
            ResourceLocation.parse("vampirism:altar_inspiration"));

    // server thread only
    private static final Set<UUID> COMPLETING = new HashSet<>();

    private GhoulGates() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, GhoulGates::refuseLevelUp);
        // last, and not for a cancelled join: only a join that is going to happen makes a Ghoul
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, GhoulGates::markSanguinareJoin);
        NeoForge.EVENT_BUS.addListener(GhoulGates::endOnLeaving);
        NeoForge.EVENT_BUS.addListener(GhoulGates::refuseAltar);
        NeoForge.EVENT_BUS.addListener(GhoulGates::biteVampire);
    }

    /** A vampire who is still a Ghoul (the flag only counts while they are in the vampire faction). */
    public static boolean isGhoul(Player p) {
        return Ghouls.isGhoul(p);
    }

    /** Runs {@code join} (the turning's own join into the vampires) so it is never taken for a Sanguinare. */
    static void completing(ServerPlayer p, Runnable join) {
        boolean added = COMPLETING.add(p.getUUID());
        try {
            join.run();
        } finally {
            if (added) {
                COMPLETING.remove(p.getUUID());
            }
        }
    }

    private static void refuseLevelUp(PlayerFactionEvent.FactionLevelChangePre e) {
        if (e.getPlayer().getPlayer() instanceof ServerPlayer p && e.getNewFaction() == VReference.VAMPIRE_FACTION
                && e.getNewLevel() > 1 && isGhoul(p)) {
            e.setCanceled(true);
            p.sendSystemMessage(Component.translatable("supernaturalbloodlines.turning.ghoul_no_level"));
        }
    }

    private static void markSanguinareJoin(PlayerFactionEvent.FactionLevelChangePre e) {
        if (!(e.getPlayer().getPlayer() instanceof ServerPlayer p) || e.getNewFaction() != VReference.VAMPIRE_FACTION
                || e.getNewLevel() != 1 || e.getCurrentFaction() != null || COMPLETING.contains(p.getUUID())) {
            return;
        }
        Holder<MobEffect> sanguinare = BuiltInRegistries.MOB_EFFECT.getHolder(SANGUINARE).orElse(null);
        if (sanguinare != null && p.hasEffect(sanguinare)) {
            BloodlineAccess.applyEvent(p, BloodlineEvent.SANGUINARE_FINISHED, SpeciesContext.of(p, null));
        }
    }

    /**
     * Any way out of the vampire faction (cure, rite, command) ends the Ghoul and the turning that went with it (the rules do it, as
     * LEFT_BASE_FACTION from the vampire faction): a later vampire is not gated by an old flag, and a leftover clock does not kill
     * someone who is human again. Only for a Ghoul: other vampires' species rules are not touched here.
     */
    private static void endOnLeaving(PlayerFactionEvent.FactionLevelChanged e) {
        if (e.getOldFaction() == VReference.VAMPIRE_FACTION && e.getCurrentFaction() != VReference.VAMPIRE_FACTION
                && e.getPlayer().getPlayer() instanceof ServerPlayer p && BloodlineAccess.get(p).ghoul()) {
            BloodlineAccess.applyEvent(p, BloodlineEvent.LEFT_BASE_FACTION,
                    SpeciesContext.of(p, BaseFaction.VAMPIRE, p.level().getGameTime(), null));
        }
    }

    private static void refuseAltar(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !isGhoul(p)
                || !ALTARS.contains(BuiltInRegistries.BLOCK.getKey(e.getLevel().getBlockState(e.getPos()).getBlock()))) {
            return;
        }
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.FAIL);
        if (e.getHand() == InteractionHand.MAIN_HAND) {
            p.sendSystemMessage(Component.translatable("supernaturalbloodlines.turning.ghoul_no_altar"));
        }
    }

    private static void biteVampire(BloodDrinkEvent.PlayerDrinkBloodEvent e) {
        LivingEntity source = e.getBloodSource().getEntity().orElse(null);
        if (!(e.getVampire().getRepresentingEntity() instanceof ServerPlayer p) || source == null
                || !TurningHandler.isFaction(source, VReference.VAMPIRE_FACTION)) {
            return;
        }
        e.setAmount(0);
        e.setSaturationModifier(0);
        int cost = SpeciesConfig.biteVampireCost();
        if (cost > 0) {
            e.getVampire().useBlood(cost, true);
        }
        p.displayClientMessage(Component.translatable("supernaturalbloodlines.turning.vampire_blood_no_fill" + (cost > 0 ? "" : "_free")), true);
    }
}
