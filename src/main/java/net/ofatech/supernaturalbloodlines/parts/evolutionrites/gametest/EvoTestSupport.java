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

    /**
     * Whether a test of the rites should be skipped because the part is off. The rite data is there exactly when the part is on, so a part that is ON with a rite
     * file missing (or one that is OFF with the file present) FAILS the test: a skip must never hide missing data.
     */
    static boolean skipped(GameTestHelper helper, String rite) {
        boolean on = net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("evolutionRites");
        boolean there = net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type(helper, rite) != null;
        helper.assertTrue(on == there, "the part is " + (on ? "on" : "off") + " but the rite " + rite + " is " + (there ? "loaded" : "missing"));
        if (!on) {
            helper.succeed();
            return true;
        }
        return false;
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

    /** A lunar eclipse running at noon of a full-moon day, where it does not count (R358: only between dusk and dawn). */
    static void atEclipseAtNoon(GameTestHelper helper, Runnable body) {
        ServerLevel level = helper.getLevel();
        var server = level.getServer();
        long before = level.getDayTime();
        long base = before - Math.floorMod(before, CYCLE);
        level.setDayTime(base + NIGHT);
        helper.assertTrue(LunarEclipse.start(server) || LunarEclipse.active(server), "could not start the eclipse");
        level.setDayTime(base + 6000);
        try {
            helper.assertTrue(LunarEclipse.active(server), "the eclipse ended when the clock moved to noon");
            body.run();
        } finally {
            LunarEclipse.stop(server);
            level.setDayTime(before);
        }
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

    /**
     * Whether Tensura's own learn of this skill goes through for a fresh player in this run. An installed addon can close it by its own rule (Mysticism
     * charges Soul Energy for every skill learned and vetoes when the player cannot pay): the rite keeps that rule, so then it must refuse cleanly.
     */
    static boolean learnOpen(GameTestHelper helper, String id) {
        RecordingPlayer control = onTheCircle(helper, false, 5_000_000);
        try {
            var skill = SkillAPI.getSkillRegistry().get(ResourceLocation.parse(id));
            return skill != null && io.github.manasmods.tensura.ability.SkillHelper.learnSkill(control, skill);
        } finally {
            forgetAll(control);
            net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.remove(helper, control);
        }
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

    /** Drops {@code count} of an item as a loose entity on the ground, {@code dx} blocks east of the circle centre (the offering lies loose on the circle). */
    static void lay(GameTestHelper helper, String itemId, int count, double dx) {
        var at = Vec3.atBottomCenterOf(centre(helper)).add(dx, 1, 0);
        helper.getLevel().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), at.x, at.y, at.z, new ItemStack(item(itemId), count)));
    }

    /** How many of an item lie loose within the rite's offering radius of the centre. */
    static int lying(GameTestHelper helper, String itemId) {
        int n = 0;
        for (var e : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(centre(helper)).inflate(net.ofatech.supernaturalbloodlines.parts.evolutionrites.rite.MasteryRiteFactory.OFFERING_RADIUS, 3,
                        net.ofatech.supernaturalbloodlines.parts.evolutionrites.rite.MasteryRiteFactory.OFFERING_RADIUS))) {
            if (e.getItem().is(item(itemId))) {
                n += e.getItem().getCount();
            }
        }
        return n;
    }

    static void clearLying(GameTestHelper helper) {
        for (var e : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(centre(helper)).inflate(net.ofatech.supernaturalbloodlines.parts.evolutionrites.rite.MasteryRiteFactory.OFFERING_RADIUS, 3,
                        net.ofatech.supernaturalbloodlines.parts.evolutionrites.rite.MasteryRiteFactory.OFFERING_RADIUS))) {
            e.discard();
        }
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
