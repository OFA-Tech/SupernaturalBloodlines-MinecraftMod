package net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite;

import net.ofatech.supernaturalbloodlines.shared.rite.RiteTell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.event.PlayerFactionEvent;
import java.util.Locale;
import java.util.Optional;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BaseFaction;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineContext;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineEvent;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
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
    public static final String NO_EFFECT = "supernaturalbloodlines.rite.no_effect";
    public static final String RITE_TAKES_YOU = "supernaturalbloodlines.rite.takes_you";
    public static final String JOIN_DENIED = "supernaturalbloodlines.rite.join_denied";
    private static final ResourceLocation WEREWOLF_ID = ResourceLocation.parse("werewolves:werewolf");

    /** What the target has to be. NONE = no check, the rules decide. Plan 4/7 rites add values here. */
    public enum Faction {
        VAMPIRE, WEREWOLF, HUNTER, NONE, VAMPIRE_OR_WEREWOLF;

        public boolean accepts(BaseFaction f) {
            return switch (this) {
                case VAMPIRE -> f == BaseFaction.VAMPIRE;
                case WEREWOLF -> f == BaseFaction.WEREWOLF;
                case HUNTER -> f == BaseFaction.HUNTER;
                case NONE -> true;
                case VAMPIRE_OR_WEREWOLF -> f == BaseFaction.VAMPIRE || f == BaseFaction.WEREWOLF;
            };
        }
    }

    public static final MapCodec<FactionRiteFactory> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            enumCodec(BloodlineEvent.class).fieldOf("event").forGetter(FactionRiteFactory::event),
            enumCodec(Faction.class).fieldOf("faction").forGetter(FactionRiteFactory::faction),
            Codec.BOOL.optionalFieldOf("requires_full_moon", false).forGetter(FactionRiteFactory::requiresFullMoon)
    ).apply(i, FactionRiteFactory::new));

    /**
     * {@code requires_full_moon} for a rite that needs the moon unless its file says otherwise (Severance, the Alpha Trial): an old file without the
     * key keeps the rule, and the key is always written back (a plain {@code optionalFieldOf(.., true)} leaves a true value out, and the hub reads it from there).
     */
    public static MapCodec<Boolean> fullMoonByDefault() {
        return Codec.BOOL.optionalFieldOf("requires_full_moon").xmap(o -> o.orElse(true), Optional::of);
    }

    public static <E extends Enum<E>> Codec<E> enumCodec(Class<E> type) {
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

    public static BaseFaction baseFaction(IFactionPlayerHandler h) {
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
            case ORIGINAL, ORIGINAL_HYBRID, HYBRID, TRIBRID, HERETIC, AUGUSTINE -> true;
            default -> false;
        };
    }

    /** The faction a JOIN action goes to; null for other actions, and for the werewolves when they are not installed. */
    private static IPlayableFaction<?> joinTarget(Outcome.Action a) {
        return switch (a) {
            case JOIN_VAMPIRE -> VReference.VAMPIRE_FACTION;
            case JOIN_WEREWOLF -> ModList.get().isLoaded("werewolves") ? WerewolfFaction.faction() : null;
            default -> null;
        };
    }

    /**
     * Vampirism's {@code canJoin} only says yes to a player without a faction, but the join comes after the leave, so
     * ask the way it does (the CanJoinFaction event) as if the target had already left.
     */
    public static boolean joinAllowed(IFactionPlayerHandler h, IPlayableFaction<?> faction, boolean leaves) {
        if (!leaves && h.getCurrentFaction() == faction) {
            return true; // only a level change
        }
        PlayerFactionEvent.CanJoinFaction event = new PlayerFactionEvent.CanJoinFaction(h, null, faction);
        NeoForge.EVENT_BUS.post(event);
        return event.getBehavior() != PlayerFactionEvent.CanJoinFaction.Behavior.DENY;
    }

    /** The default {@link FactionRiteHooks.OutcomeRunner}: only the faction actions. */
    public static void runFactionActions(ServerPlayer target, Outcome out) {
        IFactionPlayerHandler h = VampirismAPI.factionPlayerHandler(target);
        for (Outcome.Action a : out.actions()) {
            if (a == Outcome.Action.LEAVE_FACTION) {
                h.leaveFaction(false);
                if (h.getCurrentFaction() != null) {
                    SupernaturalBloodlines.LOGGER.warn("faction_rite: {} could not leave faction {}", target.getGameProfile().getName(), h.getCurrentFaction().getID());
                }
            } else if (a == Outcome.Action.KILL_PLAYER) {
                SupernaturalBloodlines.LOGGER.debug("faction_rite ignores outcome action {}", a);
            }
        }
        for (Outcome.Action a : out.actions()) {
            if (a == Outcome.Action.JOIN_VAMPIRE) {
                int level = h.getCurrentFaction() == VReference.VAMPIRE_FACTION ? Math.max(1, h.getCurrentLevel()) : 1;
                if (!h.setFactionAndLevel(VReference.VAMPIRE_FACTION, level)) {
                    SupernaturalBloodlines.LOGGER.warn("faction_rite: {} could not join the vampires", target.getGameProfile().getName());
                }
            } else if (a == Outcome.Action.JOIN_WEREWOLF && ModList.get().isLoaded("werewolves")
                    && !WerewolfFaction.join(target)) {
                SupernaturalBloodlines.LOGGER.warn("faction_rite: {} could not join the werewolves", target.getGameProfile().getName());
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
                return cancel(); // silent: no player on the centre
            }
            ServerPlayer target = found.get();
            ServerPlayer caster = RiteTargets.performer(findEntity(params.caster), params.caster, target).orElse(null);
            if (caster == null) {
                return cancel(); // silent: the performer left: only the performer may stand for themselves, so nobody is asked and nobody is changed
            }

            if (factory.requiresFullMoon && !MoonRule.nightFor(true, level.dimensionType().hasFixedTime(), level.getMoonPhase(), level.getDayTime(),
                    net.ofatech.supernaturalbloodlines.shared.moon.EclipseFeed.active(level.getServer()))) {
                RiteTell.refuse(caster, NEEDS_FULL_MOON);
                return cancel();
            }
            if (factory.faction == Faction.WEREWOLF && !ModList.get().isLoaded("werewolves")) {
                RiteTell.refuse(caster, RiteTell.NEEDS_WEREWOLVES);
                return cancel();
            }
            // a rite that changes the target needs their consent when the caster is someone else, and says nothing of what they are (R201)
            boolean consents = factory.event == BloodlineEvent.RITE_OF_AUGUSTINE;
            if (consents) {
                RiteTargets.Told asked = RiteTargets.consentAnswer(caster == target, target.isShiftKeyDown(), RiteTargets.SNEAK_TO_BE_MADE_AUGUSTINE);
                if (asked != null) {
                    RiteTargets.tell(caster, target, asked);
                    return cancel();
                }
            }
            IFactionPlayerHandler h = VampirismAPI.factionPlayerHandler(target);
            BaseFaction current = baseFaction(h);
            BloodlineState before = BloodlineAccess.get(target);
            // every refusal from here on is about the one on the centre: a caster who is someone else hears the one generic line (R201, R362)
            if (!factory.faction.accepts(current) && !rulesDecide(before.species())) {
                RiteTargets.refuseAbout(caster, target, WRONG_FACTION);
                return cancel();
            }

            // Preview with the pure rules, so a rite that cannot work leaves state and factions untouched
            BloodlineContext ctx = FactionRiteHooks.context(target, current, level.getGameTime());
            Outcome preview = BloodlineRules.apply(before, factory.event, ctx);
            if (preview.refused()) {
                RiteTargets.refuseAbout(caster, target, preview.refusalKey());
                return cancel();
            }
            if (preview.actions().isEmpty() && preview.state().equals(before)) {
                RiteTell.refuseGeneric(caster, NO_EFFECT);
                return cancel();
            }
            boolean leaves = preview.actions().contains(Outcome.Action.LEAVE_FACTION);
            for (Outcome.Action a : preview.actions()) {
                IPlayableFaction<?> to = joinTarget(a);
                if (to != null && !joinAllowed(h, to, leaves)) {
                    RiteTargets.refuseAbout(caster, target, JOIN_DENIED);
                    return cancel();
                }
            }

            if (consents && caster != target) {
                RiteTell.say(target, RITE_TAKES_YOU);
            }
            FactionRiteHooks.run(target, BloodlineAccess.applyEvent(target, factory.event, ctx));
            return false;
        }
    }
}
