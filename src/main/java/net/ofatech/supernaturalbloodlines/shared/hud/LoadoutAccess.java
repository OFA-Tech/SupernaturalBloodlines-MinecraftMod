package net.ofatech.supernaturalbloodlines.shared.hud;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The hub's Loadout tab reaches the ability HUD's six Supernatural-mode slots through this (parts never import each other). The HUD part installs the
 * provider on the client; without it {@link #available()} is false and the tab is not shown. Client side only: everything is the local player's view,
 * and {@link #set} sends a request that the server checks.
 */
public final class LoadoutAccess {
    /** One action the player may put in a slot. {@code note} says what it does for the player's current form (Bite), or null. */
    public record Entry(ResourceLocation id, Component name, ResourceLocation icon, @Nullable Component note, @Nullable String greyedReason) {
    }

    public interface Provider {
        /** The six slots as the server last sent them. */
        List<Optional<ResourceLocation>> slots();

        /** Bite first, then the player's unlocked actions. */
        List<Entry> assignable();

        void set(int slot, @Nullable ResourceLocation id);

        /** How the slot's key reads, for the right page of the tab. */
        String keyLabel(int slot);
    }

    private static volatile Provider provider;

    private LoadoutAccess() {
    }

    public static void install(Provider p) {
        provider = p;
    }

    public static boolean available() {
        return provider != null;
    }

    public static List<Optional<ResourceLocation>> get() {
        Provider p = provider;
        return p == null ? List.of() : p.slots();
    }

    public static List<Entry> assignable() {
        Provider p = provider;
        return p == null ? List.of() : p.assignable();
    }

    public static void set(int slot, @Nullable ResourceLocation id) {
        Provider p = provider;
        if (p != null) {
            p.set(slot, id);
        }
    }

    public static String keyLabel(int slot) {
        Provider p = provider;
        return p == null ? "" : p.keyLabel(slot);
    }
}
