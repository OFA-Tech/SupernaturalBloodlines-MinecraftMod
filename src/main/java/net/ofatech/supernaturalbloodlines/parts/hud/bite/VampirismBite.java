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

    private static boolean lookedUp;
    private static MethodHandle bite;
    private static MethodHandle end;

    private VampirismBite() {
    }

    static ResourceLocation factionId(ServerPlayer p) {
        IPlayableFaction<?> f = VampirismAPI.factionPlayerHandler(p).getCurrentFaction();
        return f == null ? null : f.getID();
    }

    /**
     * The blood a bite can take, by Vampirism's own values: a mob's {@code ExtendedCreature} blood (from the {@code entity_blood} data map), a
     * player's blood stats (a bloodless race has none). A vampire mob has no blood value, so nothing to drink.
     */
    static int blood(LivingEntity target) {
        if (target instanceof Player player) {
            return BloodlessPlayers.test(player) ? 0 : VampirismAPI.vampirePlayer(player).getBloodStats().getBloodLevel();
        }
        return target instanceof PathfinderMob mob ? VampirismAPI.getExtendedCreatureVampirism(mob).map(c -> c.getBlood()).orElse(0) : 0;
    }

    /** Takes up to {@code amount} blood from the victim, the way Vampirism's own feeding does. */
    static void drain(LivingEntity target, int amount) {
        int take = Math.min(amount, blood(target));
        if (take <= 0) {
            return;
        }
        if (target instanceof Player player) {
            VampirismAPI.vampirePlayer(player).useBlood(take, true);
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
