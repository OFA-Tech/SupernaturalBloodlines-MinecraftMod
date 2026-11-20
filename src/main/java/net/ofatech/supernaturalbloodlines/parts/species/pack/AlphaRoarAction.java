package net.ofatech.supernaturalbloodlines.parts.species.pack;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.VampirismSupernatural;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;
import org.jetbrains.annotations.Nullable;

/**
 * The Alpha Roar as a HUD action: an Alpha or True Alpha puts it in a Supernatural slot (the hub's Slots tab), and it is put there for them the first time.
 * It replaces the separate key. Everything is still decided by {@link AlphaRoar#roar}: rank, werewolf, eclipse, alive, cooldown.
 */
public final class AlphaRoarAction implements ExtraActions.Action {
    public static final AlphaRoarAction INSTANCE = new AlphaRoarAction();
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/hud/alpha_roar.png");

    private AlphaRoarAction() {
    }

    @Override
    public ResourceLocation id() {
        return ExtraActions.ALPHA_ROAR;
    }

    @Override
    public Component name() {
        return Component.translatable("supernaturalbloodlines.hud.action.alpha_roar");
    }

    @Override
    public Component note() {
        return Component.translatable("supernaturalbloodlines.hud.action.alpha_roar.note");
    }

    @Override
    public ResourceLocation icon() {
        return ICON;
    }

    @Override
    public boolean available(Player p) {
        return PackAccess.isAlpha(PackAccess.wolf(p).rank()) && VampirismSupernatural.isWerewolf(p);
    }

    @Nullable
    @Override
    public Component trigger(ServerPlayer p) {
        return AlphaRoar.roar(p);
    }

    @Override
    public int cooldownTicks(ServerPlayer p) {
        return AlphaRoar.cooldown(p);
    }
}
