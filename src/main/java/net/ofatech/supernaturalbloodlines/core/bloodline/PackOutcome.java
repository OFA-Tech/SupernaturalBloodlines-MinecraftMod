package net.ofatech.supernaturalbloodlines.core.bloodline;

import org.jetbrains.annotations.Nullable;

public record PackOutcome(WolfState state, @Nullable String refusalKey) {

    public boolean refused() {
        return refusalKey != null;
    }
}

