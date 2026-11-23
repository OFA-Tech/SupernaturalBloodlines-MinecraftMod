package net.ofatech.supernaturalbloodlines.parts.species.original;

import net.ofatech.supernaturalbloodlines.shared.supernatural.TaskSignals;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FluidState;
import net.ofatech.supernaturalbloodlines.core.death.FinalDeath;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackDuel;
import org.jetbrains.annotations.Nullable;
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
    /** Snapped by Neck Snap (plan 18d): out cold for a while, no health lost, wakes on their own at the health they had, no Give up. */
    public static final int MODE_SNAPPED = 2;
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
        return new DownedPayload(downed, mode(p), downed ? p.getData(SpeciesRegistries.DOWNED) : 0);
    }

    public static int mode(Player p) {
        return p.getData(SpeciesRegistries.DOWNED_MODE);
    }

    /** Downed for the turning: they have vampire blood in them, died, and stand up where they fell. */
    public static boolean isDyingForTurning(Player p) {
        return isDowned(p) && mode(p) == MODE_TURNING;
    }

    public static boolean isSnapped(Player p) {
        return isDowned(p) && mode(p) == MODE_SNAPPED;
    }

    /**
     * Everything the player had going ends: Vampirism's and Werewolves' lasting actions (a bat form, rage), flying, and a bite that is running. Their own keys
     * are refused while they are down (see the listener in {@link #register}), so nothing starts again.
     */
    private static void stopWhatTheyWereDoing(ServerPlayer p) {
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
        de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().ifPresent(f -> f.getActionHandler().deactivateAllActions());
        net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions.stop(p);
    }

    /** They can do nothing: downed (dying, for the turning, snapped) or in Vampirism's own dying state. No extra action and no bite runs for them. */
    public static boolean cannotAct(Player p) {
        return isDowned(p) || p instanceof ServerPlayer sp && de.teamlapen.vampirism.api.VampirismAPI.vampirePlayer(sp).isDBNO();
    }

    /** Neck Snap: they lie as if dead for {@code ticks}, unhurt, and wake with the health they have now. */
    public static void startSnapped(ServerPlayer p, int ticks) {
        p.setData(SpeciesRegistries.DOWNED_MODE, MODE_SNAPPED);
        p.setData(SpeciesRegistries.DOWNED, Math.max(2, ticks));
        SOURCES.put(p.getUUID(), p.damageSources().genericKill());
        p.stopUsingItem();
        p.setSprinting(false);
        p.stopRiding();
        p.stopFallFlying();
        stopWhatTheyWereDoing(p);
        hold(p, true);
        if (!canReceive(p)) {
            // an old client has no screen for it: tell them on the action bar
            p.displayClientMessage(Component.translatable("supernaturalbloodlines.snapped.body_short", Math.max(2, ticks) / 20), true);
        }
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
        stopWhatTheyWereDoing(p);
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
            boolean snapped = isSnapped(p);
            clear(p);
            if (snapped) {
                // with the health they had (a snap costs none), but with the same safety as the turning's wake: fire out, air back, a few seconds of Resistance, out of lava
                p.displayClientMessage(Component.translatable("supernaturalbloodlines.snapped.woke"), true);
                p.sendSystemMessage(Component.translatable("supernaturalbloodlines.snapped.woke"));
                wakeSafety(p);
                net.ofatech.supernaturalbloodlines.parts.species.item.SnappedEffect.startGrace(p);
                return;
            }
            p.setHealth(p.getMaxHealth() / 2.0F);
            if (turning) {
                woke(p);
            }
            TaskSignals.fire(p, "dying_survived", 1);
        } else {
            p.setData(SpeciesRegistries.DOWNED, left - 1);
            if (!isSnapped(p) && reopensScreenAt(left - 1)) { // a snapped player closed it by choice (Wait): no re-open spam
                send(p, payload(p, true));
            } else if (isSnapped(p) && reopensScreenAt(left - 1) && !canReceive(p)) {
                // an old client has no screen: the action-bar line is the only thing they see, and it fades, so it is said again
                p.displayClientMessage(Component.translatable("supernaturalbloodlines.snapped.body_short", (left - 1 + 19) / 20), true);
            }
        }
    }

    /**
     * They stand up in the turning (ours, or a Ghoul's end of Vampirism's dying state): told on the action bar and in chat how long the turning runs and
     * how to finish it, 5 s of Resistance V, the fire out and the air back, and out of lava (or another harmful fluid) to the nearest spot within 8 blocks (3 up).
     */
    public static void woke(ServerPlayer p) {
        p.displayClientMessage(Component.translatable("supernaturalbloodlines.turning.woke"), true);
        int minutes = (int) Math.ceil(BloodlineAccess.get(p).turningTicksLeft() / 1200.0);
        p.sendSystemMessage(Component.translatable("supernaturalbloodlines.turning.woke_chat", minutes));
        wakeSafety(p);
    }

    /** What every wake-up gives: 5 s of Resistance V, the fire out, the air back, and out of lava (or another harmful fluid) to the nearest safe spot. */
    private static void wakeSafety(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, WAKE_RESISTANCE_TICKS, 4));
        p.clearFire();
        p.setAirSupply(p.getMaxAirSupply());
        p.fallDistance = 0;
        if (inHarmfulFluid(p)) {
            // within 8 blocks sideways and 3 up, in line of sight (the duel's rescue). There is no claims check: this mod has no helper for one (Open Parties and
            // Claims is reflected for parties only), so a claim border near the lava can be crossed.
            BlockPos safe = PackDuel.safeSpot(p.serverLevel(), p.blockPosition(), p, 8, 3);
            if (safe != null) {
                p.teleportTo(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
            }
        }
    }

    public static final int WAKE_RESISTANCE_TICKS = 100;

    /** In lava or any fluid that is not water (feet or head). */
    private static boolean inHarmfulFluid(ServerPlayer p) {
        for (BlockPos pos : new BlockPos[] {p.blockPosition(), BlockPos.containing(p.getEyePosition())}) {
            FluidState fluid = p.level().getFluidState(pos);
            if (!fluid.isEmpty() && !fluid.is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    /** The player chose to give up: they die from the blow that downed them. */
    public static void giveUp(ServerPlayer p) {
        if (!isDowned(p) || isSnapped(p)) {
            return; // out cold: there is no choice to make
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
        // Give up is final: the Elixir and the Resurrection Ring do not take it back
        FinalDeath.run(p, () -> letDie(p, () -> {
            p.setHealth(0.0F);
            p.die(cause);
        }));
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
        send(p, payload(p, downed));
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

    private static volatile BiConsumer<ServerPlayer, DownedPayload> sender = Downed::sendToClient;

    private static void send(ServerPlayer p, DownedPayload payload) {
        sender.accept(p, payload);
    }

    private static void sendToClient(ServerPlayer p, DownedPayload payload) {
        if (canReceive(p)) {
            PacketDistributor.sendToPlayer(p, payload);
        }
    }

    /** For tests: what is sent to the client goes to {@code to} instead (null puts the real sender back). */
    public static void overrideSender(@Nullable BiConsumer<ServerPlayer, DownedPayload> to) {
        sender = to == null ? Downed::sendToClient : to;
    }

    // fake and mock players have no connection to send to
    private static boolean canReceive(ServerPlayer p) {
        return !(p instanceof FakePlayer) && p.connection != null && p.connection.hasChannel(DownedPayload.TYPE);
    }

    // ---- events ----

    static void register() {
        // a downed, snapped or Vampirism-dying player runs no extra action (the HUD asks), and no bite or faction action either (ActionService asks first of all)
        net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions.addBlocker(Downed::cannotAct);
        // Vampirism's and Werewolves' own keys (a bat form, rage, ...) go through this event: none starts for a player who cannot act
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (de.teamlapen.vampirism.api.event.ActionEvent.ActionActivatedEvent e) -> {
            if (e.getFactionPlayer().getRepresentingPlayer() instanceof ServerPlayer sp && cannotAct(sp)) {
                e.setCanceled(true);
            }
        });
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
            if (e.getEntity() instanceof ServerPlayer p && (isDyingForTurning(p) || isSnapped(p)) && !isLettingDie(p) && OriginalRules.bypassesProtection(e.getSource())) {
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
