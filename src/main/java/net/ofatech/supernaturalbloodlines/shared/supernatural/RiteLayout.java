package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a rite's items go on the circle picture, the way Enchanted's book lays them out (Modopedia's rings): the first ring holds up to 6 items 16
 * units from the centre, the next 12 items 32 units out, and so on, each ring starting at the top and turning clockwise. Units are pixels of
 * Enchanted's 100 by 100 circle picture, from its centre. Pure.
 */
public final class RiteLayout {
    public static final int FIRST_RING = 6;
    public static final int RADIUS = 16;

    /** The centre of one item, in picture units from the centre of the circle. */
    public record Spot(int x, int y) {
    }

    private RiteLayout() {
    }

    public static List<Spot> items(int count) {
        List<Spot> out = new ArrayList<>();
        int ring = 1;
        for (int i = 0; i < count; ) {
            int size = Math.min(FIRST_RING * ring, count - i);
            double a = 2 * Math.PI / size;
            double x = 0;
            double y = -RADIUS * ring;
            for (int k = 0; k < size; k++) {
                out.add(new Spot((int) Math.round(x), (int) Math.round(y)));
                double nx = x * Math.cos(a) - y * Math.sin(a);
                y = x * Math.sin(a) + y * Math.cos(a);
                x = nx;
            }
            ring++;
            i += size;
        }
        return out;
    }

    /** The picture of one circle in Enchanted's resources: {@code textures/gui/circle_magic_shapes/<shape>_<chalk>.png}. */
    public static String shapeTexture(HubSocial.Circle c) {
        return "textures/gui/circle_magic_shapes/" + c.shape() + "_" + c.chalk() + ".png";
    }
}
