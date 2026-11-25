package net.ofatech.supernaturalbloodlines.core.group.brotherhood;

import java.util.EnumSet;
import java.util.Set;

/** R297: the cure-map tattoo is drawn region by region as the kills grow; it is a picture, never a road. */
public final class MapRegions {
    public enum Region {
        SIGIL(1), TWINS(4), RING_AND_STAR(8), SLEEPER(12), SWORDS(16), WHOLE(20);

        public final int kills;

        Region(int kills) {
            this.kills = kills;
        }
    }

    private MapRegions() {
    }

    public static Set<Region> revealed(int kills) {
        Set<Region> out = EnumSet.noneOf(Region.class);
        for (Region r : Region.values()) {
            if (kills >= r.kills) {
                out.add(r);
            }
        }
        return out;
    }

    /** The whole map for everyone once any brother has the full mark; else only what the viewer's own kills show. */
    public static Set<Region> shown(int ownKills, boolean anyHasMax) {
        return anyHasMax ? EnumSet.allOf(Region.class) : revealed(ownKills);
    }
}