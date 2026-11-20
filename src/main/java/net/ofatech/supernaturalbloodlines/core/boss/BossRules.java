package net.ofatech.supernaturalbloodlines.core.boss;

/** R150: the states a boss goes through. {@code DEAD} is final (only {@code FORGET}, an operator's reset, leaves it); a lost boss may be brought back. */
public final class BossRules {
    public enum Event { SPAWNED, DESICCATED, RETURNED, KILLED, LOST, FORGET }

    private BossRules() {
    }

    public static BossState next(BossState state, Event event) {
        if (event == Event.FORGET) {
            return BossState.UNSEEN;
        }
        return switch (state) {
            case UNSEEN -> event == Event.SPAWNED ? BossState.ACTIVE : BossState.UNSEEN;
            case LOST -> event == Event.SPAWNED ? BossState.ACTIVE : BossState.LOST;
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