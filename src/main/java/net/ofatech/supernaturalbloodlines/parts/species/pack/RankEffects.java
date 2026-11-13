package net.ofatech.supernaturalbloodlines.parts.species.pack;

import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
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
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
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

    // the lunar eclipse takes the perks away (plan 9, task 5): a source says "not now" and calls sync when that changes
    private static final List<Predicate<ServerPlayer>> SUPPRESSORS = new CopyOnWriteArrayList<>();

    private RankEffects() {
    }

    public static void register() {
        BloodlineAccess.addListener((p, change) -> sync(p));
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

    public static void addSuppressor(Predicate<ServerPlayer> suppressed) {
        SUPPRESSORS.add(suppressed);
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
        Rank rank = werewolf && SUPPRESSORS.stream().noneMatch(s -> s.test(p)) ? PackAccess.wolf(p).rank() : Rank.NONE;
        double scale = rank == Rank.TRUE_ALPHA ? TRUE_ALPHA_SCALE : 1;
        boolean alpha = rank == Rank.ALPHA || rank == Rank.TRUE_ALPHA;
        set(p, Attributes.ATTACK_DAMAGE, rank == Rank.OMEGA ? OMEGA_PENALTY : alpha ? round(ALPHA_ATTACK * scale) : 0, Operation.ADD_MULTIPLIED_TOTAL);
        set(p, Attributes.MOVEMENT_SPEED, rank == Rank.OMEGA ? OMEGA_PENALTY : 0, Operation.ADD_MULTIPLIED_TOTAL);
        set(p, Attributes.MAX_HEALTH, alpha ? round(ALPHA_HEALTH * scale) : 0, Operation.ADD_VALUE);
        if (p.getHealth() > p.getMaxHealth()) {
            p.setHealth(p.getMaxHealth());
        }
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
