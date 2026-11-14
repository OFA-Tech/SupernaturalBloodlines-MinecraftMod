package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import java.util.List;

/**
 * What a species adds on top of the player's Tensura race. Skill ids are full ("tensura:drain"). The bonuses are
 * fractions of the base maximum (0.20 = +20%). No game classes.
 */
public record LayerSpec(List<String> skills, double maxMagiculeBonus, double maxAuraBonus, boolean treatAsUndead) {
    public static final LayerSpec NONE = new LayerSpec(List.of(), 0, 0, false);

    public LayerSpec {
        skills = List.copyOf(skills);
    }
}
