package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import java.util.Locale;
import java.util.Optional;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BaseFaction;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.werewolves.WerewolfFaction;

/**
 * A rite that runs one {@link BloodlineEvent} on the player standing on the circle (else the caster), then does what
 * the Outcome asks of the Vampirism/Werewolves factions. {@code faction} is the faction the target has to be in;
 * Originals, hybrids and the like are left to {@code BloodlineRules}, because a hybrid is never "just" a vampire.
 */
public record FactionRiteFactory(BloodlineEvent event, Faction faction, boolean requiresFullMoon) implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "faction_rite");
    public static final String WRONG_FACTION = "supernaturalbloodlines.rite.wrong_faction";
    public static final String NEEDS_FULL_MOON = "supernaturalbloodlines.rite.needs_full_moon";
    private static final ResourceLocation WEREWOLF_ID = ResourceLocation.parse("werewolves:werewolf");

    public enum Faction {
        VAMPIRE, WEREWOLF
    }

    public static final MapCodec<FactionRiteFactory> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            enumCodec(BloodlineEvent.class).fieldOf("event").forGetter(FactionRiteFactory::event),
            enumCodec(Faction.class).fieldOf("faction").forGetter(FactionRiteFactory::faction),
            Codec.BOOL.optionalFieldOf("requires_full_moon", false).forGetter(FactionRiteFactory::requiresFullMoon)
    ).apply(i, FactionRiteFactory::new));

    private static <E extends Enum<E>> Codec<E> enumCodec(Class<E> type) {
        return Codec.STRING.comapFlatMap(name -> {
            try {
                return DataResult.success(Enum.valueOf(type, name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                return DataResult.error(() -> "Unknown " + type.getSimpleName() + ": " + name);
            }
        }, e -> e.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new FactionRite(base, params, this);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    // ---- factions ----

    private static BaseFaction baseFaction(IFactionPlayerHandler h) {
        IPlayableFaction<?> f = h.getCurrentFaction();
        if (f == null) {
            return BaseFaction.NONE;
        }
        if (f == VReference.VAMPIRE_FACTION) {
            return BaseFaction.VAMPIRE;
        }
        if (f == VReference.HUNTER_FACTION) {
            return BaseFaction.HUNTER;
        }
        return WEREWOLF_ID.equals(f.getID()) ? BaseFaction.WEREWOLF : BaseFaction.NONE;
    }

    /** Species that carry both layers or an Original's rules are judged by BloodlineRules, not by the faction. */
    private static boolean rulesDecide(Species s) {
        return switch (s) {
            case ORIGINAL, ORIGINAL_HYBRID, HYBRID, TRIBRID, HERETIC -> true;
            default -> false;
        };
    }

    private static void tell(ServerPlayer to, String key) {
        to.displayClientMessage(Component.translatable(key), true);
    }

    private static void run(ServerPlayer target, Outcome out) {
        IFactionPlayerHandler h = VampirismAPI.factionPlayerHandler(target);
        for (Outcome.Action a : out.actions()) {
            if (a == Outcome.Action.LEAVE_FACTION) {
                h.leaveFaction(false);
            } else if (a != Outcome.Action.JOIN_VAMPIRE && a != Outcome.Action.JOIN_WEREWOLF) {
                SupernaturalBloodlines.LOGGER.debug("faction_rite ignores outcome action {}", a);
            }
        }
        for (Outcome.Action a : out.actions()) {
            if (a == Outcome.Action.JOIN_VAMPIRE) {
                int level = h.getCurrentFaction() == VReference.VAMPIRE_FACTION ? Math.max(1, h.getCurrentLevel()) : 1;
                h.setFactionAndLevel(VReference.VAMPIRE_FACTION, level);
            } else if (a == Outcome.Action.JOIN_WEREWOLF && ModList.get().isLoaded("werewolves")) {
                WerewolfFaction.join(target);
            }
        }
    }

    private static final class FactionRite extends Rite {
        private final FactionRiteFactory factory;

        FactionRite(BaseRiteParams base, RiteParams params, FactionRiteFactory factory) {
            super(base, params);
            this.factory = factory;
        }

        @Override
        protected boolean onStart(RiteParams params) {
            Optional<ServerPlayer> found = RiteTargets.centrePlayerOrCaster(this, params, level);
            if (found.isEmpty()) {
                return cancel();
            }
            ServerPlayer target = found.get();
            ServerPlayer caster = findEntity(params.caster) instanceof ServerPlayer p ? p : target;

            if (factory.requiresFullMoon && level.getMoonPhase() != 0) {
                tell(caster, NEEDS_FULL_MOON);
                return cancel();
            }
            if (factory.faction == Faction.WEREWOLF && !ModList.get().isLoaded("werewolves")) {
                return cancel();
            }
            IFactionPlayerHandler h = VampirismAPI.factionPlayerHandler(target);
            BaseFaction current = baseFaction(h);
            BaseFaction wanted = factory.faction == Faction.VAMPIRE ? BaseFaction.VAMPIRE : BaseFaction.WEREWOLF;
            if (current != wanted && !rulesDecide(BloodlineAccess.get(target).species())) {
                tell(caster, WRONG_FACTION);
                return cancel();
            }

            BloodlineContext ctx = BloodlineContext.defaults().withFaction(current).withGameTime(level.getGameTime());
            Outcome out = BloodlineAccess.applyEvent(target, factory.event, ctx);
            if (out.refused()) {
                tell(caster, out.refusalKey());
                return cancel();
            }
            run(target, out);
            return false;
        }
    }
}
