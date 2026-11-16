package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.ArrayList;
import java.util.List;

/** Nodes round a centre (a radial skill tree) and the arrow keys' move between them. Pure. */
public final class Radial {
    public record Node(int x, int y) {
    }

    public enum Dir {
        UP, DOWN, LEFT, RIGHT
    }

    private Radial() {
    }

    /** {@code n} nodes from the top, clockwise. */
    public static List<Node> ring(int cx, int cy, int radius, int n) {
        List<Node> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double a = -Math.PI / 2 + 2 * Math.PI * i / n;
            out.add(new Node(cx + (int) Math.round(radius * Math.cos(a)), cy + (int) Math.round(radius * Math.sin(a))));
        }
        return out;
    }

    /**
     * The node an arrow key moves to from {@code from}: among the nodes that lie that way, the one with the least distance along the key's
     * axis plus twice the distance across it. {@code from} itself when none lies that way.
     */
    public static int neighbour(List<Node> nodes, int from, Dir dir) {
        if (from < 0 || from >= nodes.size()) {
            return nodes.isEmpty() ? -1 : 0;
        }
        Node o = nodes.get(from);
        int best = from;
        double bestScore = Double.MAX_VALUE;
        for (int i = 0; i < nodes.size(); i++) {
            if (i == from) {
                continue;
            }
            int dx = nodes.get(i).x() - o.x();
            int dy = nodes.get(i).y() - o.y();
            int along = switch (dir) {
                case UP -> -dy;
                case DOWN -> dy;
                case LEFT -> -dx;
                case RIGHT -> dx;
            };
            int across = Math.abs(dir == Dir.UP || dir == Dir.DOWN ? dx : dy);
            if (along <= 0) {
                continue;
            }
            double score = along + 2.0 * across;
            if (score < bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }
}
