package net.ofatech.supernaturalbloodlines.parts.hud.client;

import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.hud.SupernaturalStanding;
import net.ofatech.supernaturalbloodlines.parts.hud.client.vampirism.VampireBlood;

/** Whether the local player is supernatural, worked out once a client tick; a plain human is never offered Supernatural mode. */
final class HudStanding {
    private static final boolean VAMPIRISM = ModList.get().isLoaded("vampirism");
    private static boolean supernatural;

    private HudStanding() {
    }

    static boolean supernatural() {
        return supernatural;
    }

    /** Called once a client tick, with the player (null outside a world). */
    static void tick(Player p) {
        supernatural = p != null && of(p);
    }

    private static boolean of(Player p) {
        BloodlineState s = BloodlineAccess.get(p);
        boolean faction = VAMPIRISM && VampireBlood.hasFaction(p);
        return SupernaturalStanding.is(faction, s.species() != Species.NONE || s.witchLayer(), s.inTurning(), s.ghoul());
    }
}
