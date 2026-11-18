package net.ofatech.supernaturalbloodlines.shared.supernatural.profile;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.shared.admin.AdminAccess;

/**
 * Builds a profile on the server. The species part (which owns the pack, sireline and line indexes) installs the {@link Source}; without
 * it a profile has no more than the basics. The relation always comes from the server's own data, never from the client's request.
 */
public final class ProfileService {
    /** @param opView true only for {@link #ofForOp}: the Admin page's own view, never the player-facing one */
    public record Profile(UUID subject, boolean opView, boolean npc, Map<Fact, List<String>> facts) {
        public Profile {
            facts = Map.copyOf(facts);
        }

        public Profile(UUID subject, boolean opView, Map<Fact, List<String>> facts) {
            this(subject, opView, false, facts);
        }
    }

    public interface Source {
        Relation relationOf(ServerPlayer observer, UUID subject);

        /** What the observer has discovered about the subject (their own record and their pack's pool, and what the rules show them for free). */
        Set<Fact> discovered(ServerPlayer observer, UUID subject, Relation relation);

        /** The value of one fact; empty when the server has nothing to say (an offline subject's level). Called only for facts that may be sent. */
        Optional<List<String>> read(ServerPlayer observer, UUID subject, Relation relation, Fact fact);
    }

    /** Without the species part: the subject is a stranger if they are online, else nobody, and yourself is yourself. */
    private static final Source BASIC = new Source() {
        @Override
        public Relation relationOf(ServerPlayer observer, UUID subject) {
            if (observer.getUUID().equals(subject)) {
                return Relation.SELF;
            }
            return observer.server.getPlayerList().getPlayer(subject) != null ? Relation.STRANGER : Relation.STRANGER_OFFLINE;
        }

        @Override
        public Set<Fact> discovered(ServerPlayer observer, UUID subject, Relation relation) {
            return EnumSet.noneOf(Fact.class);
        }

        @Override
        public Optional<List<String>> read(ServerPlayer observer, UUID subject, Relation relation, Fact fact) {
            ServerPlayer who = observer.server.getPlayerList().getPlayer(subject);
            return switch (fact) {
                case NAME -> Optional.ofNullable(who).map(w -> List.of(w.getGameProfile().getName()));
                case PRESENCE -> Optional.of(List.of(who != null ? "online" : "offline"));
                default -> Optional.empty();
            };
        }
    };

    private static volatile Source source = BASIC;

    private ProfileService() {}

    public static void set(Source s) {
        source = s;
    }

    public static Source source() {
        return source;
    }

    public static Source basic() {
        return BASIC;
    }

    /** The profile of {@code subject} as {@code observer} may know it. Never an OP view, whoever asks. */
    public static Profile of(ServerPlayer observer, UUID subject) {
        Source s = source;
        Relation relation = s.relationOf(observer, subject);
        if (relation == Relation.OP_VIEW) {
            relation = Relation.STRANGER; // a source must not hand out the OP view; only ofForOp does
        }
        return build(s, observer, subject, relation, false);
    }

    /** The OP view: every fact. It exists only for a sender whose permission level passes {@link AdminAccess#allowed}; below that it is empty. */
    public static Optional<Profile> ofForOp(ServerPlayer observer, int permissionLevel, UUID subject) {
        if (!AdminAccess.allowed(permissionLevel)) {
            return Optional.empty();
        }
        return Optional.of(build(source, observer, subject, Relation.OP_VIEW, true));
    }

    private static Profile build(Source s, ServerPlayer observer, UUID subject, Relation relation, boolean opView) {
        Set<Fact> discovered = relation == Relation.OP_VIEW ? EnumSet.noneOf(Fact.class) : s.discovered(observer, subject, relation);
        final Relation used = relation;
        Map<Fact, List<String>> facts = ProfileView.build(relation, discovered, f -> s.read(observer, subject, used, f));
        return new Profile(subject, opView, ProfileView.npc(relation, facts), facts);
    }
}

