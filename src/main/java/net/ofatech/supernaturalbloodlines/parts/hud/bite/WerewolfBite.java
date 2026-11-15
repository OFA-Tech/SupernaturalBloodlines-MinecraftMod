package net.ofatech.supernaturalbloodlines.parts.hud.bite;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.werewolves.api.entities.player.IWerewolfPlayer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;

/**
 * Werewolves' own bite. Touched only after {@code ModList.isLoaded("werewolves")}. {@code WerewolfPlayer} extends a Vampirism class that is
 * only in the full jar, so the form comes from the API's {@link IWerewolfPlayer} and the bite methods ({@code canBite()},
 * {@code canBiteEntity(LivingEntity)}, {@code bite(int)}, all public) are called through method handles on the runtime class, looked up once.
 */
final class WerewolfBite {
    private static boolean lookedUp;
    private static MethodHandle canBite;
    private static MethodHandle canBiteEntity;
    private static MethodHandle bite;

    private WerewolfBite() {
    }

    private static Optional<IWerewolfPlayer> wolf(net.minecraft.world.entity.player.Player p) {
        return VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().filter(f -> f instanceof IWerewolfPlayer).map(f -> (IWerewolfPlayer) f);
    }

    /** In a transformed form, the only forms in which Werewolves lets the player bite ({@code canBite()}). */
    static boolean wolfForm(net.minecraft.world.entity.player.Player p) {
        return wolf(p).map(w -> w.getForm().isTransformed()).orElse(false);
    }

    /** Null when it bit, else the refusal's lang key. */
    static String bite(ServerPlayer p, LivingEntity target) {
        IWerewolfPlayer w = wolf(p).orElse(null);
        if (w == null) {
            return BiteService.KEY + "cannot_bite";
        }
        lookup(w);
        if (bite == null) {
            return BiteService.KEY + "unavailable";
        }
        try {
            if (!(boolean) canBite.invoke(w)) {
                return BiteService.KEY + "cannot_bite";
            }
            if (!(boolean) canBiteEntity.invoke(w, target) || !(boolean) bite.invoke(w, target.getId())) {
                return BiteService.KEY + "cannot_bite_target";
            }
        } catch (Throwable t) {
            SupernaturalBloodlines.LOGGER.warn("Bite: Werewolves' bite failed", t);
            return BiteService.KEY + "unavailable";
        }
        return null;
    }

    private static synchronized void lookup(IFactionPlayer<?> w) {
        if (lookedUp) {
            return;
        }
        lookedUp = true;
        try {
            MethodHandles.Lookup l = MethodHandles.publicLookup();
            Class<?> c = w.getClass();
            canBite = l.findVirtual(c, "canBite", MethodType.methodType(boolean.class));
            canBiteEntity = l.findVirtual(c, "canBiteEntity", MethodType.methodType(boolean.class, LivingEntity.class));
            bite = l.findVirtual(c, "bite", MethodType.methodType(boolean.class, int.class));
        } catch (ReflectiveOperationException e) {
            canBite = null;
            canBiteEntity = null;
            bite = null;
            SupernaturalBloodlines.LOGGER.warn("Bite: Werewolves' bite methods are not there ({}); the Bite action cannot bite as a werewolf", e.toString());
        }
    }
}
