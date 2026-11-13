package net.ofatech.supernaturalbloodlines.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * Load condition {@code supernaturalbloodlines:part_enabled} with {@code {"part": "<configKey>"}}. True only when
 * that part is enabled (mods present and config switch on). Part data that is decoded by part-registered codecs
 * or names part-only ids must carry it, because data loads even when the part's code does not.
 */
public record PartEnabledCondition(String part) implements ICondition {
    public static final MapCodec<PartEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(Codec.STRING.fieldOf("part").forGetter(PartEnabledCondition::part)).apply(i, PartEnabledCondition::new));

    @Override
    public boolean test(IContext context) {
        return EnabledParts.isEnabled(part);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
