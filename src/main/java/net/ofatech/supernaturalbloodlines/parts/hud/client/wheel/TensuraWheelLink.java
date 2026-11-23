package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel;

import dev.architectury.networking.NetworkManager;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.tensura.network.c2s.RequestAbilityModeChangePacket;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.storage.ability.AbilitySlot;
import io.github.manasmods.tensura.storage.ability.IAbility;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.wheel.SlotPlan;
import net.ofatech.supernaturalbloodlines.parts.hud.client.HudSend;

/** The wheel's reach into Tensura's slots: what the six slots hold, and putting a skill in one. Linked to Tensura: reached only after {@code ModList.isLoaded("tensura")}. */
final class TensuraWheelLink {
    private TensuraWheelLink() {
    }

    static int activePreset(LocalPlayer p) {
        return TensuraStorages.getAbilityFrom(p).getActivePreset();
    }

    static int presetCount(LocalPlayer p) {
        return TensuraStorages.getAbilityFrom(p).getPresets().size();
    }

    /** The skill ids in the six HUD slots (left column first); null for an empty one. */
    static List<ResourceLocation> holds(LocalPlayer p) {
        IAbility ab = TensuraStorages.getAbilityFrom(p);
        List<ResourceLocation> out = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            SlotPlan.TensuraSlot t = SlotPlan.tensura(i, ab.getActivePreset(), ab.getPresets().size());
            AbilitySlot slot = ab.getAbilitySlot(t.preset(), t.slot());
            ManasSkill skill = slot == null || slot.isEmpty() ? null : slot.getSkill();
            out.add(skill == null ? null : skill.getRegistryName());
        }
        return out;
    }

    /** Puts {@code skill} in HUD slot {@code hudSlot} in {@code mode}, through Tensura's own request (the server checks it). */
    static void assign(LocalPlayer p, int hudSlot, ResourceLocation skill, int mode) {
        IAbility ab = TensuraStorages.getAbilityFrom(p);
        SlotPlan.TensuraSlot t = SlotPlan.tensura(hudSlot, ab.getActivePreset(), ab.getPresets().size());
        if (HudSend.connected()) {
            NetworkManager.sendToServer(RequestAbilityModeChangePacket.changeAbilityPacket(t.slot(), t.preset(), skill, mode));
        }
    }
}
