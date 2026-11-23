package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.race.api.Races;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.impl.SkillStorage;
import io.github.manasmods.manascore.skill.impl.TickingSkill;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.network.c2s.RequestAbilityModeChangePacket;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerKind;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.parts.hud.tensura.WheelActivation;
import org.jetbrains.annotations.Nullable;

/**
 * Starts and ends a Tensura skill, spell or art from the wheel through ManasCore's own path ({@code SkillStorage.startHoldSkill}, {@code handleSkillRelease}): the same calls
 * Tensura's activation packet makes. A skill that is in no slot is started inside {@link WheelActivation#scope} and kept bracketed while it runs; nothing is ever
 * parked in a hidden slot. The race ability has no id and no preset ({@code Races}). Linked to Tensura and ManasCore.
 */
public final class TensuraUse {
    public static final String KEY = "supernaturalbloodlines.wheel.refused.";
    /** The key number the wheel passes: ManasCore only stores it in its ticking list, so press and release must agree on it. */
    public static final int WHEEL_KEY_NUMBER = 0;

    private TensuraUse() {
    }

    /** @return the lang key of a refusal, null when the skill started */
    @Nullable
    public static String press(ServerPlayer p, ResourceLocation skillId, int mode) {
        SkillStorage storage = SkillAPI.getSkillsFrom(p);
        Optional<ManasSkillInstance> found = storage.getSkill(skillId);
        if (found.isEmpty() || !(found.get().getSkill() instanceof TensuraSkill ts)) {
            return KEY + "not_unlocked";
        }
        ManasSkillInstance inst = found.get();
        if (mode < 0 || mode >= Math.max(1, inst.getModes())) {
            return KEY + "bad_mode";
        }
        if (!ts.canBeSlotted(inst, p, 0) && !"infinity".equals(skillId.getNamespace())) {
            return TensuraProvider.isPassiveSwitch(ts, inst, p) ? flip(p, inst, skillId) : KEY + "refused";
        }
        PowerKind kind = TensuraProvider.kindOf(inst, p);
        // a plain toggle is switched by Tensura's own toggle (its skill screen's button): the key press of a skill that does not override onPressed does not flip it (T6 probe)
        if (kind == PowerKind.TOGGLE) {
            return flip(p, inst, skillId);
        }
        boolean unslotted = TensuraProvider.unslotted(p, ts);
        if (unslotted && TensuraProvider.needsSlot(p, inst, kind)) {
            return KEY + "tensura";
        }
        // always inside the scope: the wheel passes key number 0 and the mode it shows, and Tensura only accepts a skill in that very slot and mode of the active preset, so a skill
        // in slot 2, or slotted in another mode, would be refused without it
        boolean started = WheelActivation.scope(p.getUUID(), ts, mode, () -> storage.startHoldSkill(inst, WHEEL_KEY_NUMBER, mode));
        if (!started) {
            return KEY + "refused";
        }
        // the hold bracket is open for every wheel hold, slotted or not: a skill that leaves its slot while it is held (the preset changed, the slot was cleared) would else be cut off
        // after one second
        WheelActivation.holding(p, ts, mode);
        // a tap, as Tensura's own key sends: press then release at once. Tensura flips a toggle when the key comes up, and an instant skill has nothing to hold; a held
        // power (a held skill, a spell with a cast time) waits for the release the player sends
        if (kind != PowerKind.HELD) {
            release(p, skillId, mode);
        } else {
            noteCooldown(p, inst, skillId, mode);
        }
        return null;
    }

    /** The cooldown a press or release just started is the full one: the wheel shows its sweep against it (ManasCore keeps only what is left). */
    private static void noteCooldown(ServerPlayer p, ManasSkillInstance inst, ResourceLocation skillId, int mode) {
        int left = inst.onCoolDown(mode) ? inst.getCoolDown(mode) * 20 : 0;
        if (left > 0) {
            WheelClocks.of(p).notePress(new PowerRef(PowerSource.TENSURA, skillId).key(), WheelClocks.now(p), left);
        }
    }
    /** True while ManasCore still has the skill in its held list (the wheel's release is then needed). */
    public static boolean skillHeld(ServerPlayer p, ResourceLocation skillId, int mode) {
        return SkillAPI.getSkillsFrom(p).getSkill(skillId).map(i -> TickingSkill.isTickingSkill(p, i.getSkill(), mode)).orElse(false);
    }

    public static boolean raceHeld(ServerPlayer p) {
        return RaceAPI.getRaceFrom(p).hasHeldAbility();
    }

    /** A passive switch: Tensura's own toggle, the one its skill screen sends. */
    @Nullable
    private static String flip(ServerPlayer p, ManasSkillInstance inst, ResourceLocation skillId) {
        if (!inst.canInteractSkill(p)) {
            return KEY + "refused";
        }
        boolean before = inst.isToggled();
        RequestAbilityModeChangePacket.toggleAbilityPacket(skillId).toggleAbility(p);
        return inst.isToggled() != before ? null : KEY + "refused";
    }

    public static void release(ServerPlayer p, ResourceLocation skillId, int mode) {
        SkillStorage storage = SkillAPI.getSkillsFrom(p);
        storage.getSkill(skillId).ifPresent(inst -> {
            WheelActivation.scope(p.getUUID(), inst.getSkill(), mode, () -> {
                storage.handleSkillRelease(inst, WHEEL_KEY_NUMBER, mode, false);
                return null;
            });
            noteCooldown(p, inst, skillId, mode);
            // a toggle stays bracketed while it is on; the next tick prunes it when it is not
            if (!inst.isToggled()) {
                WheelActivation.stopHolding(p, inst.getSkill(), mode);
            }
        });
    }
    /** The race ability, held like Tensura's key does. @return the lang key of a refusal, null when it started */
    @Nullable
    public static String pressRace(ServerPlayer p) {
        Races races = RaceAPI.getRaceFrom(p);
        return races.startHeldAbility() ? null : KEY + "race";
    }

    public static void releaseRace(ServerPlayer p) {
        RaceAPI.getRaceFrom(p).releaseHeldAbility();
    }

    /** The player is put out of action, died, left or changed dimension: nothing the wheel started is held or bracketed. */
    public static void forget(ServerPlayer p) {
        WheelActivation.stopAll(p);
    }
}
