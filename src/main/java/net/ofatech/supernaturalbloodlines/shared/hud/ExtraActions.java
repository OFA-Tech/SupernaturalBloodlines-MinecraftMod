package net.ofatech.supernaturalbloodlines.shared.hud;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import org.jetbrains.annotations.Nullable;

/**
 * HUD actions that other parts own (today the Alpha Roar of the species part), so the HUD part can put them in a Supernatural slot without importing
 * them. A part installs its {@link Action}; without it the id is simply not an action. {@code available} runs on both sides (the hub lists only what the
 * player may use), {@code trigger} on the server only, and it keeps its own checks: the slot only asks.
 */
public final class ExtraActions {
    public static final ResourceLocation ALPHA_ROAR = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "alpha_roar");

    public static final ResourceLocation BOTTLE_BLOOD = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "bottle_blood");

    public interface Action {
        ResourceLocation id();

        Component name();

        /** One line: what it does. */
        Component note();

        ResourceLocation icon();

        /** The player may have this action in a slot (and have it put there for them): both sides. */
        boolean available(Player p);

        /** Runs it. @return the refusal to show in the action bar, null when it happened */
        @Nullable
        Component trigger(ServerPlayer p);

        /** The cooldown the action just started, in ticks, for the slot's sweep. */
        int cooldownTicks(ServerPlayer p);

        /** Ticks of cooldown left now (0 when ready): sent to a player who logs in during one. */
        int cooldownLeft(ServerPlayer p);
    }

    /** Hears every cooldown an action starts, whoever started it (a slot, a command, a hub button): the HUD part sends it to the player's slot. */
    public interface CooldownListener {
        void started(ServerPlayer p, ResourceLocation action, int ticks);
    }

    private static final List<CooldownListener> LISTENERS = new CopyOnWriteArrayList<>();

    public static void onCooldown(CooldownListener l) {
        LISTENERS.add(l);
    }

    public static void removeCooldownListener(CooldownListener l) {
        LISTENERS.remove(l);
    }

    /** The owner of an action calls this when it starts a cooldown, from every path that can start it. */
    public static void cooldownStarted(ServerPlayer p, ResourceLocation action, int ticks) {
        for (CooldownListener l : LISTENERS) {
            l.started(p, action, ticks);
        }
    }

    private static final List<Action> ACTIONS = new CopyOnWriteArrayList<>();

    private ExtraActions() {
    }

    public static void install(Action a) {
        ACTIONS.removeIf(x -> x.id().equals(a.id()));
        ACTIONS.add(a);
    }

    public static Optional<Action> get(ResourceLocation id) {
        return ACTIONS.stream().filter(a -> a.id().equals(id)).findFirst();
    }

    public static List<Action> all() {
        return List.copyOf(ACTIONS);
    }
}
