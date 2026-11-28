package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.shared.hub.Rect;
import org.jetbrains.annotations.Nullable;

/**
 * Where the names round a ring of portraits go. A name is asked for at the place it likes (beside its face, or over or under it) with one edge fixed; it is kept
 * inside the page, and where it would cover a face, the line under the model or a name already placed, it is cut shorter from its free end (the caller ends
 * it in an ellipsis) until it clears, or left out when not even {@code minWidth} fits (the tooltip of the face has the name). So two neighbours can never
 * run into one word ("LydiaStiles"). Pure.
 */
public final class RingLabels {
    public enum Anchor { LEFT, RIGHT, CENTER }

    /** A name asked for: {@code x} is its left edge, right edge or centre by {@code anchor}; {@code w} is the width it would like. */
    public record Request(int x, int y, int w, int h, Anchor anchor) {
    }

    private RingLabels() {
    }

    /** The name at width {@code w}: as asked for, cut from its right end (the left edge stays) or from its left end (the right edge stays), or slid to the free side of its face where that clears a neighbour. */
    private static Rect at(Request r, int w, int variant) {
        int full = r.w();
        int left = switch (r.anchor()) {
            case LEFT -> r.x();
            case RIGHT -> r.x() - full;
            case CENTER -> r.x() - full / 2;
        };
        int x = switch (variant) {
            case 1 -> left;
            case 2 -> left + full - w;
            case 3 -> r.anchor() == Anchor.CENTER ? r.x() - w : left + (full - w) / 2; // a name under its face may stand to the left of it
            case 4 -> r.anchor() == Anchor.CENTER ? r.x() : left + (full - w) / 2; // or to the right of it, at its full width
            default -> switch (r.anchor()) {
                case LEFT -> r.x();
                case RIGHT -> r.x() - w;
                case CENTER -> r.x() - w / 2;
            };
        };
        return new Rect(x, r.y(), w, r.h());
    }

    /**
     * @param blocked what a name may not cover: the faces (with their rings) and the line under the model
     * @return for each request the rectangle its name is drawn in, or null for a name left out
     */
    public static List<@Nullable Rect> place(List<Request> requests, List<Rect> blocked, Rect bounds, int minWidth, int gap) {
        List<Rect> placed = new ArrayList<>();
        List<Rect> out = new ArrayList<>();
        for (Request asked : requests) {
            Request r = asked;
            Rect found = search(r, blocked, bounds, minWidth, gap, placed);
            if (found == null && r.h() > LINE) {
                // two lines do not fit where it stands: one line, cut with an ellipsis (the caller draws as many lines as the rectangle is tall), at the end of the block nearer the face first
                for (int y : new int[] {r.y(), r.y() + r.h() - LINE}) {
                    found = found != null ? found : search(new Request(r.x(), y, r.w(), LINE, r.anchor()), blocked, bounds, minWidth, gap, placed);
                }
            }
            out.add(found);
            if (found != null) {
                placed.add(found);
            }
        }
        return out;
    }

    /**
     * A name that found no room beside its face goes under the face, else over it (centred on it), so the names of a small ring (GUI scale 4)
     * are not all left to the tooltip.
     *
     * @return the rectangle, or null when neither side clears
     */
    public static @Nullable Rect underOrOver(Request asked, int faceX, int faceY, int faceSize, List<Rect> blocked, Rect bounds, int minWidth, int gap) {
        for (int y : new int[] {faceY + faceSize / 2 + 3, faceY - faceSize / 2 - LINE - 2}) {
            Rect got = place(List.of(new Request(faceX, y, asked.w(), LINE, Anchor.CENTER)), blocked, bounds, minWidth, gap).get(0);
            if (got != null) {
                return got;
            }
        }
        return null;
    }

    /**
     * A name that was cut beside its face ("Rim…") moves under or over the face when it fits whole there: a whole name beats a cut one in the place it
     * liked. {@code taken} is everything else on the page (the faces and the other names).
     *
     * @return the rectangle to draw the name in: the whole one, or {@code got} when the name was not cut or no whole place clears
     */
    public static @Nullable Rect preferWhole(Request asked, @Nullable Rect got, int faceX, int faceY, int faceSize, List<Rect> taken, Rect bounds, int gap) {
        if (got == null || got.w() >= asked.w()) {
            return got;
        }
        Rect whole = underOrOver(asked, faceX, faceY, faceSize, taken, bounds, asked.w(), gap);
        return whole != null ? whole : got;
    }

    /** The height of one line of a name. */
    public static final int LINE = 9;

    private static @Nullable Rect search(Request r, List<Rect> blocked, Rect bounds, int minWidth, int gap, List<Rect> placed) {
        // the minimum is for a name that has to be cut: a short name that fits whole is never left out for being shorter than it
        for (int w = r.w(); w >= Math.min(minWidth, r.w()); w--) {
            for (int variant = 0; variant < 5; variant++) {
                Rect cand = at(r, w, variant);
                // kept inside the page by sliding, as a name at the page edge always was
                int dx = Math.max(bounds.x() - cand.x(), Math.min(0, bounds.right() - cand.right()));
                cand = new Rect(cand.x() + dx, cand.y(), cand.w(), cand.h());
                if (!cand.inside(bounds)) {
                    continue;
                }
                boolean clear = true;
                for (Rect b : blocked) {
                    clear &= !cand.overlaps(b);
                }
                Rect padded = cand.inset(-gap / 2);
                for (Rect p : placed) {
                    clear &= !padded.overlaps(p.inset(-gap / 2));
                }
                if (clear) {
                    return cand;
                }
            }
        }
        return null;
    }
}