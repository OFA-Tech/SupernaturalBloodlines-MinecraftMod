package net.ofatech.supernaturalbloodlines.parts.species.pack;

import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;

/**
 * What a rank does to a werewolf (R91): an Omega is -10% attack and speed, an Alpha +20% attack and +4 max health, a True
 * Alpha those perks +10% (+22% and +4.4). One permanent modifier id on each attribute, listed in {@code StalePartModifiers}.
 * Only while the player is in the werewolf faction (a cured wolf keeps the rank but not the perks). Registered always, like
 * {@code HybridTraits}: it links no Werewolves class and strips a modifier saved earlier when the faction is gone.
 */
public final class RankEffects {
    public static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "wolf_rank");
    static final double OMEGA_PENALTY = -0.10;
    static final double ALPHA_ATTACK = 0.20;
    static final double ALPHA_HEALTH = 4;
    static final double TRUE_ALPHA_SCALE = 1.1;

    private RankEffects() {
    }

    public static void register() {
        BloodlineAccess.addListener((p, change) -> apply(p, VampirismSupernatural.isWerewolf(p), change.after().wolf()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> syncIfPlayer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> syncIfPlayer(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> syncIfPlayer(e.getEntity()));
        // the faction is not part of the bloodline state, so a join or a cure is heard here (the event names the new faction)
        NeoForge.EVENT_BUS.addListener((PlayerFactionEvent.FactionLevelChanged e) -> {
            if (e.getPlayer().getPlayer() instanceof ServerPlayer p) {
                apply(p, e.getCurrentFaction() != null && "werewolves:werewolf".equals(e.getCurrentFaction().getID().toString()));
            }
        });
    }

    private static void syncIfPlayer(net.minecraft.world.entity.player.Player p) {
        if (p instanceof ServerPlayer sp) {
            sync(sp);
        }
    }

    /** Idempotent: makes the modifiers match the player's rank and faction. */
    public static void sync(ServerPlayer p) {
        apply(p, VampirismSupernatural.isWerewolf(p));
    }

    private static void apply(ServerPlayer p, boolean werewolf) {
        apply(p, werewolf, PackAccess.wolf(p));
    }

    private static void apply(ServerPlayer p, boolean werewolf, net.ofatech.supernaturalbloodlines.core.bloodline.WolfState wolf) {
        Rank rank = effectiveRank(p, werewolf, wolf);
        double scale = alphaScale(rank, werewolf ? wolf.sparks() : -1, SpeciesConfig.maxStackedSparks());
        boolean alpha = rank == Rank.ALPHA || rank == Rank.TRUE_ALPHA;
        set(p, Attributes.ATTACK_DAMAGE, rank == Rank.OMEGA ? OMEGA_PENALTY : alpha ? round(ALPHA_ATTACK * scale) : 0, Operation.ADD_MULTIPLIED_TOTAL);
        set(p, Attributes.MOVEMENT_SPEED, rank == Rank.OMEGA ? OMEGA_PENALTY : 0, Operation.ADD_MULTIPLIED_TOTAL);
        set(p, Attributes.MAX_HEALTH, alpha ? round(ALPHA_HEALTH * scale) : 0, Operation.ADD_VALUE);
        if (p.getHealth() > p.getMaxHealth()) {
            p.setHealth(p.getMaxHealth());
        }
    }

    /** The rank whose perks are in force for {@code p} now: none outside the werewolf faction, and none while a suppressor (the lunar eclipse) says so. The one rule {@link #apply} and the hub's bonus line share. */
    public static Rank effectiveRank(ServerPlayer p, boolean werewolf, net.ofatech.supernaturalbloodlines.core.bloodline.WolfState wolf) {
        return effectiveRank(werewolf ? wolf.rank() : Rank.NONE, PerkSuppressors.any(p));
    }

    /** A suppressor takes the perks away, not the Omega's penalty. Pure. */
    public static Rank effectiveRank(Rank rank, boolean suppressed) {
        return rank != Rank.OMEGA && suppressed ? Rank.NONE : rank;
    }

    /** What the Alpha bonuses are multiplied by: the True Alpha's 1.1, and +10% for every spark stacked on the first (the Deucalion factor); a negative {@code sparks} means no werewolf faction. */
    public static double alphaScale(Rank rank, int sparks, int cap) {
        double scale = rank == Rank.TRUE_ALPHA ? TRUE_ALPHA_SCALE : 1;
        return sparks < 0 ? scale : scale * PackRules.sparkScale(sparks, cap);
    }

    /** The pack bonus a wolf's rank gives, as the hub words it ("+20% attack, +4 max health"); empty when the rank gives none. The same numbers {@link #apply} sets. */
    public static java.util.Optional<String> bonusText(ServerPlayer p) {
        boolean werewolf = VampirismSupernatural.isWerewolf(p);
        var wolf = PackAccess.wolf(p);
        Rank rank = effectiveRank(p, werewolf, wolf); // the eclipse takes the perks away, so it takes the bonus line too
        if (rank != Rank.ALPHA && rank != Rank.TRUE_ALPHA) {
            return java.util.Optional.empty();
        }
        double scale = alphaScale(rank, wolf.sparks(), SpeciesConfig.maxStackedSparks());
        return java.util.Optional.of(bonusText(round(ALPHA_ATTACK * scale), round(ALPHA_HEALTH * scale)));
    }

    /** {@code attack} is a fraction (0.24), {@code health} hearts-points (4.8): "+24% attack, +4.8 max health". */
    public static String bonusText(double attack, double health) {
        return "+" + trim(Math.round(attack * 1000) / 10.0) + "% attack, +" + trim(health) + " max health";
    }

    private static String trim(double v) {
        return v == Math.rint(v) ? Long.toString((long) v) : Double.toString(Math.round(v * 10) / 10.0);
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }

    private static void set(ServerPlayer p, Holder<Attribute> attribute, double amount, Operation operation) {
        AttributeInstance a = p.getAttribute(attribute);
        if (a == null) {
            return;
        }
        AttributeModifier current = a.getModifier(MODIFIER_ID);
        if (amount == 0) {
            if (current != null) {
                a.removeModifier(MODIFIER_ID);
            }
        } else if (current == null || current.amount() != amount || current.operation() != operation) {
            a.removeModifier(MODIFIER_ID);
            a.addPermanentModifier(new AttributeModifier(MODIFIER_ID, amount, operation));
        }
    }
}
