package net.ofatech.supernaturalbloodlines.parts.species.rite;

import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import net.ofatech.supernaturalbloodlines.shared.rite.RiteTell;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.Lines;
import net.ofatech.supernaturalbloodlines.parts.species.pack.LunarEclipse;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/**
 * Rite of the Broken Line (R125): a witch-type performer, under a lunar eclipse, frees one player from the line of their Original.
 * The ingredients are two bottles of blood, the Original's and the target's (the pure rules pair them: one blood has to be the root
 * of the other's line), white oak ash and a rare reagent. The target becomes the head of their own line, not an Original: their sire
 * bond stays, and everyone they sired stays in the Original's line. When anything is wrong the caster is told and Enchanted gives
 * the ingredients back.
 */
public record BrokenLineRiteFactory() implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "broken_line");
    public static final String DONE = "supernaturalbloodlines.rite.broken_line_done";
    public static final String RITES_OFF = LineRules.REFUSAL_PREFIX + "rites_off";
    public static final MapCodec<BrokenLineRiteFactory> CODEC = MapCodec.unit(BrokenLineRiteFactory::new);

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new BrokenLineRite(base, params);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of();
    }

    private static final class BrokenLineRite extends Rite {
        BrokenLineRite(BaseRiteParams base, RiteParams params) {
            super(base, params);
        }

        @Override
        protected boolean onStart(RiteParams params) {
            if (!(findEntity(params.caster) instanceof ServerPlayer caster)) {
                return cancel(); // silent: the performer left
            }
            MinecraftServer server = level.getServer();
            if (!SpeciesConfig.lineRitesEnabled()) {
                return refuse(caster, RITES_OFF);
            }
            List<UUID> bloods = LineRites.bloodSources(params.consumedItems);
            Optional<LineRules.BloodPair> pair = LineRules.pairBloods(bloods, id -> Lines.rootOf(server, id), id -> Lines.isOriginalId(server, id));
            ServerPlayer target = pair.map(p -> hereOnline(server, p.target())).orElse(null);
            boolean anyBloodHere = bloods.stream().anyMatch(id -> hereOnline(server, id) != null);
            Optional<String> refusal = LineRules.brokenLineRefusal(new LineRules.BrokenLineFacts(
                    SeveranceRiteFactory.isWitchType(BloodlineAccess.get(caster).species()), MoonRule.eclipseNight(level.dimensionType().hasFixedTime(), level.getDayTime(), net.ofatech.supernaturalbloodlines.shared.moon.EclipseFeed.active(server)), bloods.size(), pair,
                    anyBloodHere, target != null, pair.isPresent() && Lines.isOriginalId(server, pair.get().target())));
            if (refusal.isPresent()) {
                return refuse(caster, refusal.get());
            }
            UUID original = pair.get().original();
            Lines.freeTarget(server, target.getUUID(), original, LineIndex.Kind.BROKEN_LINE);
            Lines.learn(target, original);
            target.sendSystemMessage(Component.translatable(DONE, Lines.rootName(server, original)));
            return false;
        }

        /** The player, when they are online and in the rite's world. */
        private ServerPlayer hereOnline(MinecraftServer server, UUID id) {
            return SireCall.player(server, id) instanceof ServerPlayer p && p.level() == level ? p : null;
        }

        private boolean refuse(ServerPlayer caster, String key) {
            RiteTell.refuse(caster, key);
            return cancel();
        }
    }
}
