package net.ofatech.supernaturalbloodlines.parts.hud.wheel;

import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.ability.battlewill.Battlewill;
import io.github.manasmods.tensura.ability.magic.Magic;
import io.github.manasmods.tensura.ability.skill.resist.ResistSkill;
import io.github.manasmods.tensura.storage.TensuraStorages;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.hud.Mastery;
import net.ofatech.supernaturalbloodlines.core.wheel.CooldownClock;
import net.ofatech.supernaturalbloodlines.core.wheel.LockReason;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerGroup;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerKind;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerRef;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerSource;
import net.ofatech.supernaturalbloodlines.core.wheel.PowerState;
import net.ofatech.supernaturalbloodlines.core.wheel.TensuraKinds;
import net.ofatech.supernaturalbloodlines.parts.hud.HudConfig;
import net.ofatech.supernaturalbloodlines.parts.hud.net.WheelEntry;

/**
 * Tensura's skills, spells (Magic) and battlewill arts, and the race ability, plus the skills of namespace {@code infinity} (a stone is a Tensura UNIQUE skill that is
 * never slottable). Linked to Tensura: reached only after {@code ModList.isLoaded("tensura")}. Only learned skills are listed.
 * <p>
 * <b>Kind rule</b> (a skill's own {@code getMaxHeldTime} is 20 or 72000 by slot, so it says nothing): a Magic is HELD unless {@code isInstantCast}; a skill whose class (below
 * {@code TensuraSkill}) declares {@code onHeld} or {@code onRelease} is HELD; {@code canBeToggled} is TOGGLE; else INSTANT; then {@code tensura_kinds.json} has the last word.
 */
public final class TensuraProvider implements PowerProvider {
    public static final ResourceLocation RACE_ABILITY = ResourceLocation.fromNamespaceAndPath("tensura", "race_ability");
    /** Set when the unslotted spike showed that a held art or a long spell in no slot is cut off: they are listed locked, put it in a slot first. */
    public static final boolean HELD_NEEDS_SLOT = false;
    /** A held skill or spell that runs longer than this many ticks needs the hold bracket (Tensura cuts a skill in no slot after 20). */
    public static final int LONG_TICKS = 20;

    private static TensuraKinds kinds;

    public static synchronized TensuraKinds kinds() {
        if (kinds == null) {
            kinds = TensuraKinds.EMPTY;
            try (InputStream in = TensuraProvider.class.getResourceAsStream("/data/supernaturalbloodlines/wheel/tensura_kinds.json")) {
                if (in != null) {
                    kinds = TensuraKinds.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                }
            } catch (IOException | RuntimeException e) {
                SupernaturalBloodlines.LOGGER.warn("Wheel: tensura_kinds.json could not be read, using the plain kind rule", e);
            }
        }
        return kinds;
    }

    public static PowerGroup groupOf(ManasSkill skill) {
        return skill instanceof Magic ? PowerGroup.MAGIC : skill instanceof Battlewill ? PowerGroup.BATTLEWILL : PowerGroup.SKILLS;
    }

    /** How many class walks ran (a test checks that building the list twice does not walk again). */
    public static final java.util.concurrent.atomic.AtomicInteger REFLECTIONS = new java.util.concurrent.atomic.AtomicInteger();

    /** True when the class chain of {@code type}, below {@code stop}, declares a method with this name. */
    static boolean declares(Class<?> type, Class<?> stop, String... names) {
        REFLECTIONS.incrementAndGet();
        for (Class<?> c = type; c != null && c != stop && c != Object.class; c = c.getSuperclass()) {
            for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                for (String n : names) {
                    if (m.getName().equals(n) && !m.isSynthetic() && !m.isBridge()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Whether a skill class declares {@code onHeld} or {@code onRelease} below {@code TensuraSkill}: reflection, so it runs once per class, not once per build. */
    private static final ClassValue<Boolean> DECLARES_HOLD = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            return declares(type, TensuraSkill.class, "onHeld", "onRelease");
        }
    };
    /** Whether a race class declares an ability below {@code ManasRace}, once per class. */
    private static final ClassValue<Boolean> DECLARES_ABILITY = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            return declares(type, ManasRace.class, "onActivateAbility", "onHeldAbility", "onReleaseAbility");
        }
    };

    public static PowerKind kindOf(ManasSkillInstance inst, ServerPlayer p) {
        ManasSkill skill = inst.getSkill();
        ResourceLocation id = inst.getSkillId();
        Optional<PowerKind> forced = kinds().override(id);
        if (forced.isPresent()) {
            return forced.get();
        }
        if (skill instanceof Magic m) {
            return m.isInstantCast(inst, p) ? PowerKind.INSTANT : PowerKind.HELD;
        }
        if (DECLARES_HOLD.get(skill.getClass())) {
            return PowerKind.HELD;
        }
        return skill.canBeToggled(inst, p) ? PowerKind.TOGGLE : PowerKind.INSTANT;
    }

    /**
     * A skill Tensura does not let you slot but lets you switch on and off (regeneration, danger sense, strengthen body...): a "passive switch", listed as a toggle and flipped through
     * Tensura's own toggle ({@code RequestAbilityModeChangePacket.toggleAbility}). The resistances and nullifications ({@code ResistSkill}) are always on and are not powers.
     */
    public static boolean isPassiveSwitch(TensuraSkill ts, ManasSkillInstance inst, ServerPlayer p) {
        return !(ts instanceof ResistSkill) && ts.canBeToggled(inst, p);
    }

    /** A held or cast-time power that runs longer than a second: the kind of power Tensura cuts off in no slot. */
    public static boolean longRunning(ManasSkillInstance inst, ServerPlayer p, PowerKind kind) {
        if (kind != PowerKind.HELD) {
            return false;
        }
        return !(inst.getSkill() instanceof Magic m) || m.getCastingTime(inst, p) > LONG_TICKS;
    }

    /** True when the skill is in no slot the HUD can reach (active preset, or the next one through the next-preset hook). */
    public static boolean unslotted(ServerPlayer p, ManasSkill skill) {
        return !TensuraStorages.getAbilityFrom(p).isAbilityInActivePreset(skill);
    }

    @Override
    public List<WheelEntry> list(ServerPlayer p) {
        List<WheelEntry> out = new ArrayList<>();
        Set<ResourceLocation> owned = new HashSet<>();
        List<ManasSkillInstance> learned = new ArrayList<>(SkillAPI.getSkillsFrom(p).getLearnedSkills());
        for (ManasSkillInstance inst : learned) {
            owned.add(inst.getSkillId());
        }
        TensuraKinds kinds = kinds();
        for (ManasSkillInstance inst : learned) {
            ManasSkill skill = inst.getSkill();
            ResourceLocation id = inst.getSkillId();
            if (!(skill instanceof TensuraSkill ts)) {
                continue;
            }
            boolean stone = "infinity".equals(id.getNamespace());
            boolean slottable = ts.canBeSlotted(inst, p, 0);
            boolean passiveSwitch = !slottable && !stone && isPassiveSwitch(ts, inst, p);
            if (!slottable && !stone && !passiveSwitch) {
                continue;
            }
            if (kinds.hiddenByHigherTier(id, owned)) {
                continue;
            }
            out.add(entry(p, inst, ts, id, slottable, passiveSwitch));
        }
        raceAbility(p).ifPresent(out::add);
        return out;
    }

    private static WheelEntry entry(ServerPlayer p, ManasSkillInstance inst, TensuraSkill ts, ResourceLocation id, boolean slottable, boolean passiveSwitch) {
        PowerKind kind = passiveSwitch ? PowerKind.TOGGLE : kindOf(inst, p);
        int modes = Math.max(1, inst.getModes());
        int mode = TensuraModes.current(p, inst);
        List<Component> modeNames = new ArrayList<>();
        for (int m = 0; modes > 1 && m < Math.min(modes, 16); m++) {
            modeNames.add(ts.getModeName(inst, m));
        }
        // ManasCore counts whole seconds and does not keep the full length: the clock keeps the end it first saw, and the press that started it noted the full length
        int left = inst.onCoolDown(mode) ? inst.getCoolDown(mode) * 20 : 0;
        CooldownClock.Reading cooldown = WheelClocks.of(p).observe(new PowerRef(PowerSource.TENSURA, id).key(), WheelClocks.now(p), left, 20, 0);
        LockReason lock = LockReason.NONE;
        if (!inst.canInteractSkill(p) || !passiveSwitch && !ts.canActivateSkill(inst, p, mode)) {
            lock = LockReason.REFUSED_NOW;
        }
        if (lock == LockReason.NONE && !passiveSwitch && kind != PowerKind.TOGGLE && unslotted(p, ts) && needsSlot(p, inst, kind)) {
            lock = LockReason.NEEDS_SLOT;
        }
        int learning = ts instanceof ResistSkill r ? r.getLearningPointRequirement() : 0;
        Mastery m = Mastery.of(inst.getMastery(), inst.getMaxMastery(), learning, inst.isMastered(p));
        return new WheelEntry(new PowerRef(PowerSource.TENSURA, id), groupOf(ts), kind, ts.getName(), modes > 1 ? ts.getModeName(inst, mode) : Component.empty(),
                modeNames, ts.getSkillIcon(), new PowerState(inst.isToggled(), 0L, 0, lock, mode, modes).withCooldown(cooldown), slottable, m == null ? -1f : m.fraction());
    }

    /** Whether a skill that is in no slot must be put in one first: the config says so, the hook is off, or the spike showed that kind does not survive. */
    static boolean needsSlot(ServerPlayer p, ManasSkillInstance inst, PowerKind kind) {
        if (!HudConfig.wheelUnslottedTensura() || !HudConfig.nextPresetActive()) {
            return true;
        }
        return HELD_NEEDS_SLOT && longRunning(inst, p, kind);
    }

    /** The one race ability entry, for a race whose class declares an ability. */
    static Optional<WheelEntry> raceAbility(ServerPlayer p) {
        Optional<ManasRaceInstance> race = RaceAPI.getRaceFrom(p).getRace();
        if (race.isEmpty()) {
            return Optional.empty();
        }
        ManasRaceInstance inst = race.get();
        ManasRace r = inst.getRace();
        if (!DECLARES_ABILITY.get(r.getClass())) {
            return Optional.empty();
        }
        LockReason lock = inst.canActivateAbility(p) ? LockReason.NONE : LockReason.REFUSED_NOW;
        int left = inst.isOnCooldown() ? Math.max(0, inst.getCooldown()) : 0;
        CooldownClock.Reading cooldown = WheelClocks.of(p).observe(new PowerRef(PowerSource.TENSURA, RACE_ABILITY).key(), WheelClocks.now(p), left, 20, 0);
        return Optional.of(new WheelEntry(new PowerRef(PowerSource.TENSURA, RACE_ABILITY), PowerGroup.SKILLS, PowerKind.HELD, Component.translatable(
                "supernaturalbloodlines.wheel.race_ability", r.getName()), Component.empty(), List.of(), r.getRaceIcon(),
                PowerState.READY.withCooldown(cooldown).withLock(lock), false, -1f));
    }
}
