package net.ofatech.supernaturalbloodlines.parts.species.hybrid;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.parts.species.OutcomeExecutor;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesContext;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.item.UnstableEffect;
import org.jetbrains.annotations.Nullable;

/**
 * The hybrid transition (R40, R45, R64): a werewolf who dies with Original's Blood in them comes back in a
 * transition. Doppelgänger Blood completes it; any other blood makes them unstable and then kills them; running out
 * of time kills them. Only touched behind {@code ModList.isLoaded("werewolves")}; it does not link Werewolves itself.
 *
 * The timer counts online ticks only: it lives in the saved bloodline state ({@code transitionTicksLeft}) and is
 * decremented from the player tick, so a relog resumes it and the state survives death (copyOnDeath).
 */
public final class TransitionHandler {
    /** Drinks that count as "other blood" for a player in transition (Vampirism's blood bottle). */
    public static final TagKey<Item> BLOOD_DRINKS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "blood_drinks"));

    private static final int STEP = 20;

    private TransitionHandler() {
    }

    public static void register() {
        // LOW and not receiving cancelled events: a death that something else prevented is not a death
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, LivingDeathEvent.class, TransitionHandler::onDeath);
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % STEP == 0) {
                secondPassed(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((LivingEntityUseItemEvent.Finish e) -> {
            if (e.getEntity() instanceof ServerPlayer p && e.getItem().is(BLOOD_DRINKS)) {
                drankOtherBlood(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(TransitionHandler::onRightClick);
    }

    /**
     * Vampirism's blood bottle refuses to start a drink below vampire level 1 ({@code use()} passes), so a werewolf
     * never reaches the use/finish events. During a stable transition the click is taken over: the bottle is drunk here.
     */
    static void onRightClick(PlayerInteractEvent.RightClickItem e) {
        ItemStack stack = e.getItemStack();
        BloodlineState s = BloodlineAccess.get(e.getEntity());
        if (!stack.is(BLOOD_DRINKS) || !s.inTransition() || s.unstable()) {
            return;
        }
        e.setCancellationResult(InteractionResult.CONSUME);
        e.setCanceled(true);
        if (e.getEntity() instanceof ServerPlayer p) {
            if (!p.isCreative()) {
                stack.shrink(1);
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (!p.getInventory().add(bottle)) {
                    p.drop(bottle, false);
                }
            }
            drankOtherBlood(p);
        }
    }

    private static void onDeath(LivingDeathEvent e) {
        // a failed transition is not a new dose: the player stays a werewolf
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getSource().is(OutcomeExecutor.FAILED_TRANSITION)) {
            return;
        }
        BloodlineState s = BloodlineAccess.get(p);
        if (s.inTransition() && s.unstable()) {
            // already dying of the instability: end it now, no kill, or they would die a second time after respawning
            apply(p, BloodlineEvent.UNSTABLE_DEATH, null);
            return;
        }
        if (!p.hasEffect(SpeciesRegistries.ORIGINAL_BLOOD_EFFECT)) {
            return;
        }
        UUID source = p.hasData(SpeciesRegistries.BLOOD_SOURCE) ? p.getData(SpeciesRegistries.BLOOD_SOURCE) : null;
        apply(p, BloodlineEvent.DIED_WITH_ORIGINAL_BLOOD, source);
    }

    /** 20 ticks of online time passed for a player. */
    public static void secondPassed(ServerPlayer p) {
        BloodlineState s = BloodlineAccess.get(p);
        if (!s.inTransition()) {
            return;
        }
        if (s.unstable()) {
            // the effect is the clock; without it (it ran out, or was removed) the instability has run its course
            if (!p.hasEffect(SpeciesRegistries.UNSTABLE_EFFECT)) {
                Outcome out = apply(p, BloodlineEvent.UNSTABLE_DEATH, null);
                OutcomeExecutor.run(p, new Outcome(out.state(), List.of(Outcome.Action.KILL_PLAYER), null));
            }
            return;
        }
        long left = s.transitionTicksLeft() - STEP;
        if (left > 0) {
            // saved but not synced: the species does not change, and a packet a second for the whole transition is noise;
            // the next real change (or a login) sends the current value
            p.setData(ModAttachments.BLOODLINE, s.toBuilder().transitionTicksLeft(left).build());
        } else {
            apply(p, BloodlineEvent.TRANSITION_TIMEOUT, null); // the rules ask for the kill
        }
    }

    public static void drankDoppelganger(ServerPlayer p) {
        apply(p, BloodlineEvent.DRANK_DOPPELGANGER_BLOOD, null);
    }

    /** Blood that is not Doppelgänger Blood, drunk during the transition. */
    public static void drankOtherBlood(ServerPlayer p) {
        boolean wasUnstable = BloodlineAccess.get(p).unstable();
        apply(p, BloodlineEvent.DRANK_OTHER_BLOOD, null);
        if (!wasUnstable && BloodlineAccess.get(p).unstable()) {
            p.addEffect(new MobEffectInstance(SpeciesRegistries.UNSTABLE_EFFECT, UnstableEffect.DURATION_TICKS));
        }
    }

    /** Runs the rule, stores the state and performs the outcome's actions. */
    private static Outcome apply(ServerPlayer p, BloodlineEvent e, @Nullable UUID bloodSource) {
        Outcome out = BloodlineAccess.applyEvent(p, e, SpeciesContext.of(p, bloodSource));
        OutcomeExecutor.run(p, out);
        return out;
    }
}
