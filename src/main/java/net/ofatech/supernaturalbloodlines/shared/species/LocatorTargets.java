package net.ofatech.supernaturalbloodlines.shared.species;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The locator (R315, R316) is made by the artifacts part (the rite and the Vampire Compass) out of what the species part knows (a sealed bottle of blood,
 * who is online, who is cloaked, the settings). The two parts do not know each other, so they meet here. The species part installs the source; without it
 * the compass has no third mode and the rite is refused. The target of an attuned compass is stored sealed (a string only the server can open), so no
 * client ever learns who is looked for.
 */
public final class LocatorTargets {
    /** Where somebody is now, and the reasons they cannot be found, as plain facts: the compass turns all of them into one answer. */
    public record Located(boolean online, boolean alive, ResourceLocation dimension, Vec3 position, boolean cloaked, boolean spectator) {}

    /** The settings of the locator ({@code locator_*}). */
    public record Settings(boolean enabled, int minutes, int range, int blur, boolean tellTarget) {}

    public interface Source {
        Settings settings();

        Optional<Located> find(MinecraftServer server, UUID target);

        /** Whose blood a sealed bottle holds, when it holds somebody's. */
        Optional<UUID> bloodOf(ItemStack bottle);

        /** The target as an opaque string for the stack. */
        Optional<String> seal(UUID target);

        /** The target of a seal made by this server. */
        Optional<UUID> open(String seal);
    }

    private static volatile Source source;

    private LocatorTargets() {
    }

    public static void install(Source installed) {
        source = installed;
    }

    public static boolean installed() {
        return source != null;
    }

    public static Optional<Settings> settings() {
        Source s = source;
        return s == null ? Optional.empty() : Optional.of(s.settings());
    }

    public static Optional<Located> find(MinecraftServer server, UUID target) {
        Source s = source;
        return s == null ? Optional.empty() : s.find(server, target);
    }

    public static Optional<UUID> bloodOf(ItemStack bottle) {
        Source s = source;
        return s == null ? Optional.empty() : s.bloodOf(bottle);
    }

    public static Optional<String> seal(UUID target) {
        Source s = source;
        return s == null ? Optional.empty() : s.seal(target);
    }

    public static Optional<UUID> open(String seal) {
        Source s = source;
        return s == null ? Optional.empty() : s.open(seal);
    }
}
