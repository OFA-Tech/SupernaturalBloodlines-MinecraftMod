package net.ofatech.supernaturalbloodlines.parts.species.pack;

import com.mojang.serialization.MapCodec;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.util.FormHelper;
import java.util.List;
import java.util.Optional;
import net.favouriteless.enchanted.api.circle_magic.RiteFactory;
import net.favouriteless.enchanted.common.enchanted.circle_magic.rites.Rite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.bloodline.PackRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.TrialConfirmLedger;
import net.ofatech.supernaturalbloodlines.core.bloodline.TrialRules;
import net.ofatech.supernaturalbloodlines.core.moon.MoonRule;
import net.ofatech.supernaturalbloodlines.core.network.TrialWarnPayload;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.npc.NpcPackSpawner;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.rite.RiteTargets;
import net.ofatech.supernaturalbloodlines.shared.hub.profile.TrueAlphaWarning;

/**
 * The Alpha Trial rite (R147): a medium circle on a full-moon night. The performer is a transformed werewolf Beta or Omega at or above
 * {@code alpha_trial_min_level}; the rite calls the trial boss ({@link AlphaTrialBoss}) at the edge of the circle. A Beta or Omega whose path to True Alpha
 * is still open is <b>refused once</b> and sent the warning ({@code TrueAlphaWarning}, shown by the hub): winning ends that path for good, so no spark is
 * taken before they have chosen. Their answer lets the rite start within two minutes ({@link TrialConfirmLedger}). Every refusal cancels the rite, so
 * Enchanted gives the ingredients back.
 */
public record AlphaTrialRiteFactory(String faction) implements RiteFactory {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "alpha_trial");
    /** {@code faction} is only what the Rites page reads to list it first for werewolves (the factory reads no faction from it). */
    public static final MapCodec<AlphaTrialRiteFactory> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(i -> i.group(
            com.mojang.serialization.Codec.STRING.optionalFieldOf("faction", "werewolf").forGetter(AlphaTrialRiteFactory::faction)
    ).apply(i, AlphaTrialRiteFactory::new));
    private static final String KEY = "supernaturalbloodlines.trial.";

    /** The warnings sent and answered; kept in memory. */
    public static final TrialConfirmLedger LEDGER = new TrialConfirmLedger();

    @Override
    public Rite create(Rite.BaseRiteParams base, Rite.RiteParams params) {
        return new AlphaTrialRite(base, params);
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

    /** Whether a werewolf player is in a transformed form now (a beast, not the human shape). */
    public static boolean transformed(ServerPlayer p) {
        var pinned = formOverride;
        return pinned != null ? pinned.test(p) : FormHelper.getForm(p).isTransformed();
    }

    /**
     * Starts a trial for {@code p} at the circle at {@code centre}, or says why not. Returns the refusal key (null when the boss stands), and sends the
     * warning when that is what is missing.
     */
    public static String tryStart(net.minecraft.server.level.ServerLevel level, BlockPos centre, ServerPlayer p, boolean transformed) {
        long now = level.getGameTime();
        var wolf = PackAccess.wolf(p);
        TrialRules.Refusal refusal = TrialRules.gate(SpeciesConfig.trial().enabled(), wolf, VampirismSupernatural.isWerewolf(p),
                VampirismAPI.factionPlayerHandler(p).getCurrentLevel(), SpeciesConfig.trial().minLevel(),
                MoonRule.isFullMoonNight(level.dimensionType().hasFixedTime(), level.getMoonPhase(), level.getDayTime()), transformed,
                AlphaTrialBoss.running(level, p.getUUID()), TrueAlphaWarning.applies(wolf.rank(), PackRules.pathOpen(wolf)), LEDGER.valid(p.getUUID(), now));
        switch (refusal) {
            case NONE -> {
            }
            case NEEDS_WARNING -> {
                LEDGER.warn(p.getUUID(), now);
                if (!(p instanceof FakePlayer) && p.connection != null && p.connection.hasChannel(TrialWarnPayload.TYPE)) {
                    PacketDistributor.sendToPlayer(p, TrialWarnPayload.INSTANCE);
                }
                return KEY + "warn_sent";
            }
            default -> {
                return KEY + refusal.name().toLowerCase(java.util.Locale.ROOT);
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

    private static final class AlphaTrialRite extends Rite {
        AlphaTrialRite(BaseRiteParams base, RiteParams params) {
            super(base, params);
        }

        @Override
        protected boolean onStart(RiteParams params) {
            if (!ModList.get().isLoaded("werewolves")) {
                return cancel();
            }
            Optional<ServerPlayer> found = RiteTargets.centrePlayerOrCaster(this, params, level);
            if (found.isEmpty()) {
                return cancel();
            }
            ServerPlayer performer = found.get();
            String refusal = tryStart(level, getPos(), performer, transformed(performer));
            if (refusal != null) {
                performer.displayClientMessage(Component.translatable(refusal, SpeciesConfig.trial().minLevel()), true);
                return cancel();
            }
            return false;
        }
    }
}
