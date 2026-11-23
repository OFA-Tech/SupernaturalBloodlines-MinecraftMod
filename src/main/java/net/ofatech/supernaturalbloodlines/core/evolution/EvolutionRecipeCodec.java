package net.ofatech.supernaturalbloodlines.core.evolution;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.rite.RiteKind;

/**
 * The rite file's {@code factory} block as a codec (spec 6.3): the recipe is the data, so a datapack can change a recipe's numbers and ingredients but not
 * invent one the engine has no probe for. Pure (no Enchanted class), so a unit test decodes the real files. An unknown tier, kind or form is an error.
 */
public final class EvolutionRecipeCodec {
    private EvolutionRecipeCodec() {
    }

    private static DataResult<Tier> evolutionTier(String s) {
        return Tier.parse(s).filter(t -> t != Tier.COMMON).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "unknown evolution tier \"" + s + "\""));
    }

    private static DataResult<Tier> anyTier(String s) {
        return Tier.parse(s).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "unknown tier \"" + s + "\""));
    }

    private static DataResult<Set<RiteKind>> kinds(List<String> names) {
        Set<RiteKind> out = EnumSet.noneOf(RiteKind.class);
        for (String n : names) {
            try {
                out.add(RiteKind.valueOf(n.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                return DataResult.error(() -> "unknown rite kind \"" + n + "\"");
            }
        }
        return DataResult.success(out);
    }

    private static final Codec<EvolutionRecipe.Need> NEED = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("skill").forGetter(EvolutionRecipe.Need::skill),
            Codec.BOOL.optionalFieldOf("mastered", false).forGetter(EvolutionRecipe.Need::mastered)
    ).apply(i, EvolutionRecipe.Need::new));

    /** An evolution rite's recipe (a Common skill is not an evolution tier). */
    public static final MapCodec<EvolutionRecipe> RECIPE = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("recipe").forGetter(EvolutionRecipe::id),
            Codec.STRING.comapFlatMap(EvolutionRecipeCodec::evolutionTier, Tier::id).fieldOf("tier").forGetter(EvolutionRecipe::tier),
            Codec.STRING.fieldOf("grants").forGetter(EvolutionRecipe::grants),
            Codec.STRING.comapFlatMap(s -> GrantForm.parse(s).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "unknown grant form \"" + s + "\"")),
                    f -> f.name().toLowerCase(Locale.ROOT)).optionalFieldOf("grant_form", GrantForm.DEFAULT).forGetter(EvolutionRecipe::grantForm),
            Codec.STRING.listOf().optionalFieldOf("consumes", List.of()).forGetter(EvolutionRecipe::consumes),
            NEED.listOf().optionalFieldOf("needs", List.of()).forGetter(EvolutionRecipe::needs),
            Codec.STRING.fieldOf("requires_mod").forGetter(EvolutionRecipe::requiresMod),
            Codec.STRING.fieldOf("probe").forGetter(EvolutionRecipe::probe),
            Codec.STRING.listOf().comapFlatMap(EvolutionRecipeCodec::kinds, s -> s.stream().map(k -> k.name().toLowerCase(Locale.ROOT)).sorted().toList())
                    .optionalFieldOf("subject_kinds", Set.of()).forGetter(EvolutionRecipe::subjectKinds),
            Codec.DOUBLE.optionalFieldOf("ep_cost", 0.0).forGetter(EvolutionRecipe::epCost)
    ).apply(i, EvolutionRecipe::new));

    /** The tier of a Rite of Mastery file (any of the four). */
    public static final MapCodec<Tier> MASTERY_TIER = Codec.STRING.comapFlatMap(EvolutionRecipeCodec::anyTier, Tier::id).fieldOf("tier");
}