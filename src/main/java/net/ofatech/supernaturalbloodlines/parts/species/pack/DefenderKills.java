package net.ofatech.supernaturalbloodlines.parts.species.pack;

import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.werewolves.WerewolfFaction;
import org.jetbrains.annotations.Nullable;

/**
 * Defender kills and the True Alpha (R91): a hostile mob killed while it was targeting a villager or a pack mate counts
 * for the killer, and every {@link #CHECK_TICKS} the Betas who may deserve the title are checked. There is no cap on True
 * Alphas. Touched only behind {@code ModList.isLoaded("werewolves")}.
 */
public final class DefenderKills {
    static final int CHECK_TICKS = 1200;
    /** 30 in-game days as a Beta. */
    public static final long BETA_TICKS = 30L * 24000;
    public static final int KILLS_NEEDED = 300;

    private DefenderKills() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, LivingDeathEvent.class, DefenderKills::onDeath);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % CHECK_TICKS == 0) {
                List.copyOf(e.getServer().getPlayerList().getPlayers()).forEach(p -> checkTrueAlpha(p, p.level().getGameTime()));
            }
        });
    }

    private static void onDeath(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide || !(victim instanceof Enemy) || !(victim instanceof Mob mob)
                || !(SparkHandler.killerOf(victim, e.getSource()) instanceof ServerPlayer killer)
                || PackAccess.wolf(killer).lineage() == Lineage.NONE) {
            return;
        }
        LivingEntity target = mob.getTarget();
        if (target instanceof Villager || (target instanceof ServerPlayer t && t != killer && samePack(killer, t))) {
            PackAccess.applyPack(killer, PackEvent.DEFENDER_KILL, PackContext.defaults());
        }
    }

    @Nullable
    private static UUID leaderOf(ServerPlayer p) {
        WolfState w = PackAccess.wolf(p);
        if (w.lineage() == Lineage.NONE) {
            return null;
        }
        return PackAccess.isAlpha(w.rank()) ? p.getUUID() : w.packAlpha();
    }

    /** Both are werewolves under the same Alpha (or one is the Alpha). */
    private static boolean samePack(ServerPlayer a, ServerPlayer b) {
        UUID leader = leaderOf(a);
        return leader != null && leader.equals(leaderOf(b));
    }

    /**
     * Tries TRUE_ALPHA_EARNED for a Beta: Werewolves' highest level, 30 days as a Beta, 300 defender kills, no innocent
     * kill, no stolen spark (the rules check it). Returns whether they were promoted.
     */
    public static boolean checkTrueAlpha(ServerPlayer p, long now) {
        if (PackAccess.wolf(p).rank() != Rank.BETA || !VampirismSupernatural.isWerewolf(p)) {
            return false;
        }
        int level = VampirismAPI.factionPlayerHandler(p).getCurrentLevel();
        PackContext c = new PackContext(null, level, now, WerewolfFaction.faction().getHighestReachableLevel(), BETA_TICKS, KILLS_NEEDED, false);
        if (PackAccess.applyPack(p, PackEvent.TRUE_ALPHA_EARNED, c).refused() || PackAccess.wolf(p).rank() != Rank.TRUE_ALPHA) {
            return false;
        }
        p.sendSystemMessage(Component.translatable("supernaturalbloodlines.pack.true_alpha"));
        return true;
    }
}
