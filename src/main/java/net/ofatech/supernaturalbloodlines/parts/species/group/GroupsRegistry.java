package net.ofatech.supernaturalbloodlines.parts.species.group;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AllyProtection;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackAbsence;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.party.PartyLink;

/**
 * What every group type needs, with or without Werewolves: logins and last-seen, the absence rule, the Open Parties and Claims link,
 * the alliance protection, the group index and the /group tree. The wolf-only pack stack stays behind its own check.
 */
public final class GroupsRegistry {
    private GroupsRegistry() {
    }

    public static void registerAlways() {
        PackIndex.registerSession();
        PackAbsence.register();
        PartyLink.register();
        AllyProtection.register();
        GroupIndex.register();
        GroupSwitcher.register();
        net.ofatech.supernaturalbloodlines.parts.species.group.coven.CovenRegistry.register();
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> GroupCommand.register(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> PackCommand.forget(e.getEntity().getUUID()));
    }
}
