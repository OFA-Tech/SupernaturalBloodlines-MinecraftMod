package net.ofatech.supernaturalbloodlines.parts.species.pack;

import com.mojang.authlib.GameProfile;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BaseFaction;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackOutcome;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory;
import org.jetbrains.annotations.Nullable;

/**
 * The one door to the Teen Wolf layer: {@link PackRules} knows neither species nor faction, so what a player may become
 * is checked here before the rules run, and the result is stored through {@link BloodlineAccess}. Links no Werewolves
 * class (the faction is matched by id).
 */
public final class PackAccess {
    private PackAccess() {
    }

    public static WolfState wolf(Player p) {
        return BloodlineAccess.get(p).wolf();
    }

    public static boolean isAlpha(WolfState.Rank rank) {
        return rank == WolfState.Rank.ALPHA || rank == WolfState.Rank.TRUE_ALPHA;
    }

    /** The last name the server knows for {@code id} (online or cached), or null. */
    @Nullable
    public static String nameOf(MinecraftServer server, UUID id) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        GameProfileCache cache = server.getProfileCache();
        return cache == null ? null : cache.get(id).map(GameProfile::getName).orElse(null);
    }

    /** True for the events that give a player a lineage (born or bitten) instead of changing what a werewolf already has. */
    private static boolean givesLineage(PackEvent e) {
        return e == PackEvent.GENE_AWAKENED || e == PackEvent.RITE_OF_LINEAGE_AWAKEN
                || e == PackEvent.BITTEN_BY_ALPHA || e == PackEvent.BITTEN_BY_WILD;
    }

    /**
     * The refusal reason (without the {@code supernaturalbloodlines.refusal.} prefix) when {@code e} must not reach the
     * rules for this faction and species, else null. A vampire or hunter never gets a bitten or born lineage here
     * (vampires turn hybrid through {@code BloodlineRules}); Originals, heretics and the Five are the bloodline rules'.
     */
    @Nullable
    static String blocked(BaseFaction faction, Species species, PackEvent e) {
        if (!givesLineage(e)) {
            return null;
        }
        if (faction == BaseFaction.VAMPIRE || faction == BaseFaction.HUNTER) {
            return "wolf_wrong_faction";
        }
        return switch (species) {
            case ORIGINAL, HERETIC, POTENTIAL, HUNTER_OF_THE_FIVE -> "wolf_wrong_species";
            default -> null;
        };
    }

    /** Runs the pack rules for {@code p} and stores the result. A blocked or refused event leaves everything as it was. */
    public static PackOutcome applyPack(ServerPlayer p, PackEvent e, PackContext c) {
        BloodlineState before = BloodlineAccess.get(p);
        String reason = blocked(FactionRiteFactory.baseFaction(VampirismAPI.factionPlayerHandler(p)), before.species(), e);
        if (reason != null) {
            return new PackOutcome(before.wolf(), Outcome.REFUSAL_PREFIX + reason);
        }
        PackOutcome out = PackRules.apply(before.wolf(), e, c);
        if (!out.refused() && !out.state().equals(before.wolf())) {
            BloodlineAccess.set(p, before.toBuilder().wolf(out.state()).build());
        }
        return out;
    }
}
