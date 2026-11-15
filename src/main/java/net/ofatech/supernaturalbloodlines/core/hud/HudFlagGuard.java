package net.ofatech.supernaturalbloodlines.core.hud;

import java.util.Optional;

/**
 * One of Tensura's in-memory HUD switches (for example {@code abilities.render}) that we lower while our own HUD is on, and put back exactly as the player
 * had it. ManasCore only saves the config when Tensura's settings screen closes, so the flag is given back when that screen opens (it shows and saves the
 * player's value) and lowered again after it closes. What the player left in the screen is their value from then on. No game classes: the caller
 * reads and writes the real flag and passes it in.
 */
public final class HudFlagGuard {
    private Boolean saved;
    private boolean lifted;

    /** @return the value to write now, empty when the flag is already right */
    public Optional<Boolean> tick(boolean want, boolean current) {
        if (lifted) {
            return Optional.empty();
        }
        if (want) {
            if (saved == null) {
                saved = current;
            }
            return current ? Optional.of(false) : Optional.empty();
        }
        return release(current);
    }

    /** Tensura's settings screen is about to open: @return the player's value to put back now. */
    public Optional<Boolean> screenOpening() {
        if (saved == null) {
            return Optional.empty();
        }
        lifted = true;
        return Optional.of(saved);
    }

    /** The screen closed: whatever the flag is now is the player's value. */
    public void screenClosed(boolean current) {
        if (lifted) {
            lifted = false;
            saved = current;
        }
    }

    /** Put the player's value back at once (our HUD is going away, the client is stopping). */
    public Optional<Boolean> restoreNow(boolean current) {
        lifted = false;
        return release(current);
    }

    public boolean active() {
        return saved != null;
    }

    private Optional<Boolean> release(boolean current) {
        Boolean back = saved;
        saved = null;
        return back == null || back == current ? Optional.empty() : Optional.of(back);
    }
}
