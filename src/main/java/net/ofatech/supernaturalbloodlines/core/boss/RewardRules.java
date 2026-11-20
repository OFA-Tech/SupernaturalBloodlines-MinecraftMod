package net.ofatech.supernaturalbloodlines.core.boss;

/** R153: a boss pays its reward once. {@code boss_reward_on = first_defeat} pays on desiccation or death, {@code death} only on a real death. */
public final class RewardRules {
    public enum When { FIRST_DEFEAT, DEATH }

    public enum Defeat { DESICCATED, DIED }

    private RewardRules() {
    }

    public static boolean pays(When when, Defeat defeat, boolean alreadyRewarded) {
        if (alreadyRewarded) {
            return false;
        }
        return when == When.FIRST_DEFEAT || defeat == Defeat.DIED;
    }

    public static When parse(String config) {
        return "death".equals(config) ? When.DEATH : When.FIRST_DEFEAT;
    }
}