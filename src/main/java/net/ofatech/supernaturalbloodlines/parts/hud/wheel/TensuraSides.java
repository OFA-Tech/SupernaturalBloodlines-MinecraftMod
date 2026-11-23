package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.race.api.RaceEvents;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.SkillEvents;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.ability.magic.Magic;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerKind;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.core.wheel.overlap.SideEvent;

/**
 * Rule C for Tensura's skills, spells, arts and race ability: it listens on ManasCore's own events, so the gate sits below every path (a slot key, Tensura's screen, the wheel).
 * Linked to Tensura: installed only after {@code ModList.isLoaded("tensura")}.
 * <ul>
 * <li>{@code ACTIVATE_SKILL}: refused (interruptFalse) when the gate says no. A press counts only if it will really start: the same checks Tensura and ManasCore make (the slot, the
 * mode, the cooldown, {@code canActivateSkill}) are made here, so a refused press leaves no cooldown behind.</li>
 * <li>{@code TOGGLE_SKILL}: switching on is gated and counted once the skill is really on; switching off counts at once.</li>
 * <li>{@code ACTIVATE_ABILITY}: the race ability.</li>
 * </ul>
 */
final class TensuraSides {
    private static boolean installed;

    private TensuraSides() {
    }

    static void install() {
        if (installed) {
            return;
        }
        installed = true;
        SkillEvents.ACTIVATE_SKILL.register(TensuraSides::onActivate);
        SkillEvents.TOGGLE_SKILL.register(TensuraSides::onToggle);
        RaceEvents.ACTIVATE_ABILITY.register(TensuraSides::onAbility);
    }

    private static EventResult onActivate(Changeable<ManasSkillInstance> c, LivingEntity owner, int key, int mode) {
        if (!(owner instanceof ServerPlayer p) || !(c.get().getSkill() instanceof TensuraSkill)) {
            return EventResult.pass();
        }
        ManasSkillInstance inst = c.get();
        PowerRef ref = new PowerRef(PowerSource.TENSURA, inst.getSkillId());
        if (WheelGate.table().groupsOf(ref).isEmpty() || !willStart(p, inst, key, mode)) {
            return EventResult.pass();
        }
        Optional<LockReason> refusal = WheelGate.check(p, ref);
        if (refusal.isPresent()) {
            WheelGate.tell(p, refusal.get());
            return EventResult.interruptFalse();
        }
        // a switch is counted at its own event: this press does not flip it
        if (TensuraProvider.kindOf(inst, p) != PowerKind.TOGGLE) {
            WheelGate.intend(p, ref, SideEvent.FIRED, false, mode);
        }
        return EventResult.pass();
    }

    private static boolean willStart(ServerPlayer p, ManasSkillInstance inst, int key, int mode) {
        if (mode < 0 || mode >= Math.max(1, inst.getModes())) {
            return false;
        }
        if (!Magic.isStaffCasting(inst, p) && !TensuraStorages.getAbilityFrom(p).isAbilityInActivePreset(key, inst.getSkill(), mode)) {
            return false;
        }
        if (inst.onCoolDown(mode) && !inst.canIgnoreCoolDown(p, mode)) {
            return false;
        }
        return inst.canActivateSkill(p, mode);
    }

    private static EventResult onToggle(Changeable<ManasSkillInstance> c, LivingEntity owner) {
        if (!(owner instanceof ServerPlayer p) || !(c.get().getSkill() instanceof TensuraSkill)) {
            return EventResult.pass();
        }
        ManasSkillInstance inst = c.get();
        PowerRef ref = new PowerRef(PowerSource.TENSURA, inst.getSkillId());
        if (WheelGate.table().groupsOf(ref).isEmpty()) {
            return EventResult.pass();
        }
        if (inst.isToggled()) {
            WheelGate.apply(p, ref, SideEvent.OFF, true, 0);
            return EventResult.pass();
        }
        Optional<LockReason> refusal = WheelGate.check(p, ref);
        if (refusal.isPresent()) {
            WheelGate.tell(p, refusal.get());
            return EventResult.interruptFalse();
        }
        WheelGate.intend(p, ref, SideEvent.ON, true, 0);
        return EventResult.pass();
    }

    private static EventResult onAbility(ManasRaceInstance race, LivingEntity owner) {
        if (!(owner instanceof ServerPlayer p)) {
            return EventResult.pass();
        }
        PowerRef ref = new PowerRef(PowerSource.TENSURA, TensuraProvider.RACE_ABILITY);
        if (WheelGate.table().groupsOf(ref).isEmpty()) {
            return EventResult.pass();
        }
        Optional<LockReason> refusal = WheelGate.check(p, ref);
        if (refusal.isPresent()) {
            WheelGate.tell(p, refusal.get());
            return EventResult.interruptFalse();
        }
        WheelGate.intend(p, ref, SideEvent.FIRED, false, 0);
        return EventResult.pass();
    }

    /** The cooldown the skill has right after its press (Tensura counts whole seconds), 0 for none. */
    static int cooldownTicks(ServerPlayer p, ResourceLocation id, int mode) {
        if (TensuraProvider.RACE_ABILITY.equals(id)) {
            return 0;
        }
        return SkillAPI.getSkillsFrom(p).getSkill(id).map(i -> i.onCoolDown(mode) ? i.getCoolDown(mode) * 20 : 0).orElse(0);
    }

    static boolean toggled(ServerPlayer p, ResourceLocation id) {
        return SkillAPI.getSkillsFrom(p).getSkill(id).map(ManasSkillInstance::isToggled).orElse(false);
    }

    static boolean stillOn(ServerPlayer p, ResourceLocation id) {
        return !TensuraProvider.RACE_ABILITY.equals(id) && toggled(p, id);
    }
}
