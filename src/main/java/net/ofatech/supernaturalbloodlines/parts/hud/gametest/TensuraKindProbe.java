package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.ability.TensuraSkill;
import io.github.manasmods.tensura.ability.battlewill.Battlewill;
import io.github.manasmods.tensura.ability.magic.Magic;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.hud.wheel.TensuraProvider;

/**
 * Run by hand ({@code WHEEL_PROBE=1}), never part of the matrix: walks Tensura's skill registry and writes, for each skill, what the wheel's kind rule reads, to
 * {@code %TEMP%\opencode\wheel-w1\tensura-kinds.txt}. It asserts nothing.
 */
@PrefixGameTestTemplate(false)
public final class TensuraKindProbe {
    private static final String NS = SupernaturalBloodlines.MODID;

    private TensuraKindProbe() {
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void writeTheKindTable(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        List<String> lines = new ArrayList<>();
        lines.add("id\tgroup\tslottable\tonHeld\tonRelease\ttoggle\tinstantCast\tcastTicks\tmodes\tkind\tclass");
        for (ManasSkill skill : SkillAPI.getSkillRegistry()) {
            ResourceLocation id = SkillAPI.getSkillRegistry().getId(skill);
            try {
                ManasSkillInstance inst = WheelTestKit.learn(p, id);
                String group = skill instanceof Magic ? "MAGIC" : skill instanceof Battlewill ? "ART" : skill instanceof TensuraSkill ? "SKILL" : "OTHER";
                boolean slottable = skill instanceof TensuraSkill ts && ts.canBeSlotted(inst, p, 0);
                boolean held = TensuraProviderProbe.declares(skill.getClass(), "onHeld");
                boolean release = TensuraProviderProbe.declares(skill.getClass(), "onRelease");
                String instant = skill instanceof Magic m ? String.valueOf(m.isInstantCast(inst, p)) : "-";
                String cast = skill instanceof Magic m ? String.valueOf(m.getCastingTime(inst, p)) : "-";
                String kind = skill instanceof TensuraSkill ? TensuraProvider.kindOf(inst, p).name() : "-";
                lines.add(id + "\t" + group + "\t" + slottable + "\t" + held + "\t" + release + "\t" + skill.canBeToggled(inst, p) + "\t" + instant + "\t" + cast
                        + "\t" + inst.getModes() + "\t" + kind + "\t" + skill.getClass().getName());
            } catch (Throwable t) {
                lines.add(id + "\tERROR\t" + t);
            }
        }
        lines.add("");
        lines.add("races: id\thasAbilityMethod\tclass");
        for (ManasRace race : RaceAPI.getRaceRegistry()) {
            boolean ability = TensuraProviderProbe.declaresRace(race.getClass());
            lines.add(RaceAPI.getRaceRegistry().getId(race) + "\t" + ability + "\t" + race.getClass().getName());
        }
        try {
            Path out = Path.of(System.getenv().getOrDefault("WHEEL_PROBE_OUT", "tensura-kinds.txt"));
            Files.write(out, lines, StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        helper.succeed();
    }

    /** What pressing and releasing a slotted toggle-capable skill does to its toggled flag (who flips it: the press, the release, or only the skill screen's toggle). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void writeTheToggleTable(GameTestHelper helper) {
        List<String> lines = new ArrayList<>();
        lines.add("id\tslottable\tkind\tafterPress\tafterRelease\tafterToggleAbility\tticking");
        for (ManasSkill skill : SkillAPI.getSkillRegistry()) {
            ResourceLocation id = SkillAPI.getSkillRegistry().getId(skill);
            if (!(skill instanceof TensuraSkill)) {
                continue;
            }
            try {
                ServerPlayer p = TestPlayers.create(helper);
                ManasSkillInstance inst = WheelTestKit.learn(p, id);
                if (!skill.canBeToggled(inst, p)) {
                    continue;
                }
                boolean slottable = ((TensuraSkill) skill).canBeSlotted(inst, p, 0);
                if (slottable) {
                    io.github.manasmods.tensura.storage.TensuraStorages.getAbilityFrom(p).setActivePreset(0);
                    io.github.manasmods.tensura.storage.TensuraStorages.getAbilityFrom(p).setAbilitySlot(0, 0, skill, 0);
                    SkillAPI.getSkillsFrom(p).startHoldSkill(inst, 0, 0);
                }
                boolean afterPress = inst.isToggled();
                boolean ticking = io.github.manasmods.manascore.skill.impl.TickingSkill.isTickingSkill(p, skill, 0);
                if (slottable) {
                    WheelTestKit.tick(p, 2);
                    SkillAPI.getSkillsFrom(p).handleSkillRelease(inst, 0, 0, false);
                    WheelTestKit.tick(p, 2);
                }
                boolean afterRelease = inst.isToggled();
                io.github.manasmods.tensura.network.c2s.RequestAbilityModeChangePacket.toggleAbilityPacket(id).toggleAbility(p);
                boolean afterToggle = inst.isToggled();
                lines.add(id + "\t" + slottable + "\t" + TensuraProvider.kindOf(inst, p) + "\t" + afterPress + "\t" + afterRelease + "\t" + afterToggle + "\t" + ticking);
            } catch (Throwable t) {
                lines.add(id + "\tERROR\t" + t);
            }
        }
        try {
            Files.write(Path.of(System.getenv().getOrDefault("WHEEL_PROBE_OUT", "tensura-kinds.txt") + ".toggles"), lines, StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        helper.succeed();
    }

    /** The same reflection the provider uses, spelled out so the probe prints what it saw. */
    private static final class TensuraProviderProbe {
        static boolean declares(Class<?> type, String name) {
            for (Class<?> c = type; c != null && c != TensuraSkill.class && c != Object.class; c = c.getSuperclass()) {
                for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                    if (m.getName().equals(name) && !m.isSynthetic() && !m.isBridge()) {
                        return true;
                    }
                }
            }
            return false;
        }

        static boolean declaresRace(Class<?> type) {
            for (Class<?> c = type; c != null && c != ManasRace.class && c != Object.class; c = c.getSuperclass()) {
                for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                    if ((m.getName().equals("onActivateAbility") || m.getName().equals("onHeldAbility") || m.getName().equals("onReleaseAbility")) && !m.isSynthetic()
                            && !m.isBridge()) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
