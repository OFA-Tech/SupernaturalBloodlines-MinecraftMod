package net.ofatech.supernaturalbloodlines.parts.species.group.brotherhood;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.ofatech.supernaturalbloodlines.core.group.brotherhood.CodeRules;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/** The Code (R294): who is a Code-breaker now. A player row, so it follows the player to another brotherhood and ends by the clock. */
public final class Codes {
    private Codes() {
    }

    /** The one question the four perks and the lead ask after {@link BrotherhoodScope#appliesTo}. */
    public static boolean isBreaker(MinecraftServer server, UUID id) {
        return GroupIndex.get(server.overworld()).breakerOf(id).map(r -> CodeRules.isBreaker(r.until(), PackIndex.now())).orElse(false);
    }
}