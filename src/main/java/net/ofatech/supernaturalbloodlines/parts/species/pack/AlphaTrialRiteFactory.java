package net.ofatech.supernaturalbloodlines.parts.species.pack;

import net.ofatech.supernaturalbloodlines.shared.rite.RiteTell;
import com.mojang.serialization.MapCodec;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.util.FormHelper;
import java.util.List;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.TrialConfirmLedger;
import net.minecraft.server.level.ServerLevel;
import net.ofatech.supernaturalbloodlines.core.bloodline.TrialRules;
import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import net.ofatech.supernaturalbloodlines.core.network.TrialWarnPayload;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackSpawner;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import net.ofatech.supernaturalbloodlines.shared.hub.profile.TrueAlphaWarning;

/**
 * The Alpha Trial rite (R147): a medium circle on a full-moon night. The performer is a transformed werewolf Beta or Omega at or above
 * {@code alpha_trial_min_level}; the rite calls the trial boss ({@link AlphaTrialBoss}) at the edge of the circle. A Beta or Omega whose path to True Alpha
 * is still open is <b>refused once</b> and sent the warning ({@code TrueAlphaWarning}, shown by the hub): winning ends that path for good, so no spark is
 * taken before they have chosen. Their answer lets the rite start within two minutes ({@link TrialConfirmLedger}). Every refusal cancels the rite, so
 * Enchanted gives the ingredients back.
 */
public record AlphaTrialRiteFactory(String faction, boolean requiresFullMoon) implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "alpha_trial");
    /** {@code faction} is no longer read by anything (the Rites page it listed the rite on is gone) (the factory reads no faction from it). */
    public static final MapCodec<AlphaTrialRiteFactory> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(i -> i.group(
            com.mojang.serialization.Codec.STRING.optionalFieldOf("faction", "werewolf").forGetter(AlphaTrialRiteFactory::faction),
            net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.FactionRiteFactory.fullMoonByDefault().forGetter(AlphaTrialRiteFactory::requiresFullMoon)
    ).apply(i, AlphaTrialRiteFactory::new));
    private static final String KEY = "supernaturalbloodlines.trial.";

    /** The warnings sent and answered; kept in memory. */
    public static final TrialConfirmLedger LEDGER = new TrialConfirmLedger();

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new AlphaTrialRite(base, params, requiresFullMoon);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public List<ItemStack> getOutputs() {
        return List.of();
    }

    private static volatile java.util.function.Predicate<ServerPlayer> formOverride;

    /** For tests: a stand-in for "is in a beast form" (a test player cannot shift); null clears it. */
    @org.jetbrains.annotations.VisibleForTesting
    public static void overrideTransformed(java.util.function.Predicate<ServerPlayer> value) {
        formOverride = value;
    }

    /** The rite's own {@code requires_full_moon}, read back from the loaded rite (true when the rite is not loaded or its file does not say). */
    public static boolean needsFullMoon(net.minecraft.server.level.ServerLevel level) {
        var registry = level.registryAccess().registryOrThrow(net.favouriteless.enchanted.common.init.EData.RITE_TYPES_REGISTRY);
        var type = registry.get(ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "alpha_trial"));
        return type == null || net.ofatech.supernaturalbloodlines.parts.species.SpeciesHub.requiresFullMoon(net.ofatech.supernaturalbloodlines.parts.species.SpeciesHub.factoryOf(level.registryAccess(), type));
    }

    /** What Home says about {@code p}'s trial tonight: running, possible (the gate would let them begin in beast form), or nothing. */
    public static TrialRules.Notice noticeOf(ServerPlayer p) {
        return noticeOf(p, needsFullMoon(p.serverLevel()));
    }

    /** {@link #noticeOf(ServerPlayer)} for a rite that needs the full moon ({@code true}) or only a night. */
    public static TrialRules.Notice noticeOf(ServerPlayer p, boolean needsFullMoon) {
        var level = p.serverLevel();
        var wolf = PackAccess.wolf(p);
        TrialRules.Refusal refusal = TrialRules.gate(SpeciesConfig.trial().enabled(), wolf, VampirismSupernatural.isWerewolf(p),
                VampirismAPI.factionPlayerHandler(p).getCurrentLevel(), SpeciesConfig.trial().minLevel(),
                MoonRule.nightFor(needsFullMoon, level.dimensionType().hasFixedTime(), level.getMoonPhase(), level.getDayTime(), net.ofatech.supernaturalbloodlines.shared.moon.EclipseFeed.active(level.getServer())), true, false, false, false);
        return TrialRules.notice(() -> AlphaTrialBoss.running(level, p.getUUID()), refusal);
    }

    /** Whether a werewolf player is in a transformed form now (a beast, not the human shape). */
    public static boolean transformed(ServerPlayer p) {
        var pinned = formOverride;
        return pinned != null ? pinned.test(p) : FormHelper.getForm(p).isTransformed();
    }

    /**
     * Starts a trial for {@code p} at the circle at {@code centre}, or says why not. Returns the refusal key (null when the boss stands), and sends the
     * warning when that is what is missing. {@code needsFullMoon} is the rite file's {@code requires_full_moon}: false lets a server run the trial on any
     * night, but never in daylight.
     */
    public static String tryStart(net.minecraft.server.level.ServerLevel level, BlockPos centre, ServerPlayer p, boolean transformed, boolean needsFullMoon) {
        long now = level.getGameTime();
        var wolf = PackAccess.wolf(p);
        TrialRules.Refusal refusal = TrialRules.gate(SpeciesConfig.trial().enabled(), wolf, VampirismSupernatural.isWerewolf(p),
                VampirismAPI.factionPlayerHandler(p).getCurrentLevel(), SpeciesConfig.trial().minLevel(),
                MoonRule.nightFor(needsFullMoon, level.dimensionType().hasFixedTime(), level.getMoonPhase(), level.getDayTime(), net.ofatech.supernaturalbloodlines.shared.moon.EclipseFeed.active(level.getServer())), transformed,
                AlphaTrialBoss.running(level, p.getUUID()), TrueAlphaWarning.applies(wolf.rank(), PackRules.pathOpen(wolf)), LEDGER.valid(p.getUUID(), now));
        switch (refusal) {
            case NONE -> {
            }
            case NEEDS_WARNING -> {
                boolean connected = !(p instanceof FakePlayer) && p.connection != null;
                // a client without the mod has no way to show the warning, so nothing is recorded: it could never be answered
                if (!TrialRules.canShowWarning(connected, connected && p.connection.hasChannel(TrialWarnPayload.TYPE))) {
                    return KEY + "warn_unavailable";
                }
                LEDGER.warn(p.getUUID(), now);
                if (connected) {
                    PacketDistributor.sendToPlayer(p, TrialWarnPayload.INSTANCE);
                }
                return KEY + "warn_sent";
            }
            default -> {
                return KEY + TrialRules.refusalKey(refusal, needsFullMoon);
            }
        }
        BlockPos spot = NpcPackSpawner.groundNear(level, centre.offset(5, 0, 0));
        if (spot == null) {
            spot = NpcPackSpawner.groundNear(level, centre.offset(-5, 0, 0));
        }
        if (spot == null || AlphaTrialBoss.spawn(level, spot, p) == null) {
            return KEY + "no_room";
        }
        LEDGER.consume(p.getUUID());
        p.sendSystemMessage(Component.translatable(KEY + "begins"));
        return null;
    }

    /**
     * The answer of the player's hub to the warning (the {@code TrialConfirmPayload}): believed only for a warning the server sent, and then the rite
     * may start for two minutes. A late or unknown answer is told so, in chat, because the hub is closed by then.
     * @return whether the answer was believed
     */
    public static boolean answer(ServerPlayer p) {
        if (LEDGER.confirm(p.getUUID(), p.level().getGameTime())) {
            p.sendSystemMessage(Component.translatable(KEY + "confirmed"));
            return true;
        }
        p.sendSystemMessage(Component.translatable(KEY + "warn_expired"));
        return false;
    }

    /** Players other than {@code caster} standing on the circle's centre block (or the block above it), who could be mistaken for the performer. */
    static List<ServerPlayer> othersOnCentre(ServerLevel level, BlockPos centre, ServerPlayer caster) {
        return level.getEntitiesOfClass(ServerPlayer.class, new AABB(centre).expandTowards(0, 1, 0), p -> p != caster && p.isAlive() && !p.isSpectator());
    }

    private static final class AlphaTrialRite extends Rite {
        private final boolean requiresFullMoon;

        AlphaTrialRite(BaseRiteParams base, RiteParams params, boolean requiresFullMoon) {
            super(base, params);
            this.requiresFullMoon = requiresFullMoon;
        }

        @Override
        protected boolean onStart(RiteParams params) {
            if (!ModList.get().isLoaded("werewolves")) {
                RiteTell.refuse(level.getPlayerByUUID(params.caster), RiteTell.NEEDS_WEREWOLVES);
                return cancel();
            }
            // the challenger is the CASTER and nobody else: whoever stands on the centre cannot be given a boss, or a warning, by someone else's rite
            if (!(level.getPlayerByUUID(params.caster) instanceof ServerPlayer caster)) {
                return cancel(); // silent: the performer left
            }
            if (!othersOnCentre(level, getPos(), caster).isEmpty()) {
                RiteTell.refuse(caster, KEY + "not_alone");
                return cancel();
            }
            String refusal = tryStart(level, getPos(), caster, transformed(caster), requiresFullMoon);
            if (refusal != null) {
                RiteTell.refuse(caster, refusal, SpeciesConfig.trial().minLevel());
                return cancel();
            }
            return false;
        }
    }
}
