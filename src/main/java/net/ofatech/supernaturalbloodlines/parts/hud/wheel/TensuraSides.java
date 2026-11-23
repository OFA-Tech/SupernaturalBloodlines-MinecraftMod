package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.race.api.RaceEvents;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.SkillEvents;
import io.github.manasmods.manascore.skill.impl.TickingSkill;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.ability.magic.Magic;
import io.github.manasmods.tensura.race.vampire.VampireRace;
import io.github.manasmods.tensura.registry.effect.TensuraMobEffects;
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
import org.jetbrains.annotations.Nullable;

/**
 * Rule C for Tensura's skills, spells, arts and race ability: it listens on ManasCore's own events, so the gate sits below every path (a slot key, Tensura's screen, the wheel).
 * Linked to Tensura: installed only after {@code ModList.isLoaded("tensura")}. A skill with several modes is never in a group (a mode is not the whole power).
 * <ul>
 * <li>{@code ACTIVATE_SKILL}: refused (interruptFalse) when the gate says no. A press counts only if it will really start: the same checks Tensura and ManasCore make (the slot, the
 * mode, the cooldown, {@code canActivateSkill}) are made here, so a refused press leaves no cooldown behind. A <b>held</b> skill is on while ManasCore keeps it ticking and counts as
 * its end when it stops ({@code RELEASE_SKILL}, or it ran out).</li>
 * <li>{@code TOGGLE_SKILL}: switching on is gated and counted once the skill is really on; switching off is counted once it is really off.</li>
 * <li>{@code ACTIVATE_ABILITY}: the race ability of the vampire race only. Bats Mode is the bat form (on while the effect is); a press that drains is the bite.</li>
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
        SkillEvents.RELEASE_SKILL.register(TensuraSides::onRelease);
        SkillEvents.TOGGLE_SKILL.register(TensuraSides::onToggle);
        RaceEvents.ACTIVATE_ABILITY.register(TensuraSides::onAbility);
    }

    /** The skill's instance when it is one the gate looks at: a Tensura skill of a player, in a group, with no modes. */
    @Nullable
    private static PowerRef groupedRef(ManasSkillInstance inst, LivingEntity owner) {
        if (!(owner instanceof ServerPlayer p) || !(inst.getSkill() instanceof TensuraSkill) || inst.getModes() > 1) {
            return null;
        }
        PowerRef ref = new PowerRef(PowerSource.TENSURA, inst.getSkillId());
        return WheelGate.inAnyGroup(p, ref) ? ref : null;
    }

    private static EventResult onActivate(Changeable<ManasSkillInstance> c, LivingEntity owner, int key, int mode) {
        ManasSkillInstance inst = c.get();
        PowerRef ref = groupedRef(inst, owner);
        if (ref == null || !willStart((ServerPlayer) owner, inst, key, mode)) {
            return EventResult.pass();
        }
        ServerPlayer p = (ServerPlayer) owner;
        Optional<LockReason> refusal = WheelGate.check(p, ref);
        if (refusal.isPresent()) {
            WheelGate.tell(p, refusal.get());
            return EventResult.interruptFalse();
        }
        switch (TensuraProvider.kindOf(inst, p)) {
            // a switch is counted at its own event: this press does not flip it
            case TOGGLE -> {
            }
            // on while ManasCore keeps it ticking: its end (the key comes up, or it runs out) starts the cooldown
            case HELD -> WheelGate.intend(p, ref, SideEvent.ON, true, mode);
            default -> WheelGate.intend(p, ref, SideEvent.FIRED, false, mode);
        }
        return EventResult.pass();
    }

    /** A held skill's key came up: whether it really ended is seen at the end of the tick. */
    private static EventResult onRelease(Changeable<ManasSkillInstance> c, LivingEntity owner, int key, int mode, Changeable<Integer> heldTicks) {
        ManasSkillInstance inst = c.get();
        PowerRef ref = groupedRef(inst, owner);
        if (ref != null && TensuraProvider.kindOf(inst, (ServerPlayer) owner) == PowerKind.HELD) {
            WheelGate.intend((ServerPlayer) owner, ref, SideEvent.OFF, true, mode);
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
        ManasSkillInstance inst = c.get();
        PowerRef ref = groupedRef(inst, owner);
        if (ref == null) {
            return EventResult.pass();
        }
        ServerPlayer p = (ServerPlayer) owner;
        if (inst.isToggled()) {
            // counted at the end of the tick, once Tensura really switched it off
            WheelGate.intend(p, ref, SideEvent.OFF, true, 0);
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
        // the gate counts the race ability as a bat form and a bite for the vampire race only
        if (!WheelGate.inAnyGroup(p, ref)) {
            return EventResult.pass();
        }
        if (batsMode(p)) {
            // this press ends the bat form: never a start
            WheelGate.intend(p, ref, SideEvent.OFF, true, 0, WheelGate.BAT_GROUP);
            return EventResult.pass();
        }
        Optional<LockReason> refusal = WheelGate.check(p, ref);
        if (refusal.isPresent()) {
            WheelGate.tell(p, refusal.get());
            return EventResult.interruptFalse();
        }
        // the press ends as Bats Mode (on) or as a drain (a bite): the end of the tick says which, and only that group counts it
        WheelGate.intend(p, ref, SideEvent.ON, true, 0, WheelGate.BAT_GROUP);
        WheelGate.intend(p, ref, SideEvent.FIRED, false, 0, WheelGate.BITE_GROUP);
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

    /** A held skill ManasCore still keeps ticking (the key is down, or its time has not run out). */
    static boolean held(ServerPlayer p, ResourceLocation id) {
        return SkillAPI.getSkillsFrom(p).getSkill(id).map(i -> TensuraProvider.kindOf(i, p) == PowerKind.HELD && TickingSkill.isTickingSkill(p, i.getSkill())).orElse(false);
    }

    static boolean batsMode(ServerPlayer p) {
        return p.hasEffect(TensuraMobEffects.getReference(TensuraMobEffects.BATS_MODE));
    }

    static boolean vampireRace(ServerPlayer p) {
        return RaceAPI.getRaceFrom(p).getRace().map(i -> i.getRace() instanceof VampireRace).orElse(false);
    }

    /** On right now: a switch that is switched on, a held skill that is held, the vampire race ability while Bats Mode lasts. */
    static boolean stillOn(ServerPlayer p, ResourceLocation id) {
        if (TensuraProvider.RACE_ABILITY.equals(id)) {
            return vampireRace(p) && batsMode(p);
        }
        return toggled(p, id) || held(p, id);
    }

    /** What an intended start of a Tensura power turned into at the end of the tick, null when it did not happen. */
    @Nullable
    static SideEvent outcome(ServerPlayer p, ResourceLocation id, SideEvent intended, int mode) {
        if (TensuraProvider.RACE_ABILITY.equals(id)) {
            // ON is Bats Mode, FIRED is the drain: a press that left Bats Mode on was no bite, and the other way round
            return batsMode(p) == (intended == SideEvent.ON) ? intended : null;
        }
        Optional<ManasSkillInstance> inst = SkillAPI.getSkillsFrom(p).getSkill(id);
        if (inst.isEmpty()) {
            return null;
        }
        if (intended != SideEvent.ON) {
            return intended;
        }
        if (TensuraProvider.kindOf(inst.get(), p) == PowerKind.HELD) {
            // held: on while it ticks; a spell that was already over by now still started its cooldown
            return held(p, id) ? SideEvent.ON : inst.get().onCoolDown(mode) ? SideEvent.FIRED : null;
        }
        return inst.get().isToggled() ? SideEvent.ON : null;
    }
}
