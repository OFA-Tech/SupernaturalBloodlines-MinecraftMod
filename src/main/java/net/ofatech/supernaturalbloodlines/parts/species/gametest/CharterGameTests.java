package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.clean;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.human;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.join;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.key;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.online;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.vampire;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.GroupTestKit.witch;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.clan.CharterState;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers.TalkingDuelist;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.CharterRow;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupSwitcher;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.group.clan.CharterScan;
import net.ofatech.supernaturalbloodlines.parts.species.group.clan.Charters;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackCommand;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;

/** Plan 20c Task 5 (R271, R278, R279, R280): the Clan Charter: forging, the scan, the protection of a dropped one, lost and reforged, dormant. */
@PrefixGameTestTemplate(false)
public final class CharterGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final long HOUR = 60L * 60 * 1000;
    private static final String ERR = "supernaturalbloodlines.clan.err.";
    private static final String CLAN = "supernaturalbloodlines.clan.";

    private CharterGameTests() {
    }

    private record Clan(TalkingDuelist leader, TalkingDuelist a, TalkingDuelist b) {
        TalkingDuelist[] all() {
            return new TalkingDuelist[] {leader, a, b};
        }
    }

    private static Clan clan(GameTestHelper helper) {
        TalkingDuelist l = vampire(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        TalkingDuelist b = human(helper, 3.5);
        online(l, a, b);
        helper.assertTrue(join(l, a) && join(l, b) && Groups_typeIsClan(a), "no clan formed");
        return new Clan(l, a, b);
    }

    private static boolean Groups_typeIsClan(TalkingDuelist p) {
        return net.ofatech.supernaturalbloodlines.parts.species.group.Groups.typeOf(p) == GroupType.CLAN;
    }

    private static GroupIndex groups(GameTestHelper helper) {
        return GroupIndex.get(helper.getLevel());
    }

    private static MinecraftServer server(GameTestHelper helper) {
        return helper.getLevel().getServer();
    }

    private static UUID groupOf(GameTestHelper helper, TalkingDuelist leader) {
        return groups(helper).existingGroupOf(leader.getUUID()).orElseThrow();
    }

    private static String ref(TalkingDuelist p) {
        return p.getUUID().toString();
    }

    private static void giveCost(TalkingDuelist p) {
        p.getInventory().add(new ItemStack(Items.WRITABLE_BOOK));
        p.getInventory().add(new ItemStack(Items.GOLD_BLOCK));
    }

    private static void said(GameTestHelper helper, Component c, String key, String why) {
        helper.assertTrue(key.equals(key(c)), why + ": " + key(c));
    }

    private static boolean isCharter(ItemStack s) {
        return s.is(SpeciesRegistries.CLAN_CHARTER.get());
    }

    private static int held(TalkingDuelist p) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) {
            n += isCharter(s) ? 1 : 0;
        }
        return n + (isCharter(p.getOffhandItem()) ? 1 : 0);
    }

    private static ItemStack firstCharter(TalkingDuelist p) {
        for (ItemStack s : p.getInventory().items) {
            if (isCharter(s)) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void take(TalkingDuelist p) {
        for (int i = 0; i < p.getInventory().items.size(); i++) {
            if (isCharter(p.getInventory().items.get(i))) {
                p.getInventory().items.set(i, ItemStack.EMPTY);
            }
        }
        if (isCharter(p.getOffhandItem())) {
            p.getInventory().offhand.set(0, ItemStack.EMPTY);
        }
    }

    private static List<ItemEntity> droppedCharters(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absoluteVec(new Vec3(4, 2, 4)), helper.absoluteVec(new Vec3(4, 2, 4))).inflate(16), e -> isCharter(e.getItem()));
    }

    private static void scan(GameTestHelper helper, TalkingDuelist... players) {
        CharterScan.overridePlayers(() -> List.of(players));
        CharterScan.scanNow(server(helper));
    }

    private static void done(GameTestHelper helper, TalkingDuelist... players) {
        CharterScan.overridePlayers(null);
        for (ItemEntity e : droppedCharters(helper)) {
            e.discard();
        }
        clean(helper, players);
    }

    private static CharterRow row(GameTestHelper helper, TalkingDuelist leader) {
        return groups(helper).charterOf(groupOf(helper, leader)).orElseThrow();
    }

    private static ItemStack forged(GameTestHelper helper, Clan c) {
        giveCost(c.leader());
        helper.assertTrue(Charters.forge(c.leader()) == null, "setup: the forge was refused");
        return firstCharter(c.leader());
    }

    // ---- forging ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theLeaderForgesWithTheCostAndGetsTheItem(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            giveCost(c.leader());
            helper.assertTrue(Charters.forge(c.leader()) == null, "the forge was refused");
            helper.assertTrue(held(c.leader()) == 1, "no Charter in the inventory");
            helper.assertTrue(c.leader().getInventory().countItem(Items.WRITABLE_BOOK) == 0 && c.leader().getInventory().countItem(Items.GOLD_BLOCK) == 0, "the cost was not taken");
            CharterRow row = row(helper, c.leader());
            ItemStack stack = firstCharter(c.leader());
            helper.assertTrue(row.state() == CharterState.HELD && row.holder().equals(Optional.of(c.leader().getUUID())), "the row is " + row);
            helper.assertTrue(row.id().equals(stack.get(SpeciesRegistries.CHARTER_ID.get())) && groupOf(helper, c.leader()).equals(stack.get(SpeciesRegistries.CHARTER_GROUP.get())), "the stack is not the row's token");
            helper.assertTrue(stack.getMaxStackSize() == 1, "a Charter stacks");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void onlyTheLeaderForges(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            giveCost(c.a());
            said(helper, Charters.forge(c.a()), ERR + "leader_only", "a member forged");
            helper.assertTrue(groups(helper).charterOf(groupOf(helper, c.leader())).isEmpty() && c.a().getInventory().countItem(Items.GOLD_BLOCK) == 1, "something changed");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void forgeNeedsThreeMembers(GameTestHelper helper) {
        TalkingDuelist l = vampire(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        online(l, a);
        try {
            helper.assertTrue(join(l, a), "setup");
            giveCost(l);
            said(helper, Charters.forge(l), ERR + "charter_too_few", "a clan of two forged");
            helper.assertTrue(l.getInventory().countItem(Items.GOLD_BLOCK) == 1 && held(l) == 0, "something was taken or made");
        } finally {
            done(helper, l, a);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void forgeNeedsTheCostAndTakesNothingOtherwise(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            c.leader().getInventory().add(new ItemStack(Items.WRITABLE_BOOK));
            said(helper, Charters.forge(c.leader()), ERR + "charter_cost", "forged without the gold block");
            helper.assertTrue(c.leader().getInventory().countItem(Items.WRITABLE_BOOK) == 1 && held(c.leader()) == 0, "the book was taken on a failed forge");
            helper.assertTrue(groups(helper).charterOf(groupOf(helper, c.leader())).isEmpty(), "a row was written");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void oneLiveCharterPerClan(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            forged(helper, c);
            giveCost(c.leader());
            said(helper, Charters.forge(c.leader()), ERR + "charter_already", "a second Charter");
            helper.assertTrue(held(c.leader()) == 1 && c.leader().getInventory().countItem(Items.GOLD_BLOCK) == 1, "a second one was made or the cost taken");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aLostCharterIsReforgedNotForged(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            forged(helper, c);
            UUID g = groupOf(helper, c.leader());
            CharterRow r = row(helper, c.leader());
            groups(helper).setCharter(g, new CharterRow(r.id(), Optional.empty(), CharterState.LOST, r.forgedAt(), r.seenAt(), Optional.empty(), PackIndex.now()));
            giveCost(c.leader());
            said(helper, Charters.forge(c.leader()), ERR + "charter_use_reforge", "a lost Charter was forged anew");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void forgeIsOffWithCharterDisabled(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            GroupsConfig.override("charter_enabled", false);
            giveCost(c.leader());
            said(helper, Charters.forge(c.leader()), ERR + "charter_off", "forged with Charters off");
            helper.assertTrue(groups(helper).charterOf(groupOf(helper, c.leader())).isEmpty(), "a row was written");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void forgeInAPackAnswersWrongType(GameTestHelper helper) {
        TalkingDuelist l = human(helper, 1.5);
        TalkingDuelist a = human(helper, 2.5);
        online(l, a);
        try {
            PackIndex.get(helper.getLevel()).join(l.getUUID(), a.getUUID());
            groups(helper).ensure(l.getUUID(), GroupType.PACK, PackIndex.now());
            giveCost(l);
            said(helper, Charters.forge(l), "supernaturalbloodlines.group.wrong_type.pack", "a pack leader forged");
        } finally {
            done(helper, l, a);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aFullInventoryDropsItAtTheFeet(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            giveCost(c.leader());
            for (int i = 0; i < c.leader().getInventory().items.size(); i++) {
                if (c.leader().getInventory().items.get(i).isEmpty()) {
                    c.leader().getInventory().items.set(i, new ItemStack(Items.DIRT));
                }
            }
            c.leader().getInventory().items.set(0, new ItemStack(Items.WRITABLE_BOOK, 2));
            c.leader().getInventory().items.set(1, new ItemStack(Items.GOLD_BLOCK, 3));
            helper.assertTrue(Charters.forge(c.leader()) == null, "refused");
            helper.assertTrue(held(c.leader()) == 0 && droppedCharters(helper).size() == 1, "the Charter was not dropped at the feet: " + droppedCharters(helper).size());
            CharterRow row = row(helper, c.leader());
            helper.assertTrue(row.state() == CharterState.DROPPED && row.entity().equals(Optional.of(droppedCharters(helper).get(0).getUUID())), "the row is " + row);
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    // ---- held ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theScanSeesTheHolder(GameTestHelper helper) {
        long[] now = {5_000_000_000L};
        PackIndex.overrideClock(() -> now[0]);
        Clan c = clan(helper);
        try {
            forged(helper, c);
            ItemStack stack = firstCharter(c.leader());
            c.leader().getInventory().items.set(c.leader().getInventory().findSlotMatchingItem(stack), ItemStack.EMPTY);
            c.a().getInventory().add(stack);
            now[0] += HOUR;
            scan(helper, c.leader(), c.a(), c.b());
            CharterRow row = row(helper, c.leader());
            helper.assertTrue(row.state() == CharterState.HELD && row.holder().equals(Optional.of(c.a().getUUID())) && row.seenAt() == now[0], "the row is " + row);
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aHeldCharterIsNotRewrittenOrPushedEveryScan(GameTestHelper helper) {
        long[] now = {5_000_000_000L};
        PackIndex.overrideClock(() -> now[0]);
        Clan c = clan(helper);
        try {
            forged(helper, c);
            scan(helper, c.leader(), c.a(), c.b());
            CharterRow first = row(helper, c.leader());
            int pushes = Charters.refreshes();
            for (int i = 0; i < 5; i++) {
                now[0] += 2_000;
                scan(helper, c.leader(), c.a(), c.b());
            }
            helper.assertTrue(row(helper, c.leader()).equals(first), "the row was rewritten while nothing changed: " + row(helper, c.leader()));
            helper.assertTrue(Charters.refreshes() == pushes, "the clan was pushed " + (Charters.refreshes() - pushes) + " times while nothing changed");
            now[0] += HOUR;
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).seenAt() == now[0] && row(helper, c.leader()).state() == CharterState.HELD, "the stale seenAt was not refreshed after an hour");
            helper.assertTrue(Charters.refreshes() == pushes, "refreshing seenAt pushed the clan");
            pass(c.leader(), c.a());
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).holder().equals(Optional.of(c.a().getUUID())) && Charters.refreshes() > pushes, "a change of holder was not written and pushed");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    private static void pass(TalkingDuelist from, TalkingDuelist to) {
        ItemStack s = firstCharter(from);
        from.getInventory().items.set(from.getInventory().findSlotMatchingItem(s), ItemStack.EMPTY);
        to.getInventory().add(s);
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theOffhandCounts(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            ItemStack stack = forged(helper, c).copy();
            take(c.leader());
            c.leader().getInventory().offhand.set(0, stack);
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.HELD && held(c.leader()) == 1, "the off hand did not count: " + row(helper, c.leader()));
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aCharterInAChestIsStored(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            ItemStack stack = forged(helper, c).copy();
            take(c.leader());
            BlockPos chestPos = helper.absolutePos(new BlockPos(3, 2, 3));
            helper.getLevel().setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
            ((ChestBlockEntity) helper.getLevel().getBlockEntity(chestPos)).setItem(0, stack);
            scan(helper, c.leader(), c.a(), c.b());
            CharterRow row = row(helper, c.leader());
            helper.assertTrue(row.state() == CharterState.STORED && row.holder().isEmpty(), "the row is " + row);
            helper.assertTrue(((ChestBlockEntity) helper.getLevel().getBlockEntity(chestPos)).getItem(0).is(SpeciesRegistries.CLAN_CHARTER.get()), "the chest was touched");
            helper.getLevel().setBlock(chestPos, Blocks.AIR.defaultBlockState(), 3);
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aShulkerInTheInventoryIsNotHeld(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            ItemStack stack = forged(helper, c).copy();
            take(c.leader());
            ItemStack box = new ItemStack(Items.SHULKER_BOX);
            box.set(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(stack)));
            c.leader().getInventory().add(box);
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.STORED, "a shulker counted as held: " + row(helper, c.leader()));
            helper.assertTrue(c.leader().getInventory().countItem(Items.SHULKER_BOX) == 1, "the box was removed");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theArmourSlotIsNotScanned(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            ItemStack stack = forged(helper, c).copy();
            take(c.leader());
            c.leader().setItemSlot(EquipmentSlot.HEAD, stack);
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.STORED, "an armour slot counted as held: " + row(helper, c.leader()));
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theHolderOfflineKeepsTheRowHeld(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            forged(helper, c);
            scan(helper, c.a(), c.b());
            CharterRow row = row(helper, c.leader());
            helper.assertTrue(row.state() == CharterState.HELD && row.holder().equals(Optional.of(c.leader().getUUID())), "an offline holder lost the row: " + row);
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aCharterThatIsDroppedByItsHolderIsDropped(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            ItemStack stack = forged(helper, c).copy();
            take(c.leader());
            ItemEntity e = new ItemEntity(helper.getLevel(), c.leader().getX(), c.leader().getY(), c.leader().getZ(), stack);
            e.setNoGravity(true);
            helper.getLevel().addFreshEntity(e);
            CharterRow row = row(helper, c.leader());
            helper.assertTrue(row.state() == CharterState.DROPPED && row.entity().equals(Optional.of(e.getUUID())), "the row is " + row);
            helper.assertTrue(e.isInvulnerable(), "the dropped Charter is hurtable");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aDroppedRowWhoseEntityIsGoneAndOldGoesLost(GameTestHelper helper) {
        long[] now = {5_000_000_000L};
        PackIndex.overrideClock(() -> now[0]);
        Clan c = clan(helper);
        try {
            ItemStack stack = forged(helper, c).copy();
            take(c.leader());
            ItemEntity e = new ItemEntity(helper.getLevel(), c.leader().getX(), c.leader().getY(), c.leader().getZ(), stack);
            e.setNoGravity(true);
            helper.getLevel().addFreshEntity(e);
            now[0] += 30 * 24 * HOUR;
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.DROPPED, "a dropped Charter whose entity is in the world went lost: " + row(helper, c.leader()));
            CharterRow r = row(helper, c.leader());
            groups(helper).setCharter(groupOf(helper, c.leader()), new CharterRow(r.id(), Optional.empty(), CharterState.DROPPED, r.forgedAt(), r.seenAt(), Optional.of(UUID.randomUUID()), 0));
            now[0] -= 29 * 24 * HOUR;
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.DROPPED, "a gone entity went lost before the days: " + row(helper, c.leader()));
            now[0] += 29 * 24 * HOUR;
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.LOST && row(helper, c.leader()).lostAt() == now[0], "a gone entity of an old row did not go lost: " + row(helper, c.leader()));
            helper.assertTrue(c.leader().received(CLAN + "charter_lost"), "the clan was not told");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aCharterEntityWhoseRowIsGoneIsRemovedWhenItJoins(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            ItemEntity stale = new ItemEntity(helper.getLevel(), c.leader().getX(), c.leader().getY(), c.leader().getZ(), Charters.stack(UUID.randomUUID(), groupOf(helper, c.leader())));
            helper.assertTrue(!helper.getLevel().addFreshEntity(stale) || stale.isRemoved(), "an entity with no row joined the world");
            ItemEntity ofNoClan = new ItemEntity(helper.getLevel(), c.leader().getX(), c.leader().getY(), c.leader().getZ(), Charters.stack(UUID.randomUUID(), UUID.randomUUID()));
            helper.assertTrue(!helper.getLevel().addFreshEntity(ofNoClan) || ofNoClan.isRemoved(), "an entity of a clan that does not exist joined the world");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    // ---- protection ----

    private static ItemEntity dropIt(GameTestHelper helper, TalkingDuelist leader) {
        ItemStack stack = firstCharter(leader).copy();
        take(leader);
        ItemEntity e = new ItemEntity(helper.getLevel(), leader.getX(), leader.getY(), leader.getZ(), stack);
        e.setNoGravity(true);
        e.setDeltaMovement(Vec3.ZERO);
        e.setNoPickUpDelay();
        helper.getLevel().addFreshEntity(e);
        return e;
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aNonMemberCannotPickItUp(GameTestHelper helper) {
        Clan c = clan(helper);
        TalkingDuelist rival = vampire(helper, 5.5);
        TalkingDuelist rivalMate = human(helper, 6.5);
        TalkingDuelist loner = human(helper, 7.5);
        online(c.leader(), c.a(), c.b(), rival, rivalMate, loner);
        try {
            helper.assertTrue(join(rival, rivalMate), "setup: no second clan");
            forged(helper, c);
            ItemEntity e = dropIt(helper, c.leader());
            e.playerTouch(rival);
            e.playerTouch(rivalMate);
            e.playerTouch(loner);
            helper.assertTrue(!e.isRemoved() && held(rival) + held(rivalMate) + held(loner) == 0, "an outsider took the Charter");
        } finally {
            done(helper, c.leader(), c.a(), c.b(), rival, rivalMate, loner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aMemberCan(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            forged(helper, c);
            ItemEntity e = dropIt(helper, c.leader());
            e.playerTouch(c.a());
            helper.assertTrue(e.isRemoved() && held(c.a()) == 1, "a member could not take it");
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.HELD && row(helper, c.leader()).holder().equals(Optional.of(c.a().getUUID())), "the row did not follow: " + row(helper, c.leader()));
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theEntityIsInvulnerableAndNeverDespawns(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            forged(helper, c);
            ItemEntity e = dropIt(helper, c.leader());
            ServerLevel level = helper.getLevel();
            helper.assertTrue(e.isInvulnerable(), "not invulnerable");
            e.hurt(level.damageSources().lava(), 4f);
            e.hurt(level.damageSources().inFire(), 1f);
            level.explode(null, e.getX(), e.getY(), e.getZ(), 4f, Level.ExplosionInteraction.NONE);
            helper.assertTrue(!e.isRemoved(), "lava, fire or an explosion destroyed it");
            net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
            e.save(saved);
            helper.assertTrue(saved.getShort("Age") == -32768, "it can age: " + saved.getShort("Age"));
            net.minecraft.nbt.CompoundTag aged = saved.copy();
            aged.remove("UUID");
            aged.putShort("Age", (short) 5999);
            ItemEntity reloaded = (ItemEntity) net.minecraft.world.entity.EntityType.create(aged, level).orElseThrow();
            reloaded.setNoGravity(true);
            level.addFreshEntity(reloaded);
            helper.assertTrue(reloaded.isInvulnerable(), "an entity loaded at Age 5999 is hurtable");
            reloaded.tick();
            reloaded.tick();
            helper.assertTrue(!reloaded.isRemoved(), "an entity loaded at Age 5999 despawned");
            reloaded.discard();
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aVoidFallMarksItLost(GameTestHelper helper) {
        long[] now = {6_000_000_000L};
        PackIndex.overrideClock(() -> now[0]);
        Clan c = clan(helper);
        try {
            forged(helper, c);
            ItemEntity e = dropIt(helper, c.leader());
            e.setPos(e.getX(), helper.getLevel().getMinBuildHeight() - 100, e.getZ());
            e.checkBelowWorld();
            CharterRow row = row(helper, c.leader());
            helper.assertTrue(row.state() == CharterState.LOST && row.lostAt() == now[0] && row.entity().isEmpty(), "the row is " + row);
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aKilledEntityMarksItLost(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            forged(helper, c);
            dropIt(helper, c.leader()).kill();
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.LOST, "the row is " + row(helper, c.leader()));
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aPickupMarksNothingLost(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            forged(helper, c);
            ItemEntity e = dropIt(helper, c.leader());
            e.playerTouch(c.b());
            helper.assertTrue(e.isRemoved() && row(helper, c.leader()).state() != CharterState.LOST, "a pick-up was read as a loss: " + row(helper, c.leader()));
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.HELD, "the row is " + row(helper, c.leader()));
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aHopperEmptiesItAndTheRowGoesStored(GameTestHelper helper) {
        Clan c = clan(helper);
        BlockPos hopperPos = helper.absolutePos(new BlockPos(3, 2, 3));
        try {
            forged(helper, c);
            ItemEntity e = dropIt(helper, c.leader());
            helper.getLevel().setBlock(hopperPos, Blocks.HOPPER.defaultBlockState(), 3);
            e.setPos(hopperPos.getX() + 0.5, hopperPos.getY() + 1.2, hopperPos.getZ() + 0.5);
            HopperBlockEntity hopper = (HopperBlockEntity) helper.getLevel().getBlockEntity(hopperPos);
            HopperBlockEntity.pushItemsTick(helper.getLevel(), hopperPos, helper.getLevel().getBlockState(hopperPos), hopper);
            helper.assertTrue(e.isRemoved(), "the hopper did not take it");
            helper.assertTrue(row(helper, c.leader()).state() != CharterState.LOST, "a hopper was read as a loss: " + row(helper, c.leader()));
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.STORED, "the row is " + row(helper, c.leader()));
        } finally {
            helper.getLevel().setBlock(hopperPos, Blocks.AIR.defaultBlockState(), 3);
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aNonMemberHolderIsStrippedAndItDropsProtected(GameTestHelper helper) {
        Clan c = clan(helper);
        TalkingDuelist loner = human(helper, 5.5);
        online(c.leader(), c.a(), c.b(), loner);
        try {
            ItemStack stack = forged(helper, c).copy();
            take(c.leader());
            loner.getInventory().add(stack);
            scan(helper, c.leader(), c.a(), c.b(), loner);
            helper.assertTrue(held(loner) == 0, "the outsider kept the Charter");
            List<ItemEntity> dropped = droppedCharters(helper);
            helper.assertTrue(dropped.size() == 1 && !dropped.get(0).getItem().isEmpty() && dropped.get(0).isInvulnerable(), "it was not dropped protected: " + dropped.size());
            dropped.get(0).setNoPickUpDelay();
            dropped.get(0).playerTouch(loner);
            helper.assertTrue(!dropped.get(0).isRemoved() && held(loner) == 0, "the outsider took it back");
            helper.assertTrue(loner.received(CLAN + "charter_taken"), "the outsider was not told");
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.DROPPED, "the row is " + row(helper, c.leader()));
            dropped.get(0).setNoPickUpDelay();
            dropped.get(0).playerTouch(c.b());
            helper.assertTrue(held(c.b()) == 1, "a member could not pick it up");
        } finally {
            done(helper, c.leader(), c.a(), c.b(), loner);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aKickedHolderIsStripped(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            ItemStack stack = forged(helper, c).copy();
            take(c.leader());
            c.a().getInventory().add(stack);
            helper.assertTrue(PackCommand.kickNamed(c.leader(), ref(c.a())) == null, "the kick was refused");
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(held(c.a()) == 0 && droppedCharters(helper).size() == 1, "the kicked holder kept it");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aHolderWhoLeavesIsStripped(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            ItemStack stack = forged(helper, c).copy();
            take(c.leader());
            c.a().getInventory().add(stack);
            helper.assertTrue(PackCommand.leave(c.a()) == null, "leaving was refused");
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(held(c.a()) == 0 && droppedCharters(helper).size() == 1, "the one who left kept it");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aHolderWhoDiesDropsItWhereTheyDie(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            forged(helper, c);
            c.leader().getInventory().dropAll();
            helper.assertTrue(held(c.leader()) == 0 && droppedCharters(helper).size() == 1, "it was not dropped");
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.DROPPED, "the row is " + row(helper, c.leader()));
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void keepInventoryKeepsItHeld(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            forged(helper, c);
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.HELD && held(c.leader()) == 1, "the row is " + row(helper, c.leader()));
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void duplicatesBecomeDust(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            ItemStack stack = forged(helper, c);
            c.leader().getInventory().add(stack.copy());
            c.a().getInventory().add(stack.copy());
            c.b().getInventory().add(stack.copy());
            helper.assertTrue(held(c.leader()) + held(c.a()) + held(c.b()) == 4, "setup");
            scan(helper, c.a(), c.b(), c.leader());
            helper.assertTrue(held(c.leader()) == 1 && held(c.a()) == 0 && held(c.b()) == 0, "the row's holder's copy did not survive alone: " + held(c.leader()) + held(c.a()) + held(c.b()));
            helper.assertTrue(droppedCharters(helper).isEmpty() && c.a().received(CLAN + "charter_dust") && !c.a().received(CLAN + "charter_replaced"), "a duplicate was dropped instead of destroyed, or its holder was not told");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aCreativeStackOfManyKeepsOneAndAStrippedStackIsDust(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            ItemStack stack = forged(helper, c);
            take(c.leader());
            ItemStack many = stack.copy();
            many.setCount(5); // a creative-mode player can set any count on a stack they copied
            c.leader().getInventory().add(many);
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(held(c.leader()) == 1 && firstCharter(c.leader()).getCount() == 1, "a stack of " + firstCharter(c.leader()).getCount() + " Charters survived the scan");
            ItemStack stripped = stack.copy();
            stripped.remove(SpeciesRegistries.CHARTER_ID.get()); // a creative copy that lost its components is no Charter
            c.a().getInventory().add(stripped);
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(held(c.a()) == 0 && held(c.leader()) == 1, "a creative stack with no id survived");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void adminGiveWithNoRowIsDust(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            c.a().getInventory().add(new ItemStack(SpeciesRegistries.CLAN_CHARTER.get()));
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(held(c.a()) == 0 && groups(helper).charterOf(groupOf(helper, c.leader())).isEmpty(), "a /give copy was accepted");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aStackOfALiveClanWithNoRowIsDustAndNothingThrows(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            c.a().getInventory().add(Charters.stack(UUID.randomUUID(), groupOf(helper, c.leader())));
            helper.assertTrue(groups(helper).charterOf(groupOf(helper, c.leader())).isEmpty(), "setup: the clan has a row");
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(held(c.a()) == 0 && groups(helper).charterOf(groupOf(helper, c.leader())).isEmpty(), "a stack of this clan with no row survived or made a row");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aCharterOfADeadClanIsDust(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            c.a().getInventory().add(Charters.stack(UUID.randomUUID(), UUID.randomUUID()));
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(held(c.a()) == 0, "a Charter of a clan that does not exist survived");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void anOldCharterIsDustAfterReforge(GameTestHelper helper) {
        long[] now = {7_000_000_000L};
        PackIndex.overrideClock(() -> now[0]);
        Clan c = clan(helper);
        try {
            ItemStack old = forged(helper, c).copy();
            UUID g = groupOf(helper, c.leader());
            take(c.leader());
            groups(helper).setCharter(g, new CharterRow(row(helper, c.leader()).id(), Optional.empty(), CharterState.LOST, 1, 1, Optional.empty(), now[0]));
            now[0] += 25 * HOUR;
            giveCost(c.leader());
            helper.assertTrue(Charters.reforge(c.leader()) == null, "the reforge was refused");
            UUID newId = row(helper, c.leader()).id();
            helper.assertTrue(!newId.equals(old.get(SpeciesRegistries.CHARTER_ID.get())), "the id did not change");
            c.a().getInventory().add(old.copy()); // Inventory.add empties the stack it is given, so a second add of the same object adds nothing
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(held(c.a()) == 0 && held(c.leader()) == 1 && newId.equals(firstCharter(c.leader()).get(SpeciesRegistries.CHARTER_ID.get())), "the old Charter survived the reforge");
            helper.assertTrue(c.a().received(CLAN + "charter_replaced") && !c.a().received(CLAN + "charter_dust"), "a replaced Charter was called false: it was the true one once");
            take(c.leader());
            c.a().getInventory().add(old.copy());
            scan(helper, c.a());
            helper.assertTrue(held(c.a()) == 0 && row(helper, c.leader()).id().equals(newId), "an old Charter held on its own survived the reforge");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    // ---- lost ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void lostAfterSevenDaysWithNoSighting(GameTestHelper helper) {
        long[] now = {8_000_000_000L};
        PackIndex.overrideClock(() -> now[0]);
        Clan c = clan(helper);
        try {
            ItemStack stack = forged(helper, c).copy();
            take(c.leader());
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.STORED, "setup: " + row(helper, c.leader()));
            now[0] += 7 * DAY - 1;
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.STORED, "lost early");
            now[0] += 1;
            scan(helper, c.leader(), c.a(), c.b());
            CharterRow row = row(helper, c.leader());
            helper.assertTrue(row.state() == CharterState.LOST && row.lostAt() == now[0], "not lost after the days: " + row);
            helper.assertTrue(c.a().received(CLAN + "charter_lost"), "the clan was not told");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void lostWhenTheHolderIsOfflineTheAbsentDays(GameTestHelper helper) {
        long[] now = {9_000_000_000L};
        PackIndex.overrideClock(() -> now[0]);
        Clan c = clan(helper);
        try {
            SpeciesConfig.overrideAbsentLeaderDays(3);
            forged(helper, c);
            PackIndex.get(helper.getLevel()).setLastSeen(c.leader().getUUID(), now[0]);
            now[0] += 3 * DAY - 1;
            scan(helper, c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.HELD, "lost early");
            now[0] += 1;
            scan(helper, c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.LOST, "an absent holder's Charter is not lost: " + row(helper, c.leader()));
        } finally {
            SpeciesConfig.overrideAbsentLeaderDays(null);
            done(helper, c.all());
        }
        helper.succeed();
    }

    private static void lose(GameTestHelper helper, Clan c, long at) {
        UUID g = groupOf(helper, c.leader());
        CharterRow r = row(helper, c.leader());
        groups(helper).setCharter(g, new CharterRow(r.id(), Optional.empty(), CharterState.LOST, r.forgedAt(), r.seenAt(), Optional.empty(), at));
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void reforgeAfterLostAnd24Hours(GameTestHelper helper) {
        long[] now = {10_000_000_000L};
        PackIndex.overrideClock(() -> now[0]);
        Clan c = clan(helper);
        try {
            forged(helper, c);
            take(c.leader());
            lose(helper, c, now[0]);
            now[0] += 24 * HOUR;
            giveCost(c.leader());
            helper.assertTrue(Charters.reforge(c.leader()) == null, "refused after 24 hours");
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.HELD && held(c.leader()) == 1 && c.leader().getInventory().countItem(Items.GOLD_BLOCK) == 0, "the reforge did not give the item and take the cost");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void reforgeBeforeThatIsRefused(GameTestHelper helper) {
        long[] now = {11_000_000_000L};
        PackIndex.overrideClock(() -> now[0]);
        Clan c = clan(helper);
        try {
            forged(helper, c);
            take(c.leader());
            lose(helper, c, now[0]);
            now[0] += 24 * HOUR - 60_000;
            giveCost(c.leader());
            said(helper, Charters.reforge(c.leader()), ERR + "charter_reforge_wait", "reforged inside the wait");
            helper.assertTrue(c.leader().getInventory().countItem(Items.GOLD_BLOCK) == 1, "the cost was taken");
            said(helper, Charters.reforge(c.a()), ERR + "leader_only", "a member reforged");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void reforgeNeedsALostCharter(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            forged(helper, c);
            giveCost(c.leader());
            said(helper, Charters.reforge(c.leader()), ERR + "charter_not_lost", "reforged a live Charter");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void reforgeGivesANewIdAndTheOldIsInert(GameTestHelper helper) {
        long[] now = {12_000_000_000L};
        PackIndex.overrideClock(() -> now[0]);
        Clan c = clan(helper);
        try {
            UUID before = forged(helper, c).get(SpeciesRegistries.CHARTER_ID.get());
            take(c.leader());
            lose(helper, c, now[0]);
            now[0] += 25 * HOUR;
            giveCost(c.leader());
            helper.assertTrue(Charters.reforge(c.leader()) == null, "refused");
            helper.assertTrue(!row(helper, c.leader()).id().equals(before), "the id is the old one");
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    // ---- dormant ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 200)
    public static void theCharterIsDormantWhileNotAClan(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            ItemStack stack = forged(helper, c).copy();
            take(c.leader());
            c.a().getInventory().add(stack);
            de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(c.leader()).leaveFaction(false);
            GroupSwitcher.refresh(c.leader());
            helper.assertTrue(!Groups_typeIsClan(c.a()), "the group is still a clan");
            CharterRow before = row(helper, c.leader());
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).equals(before) && held(c.a()) == 1 && droppedCharters(helper).isEmpty(), "the scan touched a dormant Charter");
            de.teamlapen.vampirism.api.VampirismAPI.factionPlayerHandler(c.leader()).setFactionAndLevel(de.teamlapen.vampirism.api.VReference.VAMPIRE_FACTION, 1);
            GroupSwitcher.refresh(c.leader());
            helper.assertTrue(Groups_typeIsClan(c.a()), "the clan did not come back");
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.HELD && row(helper, c.leader()).holder().equals(Optional.of(c.a().getUUID())), "a dormant Charter did not come back with the type: " + row(helper, c.leader()));
        } finally {
            done(helper, c.leader(), c.a(), c.b());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theScanDoesNotRunWithCharterOff(GameTestHelper helper) {
        Clan c = clan(helper);
        try {
            ItemStack stack = forged(helper, c).copy();
            take(c.leader());
            GroupsConfig.override("charter_enabled", false);
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(row(helper, c.leader()).state() == CharterState.HELD, "the scan moved a switched-off Charter: " + row(helper, c.leader()));
            c.a().getInventory().add(new ItemStack(SpeciesRegistries.CLAN_CHARTER.get()));
            scan(helper, c.leader(), c.a(), c.b());
            helper.assertTrue(held(c.a()) == 1, "the scan dusted something with Charters off");
            GroupsConfig.override("charter_enabled", null);
            c.a().getInventory().add(stack);
        } finally {
            done(helper, c.all());
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void clansOffMakesItInert(GameTestHelper helper) {
        Clan c = clan(helper);
        TalkingDuelist loner = human(helper, 5.5);
        online(c.leader(), c.a(), c.b(), loner);
        try {
            ItemStack stack = forged(helper, c).copy();
            take(c.leader());
            loner.getInventory().add(stack);
            GroupsConfig.override("clans_enabled", false);
            scan(helper, c.leader(), c.a(), c.b(), loner);
            helper.assertTrue(held(loner) == 1 && droppedCharters(helper).isEmpty(), "the scan acted with clans off");
            giveCost(c.leader());
            said(helper, Charters.forge(c.leader()), "supernaturalbloodlines.group.group_off.clan", "forged with clans off");
        } finally {
            done(helper, c.leader(), c.a(), c.b(), loner);
        }
        helper.succeed();
    }

    // ---- wiring ----

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void theItemAndItsComponentsAreRegistered(GameTestHelper helper) {
        helper.assertTrue(SpeciesRegistries.CLAN_CHARTER.get().getDefaultMaxStackSize() == 1, "the Charter stacks");
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NS, "charter_id"))
                && net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NS, "charter_group")), "a component is missing");
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(NS, "clan_charter")), "the item is not registered");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS, timeoutTicks = 100)
    public static void aPickupDenialNeverReachesAMemberOfAnotherCoven(GameTestHelper helper) {
        // a coven's members are outsiders to a clan's Charter like anybody else
        Clan c = clan(helper);
        TalkingDuelist w = witch(helper, 5.5);
        TalkingDuelist wm = human(helper, 6.5);
        online(c.leader(), c.a(), c.b(), w, wm);
        try {
            helper.assertTrue(join(w, wm), "setup");
            forged(helper, c);
            ItemEntity e = dropIt(helper, c.leader());
            e.playerTouch(w);
            e.playerTouch(wm);
            helper.assertTrue(!e.isRemoved() && held(w) + held(wm) == 0, "a coven took the Charter");
        } finally {
            done(helper, c.leader(), c.a(), c.b(), w, wm);
        }
        helper.succeed();
    }
}
