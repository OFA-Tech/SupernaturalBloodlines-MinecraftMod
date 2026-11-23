package net.ofatech.supernaturalbloodlines.parts.hud.net;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.hud.ActionCooldown;
import net.ofatech.supernaturalbloodlines.core.hud.RateLimiter;
import net.ofatech.supernaturalbloodlines.parts.hud.HudConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.action.ActionService;
import net.ofatech.supernaturalbloodlines.parts.hud.action.LoadoutService;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelService;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelSync;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import org.jetbrains.annotations.Nullable;

/**
 * The HUD's payloads and their rate limit. The registrar is optional, so a player without the part (or a server without it) can still connect.
 * Every payload that asks for something counts against {@code rate_limit_per_second}, a refused one as much as one that worked, and one over the
 * limit is dropped without an answer (a flood must not make the server talk back).
 */
public final class HudNet {
    private static final String VERSION = "3";
    /** The refusal of a payload over the rate limit; never shown, since nothing is sent back. */
    public static final String RATE_LIMITED = "supernaturalbloodlines.hud.rate_limited";

    private static final RateLimiter LIMITER = new RateLimiter();

    private HudNet() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(HudNet::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> sync(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
            sync(e.getEntity());
            if (e.getEntity() instanceof ServerPlayer sp) {
                WheelSync.close(sp);
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.LivingDeathEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp) {
                WheelService.forget(sp);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            sync(e.getEntity());
            if (e.getEntity() instanceof ServerPlayer sp) {
                WheelService.forget(sp);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            LIMITER.forget(e.getEntity().getUUID());
            if (e.getEntity() instanceof ServerPlayer sp) {
                WheelService.forget(sp);
                net.ofatech.supernaturalbloodlines.parts.hud.wheel.WheelClocks.forget(sp);
            }
        });
    }

    private static void sync(net.minecraft.world.entity.player.Player p) {
        if (p instanceof ServerPlayer sp) {
            LoadoutService.sync(sp);
        }
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION).optional();
        registrar.playToServer(SetLoadoutPayload.TYPE, SetLoadoutPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer p) {
                handleSetLoadout(p, payload);
            }
        }));
        registrar.playToServer(TriggerActionPayload.TYPE, TriggerActionPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer p) {
                handleTrigger(p, payload);
            }
        }));
        registrar.playToServer(WheelViewPayload.TYPE, WheelViewPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer p) {
                handleView(p, payload);
            }
        }));
        registrar.playToServer(UsePowerPayload.TYPE, UsePowerPayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer p) {
                handleUse(p, payload);
            }
        }));
        registrar.playToServer(SetPowerModePayload.TYPE, SetPowerModePayload.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer p) {
                handleMode(p, payload);
            }
        }));
        // the wheel screen comes with W4; until then the catalogue is dropped on arrival
        registrar.playToClient(WheelCataloguePayload.TYPE, WheelCataloguePayload.STREAM_CODEC, (payload, ctx) -> {
        });
        registrar.playToClient(LoadoutSyncPayload.TYPE, LoadoutSyncPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> ctx.player().setData(ModAttachments.HUD_LOADOUT, payload.loadout())));
        registrar.playToClient(ActionCooldownPayload.TYPE, ActionCooldownPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    long now = ctx.player().level().getGameTime();
                    ActionCooldowns.set(payload.action(), ActionCooldown.startedAt(now, payload.total(), payload.left()), payload.total());
                }));
    }

    /** True when this payload is within the player's rate limit; every call counts. */
    public static boolean allowed(ServerPlayer p) {
        return LIMITER.tryAcquire(p.getUUID(), p.server.getTickCount(), HudConfig.rateLimitPerSecond());
    }

    /** Test hook: forgets the rate limit window of a player. */
    public static void forgetRateLimit(ServerPlayer p) {
        LIMITER.forget(p.getUUID());
    }

    /**
     * A slot key went down (counts against the rate limit, refusals too) or up (not limited: a dropped release would leave a feeding running, and it does
     * nothing unless something is held). The action is the one in the player's own loadout.
     *
     * @return the refusal's lang key (already shown in the action bar, except for {@link #RATE_LIMITED}), null when the slot did something
     */
    @Nullable
    public static String handleTrigger(ServerPlayer p, TriggerActionPayload payload) {
        if (!payload.release() && !allowed(p)) {
            return RATE_LIMITED;
        }
        return ActionService.trigger(p, payload.slot(), payload.targetEntityId(), payload.release());
    }

    /** The wheel screen opened or closed. An open counts against the rate limit (it builds the list); a close is free. */
    @Nullable
    public static String handleView(ServerPlayer p, WheelViewPayload payload) {
        if (!payload.open()) {
            WheelSync.close(p);
            return null;
        }
        if (!allowed(p)) {
            return RATE_LIMITED;
        }
        WheelSync.open(p);
        return null;
    }

    /**
     * A power used from the wheel. A press counts against the rate limit, refusals included; a release is free (a dropped release would leave something held, and it does nothing
     * unless the press was accepted). The power is looked up on the server: the text is only a name.
     *
     * @return the refusal's lang key (already shown in the action bar, except for {@link #RATE_LIMITED}), null when the power did something
     */
    @Nullable
    public static String handleUse(ServerPlayer p, UsePowerPayload payload) {
        if (!payload.release() && !allowed(p)) {
            return RATE_LIMITED;
        }
        java.util.Optional<PowerRef> ref = PowerRef.parse(payload.ref());
        if (ref.isEmpty()) {
            if (payload.release()) {
                return null;
            }
            String key = WheelService.KEY + "unknown_power";
            p.displayClientMessage(Component.translatable(key), true);
            return key;
        }
        return WheelService.use(p, ref.get(), payload.mode(), payload.targetEntityId(), payload.release());
    }

    /** A power's mode moved from the wheel. Counts against the rate limit. */
    @Nullable
    public static String handleMode(ServerPlayer p, SetPowerModePayload payload) {
        if (!allowed(p)) {
            return RATE_LIMITED;
        }
        java.util.Optional<PowerRef> ref = PowerRef.parse(payload.ref());
        if (ref.isEmpty()) {
            String key = WheelService.KEY + "unknown_power";
            p.displayClientMessage(Component.translatable(key), true);
            return key;
        }
        return WheelService.changeMode(p, ref.get(), payload.delta());
    }
    /** @return the refusal's lang key (shown in the action bar, except for {@link #RATE_LIMITED}), null when the slot was set */
    @Nullable
    public static String handleSetLoadout(ServerPlayer p, SetLoadoutPayload payload) {
        if (!allowed(p)) {
            return RATE_LIMITED;
        }
        ResourceLocation id = payload.actionId().isEmpty() ? null : ResourceLocation.tryParse(payload.actionId());
        String refusal = !payload.actionId().isEmpty() && id == null ? LoadoutService.KEY + "unknown_action" : LoadoutService.set(p, payload.slot(), id);
        if (refusal != null) {
            p.displayClientMessage(Component.translatable(refusal), true);
        }
        return refusal;
    }
}
