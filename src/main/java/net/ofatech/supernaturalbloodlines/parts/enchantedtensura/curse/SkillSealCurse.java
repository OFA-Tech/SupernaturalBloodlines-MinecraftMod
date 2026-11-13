package net.ofatech.supernaturalbloodlines.parts.enchantedtensura.curse;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.favouriteless.enchanted.api.curses.Curse;
import net.favouriteless.enchanted.api.curses.CurseInstance;
import net.favouriteless.enchanted.api.curses.CurseManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * One learned skill, chosen from the skills sorted by id with a per-curse seed, cannot be activated or toggled on while
 * the curse lasts. Nothing is changed on the player, so removing the curse needs no undo beyond forgetting the choice.
 */
public final class SkillSealCurse implements Curse {
    static final MapCodec<SkillSealCurse> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.LONG.fieldOf("seed").forGetter(c -> c.seed),
            ResourceLocation.CODEC.optionalFieldOf("sealed").forGetter(c -> Optional.ofNullable(c.sealed))
    ).apply(i, (seed, sealed) -> new SkillSealCurse(seed, sealed.orElse(null))));

    private final long seed;
    private @Nullable ResourceLocation sealed;

    public SkillSealCurse() {
        this(ThreadLocalRandom.current().nextLong(), null);
    }

    private SkillSealCurse(long seed, @Nullable ResourceLocation sealed) {
        this.seed = seed;
        this.sealed = sealed;
    }

    public static @Nullable ResourceLocation pick(long seed, List<ResourceLocation> learned) {
        if (learned.isEmpty()) {
            return null;
        }
        return learned.stream().sorted(Comparator.comparing(ResourceLocation::toString))
                .toList().get((int) Math.floorMod(seed, (long) learned.size()));
    }

    public @Nullable ResourceLocation sealedSkill() {
        return sealed;
    }

    public static boolean blocks(ServerPlayer player, ResourceLocation skill) {
        for (CurseInstance i : CurseManager.get().getCurses(player.getUUID(), player.serverLevel())) {
            if (!i.isRemoved() && i.getCurse() instanceof SkillSealCurse c && skill.equals(c.sealed)) {
                return true;
            }
        }
        return false;
    }

    private void choose(ServerPlayer player, int strength) {
        if (sealed != null) {
            return;
        }
        sealed = pick(seed, SkillAPI.getSkillsFrom(player).getLearnedSkills().stream()
                .map(ManasSkillInstance::getSkillId).toList());
        if (sealed != null) {
            // re-applying at the same strength changes nothing but marks Enchanted's curse data dirty, so it is saved
            CurseManager.get().applyCurse(Curses.SKILL_SEAL, player.getUUID(), strength, player.serverLevel());
        }
    }

    // Enchanted 4.2.7 only calls onInitialise on login and clone, not when a rite curses an online player,
    // so the choice is also made on the first tick (and again on later ticks while the player knows no skill).
    @Override
    public void onInitialise(ServerPlayer target, int strength, long age) {
        choose(target, strength);
    }

    @Override
    public void tick(ServerPlayer target, int strength, long age) {
        if (target.isRemoved()) {
            return;
        }
        choose(target, strength);
    }

    @Override
    public void onRemove(ServerPlayer target, int strength, long age) {
        sealed = null;
    }

    @Override
    public Type<?> type() {
        return Curses.SKILL_SEAL;
    }
}
