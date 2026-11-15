package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub;

import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * The return rule of the launchers: after a launcher opens another mod's screen, the hub comes back on the page it was on as soon as no
 * screen is open (Esc out of that mod). Moving between that mod's own screens (its package) keeps waiting. Anything else forgets it:
 * the hub opened another way, a screen outside that package (another mod's, the death screen), leaving the world. Pure: the client
 * tick feeds it what is on screen.
 */
public final class HubReturn {
    public enum OnScreen { NONE, HUB, OTHER }

    @Nullable
    private HubPage pending;
    private String owner = "";

    /** @param screenPackage the package of the launched mod's screens */
    public void launched(HubPage page, String screenPackage) {
        pending = page;
        owner = screenPackage;
    }

    public void clear() {
        pending = null;
    }

    /**
     * The page to reopen the hub on now, if any.
     *
     * @param screenClass the class name of the screen open now ({@code OTHER} only)
     */
    public Optional<HubPage> tick(OnScreen now, @Nullable String screenClass) {
        if (pending == null) {
            return Optional.empty();
        }
        switch (now) {
            case OTHER -> {
                if (screenClass == null || !screenClass.startsWith(owner)) {
                    pending = null;
                }
                return Optional.empty();
            }
            case HUB -> {
                pending = null;
                return Optional.empty();
            }
            default -> {
                HubPage p = pending;
                pending = null;
                return Optional.of(p);
            }
        }
    }

    public boolean waiting() {
        return pending != null;
    }
}
