package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import io.github.manasmods.tensura.network.c2s.RequestNamingMenuPacket;
import io.github.manasmods.tensura.network.c2s.RequestNamingMenuPacket.NamingType;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import io.github.manasmods.tensura.world.TensuraGameRules;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/**
 * R101: siring names the sired player in Tensura. The name is the sired's own current name, so nothing is renamed; it is Tensura's
 * naming link, there to make the two mods compatible. It needs no consent and ignores the {@code playerNaming} game rule and the
 * sired's own nameable toggle: both are lifted for the length of the call and given back.
 * <p>
 * The naming is Tensura's own {@link RequestNamingMenuPacket#name}, so the sire pays Tensura's magicule cost and the sired gets
 * Tensura's EP and stats reward. The type is LOW: it never evolves the sired (R100), and Tensura's own LOW roll decides whether the
 * sire also loses max magicule. Tensura's other checks stay (the sire's health and EP against the sired, an insane sire), and a
 * refusal leaves the sired unnamed; so does a sire who is offline or short of magicules. The attempt is made when the link is made
 * and again whenever the sired or the sire logs in. Without a sire link (switch off, a Tensura owner already there) nothing is named.
 */
public final class SireNaming {
    private SireNaming() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                retryFor(p, p.server.getPlayerList().getPlayers());
            }
        });
    }

    /** A player logged in: name them if they are an unnamed sired, and every unnamed sired of theirs among {@code online}. */
    public static void retryFor(ServerPlayer loggedIn, Iterable<ServerPlayer> online) {
        tryName(loggedIn);
        for (ServerPlayer other : online) {
            if (other != loggedIn && loggedIn.getUUID().equals(BloodlineAccess.get(other).boundSire())) {
                tryName(other);
            }
        }
    }

    /** @return whether the sired has a Tensura name afterwards */
    public static boolean tryName(ServerPlayer sired) {
        IExistence existence = TensuraStorages.getExistenceFrom(sired);
        if (!SpeciesTensuraConfig.sireNaming() || !sired.isAlive() || existence.getName() != null) {
            return existence.getName() != null;
        }
        UUID sire = BloodlineAccess.get(sired).boundSire();
        // the owner is what Tensura's naming checks read: only the link's own sire can name
        if (sire == null || !sire.equals(existence.getPermanentOwner())) {
            return false;
        }
        ServerPlayer namer = SireCall.player(sired.server, sire);
        if (namer == null) {
            return false;
        }
        GameRules.BooleanValue rule = sired.serverLevel().getGameRules().getRule(TensuraGameRules.PLAYER_NAME);
        boolean ruleBefore = rule.get();
        boolean nameableBefore = existence.isNameable();
        try {
            rule.set(true, sired.server);
            existence.setNameable(true);
            RequestNamingMenuPacket.name(sired, namer, NamingType.LOW, sired.getGameProfile().getName());
        } finally {
            rule.set(ruleBefore, sired.server);
            if (!nameableBefore) {
                existence.setNameable(false);
                existence.markDirty();
            }
        }
        return existence.getName() != null;
    }
}
