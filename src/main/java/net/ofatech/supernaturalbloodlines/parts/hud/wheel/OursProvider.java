package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerGroup;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerKind;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerState;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.parts.hud.bite.BiteService;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;
import net.ofatech.supernaturalbloodlines.shared.hud.ExtraActions;

/** Our own powers: Bite (a held power, only for a player who can bite) and every action another of our parts installed that the player may use now. Links no other mod. */
public final class OursProvider implements PowerProvider {
    public static final ResourceLocation BITE_ICON = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "textures/gui/hud/bite.png");

    @Override
    public List<WheelEntry> list(ServerPlayer p) {
        List<WheelEntry> out = new ArrayList<>();
        if (BiteService.canBite(p)) {
            int left = BiteService.cooldownLeft(p);
            out.add(entry(Loadout.BITE, PowerKind.HELD, Component.translatable("supernaturalbloodlines.hud.bite.name"), BITE_ICON,
                    new PowerState(false, left, left > 0 ? BiteService.COOLDOWN_TICKS : 0, LockReason.NONE, 0, 1)));
        }
        for (ExtraActions.Action a : ExtraActions.all()) {
            if (!a.available(p)) {
                continue;
            }
            int left = Math.max(0, a.cooldownLeft(p));
            int total = left > 0 ? Math.max(left, a.cooldownTicks(p)) : 0;
            out.add(entry(a.id(), a.aimed() ? PowerKind.AIMED : PowerKind.INSTANT, a.name(), a.icon(), new PowerState(false, left, total, LockReason.NONE, 0, 1)));
        }
        return out;
    }

    private static WheelEntry entry(ResourceLocation id, PowerKind kind, Component name, ResourceLocation icon, PowerState state) {
        return new WheelEntry(new PowerRef(PowerSource.BLOODLINE, id), PowerGroup.MAIN, kind, name, Component.empty(), List.of(), icon, state, true, -1f);
    }
}
