package net.ofatech.supernaturalbloodlines.parts.hud.client.vampirism;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.client.VIngameOverlays;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import java.util.function.BooleanSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The one blood value, read from Vampirism, and the hiding of Vampirism's own blood bar while it is shown elsewhere. Linked to Vampirism: reached only
 * after {@code ModList.isLoaded("vampirism")}.
 */
public final class VampireBlood {
    private static final ResourceLocation VAMPIRE = ResourceLocation.parse("vampirism:vampire");

    private VampireBlood() {
    }

    public static boolean isVampire(Player p) {
        IPlayableFaction<?> f = VampirismAPI.factionPlayerHandler(p).getCurrentFaction();
        return f != null && VAMPIRE.equals(f.getID());
    }

    /** The player has a Vampirism faction (vampire, werewolf or hunter). */
    public static boolean hasFaction(Player p) {
        return VampirismAPI.factionPlayerHandler(p).getCurrentFaction() != null;
    }

    /** Vampirism's blood level of this player (0 to 20). */
    public static int level(Player p) {
        return VampirismAPI.vampirePlayer(p).getBloodStats().getBloodLevel();
    }

    /** Cancels Vampirism's blood bar layer while {@code hideBar} says so (the layer is a NeoForge GUI layer, so no mixin). It is never removed, only skipped. */
    public static void registerBarHiding(BooleanSupplier hideBar) {
        NeoForge.EVENT_BUS.addListener((RenderGuiLayerEvent.Pre e) -> {
            if (VIngameOverlays.BLOOD_BAR_ID.equals(e.getName()) && hideBar.getAsBoolean()) {
                e.setCanceled(true);
            }
        });
    }
}
