package net.ofatech.supernaturalbloodlines.core.bloodline;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** R124: which Originals are open to white oak right now, so each opening warns their line once. Pure. */
public final class WarnLatch {
    private final Set<UUID> open = new HashSet<>();

    /** @return true when this is a new opening (the line is warned); false while it is still open from before */
    public boolean open(UUID root) {
        return open.add(root);
    }

    public void close(UUID root) {
        open.remove(root);
    }

    public boolean isOpen(UUID root) {
        return open.contains(root);
    }

    public void clear() {
        open.clear();
    }
}
