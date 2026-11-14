package net.ofatech.supernaturalbloodlines.parts.artifacts.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.werewolves.api.WReference;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.ofatech.supernaturalbloodlines.parts.artifacts.MountainAsh;
import net.ofatech.supernaturalbloodlines.parts.artifacts.block.MountainAshLineBlockEntity;
import net.ofatech.supernaturalbloodlines.shared.werewolves.EmissaryLines;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;

/** Mountain ash against werewolves. Links Werewolves, so it is registered only when Werewolves is loaded. */
@PrefixGameTestTemplate(false)
public final class MountainAshWerewolfGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private MountainAshWerewolfGameTests() {
    }

    @GameTest(template = "tall", templateNamespace = NS)
    public static void werewolfCannotCrossAshLine(GameTestHelper helper) {
        MountainAshGameTests.lay(helper);
        ServerPlayer werewolf = TestPlayers.createMortal(helper);
        werewolf.setPos(helper.absoluteVec(new Vec3(4.5, 1, 1.5)));
        helper.assertTrue(VampirismAPI.factionPlayerHandler(werewolf).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        MountainAshGameTests.assertStopped(helper, werewolf, "a werewolf player");
        helper.succeed();
    }

    /** The species part answers "who is an Emissary"; here a stand-in does, so this test needs no packs. */
    @GameTest(template = "tall", templateNamespace = NS)
    public static void emissaryPlacedLineIsFlagged(GameTestHelper helper) {
        ServerPlayer emissary = TestPlayers.createMortal(helper);
        ServerPlayer other = TestPlayers.createMortal(helper);
        EmissaryLines.setEmissaryCheck(p -> p == emissary);
        try {
            BlockPos ordinary = new BlockPos(1, 1, 1);
            BlockPos laid = new BlockPos(2, 1, 1);
            BlockState line = MountainAshGameTests.lay(helper);
            for (BlockPos pos : new BlockPos[] {ordinary, laid}) {
                helper.setBlock(pos, line);
            }
            ItemStack dust = new ItemStack(MountainAsh.DUST.get());
            line.getBlock().setPlacedBy(helper.getLevel(), helper.absolutePos(ordinary), line, other, dust);
            line.getBlock().setPlacedBy(helper.getLevel(), helper.absolutePos(laid), line, emissary, dust);
            helper.assertTrue(helper.getBlockEntity(ordinary) instanceof MountainAshLineBlockEntity b && !b.isEmissary(), "an ordinary player's line is flagged");
            helper.assertTrue(helper.getBlockEntity(laid) instanceof MountainAshLineBlockEntity b && b.isEmissary(), "the Emissary's line is not flagged");
        } finally {
            EmissaryLines.setEmissaryCheck(p -> false);
        }
        helper.succeed();
    }

    /** A witch-werewolf is prey (the werewolf faction) with Ashwarden (the witch tree): their own line must not push them, another's prey is. */
    @GameTest(template = "tall", templateNamespace = NS, batch = "ashwarden")
    public static void ashwardenNeverPushesTheLinesOwner(GameTestHelper helper) {
        if (!net.ofatech.supernaturalbloodlines.core.EnabledParts.isEnabled("supernatural")) {
            helper.succeed();
            return;
        }
        ServerPlayer owner = MountainAshGameTests.player(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(owner).setFactionAndLevel(WReference.WEREWOLF_FACTION, 1), "could not make a werewolf");
        net.ofatech.supernaturalbloodlines.core.BloodlineAccess.set(owner,
                net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState.NONE.toBuilder().species(net.ofatech.supernaturalbloodlines.core.bloodline.Species.WITCH).build());
        net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess.set(owner,
                net.ofatech.supernaturalbloodlines.core.supernatural.SupernaturalAccess.get(owner).toBuilder().skills(java.util.Set.of("S-WIT-3")).build());
        try {
            MountainAshGameTests.layBy(helper, owner);
            MountainAsh.pinOwner(owner);
            helper.assertTrue(net.ofatech.supernaturalbloodlines.core.Supernatural.isPrey(owner), "setup: a witch-werewolf is not prey");
            helper.assertTrue(net.ofatech.supernaturalbloodlines.shared.supernatural.LadderQuery.owns(owner, "S-WIT-3"), "setup: Ashwarden is not active for a witch-werewolf");
            net.minecraft.world.entity.Mob other = MountainAshGameTests.touching(helper);
            MountainAsh.ashwardenNow(other);
            helper.assertTrue(other.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS), "setup: other prey was not pushed by the owner's line");
            owner.setPos(helper.absoluteVec(new Vec3(6.65, 1, 1.5)));
            owner.setDeltaMovement(Vec3.ZERO);
            MountainAsh.ashwardenNow(owner);
            helper.assertTrue(!owner.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS), "the line weakened its own placer");
            helper.assertTrue(owner.getDeltaMovement().equals(Vec3.ZERO), "the line pushed its own placer: " + owner.getDeltaMovement());
            other.discard();
        } finally {
            MountainAsh.unpinOwner(owner);
        }
        helper.succeed();
    }}
