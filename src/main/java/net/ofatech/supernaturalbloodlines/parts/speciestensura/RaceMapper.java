package net.ofatech.supernaturalbloodlines.parts.speciestensura;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.entity.factions.IPlayableFaction;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.vampirismtensura.FactionRaceRules;

/**
 * Plan 23 Task 10: maps a Tensura reincarnation pick to a supernatural faction. Only called when the
 * reincarnation menu is open (a player's own pick); admin commands and mod writes never reach here.
 */
public final class RaceMapper {
 private static final Set<String> BLOODFIENDS = Set.of(
 "ascension:fledgling_bloodfiend", "ascension:kindred_bloodfiend", "ascension:blood_noble",
 "ascension:elder_bloodfiend", "ascension:progenitor_bloodfiend");
 private static final String DIREWOLF = "mysticism:direwolf";
 private static final Map<String, Integer> DEFAULT_FLOORS = Map.of(
 "ascension:fledgling_bloodfiend", 1,
 "ascension:kindred_bloodfiend", 4,
 "ascension:blood_noble", 7,
 "ascension:elder_bloodfiend", 10,
 "ascension:progenitor_bloodfiend", -1);

 private RaceMapper() {}

 public static void applyPick(ServerPlayer p, String raceId) {
 if (p.level().isClientSide || BloodlineAccess.get(p).species() != Species.NONE) {
 return;
 }
 if (BLOODFIENDS.contains(raceId) && RaceMapperConfig.bloodfiendToVampire()) {
 joinVampire(p, raceId);
 } else if (isWolf(raceId) && ModList.get().isLoaded("werewolves")) {
 joinWerewolf(p);
 }
 }

 private static boolean isWolf(String raceId) {
 if (DIREWOLF.equals(raceId)) {
 return RaceMapperConfig.direwolfToWerewolf();
 }
 if (raceId.startsWith("mysticism:") && raceId.endsWith("_spirit_wolf")) {
 return RaceMapperConfig.spiritWolfToWerewolf();
 }
 return false;
 }

 private static void joinVampire(ServerPlayer p, String raceId) {
 IFactionPlayerHandler handler = VampirismAPI.factionPlayerHandler(p);
 if (handler.getCurrentFaction() == VReference.VAMPIRE_FACTION || !handler.canJoin(VReference.VAMPIRE_FACTION)) {
 return;
 }
 int floor = parseFloors().getOrDefault(raceId, 1);
 int max = VReference.VAMPIRE_FACTION.getHighestReachableLevel();
 int level = floor < 0 ? max : Math.min(floor, max);
 handler.setFactionAndLevel(VReference.VAMPIRE_FACTION, Math.max(1, level));
 }

 private static Map<String, Integer> parseFloors() {
 Map<String, Integer> out = new HashMap<>(DEFAULT_FLOORS);
 for (String entry : RaceMapperConfig.bloodfiendLevelFloors()) {
 int eq = entry.indexOf('=');
 if (eq <= 0) continue;
 String race = entry.substring(0, eq).trim();
 try {
 int level = Integer.parseInt(entry.substring(eq + 1).trim());
 out.put(race, level);
 } catch (NumberFormatException ignored) {
 SupernaturalBloodlines.LOGGER.warn("race_mapper: invalid bloodfiend_level_floors entry '{}'", entry);
 }
 }
 return out;
 }

 private static void joinWerewolf(ServerPlayer p) {
 IPlayableFaction<?> werewolves = null;
 for (IPlayableFaction<?> f : VampirismAPI.factionRegistry().getPlayableFactions()) {
 if (FactionRaceRules.WEREWOLF_FACTION_ID.equals(f.getID())) {
 werewolves = f;
 break;
 }
 }
 if (werewolves == null) return;
 IFactionPlayerHandler handler = VampirismAPI.factionPlayerHandler(p);
 if (handler.getCurrentFaction() != null || !handler.canJoin(werewolves)) {
 return;
 }
 handler.setFactionAndLevel(werewolves, 1);
 }
}