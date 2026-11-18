package dev.corgitaco.enhancedcelestials2core.api.lunarevent;

import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;
import net.minecraft.core.Holder;

/** A test stand-in (see EcReflectTest). */
public class LunarForecast {
    public boolean active;
    public Holder<LunarEvent> current;
    public final Long2ObjectRBTreeMap<Holder<LunarEvent>> upcoming = new Long2ObjectRBTreeMap<>();
    public long removed = -1;

    public boolean isEventActive() {
        return active;
    }

    public Holder<LunarEvent> currentLunarEventHolder() {
        return current;
    }

    public Long2ObjectRBTreeMap<Holder<LunarEvent>> upcomingEventsByDaysInAdvance() {
        return upcoming;
    }

    public void setLunarEventTonight(Holder<LunarEvent> event) {
        current = event;
        active = true;
    }

    public boolean removeLunarEvent(long daysAhead) {
        removed = daysAhead;
        active = false;
        return true;
    }
}
