package net.ofatech.supernaturalbloodlines.parts.hud.bite;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.entity.player.vampire.IVampirePlayer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.shared.vampirism.BloodlessPlayers;

/**
 * Everything the Bite action asks of Vampirism. Touched only after {@code ModList.isLoaded("vampirism")}. Vampirism's feeding start
 * ({@code VampirePlayer.biteEntity(int)}) and end ({@code endFeeding(boolean)}) are public, but only in the full jar, and the compile
 * classpath has the API jar: they are called through {@link MethodHandles#publicLookup()} on the runtime class, looked up once.
 */
final class VampirismBite {
    static final ResourceLocation VAMPIRE = ResourceLocation.parse("vampirism:vampire");
    static final ResourceLocation WEREWOLF = ResourceLocation.parse("werewolves:werewolf");
    private static final ResourceLocation BAT = ResourceLocation.parse("vampirism:bat");

    private static boolean lookedUp;
    private static MethodHandle bite;
    private static MethodHandle end;

    private VampirismBite() {
    }

    static ResourceLocation factionId(net.minecraft.world.entity.player.Player p) {
        IPlayableFaction<?> f = VampirismAPI.factionPlayerHandler(p).getCurrentFaction();
        return f == null ? null : f.getID();
    }

    /**
     * The blood a bite can take, by Vampirism's own values: a mob's {@code ExtendedCreature} blood (from the {@code entity_blood} data map); a
     * Vampirism vampire player's blood stats; for a player who is not a vampire (vampire level 0), their <b>food level</b>, which is what Vampirism's
     * own {@code VampirePlayer.onBite} takes from such a victim (its blood stats are not used below level 1). A bloodless race has none. A vampire
     * mob and a converted creature have none.
     */
    static int blood(LivingEntity target) {
        if (target instanceof Player player) {
            if (BloodlessPlayers.test(player)) {
                return 0;
            }
            IVampirePlayer vampire = VampirismAPI.vampirePlayer(player);
            return vampire.getLevel() > 0 ? vampire.getBloodStats().getBloodLevel() : player.getFoodData().getFoodLevel();
        }
        return target instanceof PathfinderMob mob ? VampirismAPI.getExtendedCreatureVampirism(mob).map(c -> c.getBlood()).orElse(0) : 0;
    }

    /** Takes up to {@code amount} blood from the victim, from the same place {@link #blood} reads. */
    static void drain(LivingEntity target, int amount) {
        int take = Math.min(amount, blood(target));
        if (take <= 0) {
            return;
        }
        if (target instanceof Player player) {
            IVampirePlayer vampire = VampirismAPI.vampirePlayer(player);
            if (vampire.getLevel() > 0) {
                vampire.useBlood(take, true);
            } else {
                player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - take);
            }
        } else if (target instanceof PathfinderMob mob) {
            VampirismAPI.getExtendedCreatureVampirism(mob).ifPresent(c -> c.setBlood(c.getBlood() - take));
        }
    }

    /** Starts Vampirism's own feeding. Null when it started, else the refusal's lang key. */
    static String start(ServerPlayer p, LivingEntity target) {
        IVampirePlayer vampire = VampirismAPI.vampirePlayer(p);
        if (vampire.getLevel() == 0 || vampire.determineBiteType(target) == IVampirePlayer.BITE_TYPE.NONE) {
            return BiteService.KEY + "cannot_bite_target";
        }
        // biteEntity ignores these without a word, and Vampirism has no public feeding state to read afterwards, so refuse them here
        if (vampire.isDBNO() || vampire.getActionHandler().isActionActive(BAT)) {
            return BiteService.KEY + "cannot_bite";
        }
        lookup(vampire);
        if (bite == null) {
            return BiteService.KEY + "unavailable";
        }
        try {
            bite.invoke(vampire, target.getId());
        } catch (Throwable t) {
            SupernaturalBloodlines.LOGGER.warn("Bite: Vampirism's biteEntity failed", t);
            return BiteService.KEY + "unavailable";
        }
        return null;
    }

    static void end(ServerPlayer p) {
        IVampirePlayer vampire = VampirismAPI.vampirePlayer(p);
        lookup(vampire);
        if (end == null) {
            return;
        }
        try {
            end.invoke(vampire, true);
        } catch (Throwable t) {
            SupernaturalBloodlines.LOGGER.warn("Bite: Vampirism's endFeeding failed", t);
        }
    }

    private static synchronized void lookup(IVampirePlayer vampire) {
        if (lookedUp) {
            return;
        }
        lookedUp = true;
        try {
            MethodHandles.Lookup l = MethodHandles.publicLookup();
            bite = l.findVirtual(vampire.getClass(), "biteEntity", MethodType.methodType(void.class, int.class));
            end = l.findVirtual(vampire.getClass(), "endFeeding", MethodType.methodType(void.class, boolean.class));
        } catch (ReflectiveOperationException e) {
            bite = null;
            end = null;
            SupernaturalBloodlines.LOGGER.warn("Bite: Vampirism's feeding methods are not there ({}); the Bite action cannot drink", e.toString());
        }
    }
}
