package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.network.c2s.RequestAbilityModeChangePacket;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Moves a Tensura skill to its next or previous mode. A skill in a slot goes through Tensura's own {@code changeMode} for <b>every</b> slot that holds it (so Tensura's screens and
 * the slots agree); a skill in no slot asks {@code TensuraSkill.nextMode} (overridden per skill: Infinity's stones skip locked modes) and keeps the answer in {@link WheelModes}.
 * Linked to Tensura.
 */
final class TensuraModeChange {
    private TensuraModeChange() {
    }

    /** @return a refusal key suffix, null when the mode changed */
    @Nullable
    static String change(ServerPlayer p, ResourceLocation id, int delta) {
        Optional<ManasSkillInstance> found = SkillAPI.getSkillsFrom(p).getSkill(id);
        if (found.isEmpty() || !(found.get().getSkill() instanceof TensuraSkill ts)) {
            return "not_unlocked";
        }
        ManasSkillInstance inst = found.get();
        int modes = Math.max(1, inst.getModes());
        if (modes <= 1) {
            return "no_modes";
        }
        boolean backwards = delta < 0;
        List<TensuraModes.Held> held = TensuraModes.slotsHolding(p, ts);
        int before = TensuraModes.current(p, inst);
        if (!held.isEmpty()) {
            for (TensuraModes.Held h : held) {
                RequestAbilityModeChangePacket.changeModePacket(h.slot(), h.preset(), backwards).changeMode(p);
            }
            return TensuraModes.current(p, inst) == before ? "no_mode" : null;
        }
        int next = ts.nextMode(p, inst, before, backwards);
        if (next < 0 || next == before || next >= modes) {
            return "no_mode";
        }
        WheelModes.set(p, id, next, modes);
        return null;
    }
}
