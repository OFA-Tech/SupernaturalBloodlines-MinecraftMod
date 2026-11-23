package net.ofatech.supernaturalbloodlines.parts.hud.client.werewolves;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.entity.player.actions.IActionHandler;
import de.teamlapen.vampirism.api.entity.player.actions.ILastingAction;
import de.teamlapen.werewolves.api.entities.player.IWerewolfPlayer;
import de.teamlapen.werewolves.network.ServerboundSimpleInputEventPacket;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.ofatech.supernaturalbloodlines.core.hud.SprintLeap;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudClientConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudSend;

/**
 * A jump while sprinting asks Werewolves for its leap, by its own path (see {@link SprintLeap}): the same packet its key sends, and the same local toggle
 * (the jump boost is applied by the client for its own player, so the action must be on before the jump). The server answers with {@code toggleAction},
 * which keeps every check and the cooldown. Runs at the start of the tick, before the player's own tick turns the key into a jump (and the move packet
 * that tells the server about it). Linked to Vampirism and Werewolves: registered only with both loaded.
 */
public final class SprintLeapClient {
    private static final ResourceLocation LEAP = ResourceLocation.parse("werewolves:leap");
    private static boolean jumpWasDown;
    /** Ticks since the leap was switched on by us, -1 when it is not waiting for a jump; and whether the player has left the ground since. */
    private static int armedFor = -1;
    private static boolean leftGround;
    private static boolean hinted;

    private SprintLeapClient() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre e) -> tick());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> {
            hinted = false;
            armedFor = -1;
            net.ofatech.supernaturalbloodlines.parts.hud.client.HybridLeapGuard.reset();
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        boolean down = mc.options.keyJump.isDown();
        boolean pressed = down && !jumpWasDown;
        jumpWasDown = down;
        if (p == null) {
            armedFor = -1;
            return;
        }
        if (armedFor >= 0) {
            watchArmed(p);
        }
        if (!pressed || !HudClientConfig.sprintLeap() || armedFor >= 0) {
            return;
        }
        Optional<IFactionPlayer<?>> fp = (Optional) VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer();
        if (fp.isEmpty() || !(fp.get() instanceof IWerewolfPlayer wolf)) {
            return;
        }
        IAction action = net.ofatech.supernaturalbloodlines.parts.hud.client.vampirism.ClientActions.byId(LEAP);
        if (!(action instanceof ILastingAction lasting) || !HudSend.canSend(ServerboundSimpleInputEventPacket.TYPE)) {
            return;
        }
        IActionHandler handler = (IActionHandler) wolf.getActionHandler();
        boolean canJump = p.onGround() && !p.isPassenger() && !p.isInWater() && !p.getAbilities().flying && !p.isFallFlying();
        if (!SprintLeap.shouldLeap(true, true, p.isSprinting(), canJump, true, wolf.getForm().isTransformed(), handler.isActionUnlocked(action),
                handler.isActionOnCooldown(action), handler.isActionActive(lasting), mc.screen != null,
                net.ofatech.supernaturalbloodlines.parts.hud.client.HybridLeapGuard.inFlight(p.level().getGameTime()))) {
            return;
        }
        HudSend.send(new ServerboundSimpleInputEventPacket(ServerboundSimpleInputEventPacket.Action.LEAP));
        handler.toggleAction(action, new Context());
        armedFor = 0;
        leftGround = false;
        if (!hinted) {
            hinted = true;
            mc.gui.setOverlayMessage(net.minecraft.network.chat.Component.translatable("supernaturalbloodlines.hud.sprint_leap.hint"), false);
        }
    }

    /** The leap is on and waiting for the jump: when the jump never starts it is switched off again (see {@link SprintLeap#jumpBlocked}). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void watchArmed(LocalPlayer p) {
        armedFor++;
        leftGround |= !p.onGround();
        if (!SprintLeap.jumpBlocked(armedFor, leftGround)) {
            if (leftGround) {
                armedFor = -1; // the jump started: the action is Werewolves' from here
            }
            return;
        }
        armedFor = -1;
        Optional<IFactionPlayer<?>> fp = (Optional) VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer();
        IAction action = net.ofatech.supernaturalbloodlines.parts.hud.client.vampirism.ClientActions.byId(LEAP);
        if (fp.isPresent() && action instanceof ILastingAction lasting && fp.get() instanceof IWerewolfPlayer wolf) {
            IActionHandler handler = (IActionHandler) wolf.getActionHandler();
            if (handler.isActionActive(lasting)) {
                HudSend.send(new ServerboundSimpleInputEventPacket(ServerboundSimpleInputEventPacket.Action.LEAP));
                handler.toggleAction(action, new Context());
            }
        }
    }

    private record Context() implements IAction.ActivationContext {
        @Override
        public Optional<BlockPos> targetBlock() {
            return Optional.empty();
        }

        @Override
        public Optional<Entity> targetEntity() {
            return Optional.empty();
        }
    }
}
