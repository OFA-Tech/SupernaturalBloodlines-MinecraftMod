package net.ofatech.supernaturalbloodlines.shared.hub.kit;

/**
 * How big the nodes of a skill ring are drawn. The kit's node is a 34 px picture; the page used to draw 18 px shapes. The big node is used where the ring has the
 * room for it (the chord between two neighbours, less the room for their names, still holds a node), else today's small one, so a crowded ring or a small
 * window keeps the approved layout. Pure.
 */
public final class NodeSize {
    public static final int BIG = 34;
    public static final int SMALL = 18;
    /** A ring smaller than this has no room for a 34 px node round its model. */
    public static final int MIN_RADIUS = 56;
    /** Room kept between two big nodes for their names. */
    public static final int NAME_ROOM = 24;

    private NodeSize() {
    }

    /** The node size for a ring of {@code count} nodes at {@code radius}. */
    public static int of(int radius, int count) {
        if (radius < MIN_RADIUS) {
            return SMALL;
        }
        double chord = count <= 1 ? Double.MAX_VALUE : 2 * radius * Math.sin(Math.PI / count);
        return chord >= BIG + NAME_ROOM ? BIG : SMALL;
    }
}
