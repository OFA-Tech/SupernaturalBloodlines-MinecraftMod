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
 * The hook of the Alpha Trial (plan 15 builds the trial itself): a Beta who wins it becomes a second Alpha of the pack they are
 * in. They keep the pack, the pack keeps its leader until the strongest Alpha is someone else.
 */
public final class AlphaTrial {
    private AlphaTrial() {
    }

    /**
     * @param warned the player saw the {@code TrueAlphaWarning} and chose to take the spark; without it a Beta whose path to True Alpha is still
     *               open is refused (winning ends that path for good), so no trial can close it by surprise
     * @return the error to show, or null when the Beta is an Alpha now
     */
    @Nullable
    public static Component win(ServerPlayer beta, boolean warned) {
        PackIndex index = PackIndex.get(beta.serverLevel());
        if (!warned && PackRules.pathOpen(PackAccess.wolf(beta))) {
            return Component.translatable("supernaturalbloodlines.pack.trial_warn");
        }
        if (PackAccess.wolf(beta).rank() != Rank.BETA || !VampirismSupernatural.isWerewolf(beta) || index.alphaOf(beta.getUUID()).isEmpty()) {
            return Component.translatable("supernaturalbloodlines.pack.trial_no");
        }
        PackIndex.KEEP_PACK.set(true);
        try {
            if (PackAccess.applyPack(beta, PackEvent.SPARK_INHERITED, PackContext.defaults()).refused()) {
                return Component.translatable("supernaturalbloodlines.pack.trial_no");
            }
        } finally {
            PackIndex.KEEP_PACK.set(false);
        }
        PackLeader.refresh(beta);
        beta.sendSystemMessage(Component.translatable("supernaturalbloodlines.pack.trial_won"));
        return null;
    }
}
