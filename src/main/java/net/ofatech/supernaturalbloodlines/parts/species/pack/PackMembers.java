package net.ofatech.supernaturalbloodlines.parts.species.pack;

import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/** Taking someone out of a pack: the kick of the leader, and a member who left the linked party. */
public final class PackMembers {
    private PackMembers() {
    }

    /**
     * Removes {@code member} from their pack. A werewolf Beta becomes an Omega now, or at their next login when away.
     * @return false if they were in no pack
     */
    public static boolean remove(MinecraftServer server, UUID member, Component reasonForThem) {
        PackIndex index = PackIndex.get(server.overworld());
        if (!index.kick(member)) {
            return false;
        }
        ServerPlayer online = SireCall.player(server, member);
        if (online != null) {
            WolfState w = PackAccess.wolf(online);
            if (w.lineage() != Lineage.NONE && !PackAccess.isAlpha(w.rank())) {
                PackAccess.applyPack(online, PackEvent.LEFT_PACK, PackContext.defaults());
            }
            // done now: left alone it would take them out of the next pack they join
            index.takeDissolved(member);
            online.sendSystemMessage(reasonForThem);
        }
        return true;
    }
}
