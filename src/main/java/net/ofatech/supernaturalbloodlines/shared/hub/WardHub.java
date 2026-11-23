package net.ofatech.supernaturalbloodlines.shared.hub;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;

/**
 * What the Path page shows of a player's wards (R310): the wards they own, each with who is invited, and the names of the people whose home
 * welcomes them. Built by the artifacts part and sent by the supernatural part, which do not know each other, so the two meet here. Only the
 * viewer's own wards and the names of the owners who invited them: nothing about anybody else's ward, place or list.
 */
public final class WardHub {
    public static final int MAX_OWN = 8;
    public static final int MAX_INVITED = 16;
    public static final int MAX_WELCOME = 16;
    public static final int MAX_NAME = 16;
    public static final int MAX_DIMENSION = 64;

    public record Member(UUID id, String name) {
        public Member {
            name = name.length() > MAX_NAME ? name.substring(0, MAX_NAME) : name;
        }
    }

    public record Own(String id, boolean door, boolean fallen, String dimension, int x, int y, int z, List<Member> invited) {
        public Own {
            invited = List.copyOf(invited.subList(0, Math.min(invited.size(), MAX_INVITED)));
        }
    }

    public record View(List<Own> own, List<String> welcomedBy) {
        public static final View EMPTY = new View(List.of(), List.of());

        public View {
            own = List.copyOf(own.subList(0, Math.min(own.size(), MAX_OWN)));
            welcomedBy = List.copyOf(welcomedBy.subList(0, Math.min(welcomedBy.size(), MAX_WELCOME)));
        }

        public boolean isEmpty() {
            return own.isEmpty() && welcomedBy.isEmpty();
        }
    }

    private static volatile Function<ServerPlayer, View> source = p -> View.EMPTY;
    private static volatile Consumer<ServerPlayer> changed = p -> { };

    private WardHub() {
    }

    /** The artifacts part gives how a view is built. */
    public static void setSource(Function<ServerPlayer, View> view) {
        source = view;
    }

    /** The supernatural part gives what to do when somebody's view changed (send it). */
    public static void onChanged(Consumer<ServerPlayer> send) {
        changed = send;
    }

    public static View of(ServerPlayer p) {
        return source.apply(p);
    }

    /** Somebody's wards or invitations changed: their view is sent again. */
    public static void changed(ServerPlayer p) {
        changed.accept(p);
    }
}
