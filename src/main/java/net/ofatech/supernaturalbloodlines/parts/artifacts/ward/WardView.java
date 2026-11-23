package net.ofatech.supernaturalbloodlines.parts.artifacts.ward;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.ward.WardRules;
import net.ofatech.supernaturalbloodlines.core.ward.WardTable;
import net.ofatech.supernaturalbloodlines.shared.hub.WardHub;

/** What a player is shown of the wards (R310): their own, with their invite lists, and who has invited them. Never another owner's ward, place or list. */
public final class WardView {
    private WardView() {
    }

    public static WardHub.View of(ServerPlayer viewer) {
        WardTable table = WardIndex.get(viewer.serverLevel()).table();
        UUID me = viewer.getUUID();
        List<WardHub.Own> own = new ArrayList<>();
        for (WardTable.Ward ward : table.ownedBy(me)) {
            List<WardHub.Member> invited = new ArrayList<>();
            for (UUID id : ward.invited()) {
                if (!id.equals(me)) {
                    invited.add(new WardHub.Member(id, WardService.nameOf(viewer.serverLevel(), id)));
                }
            }
            own.add(new WardHub.Own(ward.id(), ward.kind() == WardRules.Kind.DOOR, ward.fallen(), ward.dimension(), ward.cx(), ward.cy(), ward.cz(), invited));
        }
        List<String> welcomed = new ArrayList<>();
        for (WardTable.Ward ward : table.welcoming(me)) {
            String name = WardService.nameOf(viewer.serverLevel(), ward.owner());
            if (!welcomed.contains(name)) {
                welcomed.add(name);
            }
        }
        return new WardHub.View(own, welcomed);
    }
}
