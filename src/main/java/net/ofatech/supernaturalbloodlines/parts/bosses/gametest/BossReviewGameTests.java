package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossRules;
import net.ofatech.supernaturalbloodlines.core.boss.BossState;
import net.ofatech.supernaturalbloodlines.core.boss.MikaelRules;
import net.ofatech.supernaturalbloodlines.core.boss.SpawnRules;
import net.ofatech.supernaturalbloodlines.core.boss.BossFacts;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBars;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.MikaelEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.ai.NightHuntGoal;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineWarning;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;
import net.ofatech.supernaturalbloodlines.shared.supernatural.WorldNowHook;

/** The plan 17b panel's findings, one test each: R158 (Mikael and Originals), the feedback to players, the lost, restarted and stranded cases. */
@PrefixGameTestTemplate(false)
public final class BossReviewGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final MikaelRules.Facts HUNTABLE = new MikaelRules.Facts(true, 4, 3, true, true, true, false);
    private static final long NIGHT = 24000L * 20 + 15000;
    private static final long DAY = 24000L * 20 + 6000;

    private BossReviewGameTests() {
    }

    private static LineTestPlayer player(GameTestHelper helper, Species s) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().species(s).build());
        return p;
    }

    private static void online(ServerPlayer... ps) {
        Map<UUID, ServerPlayer> m = new HashMap<>();
        for (ServerPlayer p : ps) {
            m.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(m::get);
        LineWarning.overridePlayers(s -> List.copyOf(m.values()));
    }

    private static void forget(GameTestHelper helper, LineTestPlayer... ps) {
        for (LineTestPlayer p : ps) {
            LineIndex.get(helper.getLevel().getServer()).remove(p.getUUID());
            p.removeData(net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries.DOWNED);
            BloodlineAccess.set(p, net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState.NONE);
        }
    }

    private static void fixedWorld(GameTestHelper helper, ServerPlayer p, long dayTime) {
        BossTestRig.fixedGround();
        BossDirector.useWorld(l -> List.of(p), null);
        BossDirector.useRingCheck((l, at) -> false);
        helper.getLevel().setDayTime(dayTime);
        p.setOnGround(true);
    }

    /** The director sees this one player, with facts that make them a target, on the given time of day. */
    private static void huntable(GameTestHelper helper, ServerPlayer p, long dayTime) {
        BossTestRig.fixedGround();
        BossDirector.useWorld(l -> List.of(p), who -> HUNTABLE);
        helper.getLevel().setDayTime(dayTime);
    }

    // ---- HIGH: R158, Mikael never causes line death by default ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMobsWhiteOakHitDoesNotCountButAPlayersDoes(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            helper.assertTrue(m.getMainHandItem().is(net.ofatech.supernaturalbloodlines.parts.species.item.WhiteOak.TAG), "control: his stake is not white oak");
            helper.assertTrue(!OriginalRules.isWhiteOakKill(m.damageSources().mobAttack(m)), "Mikael's hit counts as a white oak kill by default");
            helper.assertTrue(OriginalRules.isWhiteOakKill(rig.whiteOakBlow()), "a player's white oak hit no longer counts");
            BossesConfig.override("mikael_stakes_originals", true);
            helper.assertTrue(OriginalRules.isWhiteOakKill(m.damageSources().mobAttack(m)), "mikael_stakes_originals = true did not bring the old behaviour back");
            helper.assertTrue(!OriginalRules.isWhiteOakKill(m.damageSources().generic()), "a source that is not a white oak hit counts");
        } finally {
            BossesConfig.override("mikael_stakes_originals", null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void mikaelKillingAPlayerOriginalCausesNoLineDeathByDefault(GameTestHelper helper) {
        LineTestPlayer original = player(helper, Species.ORIGINAL);
        LineTestPlayer child = player(helper, Species.HYBRID);
        try (BossTestRig rig = new BossTestRig(helper)) {
            SireRecorder.record(original, child);
            online(original, child);
            MikaelEntity m = rig.spawn();
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(original, m.damageSources().mobAttack(m)));
            helper.assertTrue(!child.isDeadOrDying(), "Mikael's kill of an Original killed their line");
        } finally {
            forget(helper, original, child);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void mikaelKillingAPlayerOriginalKillsTheLineWhenTheConfigSaysSo(GameTestHelper helper) {
        LineTestPlayer original = player(helper, Species.ORIGINAL);
        LineTestPlayer child = player(helper, Species.HYBRID);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossesConfig.override("mikael_stakes_originals", true);
            SireRecorder.record(original, child);
            online(original, child);
            MikaelEntity m = rig.spawn();
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(original, m.damageSources().mobAttack(m)));
            helper.assertTrue(child.isDeadOrDying(), "with mikael_stakes_originals on, the line did not die");
        } finally {
            BossesConfig.override("mikael_stakes_originals", null);
            forget(helper, original, child);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theDirectorSendsHimForAnOriginalOnlyWhenTheConfigSaysSo(GameTestHelper helper) {
        LineTestPlayer original = player(helper, Species.ORIGINAL);
        LineTestPlayer hybrid = player(helper, Species.ORIGINAL_HYBRID);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossesConfig.override("mikael_min_rung", 0);
            BossDirector.useRoll(() -> 0.0);
            online(original, hybrid);
            for (LineTestPlayer p : List.of(original, hybrid)) {
                fixedWorld(helper, p, NIGHT);
                BossDirector.check(rig.server);
                helper.assertTrue(BossDirector.bossEntity(rig.server, BossId.MIKAEL) == null, "he came for " + BloodlineAccess.get(p).species() + " by default");
            }
            BossesConfig.override("mikael_stakes_originals", true);
            fixedWorld(helper, original, NIGHT + 24000L);
            BossDirector.check(rig.server);
            MikaelEntity m = (MikaelEntity) BossDirector.bossEntity(rig.server, BossId.MIKAEL);
            helper.assertTrue(m != null && original.getUUID().equals(m.prey()), "with mikael_stakes_originals on he did not come for the Original");
            helper.assertTrue(original.received("supernaturalbloodlines.boss.notice.mikael.stake"), "the Original was not warned that his stake kills: " + original.heard());
        } finally {
            BossesConfig.override("mikael_min_rung", null);
            BossesConfig.override("mikael_stakes_originals", null);
            forget(helper, original, hybrid);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void hisHuntGoalSkipsOriginalsAndDaylight(GameTestHelper helper) {
        LineTestPlayer vampire = player(helper, Species.HYBRID);
        LineTestPlayer original = player(helper, Species.ORIGINAL);
        try (BossTestRig rig = new BossTestRig(helper)) {
            online(vampire, original);
            MikaelEntity m = rig.spawn();
            UUID[] prey = {vampire.getUUID()};
            NightHuntGoal goal = new NightHuntGoal(m, () -> prey[0]);
            helper.getLevel().setDayTime(NIGHT);
            helper.assertTrue(goal.canUse(), "control: he does not chase a vampire by night");
            helper.getLevel().setDayTime(DAY);
            helper.assertTrue(!goal.canUse(), "he chases by day");
            helper.getLevel().setDayTime(NIGHT);
            prey[0] = original.getUUID();
            helper.assertTrue(!goal.canUse(), "he chases an Original by default");
            BossesConfig.override("mikael_stakes_originals", true);
            helper.assertTrue(goal.canUse(), "he does not chase an Original with the config on");
        } finally {
            BossesConfig.override("mikael_stakes_originals", null);
            forget(helper, vampire, original);
        }
        helper.succeed();
    }

    // ---- MEDIUM 2: the line-death gate ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWhiteOakDeathOfAClosedOriginalKillsNoLineWhileTheStakeNeedsOpen(GameTestHelper helper) {
        LineTestPlayer child = player(helper, Species.HYBRID);
        try (BossTestRig rig = new BossTestRig(helper)) {
            LineIndex.get(rig.server).setRoot(child.getUUID(), BossTestRig.MIKAEL, true);
            online(child);
            MikaelEntity m = rig.spawn();
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(m, rig.whiteOakBlow()));
            helper.assertTrue(!child.isDeadOrDying(), "a white oak death of a standing Original (which the stake cannot reach) killed their line");
            BossesConfig.override("npc_original_stake_needs_open", false);
            NeoForge.EVENT_BUS.post(new LivingDeathEvent(m, rig.whiteOakBlow()));
            helper.assertTrue(child.isDeadOrDying(), "with npc_original_stake_needs_open off the line did not die");
        } finally {
            BossesConfig.override("npc_original_stake_needs_open", null);
            forget(helper, child);
        }
        helper.succeed();
    }

    // ---- MEDIUM 3: the lost ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLostBossThatShowsItselfAgainIsFoundWhereItIs(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            rig.index.apply(BossId.MIKAEL, BossRules.Event.LOST);
            helper.assertTrue(rig.index.state(BossId.MIKAEL) == BossState.LOST, "control: not lost");
            for (int i = 0; i < 12; i++) {
                m.tick();
            }
            helper.assertTrue(rig.index.state(BossId.MIKAEL) == BossState.ACTIVE, "a lost boss that is standing there is still lost: " + rig.index.state(BossId.MIKAEL));
            m.desiccate();
            rig.index.apply(BossId.MIKAEL, BossRules.Event.LOST);
            for (int i = 0; i < 12; i++) {
                m.tick();
            }
            helper.assertTrue(rig.index.state(BossId.MIKAEL) == BossState.OPEN, "a lost boss that lies there is not open: " + rig.index.state(BossId.MIKAEL));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theOldOneThatComesBackAfterANewOneWasMadeIsDiscarded(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity old = rig.spawn();
            rig.index.apply(BossId.MIKAEL, BossRules.Event.LOST);
            MikaelEntity fresh = BossDirector.spawnMikael(helper.getLevel(), null, helper.absolutePos(new BlockPos(4, 1, 4))).orElseThrow();
            for (int i = 0; i < 12; i++) {
                old.tick();
            }
            helper.assertTrue(old.isRemoved(), "two Mikaels walk the world");
            helper.assertTrue(!fresh.isRemoved() && fresh.getUUID().equals(rig.index.of(BossId.MIKAEL).entity), "the new one was lost with it");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aLostBossThatDiesIsDead(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            rig.index.apply(BossId.MIKAEL, BossRules.Event.LOST);
            m.kill();
            helper.assertTrue(rig.index.state(BossId.MIKAEL) == BossState.DEAD, "the index says " + rig.index.state(BossId.MIKAEL));
        }
        helper.succeed();
    }

    // ---- MEDIUM 4: feedback ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void whiteOakOnAStandingOriginalTellsTheAttackerOnceInAWhile(GameTestHelper helper) {
        LineTestPlayer attacker = LineTestPlayer.create(helper);
        attacker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.ofatech.supernaturalbloodlines.parts.species.item.WhiteOak.STAKE.get()));
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            DamageSource blow = attacker.damageSources().playerAttack(attacker);
            for (int i = 0; i < 3; i++) {
                NeoForge.EVENT_BUS.post(new LivingIncomingDamageEvent(m, new net.neoforged.neoforge.common.damagesource.DamageContainer(blow, 1.0F)));
            }
            helper.assertTrue(attacker.count("supernaturalbloodlines.boss.white_oak_closed") == 1, "told " + attacker.count("supernaturalbloodlines.boss.white_oak_closed") + " times in a row");
            helper.assertTrue(attacker.chat() == 0, "the hint went to chat, not the action bar");
            m.desiccate();
            NeoForge.EVENT_BUS.post(new LivingIncomingDamageEvent(m, new net.neoforged.neoforge.common.damagesource.DamageContainer(blow, 1.0F)));
            helper.assertTrue(attacker.count("supernaturalbloodlines.boss.white_oak_closed") == 1, "told again about an Original who is down");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBarSaysDesiccatedAndChangesColour(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            BossBars.update(m, List.of());
            ServerBossEvent bar = BossBars.barOf(m.getUUID());
            helper.assertTrue(bar.getColor() == BossEvent.BossBarColor.PURPLE && !bar.getName().toString().contains("bar.desiccated"),
                    "a standing boss's bar is " + bar.getColor() + " " + bar.getName());
            m.desiccate();
            BossBars.update(m, List.of());
            helper.assertTrue(bar.getColor() == BossEvent.BossBarColor.YELLOW, "the bar of a fallen boss is " + bar.getColor());
            helper.assertTrue(bar.getName().toString().contains("supernaturalbloodlines.boss.bar.desiccated"), "the bar's name does not say desiccated: " + bar.getName());
            m.recover();
            BossBars.update(m, List.of());
            helper.assertTrue(bar.getColor() == BossEvent.BossBarColor.PURPLE && !bar.getName().toString().contains("bar.desiccated"), "the bar did not go back: " + bar.getColor());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aViewerWithTheChannelGetsTheCardsFactsOnceAndBeforeTheVanillaBar(GameTestHelper helper) {
        MockViewer v = MockViewer.of(helper, true);
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            v.player.setPos(m.getX() + 5, m.getY(), m.getZ());
            BossBars.update(m, List.of(v.player));
            var facts = v.payloads(net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBarInfoPayload.class);
            helper.assertTrue(facts.size() == 1, "one payload for one new viewer, got " + facts.size());
            helper.assertTrue(facts.get(0).boss().orElse(null) == BossId.MIKAEL && facts.get(0).bar().equals(BossBars.barOf(m.getUUID()).getId()),
                    "the payload names the boss and the bar: " + facts.get(0));
            int info = v.indexOf(net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket.class);
            int vanilla = v.indexOf(net.minecraft.network.protocol.game.ClientboundBossEventPacket.class);
            helper.assertTrue(info >= 0 && vanilla >= 0 && info < vanilla, "the facts go before the vanilla bar: " + info + " / " + vanilla);
            BossBars.update(m, List.of(v.player));
            helper.assertTrue(v.payloads(net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBarInfoPayload.class).size() == 1, "the same facts were sent again");
            m.setHealth(m.getHealth() - 20);
            BossBars.update(m, List.of(v.player));
            helper.assertTrue(v.payloads(net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBarInfoPayload.class).size() == 2, "new health was not sent");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aViewerWithoutTheChannelGetsTheVanillaBarAndNoPayloadAndNothingThrows(GameTestHelper helper) {
        MockViewer plain = MockViewer.of(helper, false);
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            plain.player.setPos(m.getX() + 5, m.getY(), m.getZ());
            BossBars.update(m, List.of(plain.player));
            helper.assertTrue(plain.payloads(net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBarInfoPayload.class).isEmpty(), "a client without the channel was sent a payload");
            helper.assertTrue(BossBars.viewers(m) == 1 && plain.indexOf(net.minecraft.network.protocol.game.ClientboundBossEventPacket.class) >= 0, "it still gets the vanilla bar");
            helper.assertTrue(!net.ofatech.supernaturalbloodlines.parts.bosses.director.BossNet.send(plain.player,
                    new net.ofatech.supernaturalbloodlines.parts.bosses.deal.DealOfferPayload(1, 1, List.of())), "a deal offer was sent to a client without the channel");
            helper.assertTrue(plain.payloads(net.ofatech.supernaturalbloodlines.parts.bosses.deal.DealOfferPayload.class).isEmpty(), "the offer reached a client without the channel");
            MockViewer modded = MockViewer.of(helper, true);
            helper.assertTrue(net.ofatech.supernaturalbloodlines.parts.bosses.director.BossNet.send(modded.player,
                    new net.ofatech.supernaturalbloodlines.parts.bosses.deal.DealOfferPayload(1, 1, List.of())), "a deal offer was not sent to a client with the channel");
            helper.assertTrue(modded.payloads(net.ofatech.supernaturalbloodlines.parts.bosses.deal.DealOfferPayload.class).size() == 1, "the offer did not arrive");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlayerWhoHitsABossIsToldOncePerHalfSecondAndOnlyIfTheyWatchItAndCanReceiveIt(GameTestHelper helper) {
        MockViewer hitter = MockViewer.of(helper, true);
        MockViewer plain = MockViewer.of(helper, false);
        MockViewer far = MockViewer.of(helper, true);
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            hitter.player.setPos(m.getX() + 5, m.getY(), m.getZ());
            plain.player.setPos(m.getX() + 5, m.getY(), m.getZ());
            BossBars.update(m, List.of(hitter.player, plain.player));
            long t = 1000;
            helper.assertTrue(BossBars.noteHit(hitter.player, m, t), "the first hit was not told");
            var told = hitter.payloads(net.ofatech.supernaturalbloodlines.parts.bosses.director.BossHitPayload.class);
            helper.assertTrue(told.size() == 1 && told.get(0).bar().equals(BossBars.barOf(m.getUUID()).getId()), "one payload naming the boss's bar: " + told);
            helper.assertTrue(!BossBars.noteHit(hitter.player, m, t + 5), "a second blow within half a second was told again");
            helper.assertTrue(BossBars.noteHit(hitter.player, m, t + 10), "a blow after half a second was not told");
            helper.assertTrue(hitter.payloads(net.ofatech.supernaturalbloodlines.parts.bosses.director.BossHitPayload.class).size() == 2, "two payloads in all");
            helper.assertTrue(!BossBars.noteHit(plain.player, m, t), "a client without the channel was told");
            helper.assertTrue(plain.payloads(net.ofatech.supernaturalbloodlines.parts.bosses.director.BossHitPayload.class).isEmpty(), "and a payload was sent to it");
            helper.assertTrue(!BossBars.noteHit(far.player, m, t), "a player who is not watching the bar was told");
            helper.assertTrue(far.payloads(net.ofatech.supernaturalbloodlines.parts.bosses.director.BossHitPayload.class).isEmpty(), "and was sent a payload");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aViewerWhoLeftTheDimensionOrTheGameIsTakenFromTheBar(GameTestHelper helper) {
        MockViewer gone = MockViewer.of(helper, true);
        MockViewer away = MockViewer.of(helper, true);
        MockViewer stays = MockViewer.of(helper, true);
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            for (MockViewer v : List.of(gone, away, stays)) {
                v.player.setPos(m.getX() + 5, m.getY(), m.getZ());
            }
            BossBars.update(m, List.of(gone.player, away.player, stays.player));
            helper.assertTrue(BossBars.viewers(m) == 3, "control: three watch, " + BossBars.viewers(m));
            gone.player.setRemoved(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            var nether = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
            helper.assertTrue(nether != null, "control: the test server has a Nether");
            away.player.setServerLevel(nether);
            // neither of them is in this level's list any more, as in the game: the update is given only the player who stayed
            BossBars.update(m, List.of(stays.player));
            helper.assertTrue(BossBars.viewers(m) == 1, "the card stayed on a player who left: " + BossBars.viewers(m));
            helper.assertTrue(BossBars.barOf(m.getUUID()).getPlayers().contains(stays.player), "the player who stayed lost the card");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theBarsFactsFollowTheBossAndAShiftMovesTheChipAtOnce(GameTestHelper helper) {
        var human = net.ofatech.supernaturalbloodlines.core.boss.BossBarForms.shifter(net.ofatech.supernaturalbloodlines.core.boss.BossForm.HUMAN);
        MockViewer watcher = MockViewer.of(helper, true);
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            watcher.player.setPos(m.getX() + 5, m.getY(), m.getZ());
            BossBars.update(m, List.of(watcher.player)); // a real viewer: what is sent, not only what is kept
            var plain = BossBars.sentOf(m.getUUID());
            helper.assertTrue(watcher.payloads(net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBarInfoPayload.class).get(0).equals(plain), "what the viewer got is what the server kept");
            helper.assertTrue(plain.boss().orElse(null) == BossId.MIKAEL && plain.look().equals(net.ofatech.supernaturalbloodlines.core.boss.BossBarForms.Look.NONE) && !plain.fallen(),
                    "a boss without forms has no chips: " + plain);
            helper.assertTrue(plain.hp() == Math.round(m.getHealth()) && plain.max() == Math.round(m.getMaxHealth()), "the health in numbers: " + plain);
            m.desiccate();
            BossBars.update(m, List.of(watcher.player));
            helper.assertTrue(BossBars.sentOf(m.getUUID()).fallen(), "a fallen Original is not marked fallen");
            helper.assertTrue(watcher.payloads(net.ofatech.supernaturalbloodlines.parts.bosses.director.BossBarInfoPayload.class).get(1).fallen(), "the viewer was not told");
            var k = rig.spawnKlaus();
            BossBars.update(k, List.of());
            helper.assertTrue(BossBars.sentOf(k.getUUID()).look().equals(human), "Klaus walks as a man: " + BossBars.sentOf(k.getUUID()));
            // tickCount is counted by the level when it ticks an entity, not by tick() itself: a loop on tick() alone would never end, so the count is moved by hand
            k.tickCount = 4;
            k.tick();
            k.setForm(net.ofatech.supernaturalbloodlines.core.boss.BossForm.SHIFTING);
            k.tickCount = 5;
            k.tick(); // the tick after, one that is not a pass of ten: the chip must not wait for the next pass
            helper.assertTrue(k.tickCount % 10 != 0 && BossBars.sentOf(k.getUUID()).look().shifting(), "the chip did not move with the shift: " + BossBars.sentOf(k.getUUID()));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theLineIsToldWhenHeRisesAndOnlyIfTheyWereWarned(GameTestHelper helper) {
        LineTestPlayer known = player(helper, Species.HYBRID);
        LineTestPlayer unknown = player(helper, Species.HYBRID);
        try (BossTestRig rig = new BossTestRig(helper)) {
            LineIndex.get(rig.server).setRoot(known.getUUID(), BossTestRig.MIKAEL, true);
            LineIndex.get(rig.server).setRoot(unknown.getUUID(), BossTestRig.MIKAEL, true);
            BloodlineAccess.set(known, BloodlineAccess.get(known).toBuilder().knownLine(BossTestRig.MIKAEL).build());
            online(known, unknown);
            MikaelEntity m = rig.spawn();
            m.recover();
            helper.assertTrue(!known.received("supernaturalbloodlines.line.risen_named"), "told he rose when he never fell");
            m.desiccate();
            m.recover();
            helper.assertTrue(known.received("supernaturalbloodlines.line.risen_named") && known.heard().contains("[supernaturalbloodlines.original.mikael]"), "the one who knows was not told by name: " + known.heard());
            helper.assertTrue(unknown.received("supernaturalbloodlines.line.risen_hidden"), "the one who does not know was not told: " + unknown.heard());
        } finally {
            forget(helper, known, unknown);
        }
        helper.succeed();
    }

    // ---- MEDIUM 5: a restart while he lies down ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void afterARestartAnOriginalWhoLiesOpenWarnsTheLineAgain(GameTestHelper helper) {
        LineTestPlayer member = player(helper, Species.HYBRID);
        try (BossTestRig rig = new BossTestRig(helper)) {
            LineIndex.get(rig.server).setRoot(member.getUUID(), BossTestRig.MIKAEL, true);
            online(member);
            MikaelEntity m = rig.spawn();
            m.desiccate();
            int before = member.count("supernaturalbloodlines.line.warning_desiccated_hidden");
            helper.assertTrue(before == 1, "control: the first warning was not sent: " + member.heard());
            net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
            m.saveWithoutId(saved);
            m.discard();
            LineWarning.reset(); // the process restarted: the latch is empty
            MikaelEntity again = MikaelEntity.create(helper.getLevel());
            again.load(saved);
            helper.getLevel().addFreshEntity(again);
            helper.assertTrue(again.isDesiccated(), "control: he did not come back down");
            helper.assertTrue(member.count("supernaturalbloodlines.line.warning_desiccated_hidden") == before + 1, "the line was not warned after the restart: " + member.heard());
        } finally {
            forget(helper, member);
        }
        helper.succeed();
    }

    // ---- MEDIUM 6: the hunt warning ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHuntedPlayerIsWarnedOnTheActionBarAndInChatWithAHint(GameTestHelper helper) {
        LineTestPlayer p = player(helper, Species.HYBRID);
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossDirector.useRoll(() -> 0.0);
            online(p);
            huntable(helper, p, NIGHT);
            BossDirector.check(rig.server);
            helper.assertTrue(BossDirector.bossEntity(rig.server, BossId.MIKAEL) != null, "control: he did not come");
            String key = "supernaturalbloodlines.boss.notice.mikael.unknown";
            helper.assertTrue(p.count(key) == 2, "the warning was not on both the action bar and in chat: " + p.heard());
            helper.assertTrue(p.chat() == 2 && p.received() == 3, "chat " + p.chat() + ", everything " + p.received() + ": " + p.heard());
            helper.assertTrue(p.received("supernaturalbloodlines.boss.notice.mikael.hint"), "no hint that mountain ash holds him: " + p.heard());
            helper.assertTrue(!p.received("supernaturalbloodlines.boss.notice.mikael.stake"), "a mortal vampire was told about the stake");
        } finally {
            forget(helper, p);
        }
        helper.succeed();
    }

    // ---- MEDIUM 7: stranded ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMikaelWhosePreyIsGoneWithdrawsAndMayComeAgain(GameTestHelper helper) {
        LineTestPlayer later = player(helper, Species.HYBRID);
        try (BossTestRig rig = new BossTestRig(helper)) {
            helper.getLevel().setDayTime(NIGHT);
            MikaelEntity m = rig.spawn();
            m.setNoAi(true);
            m.hunt(UUID.randomUUID()); // logged off
            for (int i = 0; i < MikaelRules.WITHDRAW_AFTER + 40 && !m.isRemoved(); i++) {
                m.tick();
            }
            helper.assertTrue(m.isRemoved(), "a Mikael whose prey is gone is still standing");
            helper.assertTrue(rig.index.state(BossId.MIKAEL) == BossState.LOST, "the index says " + rig.index.state(BossId.MIKAEL));
            helper.assertTrue(SpawnRules.refusal(new BossFacts(true, true, rig.index.state(BossId.MIKAEL), false)).isEmpty(), "the director would refuse him");
            BossDirector.useRoll(() -> 0.0);
            online(later);
            huntable(helper, later, NIGHT);
            BossDirector.check(rig.server);
            helper.assertTrue(BossDirector.bossEntity(rig.server, BossId.MIKAEL) != null, "he could not come again");
        } finally {
            forget(helper, later);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aMikaelWithPreyHereStaysAndOneWhoIsDesiccatedNeverWithdraws(GameTestHelper helper) {
        LineTestPlayer prey = player(helper, Species.HYBRID);
        try (BossTestRig rig = new BossTestRig(helper)) {
            online(prey);
            helper.getLevel().setDayTime(NIGHT);
            MikaelEntity m = rig.spawn();
            m.setNoAi(true);
            m.hunt(prey.getUUID());
            for (int i = 0; i < MikaelRules.WITHDRAW_AFTER + 40; i++) {
                m.tick();
            }
            helper.assertTrue(!m.isRemoved(), "he withdrew with his prey right there");
            m.hunt(UUID.randomUUID());
            m.desiccate();
            for (int i = 0; i < MikaelRules.WITHDRAW_AFTER + 40; i++) {
                m.tick();
            }
            helper.assertTrue(!m.isRemoved(), "a fallen Original withdrew");
        } finally {
            forget(helper, prey);
        }
        helper.succeed();
    }

    // ---- MEDIUM 8: the Home line ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theHomeLineKnowsStandingFallenAndFarAndNamesOnlyWhoIsKnown(GameTestHelper helper) {
        LineTestPlayer p = player(helper, Species.HYBRID);
        try (BossTestRig rig = new BossTestRig(helper)) {
            helper.assertTrue(BossDirector.near(p).kind() == WorldNowHook.BossNear.NONE_KIND, "control: a boss is near with none out");
            MikaelEntity m = rig.spawn();
            p.setPos(m.getX() + 100, m.getY(), m.getZ());
            WorldNowHook.BossNear near = BossDirector.near(p);
            helper.assertTrue(near.kind() == WorldNowHook.BossNear.STANDING && near.name().isEmpty(), "standing, unknown: " + near);
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().knownLine(BossTestRig.MIKAEL).build());
            helper.assertTrue(BossDirector.near(p).name().equals("mikael"), "one who knows him is not told his name: " + BossDirector.near(p));
            m.desiccate();
            helper.assertTrue(BossDirector.near(p).kind() == WorldNowHook.BossNear.FALLEN, "a fallen Original is not fallen: " + BossDirector.near(p));
            helper.assertTrue(WorldNowHook.of(p).bossNear() == WorldNowHook.BossNear.FALLEN && WorldNowHook.of(p).bossName().equals("mikael"), "the hub flag disagrees");
            p.setPos(m.getX() + 127, m.getY(), m.getZ());
            helper.assertTrue(BossDirector.near(p).kind() != WorldNowHook.BossNear.NONE_KIND, "127 blocks is not near");
            p.setPos(m.getX() + 129, m.getY(), m.getZ());
            helper.assertTrue(BossDirector.near(p).kind() == WorldNowHook.BossNear.NONE_KIND, "129 blocks is near");
        } finally {
            forget(helper, p);
        }
        helper.succeed();
    }

    // ---- LOW ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theDaysCountInGameTimeNotInTheTimeOfDay(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossesConfig.override("original_return_days", 5);
            MikaelEntity m = rig.spawn();
            m.desiccate();
            helper.getLevel().setDayTime(helper.getLevel().getDayTime() + 24000L * 30); // everybody slept for a month
            m.checkReturn();
            helper.assertTrue(m.isDesiccated(), "sleeping through the nights got him up");
            helper.getLevel().setDayTime(rig.dayTime);
            rig.day(FAR_DAY);
            m.checkReturn();
            helper.assertTrue(!m.isDesiccated(), "game time passing did not get him up");
        } finally {
            BossesConfig.override("original_return_days", null);
        }
        helper.succeed();
    }

    /** The day number far enough on from any game time a test can have (the clock is offset to it). */
    private static final long FAR_DAY = 100_000L;

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theMissesAreForgottenWhenTheServerStops(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            m.discard();
            BossDirector.check(rig.server); // one miss: the chunk is loaded and he is not in it
            BossDirector.serverStopped();
            BossDirector.check(rig.server);
            helper.assertTrue(rig.index.state(BossId.MIKAEL) == BossState.ACTIVE, "the miss from before the stop was kept: " + rig.index.state(BossId.MIKAEL));
            BossDirector.check(rig.server);
            helper.assertTrue(rig.index.state(BossId.MIKAEL) == BossState.LOST, "two misses in a row did not lose him: " + rig.index.state(BossId.MIKAEL));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theAshRingIsLookedForOnlyForSomeoneWhoPassesTheRest(GameTestHelper helper) {
        LineTestPlayer mortal = player(helper, Species.NONE);
        LineTestPlayer vampire = player(helper, Species.HYBRID);
        int[] looked = {0};
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossesConfig.override("mikael_min_rung", 0);
            BossDirector.useRingCheck((l, at) -> {
                looked[0]++;
                return false;
            });
            helper.getLevel().setDayTime(NIGHT);
            mortal.setOnGround(true);
            vampire.setOnGround(true);
            BossDirector.factsOf(mortal);
            helper.assertTrue(looked[0] == 0, "the ring was searched for a mortal");
            helper.getLevel().setDayTime(DAY);
            BossDirector.factsOf(vampire);
            helper.assertTrue(looked[0] == 0, "the ring was searched by day");
            helper.getLevel().setDayTime(NIGHT);
            BossDirector.factsOf(vampire);
            helper.assertTrue(looked[0] == 1, "the ring was not searched for someone who passes everything else: " + looked[0]);
        } finally {
            BossesConfig.override("mikael_min_rung", null);
            forget(helper, mortal, vampire);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aRewardPaidOnDeathIsOnDiskAtOnceToo(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            BossesConfig.override("boss_reward_on", "death");
            MikaelEntity m = rig.spawn();
            helper.assertTrue(rig.index.isDirty(), "control: the index was already saved");
            m.kill();
            helper.assertTrue(rig.index.of(BossId.MIKAEL).rewarded && rig.dropsOf(BossItems.MIKAELS_STAKE.get()) == 1, "control: the reward was not paid on death");
            helper.assertTrue(!rig.index.isDirty(), "the index with the paid reward was not written");
        } finally {
            BossesConfig.override("boss_reward_on", null);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void thePaidRewardIsOnDiskAtOnce(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            MikaelEntity m = rig.spawn();
            helper.assertTrue(rig.index.isDirty(), "control: the index was already saved");
            m.desiccate();
            helper.assertTrue(rig.index.of(BossId.MIKAEL).rewarded && rig.dropsOf(BossItems.MIKAELS_STAKE.get()) == 1, "control: the reward was not paid");
            helper.assertTrue(!rig.index.isDirty(), "the index with the paid reward was not written");
            m.recover();
            rig.index.forget(BossId.MIKAEL);
            helper.assertTrue(rig.index.isDirty(), "control: forgetting did not dirty it");
        }
        helper.succeed();
    }
}
