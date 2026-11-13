package net.ofatech.supernaturalbloodlines.parts.species.original;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
import net.neoforged.bus.api.ICancellableEvent;
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
    private static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "downed");

    /** What downed each player; lost on relog, then giving up kills with a plain /kill source. */
    private static final Map<UUID, DamageSource> SOURCES = new ConcurrentHashMap<>();
    /** Players whose own death is on its way through: the Original rules must let it happen. */
    private static final Set<UUID> LETTING_DIE = ConcurrentHashMap.newKeySet();

    private Downed() {}

    public static boolean isDowned(Player p) {
        return p.getData(SpeciesRegistries.DOWNED) > 0;
    }

    static void start(ServerPlayer p, DamageSource cause) {
        p.setData(SpeciesRegistries.DOWNED, TICKS);
        SOURCES.put(p.getUUID(), cause);
        p.setHealth(1.0F);
        p.stopUsingItem();
        p.setSprinting(false);
        hold(p, true);
    }

    /** Once per server tick for every player. */
    public static void tick(ServerPlayer p) {
        int left = p.getData(SpeciesRegistries.DOWNED);
        if (left <= 0) {
            return;
        }
        if (left == 1) {
            clear(p);
            p.setHealth(p.getMaxHealth() / 2.0F);
        } else {
            p.setData(SpeciesRegistries.DOWNED, left - 1);
        }
    }

    /** The player chose to give up: they die from the blow that downed them. */
    public static void giveUp(ServerPlayer p) {
        if (!isDowned(p)) {
            return;
        }
        DamageSource cause = SOURCES.getOrDefault(p.getUUID(), p.damageSources().genericKill());
        clear(p);
        kill(p, cause);
    }

    /** Ends the state without healing: the player got up, died some other way, or is about to die. */
    static void clear(ServerPlayer p) {
        if (!isDowned(p)) {
            return;
        }
        p.setData(SpeciesRegistries.DOWNED, 0);
        SOURCES.remove(p.getUUID());
        hold(p, false);
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
            PacketDistributor.sendToPlayer(p, new DownedPayload(downed));
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
