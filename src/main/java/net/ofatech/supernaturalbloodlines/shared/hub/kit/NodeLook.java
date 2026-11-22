package net.ofatech.supernaturalbloodlines.shared.hub.kit;

/**
 * What a skill node says, in any size: a skill the player has is filled, an available one wears a gold ring, a locked one a faint dotted ring and a dimmed
 * picture, and the chosen one a halo. The big (kit sprite) and the small (flat shape) drawing both read this, so one state means one thing at every size. Pure.
 */
public final class NodeLook {
    public enum Kind { OWNED, AVAILABLE, LOCKED }

    public record Look(boolean filled, boolean goldRing, boolean tickRing, boolean halo, boolean dimIcon) {
    }

    private NodeLook() {
    }

    public static Look of(Kind kind, boolean selected) {
        return new Look(kind == Kind.OWNED, kind == Kind.AVAILABLE, kind == Kind.LOCKED, selected, kind == Kind.LOCKED);
    }
}