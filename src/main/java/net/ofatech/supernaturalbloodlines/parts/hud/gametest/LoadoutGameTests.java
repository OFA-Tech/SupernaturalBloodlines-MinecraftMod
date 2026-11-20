package net.ofatech.supernaturalbloodlines.parts.hud.gametest;

import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.hud.Loadout;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.hud.action.LoadoutService;
import net.ofatech.supernaturalbloodlines.parts.hud.net.HudNet;
import net.ofatech.supernaturalbloodlines.parts.hud.net.SetLoadoutPayload;

/** The loadout attachment and what the server lets a player put in it. These run without Vampirism too (Bite is always assignable). */
@PrefixGameTestTemplate(false)
public final class LoadoutGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private LoadoutGameTests() {
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aPlainHumanHasNoBiteAndNothingIsOfferedToThem(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.createMortal(helper);
        LoadoutService.offerBite(p);
        Loadout l = LoadoutService.get(p);
        helper.assertTrue(l.slots().stream().allMatch(Optional::isEmpty), "a human has something in a slot: " + l);
        helper.assertTrue(!l.biteOffered(), "Bite was spent on a human");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void biteCanMoveAndClear(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(LoadoutService.set(p, 0, Loadout.BITE) == null, "Bite into slot 1 was refused");
        helper.assertTrue(LoadoutService.set(p, 3, Loadout.BITE) == null, "Bite into slot 4 was refused");
        helper.assertTrue(LoadoutService.set(p, 0, null) == null, "clearing slot 1 was refused");
        Loadout l = LoadoutService.get(p);
        helper.assertTrue(l.get(3).equals(Optional.of(Loadout.BITE)) && l.get(0).isEmpty(), "Bite did not move: " + l);
        helper.assertTrue(LoadoutService.set(p, 3, null) == null, "clearing slot 4 was refused");
        helper.assertTrue(LoadoutService.get(p).slots().stream().allMatch(Optional::isEmpty), "a cleared loadout was put back: " + LoadoutService.get(p));
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aClearedLoadoutIsNotPutBackAfterTheSaveRoundTrip(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        LoadoutService.set(p, 0, Loadout.BITE);
        LoadoutService.set(p, 0, null);
        net.minecraft.nbt.Tag saved = Loadout.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, LoadoutService.get(p)).getOrThrow();
        Loadout back = Loadout.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, saved).getOrThrow();
        helper.assertTrue(back.slots().stream().allMatch(Optional::isEmpty), "Bite came back after a save: " + back);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void badSlotsAndBadIdsAreRefused(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        helper.assertTrue(LoadoutService.set(p, 6, Loadout.BITE) != null, "slot 7 was accepted");
        helper.assertTrue(LoadoutService.set(p, -1, Loadout.BITE) != null, "slot -1 was accepted");
        HudNet.forgetRateLimit(p);
        String bad = HudNet.handleSetLoadout(p, new SetLoadoutPayload(1, "NOT AN ID!"));
        helper.assertTrue(bad != null && bad.endsWith("unknown_action"), "a malformed id: " + bad);
        helper.assertTrue(LoadoutService.get(p).get(1).isEmpty(), "a refused set changed the loadout");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void loadoutSurvivesDeath(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        LoadoutService.set(p, 4, Loadout.BITE);
        ServerPlayer clone = TestPlayers.create(helper);
        clone.restoreFrom(p, false); // what a respawn does
        Loadout l = clone.getData(ModAttachments.HUD_LOADOUT);
        helper.assertTrue(l.get(0).isEmpty() && l.get(4).equals(Optional.of(Loadout.BITE)), "a death changed the loadout: " + l);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void payloadIdAndSlotAreOnlyNames(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        HudNet.forgetRateLimit(p);
        helper.assertTrue(HudNet.handleSetLoadout(p, new SetLoadoutPayload(2, Loadout.BITE.toString())) == null, "Bite by payload was refused");
        helper.assertTrue(LoadoutService.get(p).get(2).equals(Optional.of(Loadout.BITE)), "the payload did not set the slot");
        helper.assertTrue(HudNet.handleSetLoadout(p, new SetLoadoutPayload(2, "")) == null, "an empty id did not clear");
        helper.assertTrue(LoadoutService.get(p).get(2).isEmpty(), "the slot was not cleared");
        helper.succeed();
    }

    /** The rate limit counts every payload, a refused one as much as one that worked. */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void everyLoadoutPayloadCountsAgainstTheRateLimit(GameTestHelper helper) {
        ServerPlayer p = TestPlayers.create(helper);
        HudNet.forgetRateLimit(p);
        int limit = net.ofatech.supernaturalbloodlines.parts.hud.HudConfig.rateLimitPerSecond();
        for (int i = 0; i < limit; i++) {
            String r = HudNet.handleSetLoadout(p, new SetLoadoutPayload(9, "")); // a refused one: slot 10 does not exist
            helper.assertTrue(r != null && r.endsWith("bad_slot"), "payload " + i + ": " + r);
        }
        String over = HudNet.handleSetLoadout(p, new SetLoadoutPayload(1, Loadout.BITE.toString()));
        helper.assertTrue(HudNet.RATE_LIMITED.equals(over), "the payload over the limit was not dropped: " + over);
        helper.assertTrue(LoadoutService.get(p).get(1).isEmpty(), "a dropped payload changed the loadout");
        helper.succeed();
    }

    static ResourceLocation id(String s) {
        return ResourceLocation.parse(s);
    }
}
