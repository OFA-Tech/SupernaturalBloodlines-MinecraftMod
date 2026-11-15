package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * The return rule of the launchers: after a launcher opens another mod's screen, the hub comes back on the page it was on as soon as no
 * screen is open (Esc out of that mod). Moving between that mod's own screens keeps waiting; opening the hub any other way forgets it.
 * Pure: the client tick feeds it what is on screen.
 */
public final class HubReturn {
    public enum OnScreen { NONE, HUB, OTHER }

    @Nullable
    private HubPage pending;

    public void launched(HubPage page) {
        pending = page;
    }

    /** The page to reopen the hub on now, if any. */
    public Optional<HubPage> tick(OnScreen now) {
        if (pending == null) {
            return Optional.empty();
        }
        return switch (now) {
            case OTHER -> Optional.empty();
            case HUB -> {
                pending = null;
                yield Optional.empty();
            }
            case NONE -> {
                HubPage p = pending;
                pending = null;
                yield Optional.of(p);
            }
        };
    }

    public boolean waiting() {
        return pending != null;
    }
}
