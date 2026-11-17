package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.tensura.event.TensuraEntityEvents;
import io.github.manasmods.tensura.network.c2s.RequestNamingMenuPacket;
import io.github.manasmods.tensura.network.c2s.RequestNamingMenuPacket.NamingType;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ep.IExistence;
import io.github.manasmods.tensura.world.TensuraGameRules;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
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
 * <p>
 * After a Rite of Severance cleared the name ({@link SubordinateLink#SEVERED_KEY}) the sire's naming of the bound sired is a
 * re-naming: with {@code resire_naming_reward} off Tensura's EP and stat reward (its first event value) is set to 0 and the rest,
 * the sire's cost included, is unchanged.
 */
public final class SireNaming {
    private SireNaming() {}

    public static void register() {
        TensuraEntityEvents.NAMING_EVENT.register(SireNaming::onNaming);
    }

    /**
     * R100: Tensura's MEDIUM and HIGH namings evolve any target, a player included, and a sired player is never evolved by their
     * sire. Whoever names (the automatic naming is LOW already, a hand naming through Tensura's menu is not), the sire's naming of
     * their bound sired is LOW. Tensura's max-magicule loss roll then follows the LOW chance.
     */
    private static EventResult onNaming(LivingEntity target, Player namer, Changeable<Double> epGain, Changeable<Double> cost,
                                        Changeable<NamingType> type, Changeable<String> name) {
        if (namer != null && target instanceof ServerPlayer sired && namer.getUUID().equals(BloodlineAccess.get(sired).boundSire())) {
            if (type.get() != NamingType.LOW) {
                type.set(NamingType.LOW);
            }
            // a severance cleared the name: this is a re-naming, and the switch can take the reward away (the cost stays)
            if (sired.getPersistentData().getBoolean(SubordinateLink.SEVERED_KEY) && !SpeciesTensuraConfig.resireNamingReward()) {
                epGain.set(0.0);
            }
        }
        return EventResult.pass();
    }

    /** Called from the login handler in {@link SubordinateLink}: names the player if they are an unnamed sired, and every unnamed sired of theirs among {@code online}. */
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
        if (existence.getName() != null) {
            sired.getPersistentData().remove(SubordinateLink.SEVERED_KEY);
        }
        return existence.getName() != null;
    }
}
