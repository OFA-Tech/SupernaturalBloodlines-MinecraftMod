package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Whether the rule that grants a granted-only skill still holds for a player. A granted-only skill is in the player's saved skills but only
 * counts while this says so, so it cannot outlive its rule: S-WOL-8 Primeval Roar needs a True Alpha on {@code alpha_primeval}, and if the
 * part that grants it (supernatural_tensura) is switched off nothing registers the rule and the skill is inactive, not permanent. A granted-only
 * skill that has no entry here always counts.
 */
public final class GrantedSkills {
    /** The skills with a rule; until a part registers the real one the rule does not hold. */
    private static final Map<String, Predicate<Player>> RULES = new ConcurrentHashMap<>(Map.of("S-WOL-8", p -> false));

    private GrantedSkills() {
    }

    /** Sets the rule; returns the one it replaced (a test puts it back). */
    @Nullable
    public static Predicate<Player> register(String skillId, Predicate<Player> holds) {
        return RULES.put(skillId, holds);
    }

    public static boolean holds(Player player, String skillId) {
        Predicate<Player> rule = RULES.get(skillId);
        return rule == null || rule.test(player);
    }
}
