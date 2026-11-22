package net.ofatech.supernaturalbloodlines.parts.species.original;

import net.ofatech.supernaturalbloodlines.shared.supernatural.TaskSignals;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;

/**
 * Our own copy of Vampirism's dying state, for Original hybrids (the werewolf faction has no such state, and
 * Vampirism's only works for vampires). A downed player cannot move, jump, attack, use or break anything and takes no
 * damage that the Original rules protect against; after {@link #TICKS} they get up at half health, or they give up and
 * die from the blow that downed them. The client gets a Give up / Wait screen.
 */
public final class Downed {
    public static final int TICKS = 600;
    /** Downed because a lethal blow found an Original hybrid (Give up kills). */
    public static final int MODE_DYING = 0;
    /** Downed because a lethal blow found vampire blood in a mortal (plan 18b): the clock of the turning stands still, Give up ends the turning too. */
    public static final int MODE_TURNING = 1;
    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "downed");

    /** What downed each player; lost on relog, then giving up kills with a plain /kill source. */
    private static final Map<UUID, DamageSource> SOURCES = new ConcurrentHashMap<>();
    /** Players whose own death is on its way through: the Original rules must let it happen. */
    private static final Set<UUID> LETTING_DIE = ConcurrentHashMap.newKeySet();

    private Downed() {}

    public static boolean isDowned(Player p) {
        return p.getData(SpeciesRegistries.DOWNED) > 0;
    }

    /** What the client is told: open (or close) the screen, and which text it shows. */
    public static DownedPayload payload(Player p, boolean downed) {
        return new DownedPayload(downed, mode(p));
    }

    public static int mode(Player p) {
        return p.getData(SpeciesRegistries.DOWNED_MODE);
    }

    /** Downed for the turning: they have vampire blood in them, died, and stand up where they fell. */
    public static boolean isDyingForTurning(Player p) {
        return isDowned(p) && mode(p) == MODE_TURNING;
    }

    public static void startDying(ServerPlayer p, DamageSource cause) {
        start(p, cause, MODE_TURNING);
    }

    static void start(ServerPlayer p, DamageSource cause) {
        start(p, cause, MODE_DYING);
    }

    private static void start(ServerPlayer p, DamageSource cause, int mode) {
        p.setData(SpeciesRegistries.DOWNED_MODE, mode);
        p.setData(SpeciesRegistries.DOWNED, TICKS);
        SOURCES.put(p.getUUID(), cause);
        p.setHealth(1.0F);
        p.stopUsingItem();
        p.setSprinting(false);
        p.stopRiding();
        p.stopFallFlying();
        hold(p, true);
    }

    /** The screen is re-sent this often, so a player who chose Wait (or closed it) gets the choice back. */
    public static final int REOPEN_EVERY = 100;

    public static boolean reopensScreenAt(int ticksLeft) {
        return ticksLeft % REOPEN_EVERY == 0;
    }

    /** Once per server tick for every player. */
    public static void tick(ServerPlayer p) {
        int left = p.getData(SpeciesRegistries.DOWNED);
        if (left <= 0) {
            return;
        }
        if (left == 1) {
            boolean turning = isDyingForTurning(p);
            clear(p);
            p.setHealth(p.getMaxHealth() / 2.0F);
            if (turning) {
                p.displayClientMessage(Component.translatable("supernaturalbloodlines.turning.woke"), true);
            }
            TaskSignals.fire(p, "dying_survived", 1);
        } else {
            p.setData(SpeciesRegistries.DOWNED, left - 1);
            if (reopensScreenAt(left - 1) && canReceive(p)) {
                PacketDistributor.sendToPlayer(p, payload(p, true));
            }
        }
    }

    /** The player chose to give up: they die from the blow that downed them. */
    public static void giveUp(ServerPlayer p) {
        if (!isDowned(p)) {
            return;
        }
        DamageSource cause = SOURCES.getOrDefault(p.getUUID(), p.damageSources().genericKill());
        if (isDyingForTurning(p)) {
            BloodlineAccess.applyEvent(p, BloodlineEvent.TURNING_GAVE_UP, SpeciesContext.of(p, null)); // a given-up turning is over: this death is a plain one
        }
        clear(p);
        kill(p, cause);
    }

    /** Ends the state without healing: the player got up, died some other way, or is about to die. */
    static void clear(ServerPlayer p) {
        if (!isDowned(p)) {
            return;
        }
        p.setData(SpeciesRegistries.DOWNED, 0);
        p.setData(SpeciesRegistries.DOWNED_MODE, MODE_DYING);
        SOURCES.remove(p.getUUID());
        hold(p, false);
    }

    /** Ends the state, for a test's cleanup. */
    public static void clearForTest(ServerPlayer p) {
        clear(p);
    }

    /** Kills {@code p} with {@code cause}, past the Original rules. */
    static void kill(ServerPlayer p, DamageSource cause) {
        letDie(p, () -> {
            p.setHealth(0.0F);
            p.die(cause);
        });
    }

    static void letDie(ServerPlayer p, Runnable action) {
        UUID id = p.getUUID();
        boolean outer = LETTING_DIE.add(id);
        try {
            action.run();
        } finally {
            if (outer) {
                LETTING_DIE.remove(id);
            }
        }
    }

    static boolean isLettingDie(ServerPlayer p) {
        return LETTING_DIE.contains(p.getUUID());
    }

    /** The movement lock, the pose and the screen; idempotent. */
    private static void hold(ServerPlayer p, boolean downed) {
        lock(p, Attributes.MOVEMENT_SPEED, downed);
        lock(p, Attributes.JUMP_STRENGTH, downed);
        p.setForcedPose(downed ? Pose.SLEEPING : null);
        if (canReceive(p)) {
            PacketDistributor.sendToPlayer(p, payload(p, downed));
        }
    }

    private static void lock(ServerPlayer p, net.minecraft.core.Holder<Attribute> attribute, boolean on) {
        AttributeInstance a = p.getAttribute(attribute);
        if (a == null) {
            return;
        }
        if (on && !a.hasModifier(MODIFIER_ID)) {
            a.addTransientModifier(new AttributeModifier(MODIFIER_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!on) {
            a.removeModifier(MODIFIER_ID);
        }
    }

    // fake and mock players have no connection to send to
    private static boolean canReceive(ServerPlayer p) {
        return !(p instanceof FakePlayer) && p.connection != null && p.connection.hasChannel(DownedPayload.TYPE);
    }

    // ---- events ----

    static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                tick(p);
            }
        });
        // the modifiers and the pose do not survive a relog, the counter does
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && isDowned(p)) {
                hold(p, true);
            }
        });
        // a player dying for the turning who is killed for real (void, /kill): the state ends with them (an Original's own is ended by OriginalRules)
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, LivingDeathEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p && isDyingForTurning(p) && !isLettingDie(p) && OriginalRules.bypassesProtection(e.getSource())) {
                clear(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> SOURCES.remove(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((AttackEntityEvent e) -> deny(e.getEntity(), e));
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem e) -> deny(e.getEntity(), e));
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> deny(e.getEntity(), e));
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.LeftClickBlock e) -> deny(e.getEntity(), e));
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract e) -> deny(e.getEntity(), e));
        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent e) -> deny(e.getPlayer(), e));
        NeoForge.EVENT_BUS.addListener((LivingEntityUseItemEvent.Start e) -> deny(e.getEntity(), e));
    }

    private static void deny(Entity who, ICancellableEvent event) {
        if (who instanceof Player p && isDowned(p)) {
            event.setCanceled(true);
        }
    }
}
