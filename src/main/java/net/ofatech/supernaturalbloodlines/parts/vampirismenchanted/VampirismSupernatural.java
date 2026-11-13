package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFaction;
import de.teamlapen.vampirism.api.entity.factions.IFactionEntity;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.core.Supernatural;

/**
 * What the Vampirism factions tell {@link Supernatural}: vampires and werewolves, players and mobs, are supernatural and
 * prey. Hunters are not. Registered by this part (artifacts, species and hunters all rely on it, and any of them can be
 * off). Factions are matched by id, so no Werewolves class is linked.
 */
public final class VampirismSupernatural {
    private static final ResourceLocation VAMPIRE = ResourceLocation.parse("vampirism:vampire");
    private static final ResourceLocation WEREWOLF = ResourceLocation.parse("werewolves:werewolf");

    private VampirismSupernatural() {
    }

    public static void register() {
        Supernatural.register(e -> isVampire(e) || isWerewolf(e), VampirismSupernatural::isVampire, e -> isVampire(e) || isWerewolf(e));
    }

    public static boolean isVampire(Entity e) {
        return VAMPIRE.equals(factionId(e));
    }

    public static boolean isWerewolf(Entity e) {
        return WEREWOLF.equals(factionId(e));
    }

    private static ResourceLocation factionId(Entity e) {
        if (e instanceof Player p) {
            IPlayableFaction<?> f = VampirismAPI.factionPlayerHandler(p).getCurrentFaction();
            return f == null ? null : f.getID();
        }
        if (e instanceof IFactionEntity fe) {
            IFaction<?> f = fe.getFaction();
            return f == null ? null : f.getID();
        }
        return null;
    }
}
