package net.ofatech.supernaturalbloodlines.shared.hub;

import java.util.List;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The hub's Mastery tab (Skills page) reaches the Rite of Mastery's picker through this: the supernatural part never imports the evolution rites part,
 * which needs Tensura. The evolution rites part installs the provider on the client; without it {@link #available()} is false and the tab is not shown.
 * Client side only: the list is the local player's own skills, and a choice is sent as the command {@code bloodline mastery choose <skill>}, which the
 * server validates (what is refused there is said in chat).
 */
public final class MasteryAccess {
    /** One skill the player holds. {@code greyed} is why it cannot be chosen (known on the client), or null. */
    public record Entry(String id, Component name, Component tier, int tierOrder, @Nullable Component greyed) {
    }

    public interface Provider {
        List<Entry> entries();

        /** The skill the server last said is chosen, or null: the hub marks only what the server has accepted. */
        @Nullable
        String chosen();
    }

    private static volatile Provider provider;

    private MasteryAccess() {
    }

    public static void install(Provider p) {
        provider = p;
    }

    public static boolean available() {
        return provider != null;
    }

    @Nullable
    public static String chosen() {
        Provider p = provider;
        return p == null ? null : p.chosen();
    }

    public static List<Entry> entries() {
        Provider p = provider;
        return p == null ? List.of() : p.entries();
    }
}
