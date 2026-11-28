package net.ofatech.supernaturalbloodlines.parts.species.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.core.group.GroupType;
import net.ofatech.supernaturalbloodlines.core.group.GroupTypes.Leader;
import org.junit.jupiter.api.Test;

/** Plan 20e: multi-type computation from leader kinds (design §1.2) and per-type fit rules. */
class MultiTypesTest {
 private static Set<GroupType> of(Species s, boolean vampire, boolean werewolf, boolean hunter) {
 return MultiTypes.of(new Leader(s, vampire, werewolf, hunter, true));
 }

 @Test
 void hybridGetsPackAndClan() {
 assertEquals(Set.of(GroupType.PACK, GroupType.CLAN), of(Species.HYBRID, true, false, false));
 }

 @Test
 void originalHybridGetsPackAndClan() {
 assertEquals(Set.of(GroupType.PACK, GroupType.CLAN), of(Species.ORIGINAL_HYBRID, true, false, false));
 }

 @Test
 void tribridGetsPackClanAndCoven() {
 assertEquals(Set.of(GroupType.PACK, GroupType.CLAN, GroupType.COVEN), of(Species.TRIBRID, true, false, false));
 }

 @Test
 void vampireSiphonerGetsClanAndCoven() {
 assertEquals(Set.of(GroupType.CLAN, GroupType.COVEN), of(Species.SIPHONER, true, false, false));
 }

 @Test
 void hereticVampireGetsClanAndCoven() {
 assertEquals(Set.of(GroupType.CLAN, GroupType.COVEN), of(Species.HERETIC, true, false, false));
 }

 @Test
 void witchWerewolfGetsPackAndCoven() {
 assertEquals(Set.of(GroupType.PACK, GroupType.COVEN), of(Species.WITCH, false, true, false));
 }

 @Test
 void witchHunterGetsCovenAndBrotherhood() {
 assertEquals(Set.of(GroupType.COVEN, GroupType.BROTHERHOOD), of(Species.WITCH, false, false, true));
 }

 @Test
 void plainHumanGetsBrotherhood() {
 assertEquals(Set.of(GroupType.BROTHERHOOD), of(Species.NONE, false, false, false));
 }

 @Test
 void hunterGetsBrotherhood() {
 assertEquals(Set.of(GroupType.BROTHERHOOD), of(Species.NONE, false, false, true));
 }

 @Test
 void vampireAloneGetsClan() {
 assertEquals(Set.of(GroupType.CLAN), of(Species.NONE, true, false, false));
 }

 @Test
 void originalVampireGetsClan() {
 assertEquals(Set.of(GroupType.CLAN), of(Species.ORIGINAL, true, false, false));
 }

 @Test
 void augustineGetsClan() {
 assertEquals(Set.of(GroupType.CLAN), of(Species.AUGUSTINE, true, false, false));
 }

 @Test
 void magicUserAloneGetsCoven() {
 assertEquals(Set.of(GroupType.COVEN), of(Species.WITCH, false, false, false));
 assertEquals(Set.of(GroupType.COVEN), of(Species.SIPHONER, false, false, false));
 assertEquals(Set.of(GroupType.COVEN), of(Species.HERETIC, false, false, false));
 }

 @Test
 void werewolfAloneGetsPack() {
 assertEquals(Set.of(GroupType.PACK), of(Species.NONE, false, true, false));
 }

 @Test
 void mainTypeIsFirstInCanonicalOrder() {
 assertEquals(GroupType.PACK, MultiTypes.mainType(Set.of(GroupType.PACK, GroupType.CLAN)));
 assertEquals(GroupType.PACK, MultiTypes.mainType(Set.of(GroupType.PACK, GroupType.COVEN)));
 assertEquals(GroupType.PACK, MultiTypes.mainType(Set.of(GroupType.PACK, GroupType.BROTHERHOOD)));
 assertEquals(GroupType.CLAN, MultiTypes.mainType(Set.of(GroupType.CLAN, GroupType.COVEN)));
 assertEquals(GroupType.CLAN, MultiTypes.mainType(Set.of(GroupType.CLAN, GroupType.BROTHERHOOD)));
 assertEquals(GroupType.COVEN, MultiTypes.mainType(Set.of(GroupType.COVEN, GroupType.BROTHERHOOD)));
 }

 @Test
 void mainTypeOfSingleElementSet() {
 assertEquals(GroupType.PACK, MultiTypes.mainType(Set.of(GroupType.PACK)));
 assertEquals(GroupType.CLAN, MultiTypes.mainType(Set.of(GroupType.CLAN)));
 assertEquals(GroupType.COVEN, MultiTypes.mainType(Set.of(GroupType.COVEN)));
 assertEquals(GroupType.BROTHERHOOD, MultiTypes.mainType(Set.of(GroupType.BROTHERHOOD)));
 }

 @Test
 void emptySetGivesPackFallback() {
 assertEquals(GroupType.PACK, MultiTypes.mainType(Set.of()));
 }

 @Test
 void packFitsEveryone() {
 assertTrue(MultiTypes.fits(GroupType.PACK, new MultiTypes.Fit(false, Species.NONE, false, false, true)));
 assertTrue(MultiTypes.fits(GroupType.PACK, new MultiTypes.Fit(true, Species.WITCH, true, true, true)));
 }

 @Test
 void clanFitsVampireLike() {
 assertTrue(MultiTypes.fits(GroupType.CLAN, new MultiTypes.Fit(false, Species.NONE, true, false, true)));
 assertTrue(MultiTypes.fits(GroupType.CLAN, new MultiTypes.Fit(false, Species.NONE, false, false, true)));
 assertTrue(MultiTypes.fits(GroupType.CLAN, new MultiTypes.Fit(false, Species.ORIGINAL, false, false, true)));
 assertTrue(MultiTypes.fits(GroupType.CLAN, new MultiTypes.Fit(false, Species.AUGUSTINE, false, false, true)));
 assertTrue(MultiTypes.fits(GroupType.CLAN, new MultiTypes.Fit(false, Species.SIPHONER, false, false, true)));
 assertFalse(MultiTypes.fits(GroupType.CLAN, new MultiTypes.Fit(false, Species.WITCH, false, false, true)));
 assertFalse(MultiTypes.fits(GroupType.CLAN, new MultiTypes.Fit(false, Species.HYBRID, false, false, true)));
 }

 @Test
 void covenFitsWitchLike() {
 assertTrue(MultiTypes.fits(GroupType.COVEN, new MultiTypes.Fit(false, Species.WITCH, false, false, true)));
 assertTrue(MultiTypes.fits(GroupType.COVEN, new MultiTypes.Fit(false, Species.SIPHONER, false, false, true)));
 assertTrue(MultiTypes.fits(GroupType.COVEN, new MultiTypes.Fit(false, Species.HERETIC, false, false, true)));
 assertFalse(MultiTypes.fits(GroupType.COVEN, new MultiTypes.Fit(false, Species.NONE, false, false, true)));
 assertFalse(MultiTypes.fits(GroupType.COVEN, new MultiTypes.Fit(false, Species.HYBRID, false, false, true)));
 }

 @Test
 void brotherhoodFitsHumanOrHunter() {
 assertTrue(MultiTypes.fits(GroupType.BROTHERHOOD, new MultiTypes.Fit(true, Species.NONE, false, false, true)));
 assertTrue(MultiTypes.fits(GroupType.BROTHERHOOD, new MultiTypes.Fit(false, Species.NONE, false, false, true)));
 assertFalse(MultiTypes.fits(GroupType.BROTHERHOOD, new MultiTypes.Fit(false, Species.WITCH, false, false, true)));
 assertFalse(MultiTypes.fits(GroupType.BROTHERHOOD, new MultiTypes.Fit(false, Species.NONE, true, false, true)));
 assertFalse(MultiTypes.fits(GroupType.BROTHERHOOD, new MultiTypes.Fit(false, Species.NONE, false, true, true)));
 }

 @Test
 void witchHunterFitsBothCovenAndBrotherhood() {
 MultiTypes.Fit facts = new MultiTypes.Fit(true, Species.WITCH, false, false, true);
 assertTrue(MultiTypes.fits(GroupType.COVEN, facts));
 assertTrue(MultiTypes.fits(GroupType.BROTHERHOOD, facts));
 }
}