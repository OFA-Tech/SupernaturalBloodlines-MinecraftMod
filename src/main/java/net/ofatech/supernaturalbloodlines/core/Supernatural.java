package net.ofatech.supernaturalbloodlines.core;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;

/**
 * "Is this entity supernatural": core cannot reference Vampirism, Werewolves or Tensura, so each part registers its own
 * predicates and the answers are OR-combined. Parts register once at init; the lists are read on the server thread and on the client (block collision, break speed), so they are copy-on-write and the predicates must only use state the client has.
 * <ul>
 * <li>supernatural: an attacker the Resurrection Ring answers to, a target of the Hearing Device;</li>
 * <li>vampiric: has vampire traits (vervain burns it);</li>
 * <li>prey: vampire and werewolf types, never witches or siphoners (R81).</li>
 * </ul>
 */
public final class Supernatural {
    private static final List<Predicate<Entity>> SUPERNATURAL = new CopyOnWriteArrayList<>();
    private static final List<Predicate<Entity>> VAMPIRIC = new CopyOnWriteArrayList<>();
    private static final List<Predicate<Entity>> PREY = new CopyOnWriteArrayList<>();

    private Supernatural() {
    }

    public static void register(Predicate<Entity> isSupernatural, Predicate<Entity> isVampiric, Predicate<Entity> isPrey) {
        // all three before any is added, so a null cannot leave the lists out of step
        Objects.requireNonNull(isSupernatural, "isSupernatural");
        Objects.requireNonNull(isVampiric, "isVampiric");
        Objects.requireNonNull(isPrey, "isPrey");
        SUPERNATURAL.add(isSupernatural);
        VAMPIRIC.add(isVampiric);
        PREY.add(isPrey);
    }

    public static boolean is(Entity e) {
        return any(SUPERNATURAL, e);
    }

    public static boolean isVampiric(Entity e) {
        return any(VAMPIRIC, e);
    }

    public static boolean isPrey(Entity e) {
        return any(PREY, e);
    }

    private static boolean any(List<Predicate<Entity>> predicates, Entity e) {
        for (Predicate<Entity> p : predicates) {
            if (p.test(e)) {
                return true;
            }
        }
        return false;
    }

    static void clearForTests() {
        SUPERNATURAL.clear();
        VAMPIRIC.clear();
        PREY.clear();
    }
}
