package net.ofatech.supernaturalbloodlines.parts.evolutionrites.gametest;

import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.centre;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.item;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.util.EnergyHelper;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.enchantedtensura.gametest.RecordingPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.pack.LunarEclipse;
import net.ofatech.supernaturalbloodlines.shared.tensura.SkillOwners;

/** Shared by the evolution rites' GameTests: the night, the people and the skills. */
final class EvoTestSupport {
    static final long CYCLE = 24000L * 8;
    static final long NIGHT = 14000;

    private EvoTestSupport() {
    }

    /** Runs {@code body} at a full-moon night ({@code full}) or an ordinary one, and restores the time. */
    static void atNight(GameTestHelper helper, boolean full, Runnable body) {
        ServerLevel level = helper.getLevel();
        long before = level.getDayTime();
        long base = before - Math.floorMod(before, CYCLE);
        level.setDayTime(full ? base + NIGHT : base + 24000L * 4 + NIGHT);
        try {
            helper.assertTrue((level.getMoonPhase() == 0) == full, "could not set the moon, phase is " + level.getMoonPhase());
            body.run();
        } finally {
            level.setDayTime(before);
        }
    }

    /** A full-moon night with a lunar eclipse on it. */
    static void atEclipse(GameTestHelper helper, Runnable body) {
        atNight(helper, true, () -> {
            var server = helper.getLevel().getServer();
            helper.assertTrue(LunarEclipse.start(server) || LunarEclipse.active(server), "could not start the eclipse");
            try {
                body.run();
            } finally {
                LunarEclipse.stop(server);
            }
        });
    }

    /** In the level, away from the circle, as a witch-type performer. */
    static RecordingPlayer performer(GameTestHelper helper, Species species) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(species).build());
        p.setPos(Vec3.atBottomCenterOf(centre(helper)).add(5, 1, 0));
        helper.getLevel().addNewPlayer(p);
        return p;
    }

    static RecordingPlayer onTheCircle(GameTestHelper helper, boolean sneaking, double baseMaxEp) {
        RecordingPlayer p = RecordingPlayer.create(helper);
        p.setPos(Vec3.atBottomCenterOf(centre(helper)).add(0, 1, 0));
        helper.getLevel().addNewPlayer(p);
        p.setShiftKeyDown(sneaking);
        EnergyHelper.setBaseMaxEP(p, baseMaxEp);
        return p;
    }

    /** Gives the player the skill (free, with our mark) at this mastery. */
    static ManasSkillInstance give(ServerPlayer p, String id, double mastery) {
        var skill = SkillAPI.getSkillRegistry().get(ResourceLocation.parse(id));
        if (skill == null) {
            throw new IllegalStateException("no skill " + id);
        }
        SkillOwners.grant(p, skill);
        ManasSkillInstance inst = SkillAPI.getSkillsFrom(p).getSkill(skill).orElseThrow(() -> new IllegalStateException("could not give " + id));
        inst.setMastery(mastery);
        return inst;
    }

    static boolean has(ServerPlayer p, String id) {
        return SkillAPI.getSkillsFrom(p).getSkill(ResourceLocation.parse(id)).isPresent();
    }

    static void forgetAll(ServerPlayer p) {
        var skills = SkillAPI.getSkillsFrom(p);
        for (ManasSkillInstance inst : List.copyOf(skills.getLearnedSkills())) {
            skills.forgetSkill(inst);
        }
    }

    static List<ItemStack> stacks(Object... idAndCount) {
        java.util.ArrayList<ItemStack> out = new java.util.ArrayList<>();
        for (int i = 0; i < idAndCount.length; i += 2) {
            out.add(new ItemStack(item((String) idAndCount[i]), (Integer) idAndCount[i + 1]));
        }
        return out;
    }
}
