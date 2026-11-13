package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.player.actions.IAction;
import de.teamlapen.vampirism.api.event.ActionEvent;
import de.teamlapen.werewolves.api.WReference;
import de.teamlapen.werewolves.core.ModActions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackBonus;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.RoarEffectPayload;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.parts.species.witch.RiteCost;
import net.ofatech.supernaturalbloodlines.parts.species.witch.WitchAltarBonus;
import net.ofatech.supernaturalbloodlines.shared.werewolves.AnchorItems;
import net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift;
import net.ofatech.supernaturalbloodlines.shared.werewolves.MoonControl;
import net.ofatech.supernaturalbloodlines.shared.werewolves.MoonControl.Control;

/** Plan 9, task 4: the pack bonus, the Alpha Roar, the Emissary, anchors and moon control (R94, R96). Werewolves only. */
@PrefixGameTestTemplate(false)
public final class PackBonusGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private PackBonusGameTests() {
    }

    private static ServerPlayer wolf(GameTestHelper helper, Rank rank, Lineage lineage, UUID packAlpha, int level, double x) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        p.setPos(helper.absoluteVec(new Vec3(x, 1, 1.5)));
        WolfState w = WolfState.NONE.toBuilder().lineage(lineage).rank(rank).geneRolled(true).packAlpha(packAlpha)
                .betaSince(rank == Rank.BETA ? 1 : 0).build();
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().wolf(w).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, level), "the test wolf could not join the werewolves");
        return p;
    }

    private static ServerPlayer beta(GameTestHelper helper, ServerPlayer alpha, Lineage lineage, double x) {
        return wolf(helper, Rank.BETA, lineage, alpha.getUUID(), 1, x);
    }

    private static void online(ServerPlayer... players) {
        Map<UUID, ServerPlayer> map = new HashMap<>();
        for (ServerPlayer p : players) {
            map.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(map::get);
        AlphaRoar.overridePlayers(s -> List.of(players));
    }

    private static void clean(ServerPlayer... players) {
        SireCall.overridePlayerLookup(null);
        AlphaRoar.overridePlayers(null);
        AlphaRoar.reset();
        RoarEffectPayload.overrideSpy(null);
        MoonControl.overrideMoonBound(null);
        for (ServerPlayer p : players) {
            VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            BloodlineAccess.set(p, BloodlineState.NONE);
            PackBonus.tick(p);
            p.removeAllEffects();
            p.removeData(SpeciesRegistries.PENDING_ALPHA);
            p.removeData(SpeciesRegistries.PENDING_WILD);
        }
    }

    private static double bonus(ServerPlayer p) {
        AttributeInstance a = p.getAttribute(Attributes.ATTACK_DAMAGE);
        var m = a == null ? null : a.getModifier(PackBonus.MODIFIER_ID);
        return m == null ? 0 : m.amount();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean formCancelled(ServerPlayer p, IAction<?> action, boolean insideOwnTick) {
        if (insideOwnTick) {
            ForcedShift.enterTick(p);
        }
        try {
            var event = new ActionEvent.ActionActivatedEvent(VampirismAPI.factionPlayerHandler(p).getCurrentFactionPlayer().orElseThrow(), (IAction) action, 0, 0);
            NeoForge.EVENT_BUS.post(event);
            return event.isCanceled();
        } finally {
            ForcedShift.leaveTick(p);
        }
    }

    private static boolean beast(ServerPlayer p, boolean forced) {
        return formCancelled(p, ModActions.BEAST_FORM.get(), forced);
    }

    private static boolean half(ServerPlayer p) {
        return formCancelled(p, ModActions.SURVIVAL_FORM.get(), false) || formCancelled(p, ModActions.HUMAN_FORM.get(), false);
    }

    // ---- the pack bonus ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bonusNeedsThreeBetasNear(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, Lineage.BITTEN, null, 1, 1.5);
        ServerPlayer b1 = beta(helper, alpha, Lineage.BITTEN, 3.5);
        ServerPlayer b2 = beta(helper, alpha, Lineage.BITTEN, 4.5);
        ServerPlayer born = beta(helper, alpha, Lineage.BORN, 5.5);
        ServerPlayer b4 = beta(helper, alpha, Lineage.BITTEN, 6.5);
        online(alpha, b1, b2, born, b4);
        try {
            // two Betas, even bitten ones (4 doubled): no bonus
            born.setPos(helper.absoluteVec(new Vec3(400, 1, 1.5)));
            b4.setPos(helper.absoluteVec(new Vec3(400, 1, 1.5)));
            PackBonus.tick(alpha);
            helper.assertTrue(bonus(alpha) == 0, "two Betas gave a bonus: " + bonus(alpha));
            helper.assertFalse(b1.hasEffect(MobEffects.REGENERATION), "two Betas got Regeneration");

            // three near (a born one counts once): 2 + 2 + 1 = 5 doubled
            born.setPos(helper.absoluteVec(new Vec3(5.5, 1, 1.5)));
            PackBonus.tick(alpha);
            helper.assertTrue(Math.abs(bonus(alpha) - 0.25) < 1e-9, "three Betas gave " + bonus(alpha));
            helper.assertTrue(b1.hasEffect(MobEffects.REGENERATION) && born.hasEffect(MobEffects.REGENERATION), "a near Beta got no Regeneration");
            helper.assertFalse(b4.hasEffect(MobEffects.REGENERATION), "a Beta 400 blocks away got Regeneration");
            MobEffectInstance regen = b1.getEffect(MobEffects.REGENERATION);
            helper.assertTrue(regen.getAmplifier() == 0 && regen.getDuration() <= PackBonus.REGEN_TICKS, "regeneration is " + regen);

            // a fourth does not raise it past +25%; a repeat does not stack the modifier
            b4.setPos(helper.absoluteVec(new Vec3(6.5, 1, 1.5)));
            PackBonus.tick(alpha);
            PackBonus.tick(alpha);
            helper.assertTrue(Math.abs(bonus(alpha) - 0.25) < 1e-9, "four Betas gave " + bonus(alpha));

            // born Betas count once each: 3 of them are +15%
            for (ServerPlayer b : List.of(b1, b2, b4)) {
                BloodlineAccess.set(b, BloodlineAccess.get(b).toBuilder().wolf(stateOf(b).toBuilder().lineage(Lineage.BORN).build()).build());
            }
            b4.setPos(helper.absoluteVec(new Vec3(400, 1, 1.5)));
            born.setPos(helper.absoluteVec(new Vec3(400, 1, 1.5)));
            PackBonus.tick(alpha);
            helper.assertTrue(bonus(alpha) == 0, "two Betas left and the bonus stayed: " + bonus(alpha));
            born.setPos(helper.absoluteVec(new Vec3(5.5, 1, 1.5)));
            PackBonus.tick(alpha);
            helper.assertTrue(Math.abs(bonus(alpha) - 0.15) < 1e-9, "three born Betas gave " + bonus(alpha));
        } finally {
            clean(alpha, b1, b2, born, b4);
        }
        helper.succeed();
    }

    private static WolfState stateOf(ServerPlayer p) {
        return BloodlineAccess.get(p).wolf();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bonusNeedsAlphaOnlineAndNear(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, Lineage.BITTEN, null, 1, 1.5);
        List<ServerPlayer> betas = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            betas.add(beta(helper, alpha, Lineage.BITTEN, 3.5 + i));
        }
        ServerPlayer[] everyone = new ServerPlayer[4];
        everyone[0] = alpha;
        for (int i = 0; i < 3; i++) {
            everyone[i + 1] = betas.get(i);
        }
        online(everyone);
        try {
            PackBonus.tick(alpha);
            helper.assertTrue(bonus(alpha) > 0, "no bonus with the Alpha and three Betas near");

            // the Alpha walks away: the bonus goes, the pack stays
            alpha.setPos(helper.absoluteVec(new Vec3(300, 1, 1.5)));
            PackBonus.tick(alpha);
            helper.assertTrue(bonus(alpha) == 0, "the Alpha far away kept the bonus: " + bonus(alpha));
            alpha.setPos(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));

            // the Alpha is offline (not in the lookup): nothing to tick, a Beta's own tick gives nothing
            online(betas.toArray(new ServerPlayer[0]));
            for (ServerPlayer b : betas) {
                b.removeAllEffects();
                PackBonus.tick(b);
                helper.assertFalse(b.hasEffect(MobEffects.REGENERATION), "a Beta got Regeneration with the Alpha offline");
                helper.assertTrue(bonus(b) == 0, "a Beta got the Alpha's bonus");
            }
            PackIndex index = PackIndex.get(helper.getLevel());
            helper.assertTrue(index.betasOf(alpha.getUUID()).size() == 3, "the pack did not survive its Alpha being offline");
            helper.assertTrue(alpha.getUUID().equals(stateOf(betas.get(0)).packAlpha()), "a Beta lost the pack");

            // back online, the bonus is back
            online(everyone);
            PackBonus.tick(alpha);
            helper.assertTrue(bonus(alpha) > 0, "no bonus after the Alpha came back");

            // an Alpha that lost the spark has no bonus
            BloodlineAccess.set(alpha, BloodlineAccess.get(alpha).toBuilder().wolf(stateOf(alpha).toBuilder().rank(Rank.OMEGA).build()).build());
            PackBonus.tick(alpha);
            helper.assertTrue(bonus(alpha) == 0, "an Omega kept the pack bonus");
        } finally {
            clean(everyone);
        }
        helper.succeed();
    }

    // ---- the roar ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void roarBuffsPackAndSubmitsOthers(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, Lineage.BITTEN, null, 1, 1.5);
        ServerPlayer near = beta(helper, alpha, Lineage.BITTEN, 11.5);
        ServerPlayer far = beta(helper, alpha, Lineage.BITTEN, 60.5);
        ServerPlayer stranger = wolf(helper, Rank.OMEGA, Lineage.BITTEN, null, 1, 9.5);
        ServerPlayer distantStranger = wolf(helper, Rank.OMEGA, Lineage.BITTEN, null, 1, 40.5);
        ServerPlayer human = TestPlayers.createDuelist(helper);
        human.setPos(helper.absoluteVec(new Vec3(5.5, 1, 1.5)));
        online(alpha, near, far, stranger, distantStranger, human);
        List<RoarEffectPayload> sent = new ArrayList<>();
        RoarEffectPayload.overrideSpy(sent::add);
        try {
            helper.assertTrue(beast(near, false), "control: a pack Beta shifted at will before the roar");
            helper.assertTrue(AlphaRoar.roar(near) != null, "a Beta roared");
            helper.assertTrue(!near.hasEffect(MobEffects.DAMAGE_BOOST) && sent.isEmpty(), "a refused roar had effects");

            Component error = AlphaRoar.roar(alpha);
            helper.assertTrue(error == null, "the Alpha's roar was refused: " + error);
            helper.assertTrue(sent.size() == 1 && sent.get(0).playerId() == alpha.getId(), "the eye glow was not sent: " + sent);

            helper.assertTrue(near.hasEffect(MobEffects.DAMAGE_BOOST) && near.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() == 0, "no Strength I for the Beta");
            helper.assertTrue(near.hasEffect(MobEffects.MOVEMENT_SPEED) && near.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() == 0, "no Speed I for the Beta");
            helper.assertTrue(near.getEffect(MobEffects.DAMAGE_BOOST).getDuration() > 500, "the buff is not 30 s: " + near.getEffect(MobEffects.DAMAGE_BOOST).getDuration());
            helper.assertFalse(far.hasEffect(MobEffects.DAMAGE_BOOST), "a Beta 60 blocks away was buffed");
            helper.assertFalse(alpha.hasEffect(MobEffects.DAMAGE_BOOST), "the Alpha buffed themself");
            helper.assertFalse(beast(near, false), "the buffed Beta still could not shift");
            helper.assertTrue(beast(far, false), "a Beta out of range got the shift permit");

            MobEffectInstance slow = stranger.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            helper.assertTrue(slow != null && slow.getAmplifier() == 1 && slow.getDuration() <= AlphaRoar.SUBMIT_TICKS, "the stranger is not slowed: " + slow);
            helper.assertFalse(distantStranger.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "a werewolf 40 blocks away was slowed");
            helper.assertFalse(human.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "a human was slowed");
            helper.assertFalse(near.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "a pack member was slowed");
        } finally {
            clean(alpha, near, far, stranger, distantStranger, human);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void roarCooldown(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, Lineage.BITTEN, null, 1, 1.5);
        ServerPlayer other = wolf(helper, Rank.ALPHA, Lineage.BITTEN, null, 1, 3.5);
        online(alpha, other);
        try {
            helper.assertTrue(AlphaRoar.roar(alpha, 5000) == null, "the first roar was refused");
            helper.assertTrue(AlphaRoar.roar(alpha, 5000 + AlphaRoar.COOLDOWN - 1) != null, "the cooldown ended a tick early");
            helper.assertTrue(AlphaRoar.roar(other, 5001) == null, "another Alpha's cooldown was shared");
            helper.assertTrue(AlphaRoar.roar(alpha, 5000 + AlphaRoar.COOLDOWN) == null, "the cooldown did not end");
            helper.assertTrue(AlphaRoar.roar(alpha, 5000 + AlphaRoar.COOLDOWN + 1) != null, "no cooldown after the second roar");
            // a True Alpha roars as well; an Omega, a Beta and a human do not
            ServerPlayer truth = wolf(helper, Rank.TRUE_ALPHA, Lineage.BITTEN, null, 1, 5.5);
            ServerPlayer omega = wolf(helper, Rank.OMEGA, Lineage.BITTEN, null, 1, 7.5);
            ServerPlayer human = TestPlayers.createDuelist(helper);
            try {
                helper.assertTrue(AlphaRoar.roar(truth, 10) == null, "a True Alpha could not roar");
                helper.assertTrue(AlphaRoar.roar(omega, 10) != null, "an Omega roared");
                helper.assertTrue(AlphaRoar.roar(human, 10) != null, "a human roared");
            } finally {
                clean(truth, omega, human);
            }
        } finally {
            clean(alpha, other);
        }
        helper.succeed();
    }

    // ---- the emissary ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void emissaryCheapensRites(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, Lineage.BITTEN, null, 1, 1.5);
        ServerPlayer beta = beta(helper, alpha, Lineage.BITTEN, 10.5);
        ServerPlayer witch = TestPlayers.createDuelist(helper);
        witch.setPos(helper.absoluteVec(new Vec3(5.5, 1, 1.5)));
        BloodlineAccess.set(witch, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());
        ServerPlayer other = TestPlayers.createDuelist(helper);
        other.setPos(helper.absoluteVec(new Vec3(5.5, 1, 1.5)));
        online(alpha, beta, witch, other);
        PackIndex index = PackIndex.get(helper.getLevel());
        try {
            helper.assertFalse(WitchAltarBonus.emissaryApplies(helper.getLevel(), witch.getUUID()), "control: a witch nobody named paid less");
            index.setEmissary(alpha.getUUID(), witch.getUUID());
            helper.assertTrue(WitchAltarBonus.emissaryApplies(helper.getLevel(), witch.getUUID()), "the Emissary near the pack paid full price");
            helper.assertFalse(WitchAltarBonus.emissaryApplies(helper.getLevel(), other.getUUID()), "someone else got the Emissary's discount");

            // x0.9 on top of the witch x0.75, rounded up, never free
            helper.assertTrue(RiteCost.split(1000, true, true, 0).altarCost() == 675, "witch Emissary: " + RiteCost.split(1000, true, true, 0));
            helper.assertTrue(RiteCost.split(1000, false, true, 0).altarCost() == 900, "plain Emissary: " + RiteCost.split(1000, false, true, 0));
            helper.assertTrue(RiteCost.split(1000, true, false, 0).altarCost() == 750, "the witch discount changed");
            helper.assertTrue(RiteCost.split(1, false, true, 0).altarCost() == 1 && RiteCost.split(0, false, true, 0).altarCost() == 0, "rounding");

            // away from every pack member the discount is gone (the Alpha 5 blocks away is out of the 16 too)
            witch.setPos(helper.absoluteVec(new Vec3(200, 1, 1.5)));
            helper.assertFalse(WitchAltarBonus.emissaryApplies(helper.getLevel(), witch.getUUID()), "the Emissary far from the pack paid less");
            witch.setPos(helper.absoluteVec(new Vec3(20.5, 1, 1.5)));
            helper.assertTrue(WitchAltarBonus.emissaryApplies(helper.getLevel(), witch.getUUID()) == (witch.distanceToSqr(beta) <= 256), "the 16 block range is off");
            witch.setPos(helper.absoluteVec(new Vec3(5.5, 1, 1.5)));

            // the Alpha drops the Emissary
            index.setEmissary(alpha.getUUID(), null);
            helper.assertFalse(WitchAltarBonus.emissaryApplies(helper.getLevel(), witch.getUUID()), "a dismissed Emissary paid less");
        } finally {
            index.setEmissary(alpha.getUUID(), null);
            clean(alpha, beta, witch, other);
        }
        helper.succeed();
    }

    // ---- anchors ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void anchorHalvesForcedShift(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.OMEGA, Lineage.BITTEN, null, 1, 1.5);
        ServerPlayer anchor = TestPlayers.createDuelist(helper);
        anchor.setPos(helper.absoluteVec(new Vec3(6.5, 1, 1.5)));
        online(p, anchor);
        PackIndex index = PackIndex.get(helper.getLevel());
        Predicate<ServerPlayer> previous = AnchorItems.setWornLookup(x -> false);
        long before = helper.getLevel().getDayTime();
        try {
            int sparedWithout = 0;
            int sparedWithPlayer = 0;
            int sparedWithItem = 0;
            int nights = 60;
            AnchorItems.setWornLookup(x -> false);
            for (int pass = 0; pass < 3; pass++) {
                if (pass == 1) {
                    index.setAnchor(p.getUUID(), anchor.getUUID());
                } else if (pass == 2) {
                    index.setAnchor(p.getUUID(), null);
                    AnchorItems.setWornLookup(x -> x == p);
                }
                for (long day = 100; day < 100 + nights; day++) {
                    helper.getLevel().setDayTime(day * 24000 + 15000);
                    boolean spared = beast(p, true);
                    boolean expected = pass != 0 && MoonControl.bornSparedOn(p.getUUID(), day);
                    helper.assertTrue(spared == expected, "pass " + pass + ", night " + day + ": spared=" + spared + ", expected " + expected);
                    sparedWithout += pass == 0 && spared ? 1 : 0;
                    sparedWithPlayer += pass == 1 && spared ? 1 : 0;
                    sparedWithItem += pass == 2 && spared ? 1 : 0;
                }
            }
            helper.assertTrue(sparedWithout == 0, "a wolf with no anchor was spared " + sparedWithout + " nights");
            helper.assertTrue(sparedWithPlayer > 0 && sparedWithPlayer < nights, "the anchor player spared " + sparedWithPlayer + " of " + nights);
            helper.assertTrue(sparedWithItem > 0 && sparedWithItem < nights, "the anchor item spared " + sparedWithItem + " of " + nights);

            // the anchor player is 16 blocks away at most
            index.setAnchor(p.getUUID(), anchor.getUUID());
            AnchorItems.setWornLookup(x -> false);
            anchor.setPos(helper.absoluteVec(new Vec3(40.5, 1, 1.5)));
            for (long day = 100; day < 100 + nights; day++) {
                helper.getLevel().setDayTime(day * 24000 + 15000);
                helper.assertFalse(beast(p, true), "a far anchor spared night " + day);
            }
            // an anchor does not give control by itself to a bitten wolf
            anchor.setPos(helper.absoluteVec(new Vec3(6.5, 1, 1.5)));
            helper.assertTrue(MoonControl.of(p) == Control.NONE, "an anchor gave a bitten wolf control: " + MoonControl.of(p));
        } finally {
            AnchorItems.setWornLookup(previous);
            index.setAnchor(p.getUUID(), null);
            helper.getLevel().setDayTime(before);
            clean(p, anchor);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void bornWithAnchorHalfOnly(GameTestHelper helper) {
        ServerPlayer born = wolf(helper, Rank.OMEGA, Lineage.BORN, null, 8, 1.5);
        ServerPlayer bitten = wolf(helper, Rank.OMEGA, Lineage.BITTEN, null, 8, 3.5);
        online(born, bitten);
        Predicate<ServerPlayer> previous = AnchorItems.setWornLookup(x -> false);
        long before = helper.getLevel().getDayTime();
        try {
            helper.assertTrue(MoonControl.of(born) == Control.NONE, "control: a born wolf without an anchor has control");
            AnchorItems.setWornLookup(x -> true);
            helper.assertTrue(MoonControl.of(born) == Control.HALF, "a born wolf with an anchor has " + MoonControl.of(born) + " at level 8");
            helper.assertTrue(MoonControl.of(bitten) == Control.NONE, "a bitten wolf with an anchor has " + MoonControl.of(bitten));
            helper.assertFalse(half(born), "the anchored born wolf could not take a half form");
            helper.assertTrue(beast(born, false), "the anchored born wolf shifted into the beast at will");
            // control also spares the forced shift on every night, not on half of them
            for (long day = 100; day < 130; day++) {
                helper.getLevel().setDayTime(day * 24000 + 15000);
                helper.assertTrue(beast(born, true), "the anchored born wolf was forced to shift on night " + day);
            }
        } finally {
            AnchorItems.setWornLookup(previous);
            helper.getLevel().setDayTime(before);
            clean(born, bitten);
        }
        helper.succeed();
    }

    // ---- moon control ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void moonBoundWolfCannotShiftAtWill(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.OMEGA, Lineage.BITTEN, null, 8, 1.5);
        online(p);
        try {
            helper.assertTrue(MoonControl.of(p) == Control.NONE, "a plain wolf has control");
            helper.assertTrue(beast(p, false), "the wolf shifted into the beast at will");
            helper.assertTrue(formCancelled(p, ModActions.SURVIVAL_FORM.get(), false), "the wolf took the survivalist form at will");
            helper.assertTrue(formCancelled(p, ModActions.HUMAN_FORM.get(), false), "the wolf took the human form at will");
            helper.assertFalse(beast(p, true), "the full moon's own shift was cancelled");
            MoonControl.overrideMoonBound(false);
            helper.assertFalse(beast(p, false), "with moon_bound_werewolves off the wolf still could not shift");
        } finally {
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void alphaShiftsAtWill(GameTestHelper helper) {
        ServerPlayer low = wolf(helper, Rank.ALPHA, Lineage.BITTEN, null, 1, 1.5);
        ServerPlayer high = wolf(helper, Rank.ALPHA, Lineage.BITTEN, null, 8, 3.5);
        ServerPlayer truth = wolf(helper, Rank.TRUE_ALPHA, Lineage.BITTEN, null, 8, 5.5);
        online(low, high, truth);
        try {
            helper.assertTrue(MoonControl.of(low) == Control.HALF, "a level 1 Alpha has " + MoonControl.of(low));
            helper.assertFalse(half(low), "a level 1 Alpha could not take a half form");
            helper.assertTrue(beast(low, false), "a level 1 Alpha shifted into the beast at will");
            helper.assertTrue(MoonControl.of(high) == Control.FULL, "a level 8 Alpha has " + MoonControl.of(high));
            helper.assertFalse(beast(high, false), "a level 8 Alpha could not take the beast form");
            helper.assertTrue(MoonControl.of(truth) == Control.FULL, "a True Alpha has " + MoonControl.of(truth));
            helper.assertFalse(beast(truth, false), "a True Alpha could not take the beast form");
            helper.assertTrue(beast(low, true), "the forced shift reached an Alpha");
        } finally {
            clean(low, high, truth);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void unboundHybridShiftsAtWill(GameTestHelper helper) {
        ServerPlayer p = wolf(helper, Rank.NONE, Lineage.NONE, null, 8, 1.5);
        try {
            BloodlineAccess.set(p, BloodlineState.NONE.toBuilder().species(Species.HYBRID).hybridCurseBound(true).build());
            helper.assertTrue(beast(p, false), "a bound hybrid shifted");
            BloodlineAccess.set(p, BloodlineAccess.get(p).toBuilder().hybridCurseBound(false).build());
            helper.assertTrue(MoonControl.of(p) == Control.FULL, "an unbound hybrid has " + MoonControl.of(p));
            helper.assertFalse(beast(p, false), "an unbound hybrid could not shift at will");
            helper.assertFalse(half(p), "an unbound hybrid could not take a half form");
        } finally {
            clean(p);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void controlSkipsForcedShift(GameTestHelper helper) {
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, Lineage.BITTEN, null, 1, 1.5);
        ServerPlayer plain = wolf(helper, Rank.OMEGA, Lineage.BITTEN, null, 1, 3.5);
        ServerPlayer hybrid = wolf(helper, Rank.NONE, Lineage.NONE, null, 1, 5.5);
        BloodlineAccess.set(hybrid, BloodlineState.NONE.toBuilder().species(Species.HYBRID).build());
        online(alpha, plain, hybrid);
        try {
            helper.assertTrue(beast(alpha, true), "the forced shift reached an Alpha");
            helper.assertTrue(beast(hybrid, true), "the forced shift reached an unbound hybrid");
            helper.assertFalse(beast(plain, true), "the forced shift was cancelled for a wolf without control");
        } finally {
            clean(alpha, plain, hybrid);
        }
        helper.succeed();
    }
}
