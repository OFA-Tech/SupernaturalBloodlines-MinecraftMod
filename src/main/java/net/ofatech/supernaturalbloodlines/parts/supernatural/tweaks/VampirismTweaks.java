package net.ofatech.supernaturalbloodlines.parts.supernatural.tweaks;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.entity.factions.IFactionEntity;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig.Tweak;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;

/**
 * The four Vampirism tweaks (R213), all off by default and each a COMMON key with an Admin row. Hooks verified against Vampirism 1.10.13:
 * the {@code vampirism:sun_damage} damage type, {@code vampirism:altar_cleansing} (its use is client-side only, then the REVERT_BACK packet) with
 * {@code PlayerFactionEvent.FactionLevelChangePre}, and the target goals Vampirism and Werewolves swap at {@code EntityJoinLevelEvent}. No mixin.
 */
public final class VampirismTweaks {
    public static final ResourceLocation ALTAR = ResourceLocation.fromNamespaceAndPath("vampirism", "altar_cleansing");
    public static final ResourceLocation SUNSCREEN = ResourceLocation.fromNamespaceAndPath("vampirism", "sunscreen");
    public static final ResourceKey<DamageType> SUN_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("vampirism", "sun_damage"));
    private static final int FIRE_SECONDS = 4;

    private static final String PAYLOAD_HANDLER = "de.teamlapen.vampirism.server.ServerPayloadHandler";
    private static final String REVERT_BACK_MENU = "de.teamlapen.vampirism.inventory.RevertBackMenu";
    private static final StackWalker WALKER = StackWalker.getInstance();

    /** Exact types, so a zombified piglin, a wither skeleton or a bogged (other families, other rules) are left alone. */
    private static final Set<EntityType<?>> ZOMBIES = Set.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER);
    private static final Set<EntityType<?>> SKELETONS = Set.of(EntityType.SKELETON, EntityType.STRAY);

    /** The goals this class adds, so a mob that joins twice gets each only once and a test can find them. They always exist on the matching types; the switch is asked at every use. */
    public static final class TweakGoal extends NearestAttackableTargetGoal<Player> {
        public final Tweak tweak;
        private final Predicate<LivingEntity> picks;

        TweakGoal(Mob mob, Tweak tweak, Predicate<LivingEntity> picks) {
            super(mob, Player.class, 10, true, false, picks);
            this.tweak = tweak;
            this.picks = picks;
        }

        @Override
        public boolean canUse() {
            return SupernaturalConfig.tweak(tweak) && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return SupernaturalConfig.tweak(tweak) && super.canContinueToUse();
        }

        /** Whether this goal would choose {@code target} (the rest of the choice is the goal's usual sight and range). */
        public boolean picks(LivingEntity target) {
            return picks.test(target);
        }
    }

    private VampirismTweaks() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(VampirismTweaks::onDamaged);
        NeoForge.EVENT_BUS.addListener(VampirismTweaks::onAltarClick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, VampirismTweaks::onLeaveFaction);
        // after Vampirism and Werewolves have swapped their own goals, so ours is the last word
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, VampirismTweaks::onJoin);
    }

    // ---- sunlight ----

    /**
     * Vampirism's own sun damage landed on a player (so its level gate, grace period, attributes, Sun Ring, poppet, umbrella and creative all said yes): the switch
     * sets them on fire as well. Vampirism turns the fire into its own x3 damage.
     */
    private static void onDamaged(LivingDamageEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getNewDamage() > 0 && e.getSource().is(SUN_DAMAGE) && SupernaturalConfig.tweak(Tweak.SUNLIGHT_IGNITES_VAMPIRES)) {
            p.igniteForSeconds(FIRE_SECONDS);
        }
    }

    // ---- the cleansing altar ----

    private static void onAltarClick(PlayerInteractEvent.RightClickBlock e) {
        if (!SupernaturalConfig.tweak(Tweak.CLEANSING_ALTAR_DISABLED)
                || !BuiltInRegistries.BLOCK.getKey(e.getLevel().getBlockState(e.getPos()).getBlock()).equals(ALTAR)) {
            return;
        }
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.FAIL);
        if (e.getHand() == InteractionHand.MAIN_HAND) {
            e.getEntity().displayClientMessage(Component.translatable("supernaturalbloodlines.tweaks.altar_disabled"), true);
        }
    }

    /**
     * The altar is a client screen that sends Vampirism's REVERT_BACK packet, so a client without this mod (or on the old protocol) never hears the switch. The
     * server closes it here: a vampire leaving the faction by that packet without Vampirism's med chair menu open is refused. The packet handler still
     * kills a player who is not in hardcore, so a cheater dies and stays a vampire (R213). Anything else that removes the faction (commands, our rites) is not touched.
     */
    private static void onLeaveFaction(PlayerFactionEvent.FactionLevelChangePre e) {
        if (!SupernaturalConfig.tweak(Tweak.CLEANSING_ALTAR_DISABLED) || e.getNewFaction() != null || e.getCurrentFaction() != VReference.VAMPIRE_FACTION) {
            return;
        }
        Player player = e.getPlayer().asEntity();
        if (player.containerMenu.getClass().getName().equals(REVERT_BACK_MENU) || !fromRevertBackPacket()) {
            return;
        }
        e.setCanceled(true);
        player.displayClientMessage(Component.translatable("supernaturalbloodlines.tweaks.altar_disabled"), true);
    }

    /** Whether this change was made inside Vampirism's handler of its simple input packets (the lambda it queues is {@code lambda$handleSimpleInputEvent$n}). */
    private static boolean fromRevertBackPacket() {
        return WALKER.walk(frames -> frames.anyMatch(f -> f.getClassName().equals(PAYLOAD_HANDLER) && f.getMethodName().contains("handleSimpleInputEvent")));
    }

    // ---- who attacks whom ----

    private static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof Mob mob) || mob instanceof IFactionEntity) {
            return;
        }
        boolean skeleton = SKELETONS.contains(mob.getType());
        if (skeleton || ZOMBIES.contains(mob.getType())) {
            addOnce(mob, Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES, t -> t instanceof Player p && VampirismSupernatural.isVampire(p));
        }
        if (skeleton) {
            addOnce(mob, Tweak.SKELETONS_ATTACK_WEREWOLVES, t -> t instanceof Player p && VampirismSupernatural.isWerewolf(p));
        }
    }

    private static void addOnce(Mob mob, Tweak tweak, Predicate<LivingEntity> picks) {
        for (WrappedGoal g : mob.targetSelector.getAvailableGoals()) {
            if (g.getGoal() instanceof TweakGoal t && t.tweak == tweak) {
                return;
            }
        }
        mob.targetSelector.addGoal(2, new TweakGoal(mob, tweak, picks));
    }
}
