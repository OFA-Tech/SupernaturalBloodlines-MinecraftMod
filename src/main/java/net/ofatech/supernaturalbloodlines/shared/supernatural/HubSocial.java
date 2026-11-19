package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;

/**
 * What the hub shows about other players: names, ranks and online flags only, never their attachments. The species part (which owns the
 * sireline index, the pack index and the Five roster) installs it; without it the hub shows nobody.
 */
public final class HubSocial {
    /** @param rank {@code alpha}, {@code beta}, {@code emissary} and so on; an Alpha with stacked sparks carries the count after a colon ({@code alpha:3}) */
    public record Member(String name, String rank, boolean online, Optional<java.util.UUID> id) {
        public Member(String name, String rank, boolean online) {
            this(name, rank, online, Optional.empty());
        }

        /** The rank without the spark count: {@code alpha:3} is {@code alpha}. */
        public String base() {
            int i = rank.indexOf(':');
            return i < 0 ? rank : rank.substring(0, i);
        }

        /** How many Alpha sparks this member holds (1 when the rank does not say). */
        public int sparks() {
            int i = rank.indexOf(':');
            if (i < 0) {
                return 1;
            }
            try {
                return Math.max(1, Integer.parseInt(rank.substring(i + 1)));
            } catch (NumberFormatException bad) {
                return 1;
            }
        }
    }

    /**
     * The members of the viewer's pack the server counts for a Surge: everyone in the pack but the viewer, leaving out allied leaders (another pack) and
     * the NPC rows (a Brother, a subordinate), who do not count toward the pack of players.
     */
    public static int surgeMates(List<Member> pack, java.util.UUID self) {
        int n = 0;
        for (Member m : pack) {
            String base = m.base();
            boolean me = m.id().isPresent() && m.id().get().equals(self);
            if (!me && !base.equals("ally") && !base.equals("brother") && !base.equals("subordinate")) {
                n++;
            }
        }
        return n;
    }

    /** One of the family below the player: {@code depth} 1 is sired by them, 2 by those, and so on. */
    public record Kin(String name, int depth) {
    }

    /** A person the hub already names (the sire, someone sired, a pack member) and the id a click on them opens a Profile with. Nothing else about them. */
    public record Person(String name, java.util.UUID id) {
    }

    /**
     * A rite of this mod the server has loaded (so its part and mods are on), as the hub lists it.
     *
     * @param items   "count id" per item
     * @param shapes  the circle shape ids
     * @param factory the rite's factory type id; {@code faction} is its {@code faction} setting, or "" when it has none
     */
    public record Rite(String id, int power, List<String> items, List<String> shapes, String factory, String faction) {
        public Rite {
            items = List.copyOf(items);
            shapes = List.copyOf(shapes);
        }
    }

    /** How many of the sire's ancestors the snapshot carries (the server cuts the list, the codec bounds it). */
    public static final int MAX_ANCESTORS = 8;

    /** How a player left the line of their Original: by the Rite of the Broken Line (alone) or the Rite of the Imprisoned Original (with their branch). */
    public enum FreedKind { NONE, BROKEN_LINE, IMPRISONED }

    /**
     * Where the player stands in an Original's line (R120 to R127), for the Bloodline page. Only what the player may know: a root they have not
     * discovered is never in here.
     *
     * @param root           the Original of the player's line as a name to show, or empty to draw "?"; an NPC Original is {@code npc:<id>}
     * @param freedFrom      in {@code FREE}: the Original they were freed from, same convention, empty while undiscovered
     * @param ancestorHidden parallel to {@link Social#ancestors()}: true for an ancestor who is not a player (drawn as "?")
     */
    public record Origin(LineRules.State state, Optional<String> root, Optional<String> freedFrom, FreedKind freedKind, List<Boolean> ancestorHidden) {
        public static final Origin NONE = new Origin(LineRules.State.NOT_VAMPIRE, Optional.empty(), Optional.empty(), FreedKind.NONE, List.of());

        public Origin {
            ancestorHidden = List.copyOf(ancestorHidden);
        }
    }

    /**
     * @param ancestors the sire's sire and up, nearest first (the sire itself is {@code sire}); "" for a name the server does not know
     * @param line      everyone below the player, depth-first
     */
    public record Social(Optional<String> sire, List<String> sired, List<Member> pack, int fiveCount, List<String> ancestors, List<Kin> line,
                         List<Rite> rites, Origin origin, List<Person> people) {
        public static final Social NONE = new Social(Optional.empty(), List.of(), List.of(), 0, List.of(), List.of(), List.of(), Origin.NONE);

        public Social(Optional<String> sire, List<String> sired, List<Member> pack, int fiveCount, List<String> ancestors, List<Kin> line,
                      List<Rite> rites, Origin origin) {
            this(sire, sired, pack, fiveCount, ancestors, line, rites, origin, List.of());
        }

        public Social {
            people = List.copyOf(people);
            sired = List.copyOf(sired);
            pack = List.copyOf(pack);
            ancestors = List.copyOf(ancestors);
            line = List.copyOf(line);
            rites = List.copyOf(rites);
        }
    }

    private static volatile Function<ServerPlayer, Social> source = p -> Social.NONE;

    private HubSocial() {
    }

    public static void set(Function<ServerPlayer, Social> function) {
        source = function;
    }

    /** The installed source, so a test can put it back. */
    public static Function<ServerPlayer, Social> source() {
        return source;
    }

    public static Social of(ServerPlayer p) {
        return source.apply(p);
    }

    /**
     * A wolf (or Tensura subordinate) of the viewer's pack, as the Pack page and the NPC profile show it. {@code token} is the ledger's short id (never an entity
     * id), {@code source} {@code brother} or {@code subordinate}, {@code order} {@code follow}, {@code guard} or {@code rest} (empty for a subordinate: Tensura's
     * own orders), {@code health} a percent (-1 when it is not loaded), {@code distance} blocks (-1 when not loaded or in another dimension), {@code days} how
     * long it has been in the pack, {@code owner} the owner's name, {@code canOrder} whether the viewer is its owner or the leader of its owner's pack, {@code mine} whether the viewer is its owner (only the owner releases it).
     */
    public record Npc(int token, String name, String type, String source, String order, boolean hunt, boolean sort, boolean loaded, int distance, int health,
                      int days, String owner, boolean canOrder, boolean mine, int weightPercent) {
        public boolean ours() {
            return source.equals("brother");
        }
    }

    public static final int MAX_NPCS = 64;

    private static volatile Function<ServerPlayer, List<Npc>> npcSource = p -> List.of();

    /** The species part installs where the pack's NPC members come from; without it the hub shows none. */
    public static void setNpcs(Function<ServerPlayer, List<Npc>> function) {
        npcSource = function;
    }

    public static Function<ServerPlayer, List<Npc>> npcSource() {
        return npcSource;
    }

    public static List<Npc> npcsOf(ServerPlayer p) {
        List<Npc> all = npcSource.apply(p);
        return List.copyOf(all.subList(0, Math.min(all.size(), MAX_NPCS)));
    }
}