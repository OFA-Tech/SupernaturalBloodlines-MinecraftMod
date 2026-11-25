package net.ofatech.supernaturalbloodlines.parts.species.group.clan;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.ofatech.supernaturalbloodlines.core.group.clan.CharterCost;
import net.ofatech.supernaturalbloodlines.core.group.clan.CharterRules;
import net.ofatech.supernaturalbloodlines.core.group.clan.CharterState;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesRegistries;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupIndex.CharterRow;
import net.ofatech.supernaturalbloodlines.parts.species.group.GroupsConfig;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackIndex;
import net.ofatech.supernaturalbloodlines.parts.species.pack.PackMembers;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;
import org.jetbrains.annotations.Nullable;

/** Forging and reforging the Charter, and the few facts the scan and the guard share about a stack and its row. The server row is the truth; the stack is a token. */
public final class Charters {
    private Charters() {
    }

    public static ItemStack stack(UUID id, UUID group) {
        ItemStack s = new ItemStack(SpeciesRegistries.CLAN_CHARTER.get());
        s.set(SpeciesRegistries.CHARTER_ID.get(), id);
        s.set(SpeciesRegistries.CHARTER_GROUP.get(), group);
        return s;
    }

    public static boolean isCharter(ItemStack s) {
        return !s.isEmpty() && s.is(SpeciesRegistries.CLAN_CHARTER.get());
    }

    /** Whether {@code player} is the leader or a member of the clan with this group id (whatever its type is now). */
    public static boolean isMember(MinecraftServer server, UUID player, UUID group) {
        ServerLevel level = server.overworld();
        PackIndex packs = PackIndex.get(level);
        if (!packs.inPack(player)) {
            return false;
        }
        return GroupIndex.get(level).existingGroupOf(packs.leaderOf(player)).filter(group::equals).isPresent();
    }

    /** The row this stack is the token of, when it is: same group, same id. */
    public static Optional<CharterRow> rowOf(GroupIndex groups, ItemStack stack) {
        UUID id = stack.get(SpeciesRegistries.CHARTER_ID.get());
        UUID group = stack.get(SpeciesRegistries.CHARTER_GROUP.get());
        if (id == null || group == null) {
            return Optional.empty();
        }
        return groups.charterOf(group).filter(r -> CharterRules.valid(id, r.id(), group, group));
    }

    public static Optional<UUID> groupOfRow(GroupIndex groups, UUID entity) {
        return groups.charters().entrySet().stream().filter(e -> e.getValue().entity().filter(entity::equals).isPresent()).map(java.util.Map.Entry::getKey).findFirst();
    }

    public static void tellClan(MinecraftServer server, UUID leader, String key) {
        PackIndex packs = PackIndex.get(server.overworld());
        PackMembers.tell(server, leader, key, "");
        for (UUID id : packs.betasOf(leader)) {
            PackMembers.tell(server, id, key, "");
        }
    }

    private static int refreshes;

    /** How many times the clan was pushed (a test reads it to see that a quiet scan pushes nothing). */
    public static int refreshes() {
        return refreshes;
    }

    public static void refresh(MinecraftServer server, UUID leader) {
        refreshes++;
        PackIndex packs = PackIndex.get(server.overworld());
        List<UUID> all = new ArrayList<>(packs.betasOf(leader));
        all.add(leader);
        for (UUID id : all) {
            ServerPlayer online = server.getPlayerList().getPlayer(id);
            if (online != null) {
                HubRequests.changed(online);
            }
        }
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    private static boolean has(ServerPlayer p, List<CharterCost.Entry> cost) {
        return cost.stream().allMatch(e -> p.getInventory().countItem(item(e.item())) >= e.count());
    }

    private static void pay(ServerPlayer p, List<CharterCost.Entry> cost) {
        for (CharterCost.Entry e : cost) {
            int left = e.count();
            Item it = item(e.item());
            for (int i = 0; i < p.getInventory().getContainerSize() && left > 0; i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (s.is(it)) {
                    int take = Math.min(left, s.getCount());
                    s.shrink(take);
                    left -= take;
                }
            }
        }
    }

    private static Component costWords(List<CharterCost.Entry> cost) {
        return Component.literal(cost.stream().map(e -> e.count() + " " + item(e.item()).getDescription().getString()).collect(java.util.stream.Collectors.joining(", ")));
    }

    private static void give(ServerPlayer p, ItemStack stack) {
        if (!p.getInventory().add(stack)) {
            ItemEntity drop = new ItemEntity(p.serverLevel(), p.getX(), p.getY(), p.getZ(), stack);
            drop.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            p.serverLevel().addFreshEntity(drop);
        }
    }

    @Nullable
    private static Component common(ServerPlayer p) {
        Component gate = ClanDiscipline.gate(p);
        if (gate != null) {
            return gate;
        }
        ClanScope.Clan clan = ClanScope.of(p.serverLevel(), p.getUUID());
        if (!p.getUUID().equals(clan.leader())) {
            return ClanText.error("leader_only");
        }
        return GroupsConfig.charterEnabled() ? null : ClanText.error("charter_off");
    }

    private static String code(String rule) {
        return "charter_" + switch (rule) {
            case "lost_use_reforge" -> "use_reforge";
            default -> rule;
        };
    }

    @Nullable
    public static Component forge(ServerPlayer p) {
        Component no = common(p);
        if (no != null) {
            return no;
        }
        ServerLevel level = p.serverLevel();
        ClanScope.Clan clan = ClanScope.of(level, p.getUUID());
        GroupIndex groups = GroupIndex.get(level);
        int members = 1 + PackIndex.get(level).betasOf(clan.leader()).size();
        Optional<String> refusal = CharterRules.mayForge(members, groups.charterOf(clan.group()).map(CharterRow::state).orElse(null), GroupsConfig.charterMinMembers());
        if (refusal.isPresent()) {
            return ClanText.error(code(refusal.get()));
        }
        List<CharterCost.Entry> cost = GroupsConfig.charterForgeCost();
        if (!has(p, cost)) {
            return ClanText.error("charter_cost", costWords(cost));
        }
        pay(p, cost);
        UUID id = UUID.randomUUID();
        long now = PackIndex.now();
        groups.setCharter(clan.group(), new CharterRow(id, Optional.of(p.getUUID()), CharterState.HELD, now, now, Optional.empty(), 0));
        give(p, stack(id, clan.group()));
        p.sendSystemMessage(ClanText.of("charter_forged"));
        refresh(p.server, clan.leader());
        return null;
    }

    @Nullable
    public static Component reforge(ServerPlayer p) {
        Component no = common(p);
        if (no != null) {
            return no;
        }
        ServerLevel level = p.serverLevel();
        ClanScope.Clan clan = ClanScope.of(level, p.getUUID());
        GroupIndex groups = GroupIndex.get(level);
        Optional<CharterRow> row = groups.charterOf(clan.group());
        if (row.isEmpty()) {
            return ClanText.error("charter_none");
        }
        Optional<String> refusal = CharterRules.mayReforge(row.get().state(), row.get().lostAt(), PackIndex.now());
        if (refusal.isPresent()) {
            return ClanText.error(code(refusal.get()));
        }
        List<CharterCost.Entry> cost = GroupsConfig.charterForgeCost();
        if (!has(p, cost)) {
            return ClanText.error("charter_cost", costWords(cost));
        }
        pay(p, cost);
        UUID id = UUID.randomUUID();
        long now = PackIndex.now();
        groups.setCharter(clan.group(), new CharterRow(id, Optional.of(p.getUUID()), CharterState.HELD, now, now, Optional.empty(), 0));
        give(p, stack(id, clan.group()));
        p.sendSystemMessage(ClanText.of("charter_reforged"));
        refresh(p.server, clan.leader());
        return null;
    }
}
