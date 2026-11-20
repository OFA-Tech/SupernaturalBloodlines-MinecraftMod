package net.ofatech.supernaturalbloodlines.parts.bosses.gametest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.boss.BossId;
import net.ofatech.supernaturalbloodlines.core.boss.BossState;
import net.ofatech.supernaturalbloodlines.core.boss.DealRules;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossItems;
import net.ofatech.supernaturalbloodlines.parts.bosses.BossesConfig;
import net.ofatech.supernaturalbloodlines.parts.bosses.deal.DealOfferPayload;
import net.ofatech.supernaturalbloodlines.parts.bosses.deal.Deals;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossClock;
import net.ofatech.supernaturalbloodlines.parts.bosses.director.BossDirector;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.ElijahEntity;
import net.ofatech.supernaturalbloodlines.parts.bosses.entity.NpcOriginalBoss;
import net.ofatech.supernaturalbloodlines.parts.species.gametest.LineTestPlayer;
import net.ofatech.supernaturalbloodlines.parts.species.item.SiresBloodItem;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineWarning;
import net.ofatech.supernaturalbloodlines.parts.species.original.OriginalRules;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;

/** Elijah (plan 17d, R167 and R170): when he comes, the deal and its oath, what breaks it, and that a mob's hit never counts. */
@PrefixGameTestTemplate(false)
public final class ElijahGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final String UNKNOWN = "supernaturalbloodlines.boss.notice.elijah.unknown";
    private static final String KNOWN = "supernaturalbloodlines.boss.notice.elijah.known";
    private static final String SWORN = "supernaturalbloodlines.boss.elijah.sworn";
    private static final String BROKEN = "supernaturalbloodlines.boss.elijah.broken";
    private static final String KEEP = "supernaturalbloodlines.boss.elijah.keep";
    private static final long DAY = 24000L * 20;

    private ElijahGameTests() {
    }

    /** A player near the middle of the test who has discovered this Original (or none). */
    private static LineTestPlayer player(GameTestHelper helper, UUID knownLine) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().knownLine(knownLine).build());
        p.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(3.5, 1, 3.5)));
        return p;
    }

    /** The mods there, a roll that passes, the ground fixed, and these players the only ones in the overworld. */
    private static void world(GameTestHelper helper, long dayTime, LineTestPlayer... ps) {
        BossTestRig.fixedGround();
        BossDirector.useMods(b -> true);
        BossDirector.useOnGround(p -> true);
        BossDirector.useRoll(() -> 0.0);
        BossDirector.useWorld(l -> List.of(ps), null);
        Map<UUID, ServerPlayer> m = new HashMap<>();
        for (ServerPlayer p : ps) {
            m.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(m::get);
        LineWarning.overridePlayers(s -> List.copyOf(m.values()));
        helper.getLevel().setDayTime(dayTime);
    }

    private static void forget(GameTestHelper helper, LineTestPlayer... ps) {
        for (LineTestPlayer p : ps) {
            LineIndex.get(helper.getLevel().getServer()).remove(p.getUUID());
            BloodlineAccess.set(p, BloodlineState.NONE);
        }
    }

    private static ElijahEntity elijahOf(BossTestRig rig) {
        return BossDirector.bossEntity(rig.server, BossId.ELIJAH) instanceof ElijahEntity e ? e : null;
    }

    // ---- when he comes ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heVisitsSomeoneWhoKnowsAnOriginalByDayAndTellsThemWithoutHisName(GameTestHelper helper) {
        LineTestPlayer p = player(helper, NpcOriginal.MIKAEL.uuid());
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(helper, DAY + 6000, p);
            BossDirector.check(rig.server);
            ElijahEntity e = elijahOf(rig);
            helper.assertTrue(e != null && e.isAlive(), "he did not come, by day, to someone who knows an Original");
            helper.assertTrue(rig.index.state(BossId.ELIJAH) == BossState.ACTIVE && rig.index.of(BossId.ELIJAH).appearances == 1, "the index says " + rig.index.state(BossId.ELIJAH));
            helper.assertTrue(p.received(UNKNOWN) && !p.received(KNOWN), "the visit named him to someone who has not discovered him: " + p.heard());
            helper.assertTrue(e.getTarget() == null && !e.hostileForGood(), "he arrived angry");
        } finally {
            forget(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heIsNamedOnlyToSomeoneWhoHasDiscoveredHim(GameTestHelper helper) {
        LineTestPlayer p = player(helper, NpcOriginal.ELIJAH.uuid());
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(helper, DAY + 15000, p);
            BossDirector.check(rig.server);
            helper.assertTrue(elijahOf(rig) != null, "he did not come by night");
            helper.assertTrue(p.received(KNOWN) && !p.received(UNKNOWN), "his line's member was not told his name: " + p.heard());
        } finally {
            forget(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bloodOfAnNpcOriginalInTheBagCountsAndOtherBloodDoesNot(GameTestHelper helper) {
        LineTestPlayer holder = player(helper, null);
        LineTestPlayer other = player(helper, null);
        try (BossTestRig rig = new BossTestRig(helper)) {
            other.getInventory().add(SiresBloodItem.of(UUID.randomUUID(), "Somebody"));
            world(helper, DAY + 6000, other);
            BossDirector.check(rig.server);
            helper.assertTrue(elijahOf(rig) == null, "the blood of a nobody called him");
            holder.getInventory().add(SiresBloodItem.of(NpcOriginal.KOL.uuid(), "Kol"));
            world(helper, DAY + 6000, holder);
            BossDirector.check(rig.server);
            helper.assertTrue(elijahOf(rig) != null, "the blood of an NPC Original did not call him");
            elijahOf(rig).discard();
            rig.index.forget(BossId.ELIJAH);
            holder.getInventory().clearContent();
            holder.getInventory().add(new ItemStack(BossItems.KLAUS_BLOOD.get()));
            BossDirector.check(rig.server);
            helper.assertTrue(elijahOf(rig) != null, "Klaus's Blood in the bag did not call him");
        } finally {
            forget(helper, holder, other);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heStaysAwayFromSomeoneWhoKnowsNothingAndRollsOnceADay(GameTestHelper helper) {
        LineTestPlayer none = player(helper, null);
        LineTestPlayer knows = player(helper, NpcOriginal.MIKAEL.uuid());
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(helper, DAY + 6000, none);
            BossDirector.check(rig.server);
            helper.assertTrue(elijahOf(rig) == null, "he came to someone who knows nothing");
            helper.assertTrue(rig.index.of(BossId.ELIJAH).lastRollDay == -1, "a roll was spent when nobody could be visited");
            world(helper, DAY + 6000, knows);
            BossDirector.useRoll(() -> 0.99);
            BossDirector.check(rig.server);
            helper.assertTrue(elijahOf(rig) == null, "he came on a roll above the chance");
            BossDirector.useRoll(() -> 0.0);
            BossDirector.check(rig.server);
            helper.assertTrue(elijahOf(rig) == null, "a second roll was taken the same day");
            helper.getLevel().setDayTime(DAY + 24000 + 6000);
            BossDirector.check(rig.server);
            helper.assertTrue(elijahOf(rig) != null, "he did not come the next day");
        } finally {
            forget(helper, none, knows);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heIsNotSentWhileSwitchedOffOrWhileHeIsOutThere(GameTestHelper helper) {
        LineTestPlayer p = player(helper, NpcOriginal.MIKAEL.uuid());
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(helper, DAY + 6000, p);
            BossesConfig.override("elijah_enabled", false);
            BossDirector.check(rig.server);
            helper.assertTrue(elijahOf(rig) == null, "he came although he is switched off");
            BossesConfig.override("elijah_enabled", null);
            helper.getLevel().setDayTime(DAY + 24000 + 6000);
            BossDirector.check(rig.server);
            ElijahEntity first = elijahOf(rig);
            helper.assertTrue(first != null, "control: he did not come");
            helper.getLevel().setDayTime(DAY + 48000 + 6000);
            BossDirector.check(rig.server);
            helper.assertTrue(rig.index.of(BossId.ELIJAH).appearances == 1, "a second Elijah was made");
        } finally {
            BossesConfig.override("elijah_enabled", null);
            forget(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heIsCalmAndFightsOnlyAfterAnOathWasBroken(GameTestHelper helper) {
        LineTestPlayer p = player(helper, null);
        try (BossTestRig rig = new BossTestRig(helper)) {
            ElijahEntity e = rig.spawnElijah();
            helper.assertTrue(!e.wouldAttack(p), "he picks a fight on his own");
            helper.assertTrue(!e.wouldAttack(helper.spawn(EntityType.COW, new BlockPos(5, 1, 5))), "he would attack a cow");
            rig.index.of(BossId.ELIJAH).hostile = true;
            helper.assertTrue(e.hostileForGood() && e.wouldAttack(p), "after a broken oath he does not fight");
            helper.assertTrue(!e.wouldAttack(helper.spawn(EntityType.COW, new BlockPos(6, 1, 5))), "after a broken oath he attacks a cow");
        } finally {
            forget(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void rightClickingAStandingElijahTalksAndAFallenOneDoesNot(GameTestHelper helper) {
        LineTestPlayer p = player(helper, null);
        try (BossTestRig rig = new BossTestRig(helper)) {
            ElijahEntity e = rig.spawnElijah();
            helper.assertTrue(e.mobInteract(p, net.minecraft.world.InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.CONSUME, "a right click on a standing Elijah did not talk");
            helper.assertTrue(e.mobInteract(p, net.minecraft.world.InteractionHand.OFF_HAND) == net.minecraft.world.InteractionResult.PASS, "the off hand talked too");
            e.desiccate();
            helper.assertTrue(e.mobInteract(p, net.minecraft.world.InteractionHand.MAIN_HAND) == net.minecraft.world.InteractionResult.PASS, "a fallen Elijah talked");
        } finally {
            forget(helper, p);
        }
        helper.succeed();
    }

    // ---- the deal ----

    private static final String GONE = "supernaturalbloodlines.boss.elijah.gone";
    private static final String DONE = "supernaturalbloodlines.boss.elijah.done";
    private static final String REFUSED = "supernaturalbloodlines.boss.elijah.refused";
    private static final String DECLINED = "supernaturalbloodlines.boss.elijah.declined";
    private static final List<String> ALL = List.of("mikael", "finn", "kol", "rebekah", "klaus");

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theDealGivesTheChosenVialSwearsTheOathAndIsMadeOnce(GameTestHelper helper) {
        LineTestPlayer p = player(helper, null);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(helper, DAY + 6000, p);
            ElijahEntity e = rig.spawnElijah();
            DealOfferPayload offer = Deals.talk(e, p);
            helper.assertTrue(offer != null && offer.entityId() == e.getId() && offer.days() == BossesConfig.elijahOathDays(), "no offer, or a wrong one: " + offer);
            helper.assertTrue(offer.originals().equals(ALL), "the offer lists " + offer.originals());
            Deals.choose(p, e.getId(), "kol");
            ItemStack vial = p.getInventory().items.stream().filter(s -> s.getItem() instanceof SiresBloodItem).findFirst().orElse(ItemStack.EMPTY);
            helper.assertTrue(!vial.isEmpty() && NpcOriginal.KOL.uuid().equals(SiresBloodItem.sourceOf(vial)), "the vial is not Kol's blood: " + vial);
            long today = BossClock.today(rig.server.overworld());
            helper.assertTrue(rig.index.oathOf(p.getUUID()) == DealRules.sworn(today, BossesConfig.elijahOathDays()), "the oath is " + rig.index.oathOf(p.getUUID()));
            helper.assertTrue(p.received(SWORN), "no word of the oath: " + p.heard());
            Deals.choose(p, e.getId(), "mikael");
            long vials = p.getInventory().items.stream().filter(s -> s.getItem() instanceof SiresBloodItem).count();
            helper.assertTrue(vials == 1, "a second deal gave a second vial: " + vials);
            helper.assertTrue(p.received(DONE), "the second deal was refused in silence: " + p.heard());
            helper.assertTrue(Deals.talk(e, p) == null && p.received(KEEP), "a player with an oath was offered another deal: " + p.heard());
        } finally {
            forget(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anAnswerFromAfarWithAnUnlistedIdOrToAFallenOrHostileElijahDoesNothingAndSaysSo(GameTestHelper helper) {
        LineTestPlayer p = player(helper, null);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(helper, DAY + 6000, p);
            ElijahEntity e = rig.spawnElijah();
            Deals.choose(p, e.getId(), "elijah");
            helper.assertTrue(p.received(REFUSED), "Elijah's own blood was offered, or the refusal was silent: " + p.heard());
            Deals.choose(p, e.getId(), "nobody");
            Deals.choose(p, e.getId() + 1000, "mikael");
            helper.assertTrue(p.received(GONE), "an answer to an Elijah who is not there was silent: " + p.heard());
            p.setPos(e.getX() + Deals.REACH + 4, e.getY(), e.getZ());
            Deals.choose(p, e.getId(), "mikael");
            helper.assertTrue(rig.index.oathOf(p.getUUID()) == DealRules.NEVER, "a deal was made that should not have been: " + rig.index.oathOf(p.getUUID()));
            p.setPos(e.getX() + 1, e.getY(), e.getZ());
            e.desiccate();
            Deals.choose(p, e.getId(), "mikael");
            helper.assertTrue(rig.index.oathOf(p.getUUID()) == DealRules.NEVER, "a fallen Elijah made a deal");
            e.recover();
            rig.index.of(BossId.ELIJAH).hostile = true;
            Deals.choose(p, e.getId(), "mikael");
            helper.assertTrue(rig.index.oathOf(p.getUUID()) == DealRules.NEVER && Deals.talk(e, p) == null, "a hostile Elijah made a deal");
            helper.assertTrue(p.getInventory().items.stream().noneMatch(s -> s.getItem() instanceof SiresBloodItem), "a vial was given");
        } finally {
            forget(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void decliningLeavesNoRecordSoTheOfferStands(GameTestHelper helper) {
        LineTestPlayer p = player(helper, null);
        try (BossTestRig rig = new BossTestRig(helper)) {
            ElijahEntity e = rig.spawnElijah();
            Deals.choose(p, e.getId(), Deals.NO_DEAL);
            helper.assertTrue(rig.index.oathOf(p.getUUID()) == DealRules.NEVER, "declining wrote an oath");
            helper.assertTrue(p.received(DECLINED), "declining was not answered: " + p.heard());
            helper.assertTrue(Deals.talk(e, p) != null, "after declining he did not offer again");
        } finally {
            forget(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theOfferIsTheSameForEveryoneSoItNeverGivesAwayWhichLineYouAreIn(GameTestHelper helper) {
        LineTestPlayer p = player(helper, null);
        LineTestPlayer other = player(helper, null);
        try (BossTestRig rig = new BossTestRig(helper)) {
            LineIndex.get(rig.server).setRoot(p.getUUID(), NpcOriginal.KLAUS.uuid(), true);
            ElijahEntity e = rig.spawnElijah();
            DealOfferPayload mine = Deals.talk(e, p);
            DealOfferPayload theirs = Deals.talk(e, other);
            helper.assertTrue(mine != null && theirs != null && mine.originals().equals(ALL) && theirs.originals().equals(ALL),
                    "the offer differs by line: " + (mine == null ? null : mine.originals()) + " and " + (theirs == null ? null : theirs.originals()));
            Deals.choose(p, e.getId(), "klaus");
            helper.assertTrue(p.getInventory().items.stream().anyMatch(s -> s.getItem() instanceof SiresBloodItem && NpcOriginal.KLAUS.uuid().equals(SiresBloodItem.sourceOf(s))),
                    "the player who is in Klaus's line could not choose him");
            helper.assertTrue(!p.received(REFUSED) && rig.index.oathOf(p.getUUID()) != DealRules.NEVER, "choosing their own line's Original was refused: " + p.heard());
        } finally {
            forget(helper, p, other);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void heStandsStillWhileAnOfferIsOpenAndWalksOnWhenItIsAnswered(GameTestHelper helper) {
        LineTestPlayer p = player(helper, null);
        try (BossTestRig rig = new BossTestRig(helper)) {
            ElijahEntity e = rig.spawnElijah();
            helper.assertTrue(!e.listening(), "control: he listens before anyone spoke");
            Deals.talk(e, p);
            helper.assertTrue(e.listening(), "he did not wait for the answer");
            helper.getLevel().setDayTime(helper.getLevel().getDayTime() + Deals.LISTEN_TICKS + 5);
            rig.server.overworld().setDayTime(rig.server.overworld().getDayTime());
            Deals.choose(p, e.getId(), Deals.NO_DEAL);
            helper.assertTrue(!e.listening(), "he kept waiting after the answer");
            Deals.talk(e, p);
            e.setTarget(p);
            helper.assertTrue(!e.listening(), "he stood still with something to fight");
        } finally {
            forget(helper, p);
        }
        helper.succeed();
    }
    // ---- the oath ----

    private static LineTestPlayer swornPlayer(GameTestHelper helper, BossTestRig rig) {
        LineTestPlayer p = player(helper, null);
        rig.index.setOath(p.getUUID(), DealRules.sworn(BossClock.today(rig.server.overworld()), 7));
        return p;
    }

    private static void strike(LineTestPlayer p, NpcOriginalBoss victim) {
        victim.hurt(p.damageSources().playerAttack(p), 2.0F);
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBlowToElijahKlausOrRebekahBreaksTheOathAndMakesElijahHostileForGood(GameTestHelper helper) {
        for (BossId victim : new BossId[] {BossId.ELIJAH, BossId.KLAUS, BossId.REBEKAH}) {
            LineTestPlayer p = null;
            try (BossTestRig rig = new BossTestRig(helper)) {
                p = swornPlayer(helper, rig);
                ElijahEntity elijah = rig.spawnElijah();
                NpcOriginalBoss target = switch (victim) {
                    case KLAUS -> rig.spawnKlaus();
                    case REBEKAH -> rig.spawnRebekah();
                    default -> elijah;
                };
                strike(p, target);
                helper.assertTrue(rig.index.oathOf(p.getUUID()) == DealRules.BROKEN, victim + ": the oath is " + rig.index.oathOf(p.getUUID()));
                helper.assertTrue(rig.index.of(BossId.ELIJAH).hostile && elijah.hostileForGood(), victim + ": Elijah is not hostile for good");
                helper.assertTrue(p.received(BROKEN), victim + ": the player was not told: " + p.heard());
                helper.assertTrue(elijah.getTarget() == p, victim + ": Elijah does not go for them");
                helper.assertTrue(Deals.talk(elijah, p) == null, victim + ": a hostile Elijah offered a deal");
            } finally {
                if (p != null) {
                    forget(helper, p);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBlowToMikaelDoesNotBreakTheOath(GameTestHelper helper) {
        LineTestPlayer p = null;
        try (BossTestRig rig = new BossTestRig(helper)) {
            p = swornPlayer(helper, rig);
            long oath = rig.index.oathOf(p.getUUID());
            strike(p, rig.spawn());
            helper.assertTrue(rig.index.oathOf(p.getUUID()) == oath && !rig.index.of(BossId.ELIJAH).hostile, "Mikael is the one exception, but the oath is " + rig.index.oathOf(p.getUUID()));
        } finally {
            if (p != null) {
                forget(helper, p);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anOathThatRanOutAndOnePlayerWithoutOneAreNotBrokenByABlow(GameTestHelper helper) {
        LineTestPlayer ranOut = player(helper, null);
        LineTestPlayer never = player(helper, null);
        try (BossTestRig rig = new BossTestRig(helper)) {
            rig.index.setOath(ranOut.getUUID(), BossClock.today(rig.server.overworld()) - 1);
            var klaus = rig.spawnKlaus();
            strike(ranOut, klaus);
            strike(never, klaus);
            helper.assertTrue(rig.index.oathOf(never.getUUID()) == DealRules.NEVER && !rig.index.of(BossId.ELIJAH).hostile, "somebody with no oath broke it");
            helper.assertTrue(rig.index.oathOf(ranOut.getUUID()) < BossClock.today(rig.server.overworld()) && !rig.index.of(BossId.ELIJAH).hostile, "an oath that ran out broke");
        } finally {
            forget(helper, ranOut, never);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBlowThatDoesNoDamageIsNoAttack(GameTestHelper helper) {
        LineTestPlayer p = null;
        try (BossTestRig rig = new BossTestRig(helper)) {
            p = swornPlayer(helper, rig);
            rig.spawnKlaus().hurt(p.damageSources().playerAttack(p), 0.0F);
            helper.assertTrue(rig.index.oathOf(p.getUUID()) != DealRules.BROKEN, "a blow of no damage broke the oath");
        } finally {
            if (p != null) {
                forget(helper, p);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aTamedAnimalStrikesForItsOwnerAndAWildOneDoesNot(GameTestHelper helper) {
        LineTestPlayer owner = null;
        LineTestPlayer bystander = null;
        try (BossTestRig rig = new BossTestRig(helper)) {
            owner = swornPlayer(helper, rig);
            bystander = swornPlayer(helper, rig);
            world(helper, DAY + 6000, owner, bystander);
            ElijahEntity elijah = rig.spawnElijah();
            net.ofatech.supernaturalbloodlines.parts.bosses.entity.KlausEntity klaus = rig.spawnKlaus();
            net.minecraft.world.entity.animal.Wolf wild = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.WOLF, new net.minecraft.core.BlockPos(1, 1, 1));
            klaus.hurt(wild.damageSources().mobAttack(wild), 2.0F);
            helper.assertTrue(rig.index.oathOf(owner.getUUID()) != DealRules.BROKEN && !elijah.hostileForGood(), "a wild wolf broke somebody's oath");
            net.minecraft.world.entity.animal.Wolf pet = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.WOLF, new net.minecraft.core.BlockPos(2, 1, 1));
            pet.tame(owner);
            klaus.invulnerableTime = 0;
            klaus.hurt(pet.damageSources().mobAttack(pet), 2.0F);
            helper.assertTrue(rig.index.oathOf(owner.getUUID()) == DealRules.BROKEN, "the owner of the wolf that bit Klaus kept their oath: " + rig.index.oathOf(owner.getUUID()));
            helper.assertTrue(rig.index.oathOf(bystander.getUUID()) != DealRules.BROKEN, "somebody else's oath broke with it");
            helper.assertTrue(elijah.hostileForGood() && owner.received(BROKEN), "Elijah did not turn, or the owner was not told: " + owner.heard());
        } finally {
            if (owner != null) {
                forget(helper, owner);
            }
            if (bystander != null) {
                forget(helper, bystander);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPetDefendingItsOwnerDoesNotBreakTheOath(GameTestHelper helper) {
        LineTestPlayer owner = null;
        try (BossTestRig rig = new BossTestRig(helper)) {
            owner = swornPlayer(helper, rig);
            world(helper, DAY + 6000, owner);
            ElijahEntity elijah = rig.spawnElijah();
            net.ofatech.supernaturalbloodlines.parts.bosses.entity.RebekahEntity rebekah = rig.spawnRebekah();
            net.minecraft.world.entity.animal.Wolf pet = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.WOLF, new net.minecraft.core.BlockPos(2, 1, 1));
            pet.tame(owner);
            rebekah.setTarget(owner);
            rebekah.hurt(pet.damageSources().mobAttack(pet), 2.0F);
            helper.assertTrue(rig.index.oathOf(owner.getUUID()) != DealRules.BROKEN && !elijah.hostileForGood(), "the wolf defending its owner broke the oath");
            rebekah.setTarget(null);
            rebekah.setLastHurtByMob(owner);
            rebekah.invulnerableTime = 0;
            rebekah.hurt(pet.damageSources().mobAttack(pet), 2.0F);
            helper.assertTrue(rig.index.oathOf(owner.getUUID()) != DealRules.BROKEN, "the wolf finishing what hurt its owner broke the oath");
            rebekah.setLastHurtByMob(null);
            rebekah.invulnerableTime = 0;
            rebekah.hurt(pet.damageSources().mobAttack(pet), 2.0F);
            helper.assertTrue(rig.index.oathOf(owner.getUUID()) == DealRules.BROKEN, "a wolf that attacked on its own did not break it");
            helper.assertTrue(rig.index.oathOf(owner.getUUID()) != DealRules.NEVER, "control");
        } finally {
            if (owner != null) {
                forget(helper, owner);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void yourOwnBlowBreaksTheOathEvenWhenHeWasAlreadyAfterYou(GameTestHelper helper) {
        LineTestPlayer p = null;
        try (BossTestRig rig = new BossTestRig(helper)) {
            p = swornPlayer(helper, rig);
            ElijahEntity elijah = rig.spawnElijah();
            net.ofatech.supernaturalbloodlines.parts.bosses.entity.RebekahEntity rebekah = rig.spawnRebekah();
            rebekah.setTarget(p);
            strike(p, rebekah);
            helper.assertTrue(rig.index.oathOf(p.getUUID()) == DealRules.BROKEN && elijah.hostileForGood(), "a blow of the player themselves was excused because she was after them");
        } finally {
            if (p != null) {
                forget(helper, p);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void onlyTheOneHeListensToFreesHimAndOnlyByAnswering(GameTestHelper helper) {
        LineTestPlayer a = player(helper, null);
        LineTestPlayer b = player(helper, null);
        try (BossTestRig rig = new BossTestRig(helper)) {
            world(helper, DAY + 6000, a, b);
            ElijahEntity e = rig.spawnElijah();
            Deals.talk(e, a);
            helper.assertTrue(e.listening(), "control: he is not waiting");
            Deals.choose(b, e.getId(), "nobody");
            helper.assertTrue(e.listening(), "somebody else answering freed him from the one he was waiting for");
            b.setPos(e.getX() + Deals.REACH + 4, e.getY(), e.getZ());
            Deals.choose(a, e.getId() + 1000, "kol");
            a.setPos(e.getX() + Deals.REACH + 4, e.getY(), e.getZ());
            Deals.choose(a, e.getId(), Deals.NO_DEAL);
            helper.assertTrue(e.listening(), "an answer from out of reach freed him");
            a.setPos(e.getX() + 1, e.getY(), e.getZ());
            Deals.choose(a, e.getId(), "nobody");
            helper.assertTrue(!e.listening(), "the one he waits for gave a bad answer and he is still waiting");
        } finally {
            forget(helper, a, b);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aBlowFromAMobNeverBreaksAnOath(GameTestHelper helper) {
        LineTestPlayer p = null;
        try (BossTestRig rig = new BossTestRig(helper)) {
            p = swornPlayer(helper, rig);
            var klaus = rig.spawnKlaus();
            klaus.hurt(helper.getLevel().damageSources().mobAttack(helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 1, 5))), 2.0F);
            helper.assertTrue(rig.index.oathOf(p.getUUID()) != DealRules.BROKEN && !rig.index.of(BossId.ELIJAH).hostile, "a zombie broke somebody's oath");
        } finally {
            if (p != null) {
                forget(helper, p);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void forgettingElijahClearsTheOathsAndTheHostility(GameTestHelper helper) {
        LineTestPlayer p = null;
        try (BossTestRig rig = new BossTestRig(helper)) {
            p = swornPlayer(helper, rig);
            rig.index.of(BossId.ELIJAH).hostile = true;
            rig.index.forget(BossId.ELIJAH);
            helper.assertTrue(rig.index.oathOf(p.getUUID()) == DealRules.NEVER && !rig.index.of(BossId.ELIJAH).hostile, "the reset kept an oath or the anger");
        } finally {
            if (p != null) {
                forget(helper, p);
            }
        }
        helper.succeed();
    }

    // ---- line safety ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWhiteOakStakeInTheHandOfElijahOrRebekahIsNeverAWhiteOakKill(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            for (net.minecraft.world.entity.LivingEntity mob : List.of(rig.spawnElijah(), rig.spawnRebekah())) {
                mob.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.ofatech.supernaturalbloodlines.parts.species.item.WhiteOak.STAKE.get()));
                helper.assertTrue(!OriginalRules.isWhiteOakKill(mob.damageSources().mobAttack(mob)), mob.getType() + " with a stake started a line death");
            }
        }
        helper.succeed();
    }

    // ---- the reward ----

    /** Own batch: other tests pin {@code boss_reward_on} to "death" for a while, and the config is global, so a test that needs the default must not share a batch with them. */
    @GameTest(template = "empty", templateNamespace = NS, batch = "bossRewardDefault")
    public static void elijahPaysTheElixirOnceWhenHeIsBroughtDown(GameTestHelper helper) {
        try (BossTestRig rig = new BossTestRig(helper)) {
            ElijahEntity e = rig.spawnElijah();
            e.desiccate();
            helper.assertTrue(rig.dropsNear(e, BossItems.THE_ELIXIR.get()) == 1, "no Elixir, or more than one: " + rig.dropsNear(e, BossItems.THE_ELIXIR.get()));
            e.recover();
            rig.helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, e.getBoundingBox().inflate(8)).forEach(net.minecraft.world.entity.Entity::discard);
            e.desiccate();
            helper.assertTrue(rig.dropsNear(e, BossItems.THE_ELIXIR.get()) == 0, "a second Elixir was paid");
        }
        helper.succeed();
    }

    // ---- the operator ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theCommandSpawnsElijahAndRebekahAndShowsTheirState(GameTestHelper helper) {
        BossTestRig.fixedGround();
        LineTestPlayer op = LineTestPlayer.create(helper);
        op.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 1, 2.5)));
        try (BossTestRig rig = new BossTestRig(helper)) {
            var source = op.createCommandSourceStack().withPermission(2);
            var commands = rig.server.getCommands();
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss elijah spawn");
            ElijahEntity e = elijahOf(rig);
            helper.assertTrue(e != null && e.isAlive(), "the spawn command made no Elijah");
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss elijah desiccate");
            helper.assertTrue(e.isDesiccated(), "the desiccate command did nothing");
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss elijah reset");
            helper.assertTrue(!e.isAlive() && rig.index.state(BossId.ELIJAH) == BossState.UNSEEN, "the reset left him or his record");
            commands.performPrefixedCommand(source, "supernaturalbloodlines boss rebekah spawn");
            helper.assertTrue(BossDirector.bossEntity(rig.server, BossId.REBEKAH) != null, "the spawn command made no Rebekah");
        } finally {
            BossTestRig.fixedGround();
        }
        helper.succeed();
    }
}