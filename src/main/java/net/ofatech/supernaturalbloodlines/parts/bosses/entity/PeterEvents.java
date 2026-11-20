package net.ofatech.supernaturalbloodlines.parts.bosses.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossRules;
import net.ofatech.supernaturalbloodlines.core.boss.RewardRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.deal.Deals;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossClock;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.SparkHandler;
import org.jetbrains.annotations.Nullable;

/**
 * What Peter's death does (R183). The damage players deal to him is counted (a pet's for its owner, a mob's or the environment's for nobody); a real death (not a
 * prevented one) writes him down as dead and the day he died (for {@code peter_respawn}) and pays once: the Memory Claws drop where he fell (they never burn
 * and never despawn, so anybody may take them), and his Alpha spark goes to <b>the player who did the most to bring him down</b> (a tie goes to the last hitter) through
 * {@link SparkHandler#grantFromBoss}, by the rules of a stolen spark: a True Alpha takes none, an open protected path destroys it, the cap holds, somebody who is no
 * werewolf is told so. He is no Original, so nothing he does or that is done to him touches a line.
 */
public final class PeterEvents {
    /** The one Alpha spark he carries. */
    public static final int SPARKS = 1;

    private PeterEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, true, LivingDamageEvent.Post.class, PeterEvents::onDamaged);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, false, LivingDeathEvent.class, PeterEvents::onDeath);
    }

    private static void onDamaged(LivingDamageEvent.Post e) {
        if (e.getEntity() instanceof PeterHaleEntity p && p.level() instanceof ServerLevel level) {
            ServerPlayer who = Deals.playerBehind(level.getServer(), e.getSource().getEntity());
            if (who != null) {
                p.tally().add(who.getUUID(), e.getNewDamage());
            }
        }
    }

    private static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof PeterHaleEntity p) || !(p.level() instanceof ServerLevel level)) {
            return;
        }
        BossIndex index = BossIndex.get(level.getServer());
        index.apply(BossId.PETER_HALE, BossRules.Event.KILLED);
        index.of(BossId.PETER_HALE).returnDay = BossClock.today(level);
        // a player's last blow counts when nothing was tallied before it (a command, a one-shot): at least that much
        if (p.tally().isEmpty() && SparkHandler.killerOf(p, e.getSource()) instanceof ServerPlayer killer) {
            p.tally().add(killer.getUUID(), 1F);
        }
        pay(level, p, p.topPlayer(level.getServer()));
        index.setDirty();
        BossIndex.flush(level.getServer());
    }

    /** The reward, once: the claws where he stands, the spark for {@code recipient}, who may be null (nobody to pay). Whether it was paid. */
    public static boolean pay(ServerLevel level, PeterHaleEntity p, @Nullable ServerPlayer recipient) {
        BossIndex index = BossIndex.get(level.getServer());
        BossIndex.Entry entry = index.of(BossId.PETER_HALE);
        if (!RewardRules.pays(BossesConfig.rewardOn(), RewardRules.Defeat.DIED, entry.rewarded)) {
            return false;
        }
        entry.rewarded = true;
        ItemEntity claws = new ItemEntity(level, p.getX(), p.getY() + 0.5, p.getZ(), new ItemStack(BossItems.MEMORY_CLAWS.get()));
        claws.setUnlimitedLifetime();
        claws.setInvulnerable(true);
        level.addFreshEntity(claws);
        if (recipient != null) {
            SparkHandler.grantFromBoss(recipient, SPARKS, "supernaturalbloodlines.boss.peter.reward_no_werewolf", "supernaturalbloodlines.boss.peter.reward_true_alpha");
        }
        return true;
    }
}
