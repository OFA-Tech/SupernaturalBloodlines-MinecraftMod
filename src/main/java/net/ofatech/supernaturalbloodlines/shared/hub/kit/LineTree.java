package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/**
 * The player's line as one tree, whichever theme draws it (R120 to R127): the crowned Original at the top (a "?" until discovered), the
 * ancestors, the sire, the player, and the ones they sired below. It decides which nodes exist and how they are joined (solid, broken or
 * faint), where the arrow keys go and what the narrator says; the three themes only place and paint them. Pure: no Minecraft classes.
 */
public final class LineTree {
    /** How many of the player's own sired are drawn; the rest are counted. */
    public static final int MAX_BELOW = 3;
    /** How many ancestors above the sire are drawn (the nearest ones). */
    public static final int MAX_ANCESTORS = 2;

    public static final String ROOT = "line:root";
    public static final String SIRE = "line:sire";
    public static final String YOU = "line:you";
    public static final String NARR = "supernaturalbloodlines.hub.bloodline.narr.";

    public enum Kind { ROOT, ANCESTOR, SIRE, YOU, DESCENDANT }

    /** @param name the raw name, or {@code npc:<id>} for an NPC Original; {@code hidden} means draw "?" and ignore it */
    public record Node(String id, Kind kind, String name, boolean hidden, int depth) {
    }

    public enum LinkStyle { SOLID, BROKEN, FAINT }

    public record Link(String upper, String lower, LinkStyle style) {
    }

    public record Model(LineRules.State state, List<Node> nodes, List<Link> links, boolean padlock, int hiddenBelow) {
        public Model {
            nodes = List.copyOf(nodes);
            links = List.copyOf(links);
        }

        public Optional<Node> node(String id) {
            return nodes.stream().filter(n -> n.id().equals(id)).findFirst();
        }

        public Optional<Link> linkAbove(String id) {
            return links.stream().filter(l -> l.lower().equals(id) && l.style() != LinkStyle.FAINT).findFirst();
        }
    }

    public enum Key { UP, DOWN, LEFT, RIGHT }

    public record Narr(String key, List<String> args) {
    }

    private LineTree() {
    }

    /**
     * @param origin    where the player stands, as the server sent it
     * @param sire      the sire's name, when known
     * @param severed   the Rite of Severance broke the bond with the sire
     * @param ancestors the sire's sire and up, <b>nearest first</b> (as the snapshot sends them), parallel to {@code origin.ancestorHidden()}
     * @param below     everyone below the player, depth-first; the player's own sired are the ones with the least depth
     */
    public static Model build(HubSocial.Origin origin, String you, Optional<String> sire, boolean severed, List<String> ancestors, List<HubSocial.Kin> below) {
        LineRules.State state = origin.state();
        boolean crown = state == LineRules.State.ORIGINAL;
        boolean free = state == LineRules.State.FREE;
        List<Node> nodes = new ArrayList<>();
        List<Link> links = new ArrayList<>();
        int row = 0;
        String upper = null;

        if (!crown) {
            boolean hasRoot = state == LineRules.State.IN_LINE || (free && (sire.isPresent() || origin.freedFrom().isPresent()));
            if (hasRoot) {
                Optional<String> name = free ? origin.freedFrom() : origin.root();
                nodes.add(new Node(ROOT, Kind.ROOT, name.orElse(""), name.isEmpty(), row++));
                upper = ROOT;
            }
            int shown = Math.min(MAX_ANCESTORS, ancestors.size());
            for (int i = 0; i < shown; i++) {
                // anc:0 is the farthest of the nodes shown; the list is nearest first
                int source = shown - 1 - i;
                String id = "line:anc:" + i;
                boolean hidden = hiddenAt(origin, source) || ancestors.get(source).isEmpty();
                nodes.add(new Node(id, Kind.ANCESTOR, hidden ? "" : ancestors.get(source), hidden, row++));
                if (upper != null) {
                    links.add(new Link(upper, id, LinkStyle.SOLID));
                }
                upper = id;
            }
            if (sire.isPresent()) {
                nodes.add(new Node(SIRE, Kind.SIRE, sire.get(), sire.get().isEmpty(), row++));
                if (upper != null) {
                    links.add(new Link(upper, SIRE, LinkStyle.SOLID));
                }
                upper = SIRE;
            } else if (state == LineRules.State.SIRE_UNKNOWN) {
                // somebody turned this player and nobody recorded who: a "?" above them
                nodes.add(new Node(SIRE, Kind.SIRE, "", true, row++));
                upper = SIRE;
            }
        }
        nodes.add(new Node(YOU, Kind.YOU, you, false, row++));
        if (upper != null) {
            // in FREE the link above the player is the one the rite broke; a severed bond to the sire is broken in any state
            boolean broken = free || (severed && SIRE.equals(upper));
            links.add(new Link(upper, YOU, broken ? LinkStyle.BROKEN : LinkStyle.SOLID));
        }

        int top = below.stream().mapToInt(HubSocial.Kin::depth).min().orElse(0);
        List<HubSocial.Kin> direct = below.stream().filter(k -> k.depth() == top).toList();
        int shownBelow = Math.min(MAX_BELOW, direct.size());
        boolean faintToRoot = nodes.stream().anyMatch(n -> n.id().equals(ROOT))
                && (state == LineRules.State.IN_LINE || (free && origin.freedKind() == HubSocial.FreedKind.BROKEN_LINE));
        for (int i = 0; i < shownBelow; i++) {
            String id = "line:down:" + i;
            String name = direct.get(i).name();
            nodes.add(new Node(id, Kind.DESCENDANT, name, name.isEmpty(), row));
            links.add(new Link(YOU, id, LinkStyle.SOLID));
            if (faintToRoot) {
                links.add(new Link(ROOT, id, LinkStyle.FAINT));
            }
        }
        return new Model(state, nodes, links, free, below.size() - shownBelow);
    }

    private static boolean hiddenAt(HubSocial.Origin origin, int i) {
        return i < origin.ancestorHidden().size() && origin.ancestorHidden().get(i);
    }

    private static List<String> chain(Model m) {
        return m.nodes().stream().map(Node::id).filter(id -> !id.startsWith("line:down:")).toList();
    }

    /** One step of the arrow keys: up and down walk the chain (down from the player enters the sired), left and right walk the sired. */
    public static Optional<String> move(Model m, String at, Key key) {
        boolean isDown = at.startsWith("line:down:");
        List<String> chain = chain(m);
        List<String> kids = m.nodes().stream().map(Node::id).filter(id -> id.startsWith("line:down:")).toList();
        switch (key) {
            case UP -> {
                if (isDown) {
                    return Optional.of(YOU);
                }
                int i = chain.indexOf(at);
                return i > 0 ? Optional.of(chain.get(i - 1)) : Optional.empty();
            }
            case DOWN -> {
                if (isDown) {
                    return Optional.empty();
                }
                if (YOU.equals(at)) {
                    return kids.isEmpty() ? Optional.empty() : Optional.of(kids.get(0));
                }
                int i = chain.indexOf(at);
                return i >= 0 && i < chain.size() - 1 ? Optional.of(chain.get(i + 1)) : Optional.empty();
            }
            case LEFT, RIGHT -> {
                int i = kids.indexOf(at);
                int to = i + (key == Key.LEFT ? -1 : 1);
                return i >= 0 && to >= 0 && to < kids.size() ? Optional.of(kids.get(to)) : Optional.empty();
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    /** What the narrator says about a node: a lang key under {@link #NARR} and its arguments, the node's name and the Original's (or "?"). */
    public static Narr narrate(Model m, Node n) {
        String name = n.hidden() ? "?" : n.name();
        String root = m.node(ROOT).filter(r -> !r.hidden()).map(Node::name).orElse("?");
        boolean free = m.state() == LineRules.State.FREE;
        String key = switch (n.kind()) {
            case ROOT -> n.hidden() ? "root_hidden" : "root";
            case ANCESTOR -> n.hidden() ? "ancestor_hidden" : "ancestor";
            case SIRE -> free ? "sire_free" : "sire";
            case YOU -> m.state() == LineRules.State.ORIGINAL ? "you_original" : free ? "you_free" : "you";
            case DESCENDANT -> "descendant";
        };
        return new Narr(NARR + key, List.of(name, root));
    }
}
