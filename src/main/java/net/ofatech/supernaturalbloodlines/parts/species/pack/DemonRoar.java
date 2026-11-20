package net.ofatech.supernaturalbloodlines.parts.species.pack;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import de.teamlapen.werewolves.api.entities.player.IWerewolfPlayer;
import de.teamlapen.werewolves.core.ModActions;
import de.teamlapen.werewolves.util.FormHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.ofatech.supernaturalbloodlines.core.boss.DemonRoarRules;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery;

/**
 * The Demon Wolf Roar (S-WOL-11, R172): the Alpha Roar's key, after the ordinary roar, with a cooldown of its own. Every other player Alpha within 32
 * blocks who is not in the roarer's pack is made to shift (the beast form, else a half form, else the human form: whichever of them the Werewolves mod lets
 * them take). Links Werewolves; called only from {@link AlphaRoar#roar}.
 */
public final class DemonRoar {
    private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();

    private DemonRoar() {
    }

    public static void reset() {
        LAST.clear();
    }

    /** The player Alphas this roar shifts, for {@code roarer}. */
    public static List<ServerPlayer> targets(ServerPlayer roarer, Iterable<ServerPlayer> players) {
        PackIndex index = PackIndex.get(roarer.serverLevel());
        Set<UUID> pack = new HashSet<>(index.pack(roarer.getUUID()));
        List<ServerPlayer> out = new java.util.ArrayList<>();
        for (ServerPlayer other : players) {
            if (DemonRoarRules.shifts(VampirismSupernatural.isWerewolf(other), PackAccess.isAlpha(PackAccess.wolf(other).rank()), other == roarer,
                    pack.contains(other.getUUID()), other.level() == roarer.level(), roarer.distanceToSqr(other))) {
                out.add(other);
            }
        }
        return out;
    }

    /** After a successful Alpha Roar of {@code p}: the Demon Wolf Roar, when they hold it and it is ready. {@code now} is the server tick count. */
    static void after(ServerPlayer p, long now, Iterable<ServerPlayer> players) {
        if (!LadderQuery.owns(p, DemonRoarRules.SKILL)) {
            return;
        }
        long last = LAST.getOrDefault(p.getUUID(), -1L);
        if (!DemonRoarRules.ready(last, now)) {
            p.displayClientMessage(Component.translatable("supernaturalbloodlines.pack.demon_roar_cooldown", DemonRoarRules.secondsLeft(last, now)), true);
            return;
        }
        LAST.put(p.getUUID(), now);
        for (ServerPlayer other : targets(p, players)) {
            if (shift(other)) {
                other.sendSystemMessage(Component.translatable("supernaturalbloodlines.pack.demon_roar_shifted", p.getGameProfile().getName()));
            }
        }
    }

    /** Whether {@code p} took a form: nothing happens for a wolf who is in one already, or whom no form lets in (an eclipse, a shock, a bound hybrid). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static boolean shift(ServerPlayer p) {
        Optional<? extends de.teamlapen.vampirism.api.entity.player.IFactionPlayer<?>> fp = VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer();
        if (fp.isEmpty() || !(fp.get() instanceof IWerewolfPlayer w) || FormHelper.isFormActionActive(w)) {
            return false;
        }
        IActionHandler handler = fp.get().getActionHandler();
        IAction.ActivationContext none = new IAction.ActivationContext() {
            @Override
            public Optional<BlockPos> targetBlock() {
                return Optional.empty();
            }

            @Override
            public Optional<Entity> targetEntity() {
                return Optional.empty();
            }
        };
        for (IAction<?> form : List.of(ModActions.BEAST_FORM.get(), ModActions.SURVIVAL_FORM.get(), ModActions.HUMAN_FORM.get())) {
            if (handler.toggleAction((IAction) form, none) == IAction.PERM.ALLOWED) {
                return true;
            }
        }
        return false;
    }
}
