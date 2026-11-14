package net.ofatech.supernaturalbloodlines.parts.species.sireline;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.CompulsionImmunity;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Compulsion: sneak and click with an empty hand. The caster is an Original, a hybrid or a tribrid, or a Vampirism vampire from level
 * 10; the target is within 4 blocks and the two are eye to eye (the target looks within 30 degrees of the caster, the caster within 15 of
 * the target). Players, vervain users and hunters are immune ({@link CompulsionImmunity}).
 * <ul>
 * <li>A villager: the click opens a five second choice in chat, [Discount] or [Follow] (a click on the buttons runs
 * {@code /bloodline compel <token> <choice>}). The discount is 20 of the villager's MAJOR_POSITIVE gossip about the caster for a day,
 * and what it added is written down on the villager ({@link VillagerDiscount}) and taken back exactly, lazily: when anyone
 * clicks the villager and every 200 ticks of its own. Follow is five minutes of {@link CallFollowers}.</li>
 * <li>A lesser Vampirism vampire (the {@code vampirism:vampire} tag without advanced vampires, bosses and converted creatures): it is
 * sired by the caster through {@link SireRecorder} and never targets the caster or the caster's line.</li>
 * </ul>
 */
public final class Compulsion {
    public static final long CHOICE_TICKS = 5 * 20;
    public static final long DISCOUNT_TICKS = 24000;
    public static final long FOLLOW_TICKS = 5 * 60 * 20;
    static final int DISCOUNT_GOSSIP = 20;
    static final long GUARD_TICKS = 20;
    static final double REACH = 4;
    static final double CHOICE_REACH = 8;
    static final double TARGET_CONE = 30;
    static final double CASTER_CONE = 15;
    static final int VAMPIRE_LEVEL = 10;
    private static final int EXPIRY_CHECK_TICKS = 200;
    private static final Set<Species> SPECIES = EnumSet.of(Species.ORIGINAL, Species.ORIGINAL_HYBRID, Species.HYBRID, Species.TRIBRID);
    private static final String KEY = "supernaturalbloodlines.bloodline.compel.";

    private static final TagKey<EntityType<?>> VAMPIRE = tag("vampire");
    private static final TagKey<EntityType<?>> ADVANCED_VAMPIRE = tag("advanced_vampire");
    private static final TagKey<EntityType<?>> CONVERTED = tag("converted_creatures");
    // the tag has no boss tag of its own: the baron fights back, the task master hands out tasks
    private static final Set<ResourceLocation> NOT_LESSER = Set.of(
            ResourceLocation.fromNamespaceAndPath("vampirism", "vampire_baron"),
            ResourceLocation.fromNamespaceAndPath("vampirism", "task_master_vampire"));

    private record Choice(UUID caster, UUID villager, long expires) {
    }

    // server thread only
    private static final Map<String, Choice> CHOICES = new HashMap<>();
    private static final Map<UUID, Long> LAST_TRY = new HashMap<>();

    private Compulsion() {
    }

    private static TagKey<EntityType<?>> tag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("vampirism", name));
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Compulsion::onInteract);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, Compulsion::onChangeTarget);
        NeoForge.EVENT_BUS.addListener((EntityTickEvent.Post e) -> {
            if (e.getEntity() instanceof Villager v && v.tickCount % EXPIRY_CHECK_TICKS == 0 && !v.level().isClientSide && v.hasData(SpeciesRegistries.VILLAGER_DISCOUNT)) {
                expireDiscount(v, v.level().getGameTime());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            forgetChoices(e.getEntity().getUUID());
            clearGuard(e.getEntity().getUUID());
        });
    }

    // ---- who, whom, and how closely ----

    static boolean mayCompel(ServerPlayer p) {
        if (SPECIES.contains(BloodlineAccess.get(p).species())) {
            return true;
        }
        IFactionPlayerHandler h = VampirismAPI.factionPlayerHandler(p);
        return h.getCurrentFaction() == VReference.VAMPIRE_FACTION && h.getCurrentLevel() >= VAMPIRE_LEVEL;
    }

    static boolean isLesserVampire(Entity e) {
        EntityType<?> type = e.getType();
        return e instanceof Mob && type.is(VAMPIRE) && !type.is(ADVANCED_VAMPIRE) && !type.is(CONVERTED)
                && !NOT_LESSER.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    /** Whether something at {@code from} facing {@code look} has {@code to} within {@code maxDegrees} of where it looks. */
    @VisibleForTesting
    public static boolean looksAt(Vec3 look, Vec3 from, Vec3 to, double maxDegrees) {
        Vec3 toward = to.subtract(from);
        double length = toward.length();
        if (length < 1.0E-6 || look.lengthSqr() < 1.0E-12) {
            return false;
        }
        return look.normalize().dot(toward) / length >= Math.cos(Math.toRadians(maxDegrees));
    }

    // the head, not the body: a villager turns its head to a player and leaves its body where it was
    private static Vec3 gaze(LivingEntity e) {
        return e.calculateViewVector(e.getXRot(), e.getYHeadRot());
    }

    private static boolean eyeToEye(ServerPlayer p, LivingEntity t) {
        return p.distanceToSqr(t) <= REACH * REACH
                && looksAt(gaze(t), t.getEyePosition(), p.getEyePosition(), TARGET_CONE)
                && looksAt(gaze(p), p.getEyePosition(), t.getEyePosition(), CASTER_CONE);
    }

    // ---- the click ----

    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        Entity target = e.getTarget();
        long now = p.serverLevel().getGameTime();
        if (target instanceof Villager v) {
            expireDiscount(v, now);
        }
        boolean villager = target instanceof Villager;
        if (!p.isShiftKeyDown() || !p.getMainHandItem().isEmpty() || !(villager || isLesserVampire(target)) || !mayCompel(p)) {
            return;
        }
        // from here the click is the compulsion's, and no trade screen opens whatever comes of it
        e.setCancellationResult(InteractionResult.SUCCESS);
        e.setCanceled(true);
        Long last = LAST_TRY.get(p.getUUID());
        if (last != null && now - last < GUARD_TICKS) {
            return;
        }
        LAST_TRY.put(p.getUUID(), now);
        LivingEntity victim = (LivingEntity) target;
        if (CompulsionImmunity.isImmune(victim)) {
            p.sendSystemMessage(Component.translatable(KEY + "immune", victim.getDisplayName()));
        } else if (!eyeToEye(p, victim)) {
            p.sendSystemMessage(Component.translatable(KEY + "no_contact"));
        } else if (villager) {
            offer(p, (Villager) victim, now);
        } else {
            bind(p, (Mob) victim);
        }
    }

    @VisibleForTesting
    public static void clearGuard(UUID caster) {
        LAST_TRY.remove(caster);
    }

    // ---- villagers ----

    private static void offer(ServerPlayer p, Villager v, long now) {
        forgetChoices(p.getUUID());
        CHOICES.values().removeIf(c -> now > c.expires());
        String token = UUID.randomUUID().toString().substring(0, 8);
        CHOICES.put(token, new Choice(p.getUUID(), v.getUUID(), now + CHOICE_TICKS));
        p.sendSystemMessage(Component.translatable(KEY + "choose").append(" ").append(button("discount", token)).append(" ").append(button("follow", token)));
    }

    private static Component button(String choice, String token) {
        return Component.translatable(KEY + choice).withStyle(s -> s.withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bloodline compel " + token + " " + choice))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(KEY + choice + "_hover"))));
    }

    @VisibleForTesting
    public static Optional<String> pendingChoice(UUID caster, UUID villager) {
        return CHOICES.entrySet().stream()
                .filter(e -> e.getValue().caster().equals(caster) && e.getValue().villager().equals(villager))
                .map(Map.Entry::getKey).findFirst();
    }

    public static void forgetChoices(UUID caster) {
        CHOICES.values().removeIf(c -> c.caster().equals(caster));
    }

    /** What {@code /bloodline compel <token> discount|follow} does. @return an error to show, or null when it worked */
    @Nullable
    public static Component choose(ServerPlayer caster, String token, String choice) {
        return choose(caster, token, choice, caster.serverLevel().getGameTime());
    }

    @Nullable
    public static Component choose(ServerPlayer caster, String token, String choice, long now) {
        Choice c = CHOICES.get(token);
        if (c == null || !c.caster().equals(caster.getUUID())) {
            return Component.translatable(KEY + "expired");
        }
        CHOICES.remove(token);
        if (now > c.expires() || !(caster.serverLevel().getEntity(c.villager()) instanceof Villager v) || !v.isAlive()
                || caster.distanceToSqr(v) > CHOICE_REACH * CHOICE_REACH) {
            return Component.translatable(KEY + "expired");
        }
        if (!mayCompel(caster) || CompulsionImmunity.isImmune(v)) {
            return Component.translatable(KEY + "immune", v.getDisplayName());
        }
        if ("discount".equals(choice)) {
            discount(caster, v, now);
            caster.sendSystemMessage(Component.translatable(KEY + "discounted"));
        } else {
            CallFollowers.follow(v, caster.getUUID(), now, FOLLOW_TICKS);
            caster.sendSystemMessage(Component.translatable(KEY + "following"));
        }
        return null;
    }

    private static void discount(ServerPlayer caster, Villager v, long now) {
        expireDiscount(v, Long.MAX_VALUE); // an earlier one is given back first, so the amounts below are against the villager's own gossip
        UUID id = caster.getUUID();
        int before = gossip(v, id);
        v.getGossips().add(id, GossipType.MAJOR_POSITIVE, DISCOUNT_GOSSIP);
        v.setData(SpeciesRegistries.VILLAGER_DISCOUNT, new VillagerDiscount(id, gossip(v, id) - before, now + DISCOUNT_TICKS));
    }

    private static int gossip(Villager v, UUID player) {
        var entries = v.getGossips().getGossipEntries().get(player);
        return entries == null ? 0 : entries.getInt(GossipType.MAJOR_POSITIVE);
    }

    /** Takes back the villager's discount once it has run out ({@code now} is the game time). */
    public static void expireDiscount(Villager v, long now) {
        if (!v.hasData(SpeciesRegistries.VILLAGER_DISCOUNT)) {
            return;
        }
        VillagerDiscount d = v.getData(SpeciesRegistries.VILLAGER_DISCOUNT);
        if (!d.expired(now)) {
            return;
        }
        if (d.amount() > 0) {
            v.getGossips().remove(d.player(), GossipType.MAJOR_POSITIVE, d.amount());
        }
        v.removeData(SpeciesRegistries.VILLAGER_DISCOUNT);
    }

    // ---- lesser vampires ----

    private static void bind(ServerPlayer p, Mob vampire) {
        UUID id = p.getUUID();
        boolean already = vampire.hasData(SpeciesRegistries.SIRE) && id.equals(vampire.getData(SpeciesRegistries.SIRE));
        if (!already && !SireRecorder.record(p, vampire)) {
            p.sendSystemMessage(Component.translatable(KEY + "bound"));
            return;
        }
        vampire.setData(SpeciesRegistries.COMPELLED_BY, id);
        vampire.setPersistenceRequired(); // a sired creature that despawns takes its place in the tree with it
        if (protects(id, vampire.getTarget())) {
            vampire.setTarget(null);
        }
        p.sendSystemMessage(Component.translatable(KEY + "sired", vampire.getDisplayName()));
    }

    /** The caster and everyone below them in the sireline are safe from the creature, except a player whose bond was severed (R101b). */
    @VisibleForTesting
    public static boolean protects(UUID owner, @Nullable LivingEntity target) {
        return target instanceof Player p && (p.getUUID().equals(owner)
                || !BloodlineAccess.get(p).bondBroken() && SirelineIndex.get((ServerLevel) p.level()).isDescendant(owner, p.getUUID()));
    }

    private static void onChangeTarget(LivingChangeTargetEvent e) {
        LivingEntity self = e.getEntity();
        if (!self.level().isClientSide && self.hasData(SpeciesRegistries.COMPELLED_BY) && protects(self.getData(SpeciesRegistries.COMPELLED_BY), e.getNewAboutToBeSetTarget())) {
            e.setNewAboutToBeSetTarget(null);
        }
    }
}
