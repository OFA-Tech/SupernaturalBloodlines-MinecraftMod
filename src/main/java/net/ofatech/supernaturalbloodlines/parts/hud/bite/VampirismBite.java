package net.ofatech.supernaturalbloodlines.parts.hud.bite;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.entity.player.vampire.IVampirePlayer;
import de.teamlapen.vampirism.api.entity.vampire.IVampire;
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

    /** Blood by Vampirism's rule: a mob with {@code entity_blood} above 0, a vampire (a vampire mob is as full of blood as a vampire player), or a player that is not bloodless. */
    static boolean hasBlood(LivingEntity target) {
        if (target instanceof Player player) {
            return !BloodlessPlayers.test(player);
        }
        if (target instanceof IVampire) {
            return true;
        }
        return target instanceof PathfinderMob mob && VampirismAPI.getExtendedCreatureVampirism(mob).map(c -> c.getBlood() > 0).orElse(false);
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
