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
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Lineage;
import net.ofatech.supernaturalbloodlines.core.bloodline.WolfState.Rank;
import net.ofatech.supernaturalbloodlines.core.network.EclipsePayload;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.item.ShockedEffect;
import net.ofatech.supernaturalbloodlines.parts.species.item.Taser;
import net.ofatech.supernaturalbloodlines.parts.species.pack.AlphaRoar;
import net.ofatech.supernaturalbloodlines.parts.species.pack.Electricity;
import net.ofatech.supernaturalbloodlines.parts.species.pack.LunarEclipse;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackBonus;
import net.ofatech.supernaturalbloodlines.parts.species.pack.RankEffects;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireCall;
import net.ofatech.supernaturalbloodlines.shared.werewolves.ForcedShift;

/** Plan 9, task 5: the lunar eclipse and electricity (Shocked, the Taser). Werewolves only. */
@PrefixGameTestTemplate(false)
public final class EclipseElectricityGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final long FULL_MOON_DAY = 8; // day % 8 == 0
    private static final long NIGHT = 15000;

    private EclipseElectricityGameTests() {
    }

    private static ServerPlayer wolf(GameTestHelper helper, Rank rank, int level, double x) {
        ServerPlayer p = TestPlayers.createDuelist(helper);
        p.setPos(helper.absoluteVec(new Vec3(x, 1, 1.5)));
        BloodlineAccess.set(p, BloodlineState.NONE.toBuilder()
                .wolf(WolfState.NONE.toBuilder().lineage(Lineage.BITTEN).rank(rank).geneRolled(true).build()).build());
        helper.assertTrue(VampirismAPI.factionPlayerHandler(p).setFactionAndLevel(WReference.WEREWOLF_FACTION, level), "the test wolf could not join the werewolves");
        return p;
    }

    private static void clean(ServerPlayer... players) {
        for (ServerPlayer p : players) {
            VampirismAPI.factionPlayerHandler(p).leaveFaction(false);
            BloodlineAccess.set(p, BloodlineState.NONE);
            p.removeAllEffects();
            RankEffects.sync(p);
            PackBonus.tick(p);
        }
    }

    private static void online(ServerPlayer... players) {
        Map<UUID, ServerPlayer> map = new HashMap<>();
        for (ServerPlayer p : players) {
            map.put(p.getUUID(), p);
        }
        SireCall.overridePlayerLookup(map::get);
        AlphaRoar.overridePlayers(s -> List.of(players));
        LunarEclipse.overridePlayers(s -> List.of(players));
    }

    private static void reset(MinecraftServer server, long dayTime) {
        LunarEclipse.stop(server);
        LunarEclipse.resetRoll();
        LunarEclipse.overridePlayers(null);
        EclipsePayload.overrideSpy(null);
        SireCall.overridePlayerLookup(null);
        AlphaRoar.overridePlayers(null);
        server.overworld().setDayTime(dayTime);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean formCancelled(ServerPlayer p, IAction<?> action, boolean insideOwnTick) {
        ForcedShift.register();
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

    private static double attackModifier(ServerPlayer p, net.minecraft.resources.ResourceLocation id) {
        AttributeInstance a = p.getAttribute(Attributes.ATTACK_DAMAGE);
        var m = a == null ? null : a.getModifier(id);
        return m == null ? 0 : m.amount();
    }

    // ---- the lunar eclipse ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void eclipseBlocksForms(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        long before = server.overworld().getDayTime();
        server.overworld().setDayTime(FULL_MOON_DAY * 24000 + NIGHT);
        ServerPlayer alpha = wolf(helper, Rank.ALPHA, 8, 1.5);
        ServerPlayer plain = wolf(helper, Rank.OMEGA, 1, 3.5);
        List<ServerPlayer> betas = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            betas.add(wolf(helper, Rank.BETA, 1, 5.5 + i));
            BloodlineAccess.set(betas.get(i), BloodlineAccess.get(betas.get(i)).toBuilder()
                    .wolf(BloodlineAccess.get(betas.get(i)).wolf().toBuilder().packAlpha(alpha.getUUID()).build()).build());
        }
        List<ServerPlayer> everyone = new ArrayList<>(List.of(alpha, plain));
        everyone.addAll(betas);
        online(everyone.toArray(new ServerPlayer[0]));
        List<String> seen = new ArrayList<>();
        EclipsePayload.overrideSpy((to, payload) -> seen.add((to == alpha ? "alpha:" : "other:") + payload.active()));
        try {
            helper.assertFalse(LunarEclipse.active(server), "control: an eclipse is running");
            helper.assertFalse(formCancelled(alpha, ModActions.BEAST_FORM.get(), false), "control: the Alpha could not shift at will");
            helper.assertFalse(formCancelled(plain, ModActions.BEAST_FORM.get(), true), "control: the full moon's shift was cancelled");
            helper.assertTrue(attackModifier(alpha, RankEffects.MODIFIER_ID) > 0, "control: the Alpha has no rank perk");
            PackBonus.tick(alpha);
            helper.assertTrue(attackModifier(alpha, PackBonus.MODIFIER_ID) > 0, "control: the Alpha has no pack bonus");

            betas.forEach(ServerPlayer::removeAllEffects);
            helper.assertTrue(LunarEclipse.start(server), "the eclipse did not start");
            helper.assertFalse(LunarEclipse.start(server), "a second start counted as a start");
            helper.assertTrue(LunarEclipse.active(server), "the eclipse is not active");
            helper.assertTrue(seen.contains("alpha:true"), "the clients were not told: " + seen);
            helper.assertTrue(formCancelled(alpha, ModActions.BEAST_FORM.get(), false), "the Alpha shifted at will in the eclipse");
            helper.assertTrue(formCancelled(alpha, ModActions.SURVIVAL_FORM.get(), false), "the Alpha took a half form in the eclipse");
            helper.assertTrue(formCancelled(plain, ModActions.BEAST_FORM.get(), true), "the full moon's shift went through in the eclipse");
            helper.assertTrue(attackModifier(alpha, RankEffects.MODIFIER_ID) == 0, "the Alpha kept the rank perk in the eclipse");
            PackBonus.tick(alpha);
            helper.assertTrue(attackModifier(alpha, PackBonus.MODIFIER_ID) == 0, "the Alpha kept the pack bonus in the eclipse");
            helper.assertFalse(betas.get(0).hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION), "a Beta got the pack's Regeneration in the eclipse");

            // a player who logs in during it is told
            seen.clear();
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(alpha));
            helper.assertTrue(seen.contains("alpha:true"), "a login in the eclipse was not told: " + seen);

            seen.clear();
            helper.assertTrue(LunarEclipse.stop(server), "the eclipse did not stop");
            helper.assertFalse(LunarEclipse.active(server), "the eclipse is still active");
            helper.assertTrue(seen.contains("alpha:false"), "the clients were not told it ended: " + seen);
            helper.assertFalse(formCancelled(alpha, ModActions.BEAST_FORM.get(), false), "the Alpha could not shift after the eclipse");
            helper.assertTrue(attackModifier(alpha, RankEffects.MODIFIER_ID) > 0, "the rank perk did not come back");
            PackBonus.tick(alpha);
            helper.assertTrue(attackModifier(alpha, PackBonus.MODIFIER_ID) > 0, "the pack bonus did not come back");
        } finally {
            reset(server, before);
            clean(everyone.toArray(new ServerPlayer[0]));
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void eclipseEndsAtDawnAfterReload(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        long before = server.overworld().getDayTime();
        ServerPlayer watcher = TestPlayers.createDuelist(helper);
        online(watcher);
        List<Boolean> told = new ArrayList<>();
        EclipsePayload.overrideSpy((to, payload) -> told.add(payload.active()));
        try {
            server.overworld().setDayTime(FULL_MOON_DAY * 24000 + NIGHT);
            LunarEclipse.start(server);
            LunarEclipse.Data data = LunarEclipse.data(server);
            helper.assertTrue(data.eclipseNight() == FULL_MOON_DAY, "the eclipse is stored as night " + data.eclipseNight());

            // the file survives a restart; the night does not outlast its dawn
            CompoundTag saved = data.save(new CompoundTag(), server.registryAccess());
            LunarEclipse.Data reloaded = LunarEclipse.Data.load(saved, server.registryAccess());
            helper.assertTrue(reloaded.eclipseNight() == FULL_MOON_DAY, "the reloaded eclipse is night " + reloaded.eclipseNight());
            helper.assertTrue(LunarEclipse.isActive(reloaded, FULL_MOON_DAY * 24000 + NIGHT), "a restart mid-night ended the eclipse");
            helper.assertTrue(LunarEclipse.isActive(reloaded, FULL_MOON_DAY * 24000 + LunarEclipse.DAWN - 1), "the eclipse ended before dawn");
            helper.assertFalse(LunarEclipse.isActive(reloaded, FULL_MOON_DAY * 24000 + LunarEclipse.DAWN), "the eclipse outlived the dawn");
            helper.assertFalse(LunarEclipse.isActive(reloaded, (FULL_MOON_DAY + 1) * 24000 + NIGHT), "the eclipse stayed into the next night");
            helper.assertFalse(LunarEclipse.isActive(reloaded, 0), "the eclipse is active before its night");

            // the server starts again after the dawn: it is cleared, quietly
            server.overworld().setDayTime(FULL_MOON_DAY * 24000 + LunarEclipse.DAWN + 500);
            helper.assertFalse(LunarEclipse.active(server), "the dawn did not end the eclipse");
            told.clear();
            LunarEclipse.onServerStarted(server);
            helper.assertTrue(data.eclipseNight() == LunarEclipse.NONE, "the stored eclipse stayed after the dawn: " + data.eclipseNight());

            // days later (the world was off for a week): still cleared
            server.overworld().setDayTime(FULL_MOON_DAY * 24000 + NIGHT);
            LunarEclipse.start(server);
            server.overworld().setDayTime((FULL_MOON_DAY + 7) * 24000 + NIGHT);
            LunarEclipse.onServerStarted(server);
            helper.assertTrue(data.eclipseNight() == LunarEclipse.NONE, "an eclipse from a week ago stayed: " + data.eclipseNight());

            // while the server runs, the tick at dawn ends it and tells everyone
            server.overworld().setDayTime(FULL_MOON_DAY * 24000 + NIGHT);
            LunarEclipse.start(server);
            told.clear();
            server.overworld().setDayTime(FULL_MOON_DAY * 24000 + LunarEclipse.DAWN + 20);
            LunarEclipse.tick(server);
            helper.assertTrue(data.eclipseNight() == LunarEclipse.NONE && told.equals(List.of(false)), "the dawn tick did not end it: " + data.eclipseNight() + " " + told);
        } finally {
            reset(server, before);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void eclipseRollsOnFullMoonNightsOnly(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        long before = server.overworld().getDayTime();
        ServerPlayer watcher = TestPlayers.createDuelist(helper);
        online(watcher);
        try {
            // a night with a half moon is never rolled
            LunarEclipse.useRoll(() -> 0.0);
            server.overworld().setDayTime(4 * 24000 + NIGHT);
            LunarEclipse.tick(server);
            helper.assertFalse(LunarEclipse.active(server), "an eclipse on a half moon");

            // the day before dusk is not rolled either
            server.overworld().setDayTime(FULL_MOON_DAY * 24000 + 6000);
            LunarEclipse.tick(server);
            helper.assertFalse(LunarEclipse.active(server), "an eclipse before dusk");

            // a bad roll: no eclipse, and the night is not rolled again
            LunarEclipse.useRoll(() -> 0.99);
            server.overworld().setDayTime(FULL_MOON_DAY * 24000 + NIGHT);
            LunarEclipse.tick(server);
            helper.assertFalse(LunarEclipse.active(server), "a 0.99 roll started an eclipse");
            LunarEclipse.useRoll(() -> 0.0);
            LunarEclipse.tick(server);
            helper.assertFalse(LunarEclipse.active(server), "the same night was rolled twice");

            // the next full moon, a good roll
            server.overworld().setDayTime((FULL_MOON_DAY + 8) * 24000 + NIGHT);
            LunarEclipse.tick(server);
            helper.assertTrue(LunarEclipse.active(server), "a 0.0 roll on a full moon did not start an eclipse");
            helper.assertTrue(LunarEclipse.data(server).eclipseNight() == FULL_MOON_DAY + 8, "the night is " + LunarEclipse.data(server).eclipseNight());

            // nobody online at dusk: no roll, the night stays open for the next tick
            LunarEclipse.stop(server);
            LunarEclipse.overridePlayers(s -> List.of());
            server.overworld().setDayTime((FULL_MOON_DAY + 16) * 24000 + NIGHT);
            LunarEclipse.tick(server);
            helper.assertFalse(LunarEclipse.active(server), "an eclipse with nobody online");
            LunarEclipse.overridePlayers(s -> List.of(watcher));
            LunarEclipse.tick(server);
            helper.assertTrue(LunarEclipse.active(server), "the night was used up with nobody online");
        } finally {
            reset(server, before);
        }
        helper.succeed();
    }

    // ---- electricity ----

    @GameTest(template = "empty", templateNamespace = NS)
    public static void lightningShocksWerewolf(GameTestHelper helper) {
        ServerPlayer struck = wolf(helper, Rank.OMEGA, 1, 1.5);
        ServerPlayer near = wolf(helper, Rank.OMEGA, 1, 3.5);
        ServerPlayer far = wolf(helper, Rank.OMEGA, 1, 12.5);
        ServerPlayer human = TestPlayers.createDuelist(helper);
        human.setPos(helper.absoluteVec(new Vec3(2.5, 1, 1.5)));
        ServerPlayer direct = wolf(helper, Rank.OMEGA, 1, 20.5);
        Electricity.overrideCandidates(b -> List.of(struck, near, far, human, direct));
        try {
            helper.assertFalse(struck.hasEffect(SpeciesRegistries.SHOCKED), "control: a wolf is shocked");

            // a visual bolt (no damage, no fire) at the first wolf: the join scan reaches within 3 blocks
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(helper.getLevel());
            bolt.moveTo(helper.absoluteVec(new Vec3(1.5, 1, 1.5)));
            bolt.setVisualOnly(true);
            helper.getLevel().addFreshEntity(bolt);
            MobEffectInstance shocked = struck.getEffect(SpeciesRegistries.SHOCKED);
            helper.assertTrue(shocked != null && shocked.getDuration() > 590 && shocked.getDuration() <= 600, "the wolf at the strike: " + shocked);
            helper.assertTrue(near.hasEffect(SpeciesRegistries.SHOCKED), "a wolf 2 blocks away was not shocked");
            helper.assertFalse(far.hasEffect(SpeciesRegistries.SHOCKED), "a wolf 11 blocks away was shocked");
            helper.assertFalse(human.hasEffect(SpeciesRegistries.SHOCKED), "a human was shocked");

            // being struck itself, wherever the bolt is
            NeoForge.EVENT_BUS.post(new EntityStruckByLightningEvent(direct, bolt));
            helper.assertTrue(direct.hasEffect(SpeciesRegistries.SHOCKED), "a wolf struck by lightning was not shocked");

            // shocked: no form actions, the full moon's included; and Slowness I
            helper.assertTrue(formCancelled(struck, ModActions.BEAST_FORM.get(), false), "a shocked wolf shifted at will");
            helper.assertTrue(formCancelled(struck, ModActions.SURVIVAL_FORM.get(), true), "a shocked wolf was forced to shift");
            var slow = struck.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(
                    ShockedEffect.SLOW_ID);
            helper.assertTrue(slow != null && slow.amount() < 0, "a shocked wolf is not slowed: " + slow);
        } finally {
            Electricity.overrideCandidates(null);
            clean(struck, near, far, human, direct);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void taserShocksSupernatural(GameTestHelper helper) {
        ServerPlayer attacker = TestPlayers.createDuelist(helper);
        ServerPlayer wolf = wolf(helper, Rank.OMEGA, 1, 3.5);
        ServerPlayer human = TestPlayers.createDuelist(helper);
        try {
            ItemStack taser = new ItemStack(SpeciesRegistries.TASER.get());
            helper.assertTrue(taser.getMaxDamage() == Taser.DURABILITY && Taser.DURABILITY == 16, "the taser has durability " + taser.getMaxDamage());

            taser.hurtEnemy(human, attacker);
            helper.assertFalse(human.hasEffect(SpeciesRegistries.SHOCKED), "a human was shocked by the taser");

            taser.hurtEnemy(wolf, attacker);
            MobEffectInstance shocked = wolf.getEffect(SpeciesRegistries.SHOCKED);
            helper.assertTrue(shocked != null && shocked.getDuration() > 590 && shocked.getDuration() <= 600, "the wolf is " + shocked);
            helper.assertTrue(taser.getDamageValue() == 2, "two hits used " + taser.getDamageValue() + " charges");

            // redstone repairs it in an anvil (isValidRepairItem is what the anvil asks); nothing else does
            helper.assertTrue(taser.getItem().isValidRepairItem(taser, new ItemStack(Items.REDSTONE)), "redstone does not repair the taser");
            helper.assertFalse(taser.getItem().isValidRepairItem(taser, new ItemStack(Items.IRON_INGOT)), "iron repairs the taser");
        } finally {
            clean(wolf);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void shockedBlocksRegen(GameTestHelper helper) {
        ServerPlayer control = TestPlayers.createDuelist(helper);
        ServerPlayer p = TestPlayers.createDuelist(helper);
        try {
            control.setHealth(10f);
            control.getFoodData().setFoodLevel(20);
            control.getFoodData().setSaturation(5f);
            for (int i = 0; i < 12; i++) {
                control.getFoodData().tick(control);
            }
            helper.assertTrue(control.getHealth() > 10f, "control: natural regeneration did not heal (" + control.getHealth() + ")");

            p.addEffect(new MobEffectInstance(SpeciesRegistries.SHOCKED, 600));
            p.setHealth(10f);
            p.getFoodData().setFoodLevel(20);
            p.getFoodData().setSaturation(5f);
            for (int i = 0; i < 12; i++) {
                p.getFoodData().tick(p);
            }
            helper.assertTrue(p.getHealth() == 10f, "natural regeneration healed through the shock: " + p.getHealth());
            p.heal(2f);
            helper.assertTrue(p.getHealth() == 12f, "a direct heal (potion, golden apple) was cancelled: " + p.getHealth());

            p.removeEffect(SpeciesRegistries.SHOCKED);
            for (int i = 0; i < 12; i++) {
                p.getFoodData().tick(p);
            }
            helper.assertTrue(p.getHealth() > 12f, "natural regeneration did not come back (" + p.getHealth() + ")");
        } finally {
            p.removeAllEffects();
        }
        helper.succeed();
    }
}
