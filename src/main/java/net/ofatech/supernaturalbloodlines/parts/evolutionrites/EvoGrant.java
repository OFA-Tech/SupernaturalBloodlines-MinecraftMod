package net.ofatech.supernaturalbloodlines.parts.evolutionrites;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.ability.SkillHelper;
import io.github.manasmods.tensura.ability.TensuraSkillInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * The free grant ({@link net.ofatech.supernaturalbloodlines.core.evolution.GrantForm#FREE}): the way Great Sage's {@code grantIfMissing} and FoxAblaze's
 * {@code doGrant} give their Raphael. An instance that carries {@code NoMagiculeCost} (Tensura's own listener charges nothing for it) and
 * {@code GiftedInstance} (FoxAblaze's mark), learned through Tensura's own call, so every listener that may veto a learn still can.
 */
public final class EvoGrant {
    public static final String NO_COST = "NoMagiculeCost";
    public static final String GIFTED = "GiftedInstance";

    private EvoGrant() {
    }

    /** Whether the skill was learned; false when a listener vetoed it (then nothing is held and nothing was charged). */
    public static boolean learnFree(ServerPlayer player, ManasSkill skill) {
        TensuraSkillInstance instance = new TensuraSkillInstance(skill);
        CompoundTag tag = instance.getOrCreateTag();
        tag.putBoolean(NO_COST, true);
        tag.putBoolean(GIFTED, true);
        instance.markDirty();
        return SkillHelper.learnSkill(player, instance);
    }
}
