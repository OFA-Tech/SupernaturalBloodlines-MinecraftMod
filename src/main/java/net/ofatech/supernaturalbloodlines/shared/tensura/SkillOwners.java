package net.ofatech.supernaturalbloodlines.shared.tensura;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Several sources can grant the same Tensura skill (the species layer and the werewolf level grants both give
 * {@code godwolf_sense}, and the rank skills will too). One rule keeps them from taking it from each other:
 * <ul>
 * <li>a skill this mod granted carries the instance mark {@link #GRANTED_TAG}; a skill the player had from elsewhere
 * has none and is never forgotten;</li>
 * <li>every source registers a predicate "do I still want skill S for player P" ({@link #register});</li>
 * <li>a source that stops wanting a skill forgets it only if no other registered source still wants it
 * ({@link #release}); otherwise the skill stays, with its mark, and whoever wants it last forgets it.</li>
 * </ul>
 * A new source (Task 5's werewolf rank skills) registers its predicate once at init and releases through
 * {@link #release}. No Tensura classes: only ManasCore's skill API.
 */
public final class SkillOwners {
    public static final String GRANTED_TAG = "supernaturalbloodlines:granted";
    private static final String NO_MAGICULE_COST = "NoMagiculeCost";

    /** Does this source still want {@code skill} for {@code p}, judging by the player's current state. */
    @FunctionalInterface
    public interface Wants {
        boolean wants(ServerPlayer p, ResourceLocation skill);
    }

    private static final Map<String, Wants> SOURCES = new ConcurrentHashMap<>();

    private SkillOwners() {}

    /** Registers (or replaces) the source with this name. */
    public static void register(String source, Wants wants) {
        SOURCES.put(source, wants);
    }

    /** Some registered source other than {@code self} wants the skill. */
    public static boolean wantedByAnother(ServerPlayer p, ResourceLocation skill, String self) {
        for (Map.Entry<String, Wants> e : SOURCES.entrySet()) {
            if (!e.getKey().equals(self) && e.getValue().wants(p, skill)) {
                return true;
            }
        }
        return false;
    }

    /** The player holds the skill and this mod put it there. */
    public static boolean isGranted(ServerPlayer p, ResourceLocation skill) {
        return SkillAPI.getSkillsFrom(p).getSkill(skill)
                .filter(held -> held.getTag() != null && held.getTag().getBoolean(GRANTED_TAG)).isPresent();
    }

    /**
     * Learns the skill without Tensura's magicule charge (it vetoes a learned skill when max magicule is too low) and
     * marks it as granted. Returns whether the player holds it afterwards.
     */
    public static boolean grant(ServerPlayer p, ManasSkill skill) {
        ManasSkillInstance instance = skill.createDefaultInstance();
        // Tensura removes this switch itself while it unlocks, so it is one-shot; the grant mark stays
        instance.getOrCreateTag().putBoolean(NO_MAGICULE_COST, true);
        instance.getOrCreateTag().putBoolean(GRANTED_TAG, true);
        var skills = SkillAPI.getSkillsFrom(p);
        skills.learnSkill(instance);
        return skills.getSkill(skill.getRegistryName()).isPresent();
    }

    /**
     * {@code self} no longer wants the skill: forgets it unless another source still does. Returns whether it was
     * forgotten. Call it only for a skill {@code self} granted or co-owns.
     */
    public static boolean release(ServerPlayer p, ResourceLocation skill, String self) {
        if (wantedByAnother(p, skill, self)) {
            return false;
        }
        var skills = SkillAPI.getSkillsFrom(p);
        skills.getSkill(skill).ifPresent(skills::forgetSkill);
        return true;
    }
}
