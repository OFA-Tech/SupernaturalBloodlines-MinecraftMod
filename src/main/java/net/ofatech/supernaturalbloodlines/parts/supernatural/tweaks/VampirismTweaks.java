package net.ofatech.supernaturalbloodlines.parts.supernatural.tweaks;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionEntity;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig.Tweak;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;

/**
 * The four Vampirism tweaks (R213), all off by default and each a COMMON key with an Admin row. Hooks verified against Vampirism 1.10.13:
 * {@code IVampire.isGettingSundamage}, the {@code vampirism:sunscreen} effect, {@code vampirism:altar_cleansing} (its use is client-side only), and the
 * target goals Vampirism and Werewolves swap at {@code EntityJoinLevelEvent}. No mixin.
 */
public final class VampirismTweaks {
    public static final ResourceLocation ALTAR = ResourceLocation.fromNamespaceAndPath("vampirism", "altar_cleansing");
    public static final ResourceLocation SUNSCREEN = ResourceLocation.fromNamespaceAndPath("vampirism", "sunscreen");
    private static final int SUN_PERIOD = 20;
    private static final int FIRE_SECONDS = 4;

    /** The goals this class adds, so a mob that joins twice gets each only once and a test can find them. */
    public static final class TweakGoal extends NearestAttackableTargetGoal<Player> {
        public final Tweak tweak;
        private final Predicate<LivingEntity> picks;

        TweakGoal(Mob mob, Tweak tweak, Predicate<LivingEntity> picks) {
            super(mob, Player.class, 10, true, false, picks);
            this.tweak = tweak;
            this.picks = picks;
        }

        /** Whether this goal would choose {@code target} (the rest of the choice is the goal's usual sight and range). */
        public boolean picks(LivingEntity target) {
            return picks.test(target);
        }
    }

    private VampirismTweaks() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % SUN_PERIOD == 0) {
                igniteIfSunlit(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(VampirismTweaks::onAltarClick);
        // after Vampirism and Werewolves have swapped their own goals, so ours is the last word
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, VampirismTweaks::onJoin);
    }

    // ---- sunlight ----

    /** The switch is on and {@code p} is a living vampire player who is being hurt by the sun, with no Sun Ring, not in water: set on fire. Vampirism turns the fire into its own x3 damage. */
    public static boolean igniteIfSunlit(ServerPlayer p) {
        if (!SupernaturalConfig.tweak(Tweak.SUNLIGHT_IGNITES_VAMPIRES) || !p.isAlive() || p.isCreative() || p.isSpectator() || !VampirismSupernatural.isVampire(p)
                || p.isInWater() || hasEffect(p, SUNSCREEN)) {
            return false;
        }
        if (!VampirismAPI.vampirePlayer(p).isGettingSundamage(p.level(), true)) {
            return false;
        }
        p.igniteForSeconds(FIRE_SECONDS);
        return true;
    }

    private static boolean hasEffect(LivingEntity e, ResourceLocation id) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(id).map(h -> e.hasEffect(h)).orElse(false);
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

    // ---- who attacks whom ----

    private static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof Mob mob) || mob instanceof IFactionEntity) {
            return;
        }
        boolean skeleton = mob instanceof AbstractSkeleton;
        if (SupernaturalConfig.tweak(Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES) && (skeleton || mob instanceof Zombie)) {
            addOnce(mob, Tweak.HOSTILE_MOBS_ATTACK_VAMPIRES, t -> t instanceof Player p && VampirismSupernatural.isVampire(p));
        }
        if (skeleton && SupernaturalConfig.tweak(Tweak.SKELETONS_ATTACK_WEREWOLVES)) {
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
