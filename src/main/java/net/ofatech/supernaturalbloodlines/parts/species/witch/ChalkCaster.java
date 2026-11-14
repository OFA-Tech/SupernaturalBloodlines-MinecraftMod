package net.ofatech.supernaturalbloodlines.parts.species.witch;

import java.util.UUID;

/** Implemented onto Enchanted's gold chalk by the species mixin: lets the static start-cost hook read its private caster. */
public interface ChalkCaster {
    UUID supernaturalbloodlines$caster();

    /** The rite type on the circle (null before it started), for the cost hook. */
    net.favouriteless.enchanted.common.enchanted.circle_magic.RiteType supernaturalbloodlines$type();
}
