package net.ofatech.supernaturalbloodlines.core.boss;

/** R150: the states a boss goes through. {@code DEAD} is final (only {@code FORGET}, an operator's reset, leaves it); a lost boss may be brought back, or found again where it was, or die. */
public final class BossRules {
    /** {@code FOUND} and {@code FOUND_OPEN}: the entity of a boss written down as lost is seen again, standing or desiccated. */
    public enum Event { SPAWNED, DESICCATED, RETURNED, KILLED, LOST, FOUND, FOUND_OPEN, FORGET }

    private BossRules() {
    }

    public static BossState next(BossState state, Event event) {
        if (event == Event.FORGET) {
            return BossState.UNSEEN;
        }
        return switch (state) {
            case UNSEEN -> event == Event.SPAWNED ? BossState.ACTIVE : BossState.UNSEEN;
            case LOST -> switch (event) {
                case SPAWNED, FOUND -> BossState.ACTIVE;
                case FOUND_OPEN -> BossState.OPEN;
                case KILLED -> BossState.DEAD;
                default -> BossState.LOST;
            };
            case ACTIVE -> switch (event) {
                case DESICCATED -> BossState.OPEN;
                case KILLED -> BossState.DEAD;
                case LOST -> BossState.LOST;
                default -> BossState.ACTIVE;
            };
            case OPEN -> switch (event) {
                case RETURNED -> BossState.ACTIVE;
                case KILLED -> BossState.DEAD;
                case LOST -> BossState.LOST;
                default -> BossState.OPEN;
            };
            case DEAD -> BossState.DEAD;
        };
    }
}