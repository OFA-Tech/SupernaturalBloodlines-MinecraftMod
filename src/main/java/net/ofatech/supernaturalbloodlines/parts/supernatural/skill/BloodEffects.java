package net.ofatech.supernaturalbloodlines.parts.supernatural.skill;

import net.ofatech.supernaturalbloodlines.shared.supernatural.BiteProtection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.event.BloodDrinkEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.parts.supernatural.SupernaturalConfig;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HumanLikeBlood;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;

/**
 * The BLOOD skills: what a player gets when they feed, in Vampirism's own {@link BloodDrinkEvent.PlayerDrinkBloodEvent} (its amount is
 * settable and is what the vampire's blood level receives). Both run at LOW, after the Vervain rule (a protected victim's amount is 0).
 * <ul>
 * <li><b>Keen Fangs (S-VAM-1):</b> a feed from a human-like victim gives {@code extra_blood} more, once per feed (see
 * {@link #firstBiteOfThisFeed}).</li>
 * <li><b>Predator's Thirst (S-VAM-6):</b> a bite on a vampire feeds the biter and drains the victim. The species part's refusal of
 * that bite (plan 11) steps aside for an owner of the skill. The biter gets the amount Vampirism gives for that victim (its
 * {@code entity_blood} value), else {@code vampire_feed_blood}. A vampire player victim loses the same amount of Vampirism blood; a
 * mob takes 2 damage and Weakness I for 10 s. The drain never kills: the damage stops one health short, and a player loses only blood
 * (so an Original, who only white oak kills, is safe).</li>
 * </ul>
 */
public final class BloodEffects {
    static final long CHAIN_TICKS = 5 * 20;
    private record BiteKey(UUID biter, UUID victim) {
    }

    private static final Map<BiteKey, Long> LAST_BITE = new HashMap<>();
    private static final float DRAIN_DAMAGE = 2f;
    private static final int DRAIN_WEAKNESS_TICKS = 10 * 20;

    private BloodEffects() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, BloodEffects::onDrinkBlood);
    }

    private static void onDrinkBlood(BloodDrinkEvent.PlayerDrinkBloodEvent e) {
        LivingEntity source = e.getBloodSource().getEntity().orElse(null);
        if (source == null || !(e.getVampire().getRepresentingEntity() instanceof ServerPlayer p)) {
            return;
        }
        int extra = (int) LadderQuery.value(p, "S-VAM-1", "extra_blood", 0);
        if (extra > 0 && e.getAmount() > 0 && HumanLikeBlood.test(source) && firstBiteOfThisFeed(p, source)) {
            e.setAmount(e.getAmount() + extra);
        }
        // a bite that something emptied on purpose (a desiccated biter, a vervain victim) stays empty: no fallback blood, no drain
        if (source != p && VampirismAPI.factionRegistry().getFaction(source) == VReference.VAMPIRE_FACTION && (LadderQuery.owns(p, "S-VAM-6") || BloodlineAccess.get(p).species() == Species.AUGUSTINE) // an Augustine drains vampires without the skill (R203)
                && !BiteProtection.protectedBite(p, source)) {
            predatorsThirst(e, source);
        }
    }

    /**
     * Keen Fangs is +1 per feed, not per bite: a victim bitten again within {@link #CHAIN_TICKS} of the last bite by the same biter is
     * the same feed. Each bite renews the window, so a bite chain counts once and a new feed starts after a pause. (Server thread only.)
     */
    static boolean firstBiteOfThisFeed(ServerPlayer biter, LivingEntity victim) {
        long now = biter.level().getGameTime();
        LAST_BITE.values().removeIf(t -> now - t > CHAIN_TICKS);
        Long last = LAST_BITE.put(new BiteKey(biter.getUUID(), victim.getUUID()), now);
        return last == null || now - last > CHAIN_TICKS;
    }

    @org.jetbrains.annotations.VisibleForTesting
    public static void forgetBitesForTest() {
        LAST_BITE.clear();
    }

    private static void predatorsThirst(BloodDrinkEvent.PlayerDrinkBloodEvent e, LivingEntity victim) {
        int blood = e.getAmount() > 0 ? e.getAmount() : SupernaturalConfig.vampireFeedBlood();
        e.setAmount(blood);
        if (victim instanceof Player v) {
            VampirismAPI.vampirePlayer(v).useBlood(blood, true);
        } else {
            float damage = Math.min(DRAIN_DAMAGE, victim.getHealth() - 1f);
            if (damage > 0) {
                victim.hurt(victim.damageSources().magic(), damage);
            }
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DRAIN_WEAKNESS_TICKS, 0));
        }
    }
}