package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/** The only door from {@link WheelService} to Tensura and ManasCore: every method checks {@code isLoaded} first. */
final class TensuraGate {
    private TensuraGate() {
    }

    static boolean loaded() {
        return ModList.get().isLoaded("tensura");
    }

    @Nullable
    static String press(ServerPlayer p, ResourceLocation id, int mode) {
        if (!loaded()) {
            return TensuraUse.KEY + "not_unlocked";
        }
        return TensuraProvider.RACE_ABILITY.equals(id) ? TensuraUse.pressRace(p) : TensuraUse.press(p, id, mode);
    }

    /** True while the power the wheel just pressed is still held (so a release from the wheel has something to end). */
    static boolean held(ServerPlayer p, ResourceLocation id, int mode) {
        if (!loaded()) {
            return false;
        }
        return TensuraProvider.RACE_ABILITY.equals(id) ? TensuraUse.raceHeld(p) : TensuraUse.skillHeld(p, id, mode);
    }

    static void release(ServerPlayer p, ResourceLocation id, int mode) {
        if (loaded()) {
            if (TensuraProvider.RACE_ABILITY.equals(id)) {
                TensuraUse.releaseRace(p);
            } else {
                TensuraUse.release(p, id, mode);
            }
        }
    }

    static void forget(ServerPlayer p) {
        if (loaded()) {
            TensuraUse.forget(p);
        }
    }

    /** @return a refusal key suffix, null when the mode changed */
    @Nullable
    static String changeMode(ServerPlayer p, ResourceLocation id, int delta) {
        return loaded() ? TensuraModeChange.change(p, id, delta) : "not_unlocked";
    }
}
