package net.ofatech.supernaturalbloodlines.parts.artifacts.ward;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.ofatech.supernaturalbloodlines.core.ward.WardSpeech;
import net.ofatech.supernaturalbloodlines.core.ward.WardTable;
import net.ofatech.supernaturalbloodlines.parts.artifacts.ArtifactsConfig;

/**
 * "Come in" and "get out" (R305): the owner of a ward says the whole phrase in chat and the player they look at, or the nearest one, is invited or
 * withdrawn. The message is not cancelled (everybody reads it) and a phrase that finds nobody does nothing and says nothing, so it reveals nothing.
 * The chat event may run off the server thread, so the work is handed to the server.
 */
public final class WardChat {
    private static final double MARGIN = 8.0;
    private static final double LOOK_DOT = 0.97;

    private WardChat() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerChatEvent e) -> {
            ServerPlayer speaker = e.getPlayer();
            boolean in = WardSpeech.matches(e.getRawText(), WardSpeech.COME_IN);
            boolean out = !in && WardSpeech.matches(e.getRawText(), WardSpeech.GET_OUT);
            if (in || out) {
                speaker.server.execute(() -> speak(speaker, in));
            }
        });
    }

    /** The phrase said by {@code speaker}: {@code invite} for "come in", else "get out". Public for the tests. */
    public static void speak(ServerPlayer speaker, boolean invite) {
        if (!ArtifactsConfig.wardSpokenInvites()) {
            return;
        }
        WardIndex index = WardIndex.get(speaker.serverLevel());
        List<WardTable.Ward> mine = index.table().standingOf(speaker.getUUID());
        String dimension = speaker.serverLevel().dimension().location().toString();
        boolean near = mine.stream().anyMatch(w -> w.dimension().equals(dimension) && w.box().near(speaker.getX(), speaker.getY(), speaker.getZ(), MARGIN));
        if (!near) {
            return;
        }
        Optional<UUID> picked = WardSpeech.pick(candidates(speaker));
        if (picked.isEmpty()) {
            return;
        }
        ServerPlayer who = WardService.find(speaker.serverLevel(), picked.get());
        if (who == null) {
            return;
        }
        if (invite) {
            WardService.invite(speaker, who);
        } else {
            WardService.withdraw(speaker, who.getUUID());
        }
    }

    private static List<WardSpeech.Candidate> candidates(ServerPlayer speaker) {
        List<WardSpeech.Candidate> out = new ArrayList<>();
        Vec3 look = speaker.getLookAngle();
        for (ServerPlayer other : speaker.serverLevel().players()) {
            if (other == speaker || other.isSpectator() || !other.isAlive()) {
                continue;
            }
            Vec3 to = other.getEyePosition().subtract(speaker.getEyePosition());
            double distance = to.length();
            boolean looked = distance > 1.0E-6 && to.normalize().dot(look) >= LOOK_DOT && speaker.hasLineOfSight(other);
            out.add(new WardSpeech.Candidate(other.getUUID(), distance, looked));
        }
        return out;
    }
}
