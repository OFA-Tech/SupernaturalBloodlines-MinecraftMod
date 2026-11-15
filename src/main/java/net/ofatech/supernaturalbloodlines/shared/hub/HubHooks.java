package net.ofatech.supernaturalbloodlines.shared.hub;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;

/**
 * What the hub (in the supernatural part) and the parts around it give each other on the client, without importing each other:
 * the parts add launchers and Tensura's live numbers; the supernatural part says how to open the hub (for Tensura's menu tab).
 * Touched on the client only; nothing here loads a client class.
 */
public final class HubHooks {
    /**
     * Another mod's own screen, opened unchanged from a wax seal.
     *
     * @param letter one letter drawn on the seal
     * @param color  the letter's colour
     */
    public record Launcher(String id, String letter, int color, Component name, BooleanSupplier available, Runnable open) {
    }

    /** Tensura's live numbers for the player, as Tensura's own client data has them. */
    public record TensuraInfo(Component race, double ep, double magicule) {
    }

    private static final List<Launcher> LAUNCHERS = new CopyOnWriteArrayList<>();
    private static volatile Supplier<Optional<TensuraInfo>> tensura = Optional::empty;
    private static volatile Runnable openHub;

    private HubHooks() {
    }

    public static void addLauncher(Launcher l) {
        LAUNCHERS.removeIf(o -> o.id().equals(l.id()));
        LAUNCHERS.add(l);
    }

    /** The launchers that can open now, in the order they were added. */
    public static List<Launcher> launchers() {
        return LAUNCHERS.stream().filter(l -> l.available().getAsBoolean()).toList();
    }

    public static void setTensura(Supplier<Optional<TensuraInfo>> supplier) {
        tensura = supplier;
    }

    public static Optional<TensuraInfo> tensura() {
        try {
            return tensura.get();
        } catch (RuntimeException e) {
            return Optional.empty(); // Tensura's data not there yet (the first frames after joining): show nothing rather than fail the frame
        }
    }

    public static void setHubOpener(Runnable opener) {
        openHub = opener;
    }

    /** Opens the hub; false when the supernatural part is off (nothing to open). */
    public static boolean openHub() {
        Runnable r = openHub;
        if (r == null) {
            return false;
        }
        r.run();
        return true;
    }

    public static boolean hubAvailable() {
        return openHub != null;
    }
}
