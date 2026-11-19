package net.ofatech.supernaturalbloodlines.parts.species.pack;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import org.jetbrains.annotations.Nullable;

/**
 * What winning the Alpha Trial does (R147; the rite and the boss are {@code AlphaTrialRiteFactory} and {@code AlphaTrialBoss}): a werewolf Beta becomes a
 * second Alpha of the pack they are in (they keep the pack, the pack keeps its leader until the strongest Alpha is someone else); an Omega becomes the
 * Alpha of a pack of their own. Either way they hold a spark for good, so the path to True Alpha is closed.
 */
public final class AlphaTrial {
    private AlphaTrial() {
    }

    /**
     * @param warned the player saw the {@code TrueAlphaWarning} and chose to take the spark; without it a Beta or Omega whose path to True Alpha is still
     *               open is refused (winning ends that path for good), so no trial can close it by surprise
     * @return the error to show, or null when the player is an Alpha now
     */
    @Nullable
    public static Component win(ServerPlayer player, boolean warned) {
        PackIndex index = PackIndex.get(player.serverLevel());
        var wolf = PackAccess.wolf(player);
        if (!warned && PackRules.pathOpen(wolf)) {
            return Component.translatable("supernaturalbloodlines.pack.trial_warn");
        }
        boolean beta = wolf.rank() == Rank.BETA;
        if (!VampirismSupernatural.isWerewolf(player) || (beta && index.alphaOf(player.getUUID()).isEmpty())) {
            return Component.translatable("supernaturalbloodlines.pack.trial_no");
        }
        PackIndex.KEEP_PACK.set(beta);
        try {
            if (PackAccess.applyPack(player, PackEvent.TRIAL_WON, PackContext.defaults()).refused()) {
                return Component.translatable("supernaturalbloodlines.pack.trial_no");
            }
        } finally {
            PackIndex.KEEP_PACK.set(false);
        }
        PackLeader.refresh(player);
        player.sendSystemMessage(Component.translatable("supernaturalbloodlines.pack.trial_won"));
        return null;
    }
}
