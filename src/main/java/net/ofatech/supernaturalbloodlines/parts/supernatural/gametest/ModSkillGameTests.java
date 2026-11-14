package net.ofatech.supernaturalbloodlines.parts.supernatural.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.VampirismRegistries;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import de.teamlapen.vampirism.api.entity.player.skills.ISkill;
import de.teamlapen.vampirism.api.entity.player.skills.ISkillHandler;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.supernatural.skill.ModSkillService;

/** Phase 12c Task 5: unlocking a Vampirism or Werewolves skill through the mod's own skill handler. */
@PrefixGameTestTemplate(false)
public final class ModSkillGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private ModSkillGameTests() {}

    private static ServerPlayer member(GameTestHelper helper, IPlayableFaction<?> faction, int level) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(faction, level), "could not join " + faction.getID());
        return p;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ISkillHandler handler(ServerPlayer p) {
        return VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow().getSkillHandler();
    }

    /** The first skill of the faction whose own check says {@code wanted} for this player. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ResourceLocation skillWith(ServerPlayer p, IPlayableFaction<?> faction, ISkillHandler.Result wanted) {
        StringBuilder seen = new StringBuilder();
        for (var entry : VampirismRegistries.SKILL.get().entrySet()) {
            ISkill skill = entry.getValue();
            if (skill.getFaction().filter(f -> f == faction).isEmpty()) {
                continue;
            }
            ISkillHandler.Result r = handler(p).canSkillBeEnabled(skill);
            if (r == wanted) {
                return entry.getKey().location();
            }
            seen.append(r).append(' ');
        }
        throw new AssertionError("no " + faction.getID() + " skill the handler answers " + wanted + " for; it answers: " + seen);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean enabled(ServerPlayer p, ResourceLocation id) {
        return handler(p).isSkillEnabled((ISkill) VampirismRegistries.SKILL.get().get(id));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int cost(ResourceLocation id) {
        return ((ISkill) VampirismRegistries.SKILL.get().get(id)).getSkillPointCost();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unlockSpendsModPoints(GameTestHelper helper) {
        ServerPlayer p = member(helper, VReference.VAMPIRE_FACTION, 14);
        ResourceLocation skill = skillWith(p, VReference.VAMPIRE_FACTION, ISkillHandler.Result.OK);
        int points = handler(p).getLeftSkillPoints();
        helper.assertTrue(ModSkillService.unlock(p, skill.toString()).ok(), "the unlock was refused");
        helper.assertTrue(enabled(p, skill), "the skill is not enabled");
        helper.assertTrue(handler(p).getLeftSkillPoints() == points - cost(skill), "points " + points + " -> " + handler(p).getLeftSkillPoints());
        helper.assertTrue(ModSkillService.unlock(p, skill.toString()).reason().equals("already_enabled"), "enabled twice");
        helper.succeed();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @GameTest(template = "empty", templateNamespace = NS)
    public static void unlockRefusedWithoutPoints(GameTestHelper helper) {
        ServerPlayer p = member(helper, VReference.VAMPIRE_FACTION, 14);
        // spend every point on skills the handler allows, so that what is left is out of reach by points alone
        for (int guard = 0; guard < 80; guard++) {
            ResourceLocation next = null;
            for (var entry : VampirismRegistries.SKILL.get().entrySet()) {
                ISkill skill = entry.getValue();
                if (skill.getFaction().filter(f -> f == VReference.VAMPIRE_FACTION).isPresent() && handler(p).canSkillBeEnabled(skill) == ISkillHandler.Result.OK) {
                    next = entry.getKey().location();
                    break;
                }
            }
            if (next == null) {
                break;
            }
            helper.assertTrue(ModSkillService.unlock(p, next.toString()).ok(), "could not spend the points on " + next);
        }
        ResourceLocation skill = skillWith(p, VReference.VAMPIRE_FACTION, ISkillHandler.Result.NO_POINTS);
        int points = handler(p).getLeftSkillPoints();
        var r = ModSkillService.unlock(p, skill.toString());
        helper.assertTrue(r.reason().equals("no_points") && !enabled(p, skill) && handler(p).getLeftSkillPoints() == points, "result " + r.reason());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unlockRefusedWithoutParent(GameTestHelper helper) {
        ServerPlayer p = member(helper, VReference.VAMPIRE_FACTION, 14);
        ResourceLocation skill = skillWith(p, VReference.VAMPIRE_FACTION, ISkillHandler.Result.PARENT_NOT_ENABLED);
        int points = handler(p).getLeftSkillPoints();
        var r = ModSkillService.unlock(p, skill.toString());
        helper.assertTrue(r.reason().equals("parent_not_enabled") && !enabled(p, skill) && handler(p).getLeftSkillPoints() == points, "result " + r.reason());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void werewolfSkillUnlock(GameTestHelper helper) {
        if (!ModList.get().isLoaded("werewolves")) {
            helper.succeed();
            return;
        }
        Wolves.run(helper);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void ghoulRefused(GameTestHelper helper) {
        ServerPlayer p = member(helper, VReference.VAMPIRE_FACTION, 14);
        ResourceLocation skill = skillWith(p, VReference.VAMPIRE_FACTION, ISkillHandler.Result.OK);
        int points = handler(p).getLeftSkillPoints();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().ghoul(true).build());
        var r = ModSkillService.unlock(p, skill.toString());
        helper.assertTrue(r.reason().equals("ghoul") && !enabled(p, skill) && handler(p).getLeftSkillPoints() == points, "a Ghoul unlocked: " + r.reason());
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anotherFactionsSkillAndInventedIdsAreRefused(GameTestHelper helper) {
        ServerPlayer p = member(helper, VReference.VAMPIRE_FACTION, 14);
        ResourceLocation hunterSkill = skillWith(member(helper, VReference.HUNTER_FACTION, 14), VReference.HUNTER_FACTION, ISkillHandler.Result.OK);
        int points = handler(p).getLeftSkillPoints();
        helper.assertTrue(ModSkillService.unlock(p, hunterSkill.toString()).reason().equals("wrong_faction"), "a vampire took a hunter skill");
        for (String id : new String[] {"", "x", "../../etc", "vampirism:nope", "\u0000", "a:b:c"}) {
            helper.assertTrue(ModSkillService.unlock(p, id).reason().equals("unknown_skill"), "'" + id + "' was accepted");
        }
        ServerPlayer nobody = TestPlayers.create(helper);
        helper.assertTrue(ModSkillService.unlock(nobody, hunterSkill.toString()).reason().equals("no_faction"), "a player in no faction unlocked");
        helper.assertTrue(handler(p).getLeftSkillPoints() == points, "points changed");
        helper.succeed();
    }

    /** Keeps the Werewolves classes out of this one's linking when the mod is missing. */
    private static final class Wolves {
        static void run(GameTestHelper helper) {
            IPlayableFaction<?> wolves = de.teamlapen.werewolves.api.WReference.WEREWOLF_FACTION;
            ServerPlayer p = member(helper, wolves, 14);
            ResourceLocation skill = skillWith(p, wolves, ISkillHandler.Result.OK);
            int points = handler(p).getLeftSkillPoints();
            helper.assertTrue(ModSkillService.unlock(p, skill.toString()).ok(), "the unlock was refused");
            helper.assertTrue(enabled(p, skill) && handler(p).getLeftSkillPoints() == points - cost(skill), "the werewolf skill was not paid for with werewolf points");
        }
    }
}