package net.ofatech.supernaturalbloodlines.parts.species.pack;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackOutcome;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.werewolves.WerewolfFaction;
import org.jetbrains.annotations.Nullable;

/**
 * Only Alphas turn humans (R92). Werewolves' {@code lupus_sanguinem} records no source, so the last werewolf to hurt each
 * player is remembered here (in memory, {@link #WINDOW} ticks): a werewolf player below Alpha cannot infect
 * ({@code Applicable} is denied), an Alpha's bite leaves their id on the victim ({@code pending_alpha}), a wild mob's (or a
 * bite nobody can name: a potion, a command) leaves {@code pending_wild}. When the effect runs out Werewolves joins the
 * player to the faction and {@link #onFaction} turns the pending bite into a lineage, or into the rejected bite's death.
 * A werewolf who is already one (a Beta bitten by another Alpha) never gets a pending bite, so a pack is never switched
 * silently. Touched only behind {@code ModList.isLoaded("werewolves")}.
 */
public final class AlphaBite {
    public static final ResourceKey<DamageType> REJECTED_BITE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "rejected_bite"));
    /** How long a hit counts as the source of an effect that arrives after it. */
    static final long WINDOW = 20;
    private static final ResourceLocation LUPUS = ResourceLocation.parse("werewolves:lupus_sanguinem");
    private static final DoubleSupplier RANDOM = () -> ThreadLocalRandom.current().nextDouble();
    private static volatile DoubleSupplier roll = RANDOM;

    /** The attacker is a mob, or a player whose rank at that moment was (or was not) Alpha. */
    private record Attack(UUID attacker, boolean mob, boolean alpha, long time) {
    }

    private static final Map<UUID, Attack> LAST = new HashMap<>();

    private AlphaBite() {
    }

    public static void register() {
        // LOWEST and not receiving cancelled events: a hit something cancelled is no bite
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LivingIncomingDamageEvent.class, AlphaBite::onHurt);
        NeoForge.EVENT_BUS.addListener(AlphaBite::onApplicable);
        NeoForge.EVENT_BUS.addListener(AlphaBite::onAdded);
        NeoForge.EVENT_BUS.addListener(AlphaBite::onRemoved);
        NeoForge.EVENT_BUS.addListener(AlphaBite::onFaction);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> LAST.remove(e.getEntity().getUUID()));
    }

    /** For tests: makes the death roll deterministic (a value in [0, 1)). */
    public static void useRoll(DoubleSupplier forced) {
        roll = forced;
    }

    public static void resetRoll() {
        roll = RANDOM;
    }

    private static void onHurt(LivingIncomingDamageEvent e) {
        Entity biter = e.getSource().getEntity();
        if (!(e.getEntity() instanceof ServerPlayer victim) || biter == null || !VampirismSupernatural.isWerewolf(biter)) {
            return;
        }
        boolean mob = !(biter instanceof Player);
        boolean alpha = biter instanceof Player p && PackAccess.isAlpha(PackAccess.wolf(p).rank());
        LAST.put(victim.getUUID(), new Attack(biter.getUUID(), mob, alpha, victim.level().getGameTime()));
    }

    @Nullable
    private static Attack recent(ServerPlayer victim) {
        Attack a = LAST.get(victim.getUUID());
        long age = a == null ? -1 : victim.level().getGameTime() - a.time();
        return age >= 0 && age <= WINDOW ? a : null;
    }

    private static boolean isLupus(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        return effect.is(LUPUS);
    }

    private static void onApplicable(MobEffectEvent.Applicable e) {
        if (!(e.getEntity() instanceof ServerPlayer victim) || !isLupus(e.getEffectInstance().getEffect())) {
            return;
        }
        Attack a = recent(victim);
        if (a != null && !a.mob() && !a.alpha()) {
            e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    private static void onAdded(MobEffectEvent.Added e) {
        if (!(e.getEntity() instanceof ServerPlayer victim) || !isLupus(e.getEffectInstance().getEffect())
                || VampirismSupernatural.isWerewolf(victim) || PackAccess.wolf(victim).lineage() != Lineage.NONE
                || !victim.getData(SpeciesRegistries.PENDING_ALPHA).equals(Util.NIL_UUID) || victim.getData(SpeciesRegistries.PENDING_WILD)) {
            return;
        }
        Attack a = recent(victim);
        if (a == null || a.mob()) {
            victim.setData(SpeciesRegistries.PENDING_WILD, true);
        } else if (a.alpha()) {
            victim.setData(SpeciesRegistries.PENDING_ALPHA, a.attacker());
        }
    }

    /** A cure, milk or {@code /effect clear}: the bite will not turn them. Running out is not a removal. */
    private static void onRemoved(MobEffectEvent.Remove e) {
        if (e.getEntity() instanceof ServerPlayer victim && isLupus(e.getEffect())) {
            clearPending(victim);
        }
    }

    private static void clearPending(ServerPlayer p) {
        p.removeData(SpeciesRegistries.PENDING_ALPHA);
        p.removeData(SpeciesRegistries.PENDING_WILD);
    }

    private static void onFaction(PlayerFactionEvent.FactionLevelChanged e) {
        if (!(e.getPlayer().getPlayer() instanceof ServerPlayer p) || p.level().isClientSide
                || e.getCurrentFaction() != WerewolfFaction.faction() || e.getOldFaction() == WerewolfFaction.faction()) {
            return;
        }
        UUID alpha = p.getData(SpeciesRegistries.PENDING_ALPHA);
        boolean wild = p.getData(SpeciesRegistries.PENDING_WILD);
        clearPending(p);
        // someone who already has a lineage (born, or turned before) is not turned again
        if ((alpha.equals(Util.NIL_UUID) && !wild) || PackAccess.wolf(p).lineage() != Lineage.NONE) {
            return;
        }
        if (alpha.equals(Util.NIL_UUID)) {
            PackAccess.applyPack(p, PackEvent.BITTEN_BY_WILD, PackContext.defaults());
        } else if (roll.getAsDouble() < SpeciesConfig.alphaBiteDeathChance()) {
            // after Vampirism's setFactionAndLevel has returned. execute() would run it at once on the server thread; tell() queues it
            p.server.tell(new TickTask(p.server.getTickCount(), () -> reject(p)));
        } else {
            PackContext c = new PackContext(alpha, 0, p.level().getGameTime(), 0, 30L * 24000, 300, true);
            PackOutcome out = PackAccess.applyPack(p, PackEvent.BITTEN_BY_ALPHA, c);
            if (!out.refused()) {
                // the pack index follows the wolf state's packAlpha; the sire is the Alpha (first sire wins)
                SireRecorder.record(alpha, PackAccess.nameOf(p.server, alpha), p);
            }
        }
    }

    /** The body refuses the bite: they leave the werewolves and die of the {@code rejected_bite} (a totem does not save them). */
    private static void reject(ServerPlayer p) {
        VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
        if (!p.isCreative() && !p.isSpectator()) {
            p.hurt(p.damageSources().source(REJECTED_BITE), Float.MAX_VALUE);
            if (p.isAlive()) {
                p.kill();
            }
        }
    }
}
