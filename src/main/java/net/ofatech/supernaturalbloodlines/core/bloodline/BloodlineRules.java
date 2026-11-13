package net.ofatech.supernaturalbloodlines.core.bloodline;

import net.ofatech.supernaturalbloodlines.core.bloodline.Outcome.Action;

/**
 * The transition matrix (SPECIES-RULES.md). Pure: no Minecraft classes. A refusal leaves the state
 * unchanged and carries a {@code supernaturalbloodlines.refusal.<reason>} key; an event that does not
 * apply is a silent no-op.
 */
public final class BloodlineRules {
    private BloodlineRules() {
    }

    public static Outcome apply(BloodlineState s, BloodlineEvent e, BloodlineContext c) {
        Outcome out = transition(s, e, c);
        return needsWerewolves(s, out, c) ? Outcome.refuse(s, "needs_werewolves") : out;
    }

    /** Becoming a hybrid type (or starting the transition that leads to one) is impossible without Werewolves. */
    private static boolean needsWerewolves(BloodlineState s, Outcome out, BloodlineContext c) {
        if (c.werewolvesLoaded() || out.refused()) {
            return false;
        }
        BloodlineState next = out.state();
        return isHybridType(next.species()) && !isHybridType(s.species()) || next.inTransition() && !s.inTransition();
    }

    private static Outcome transition(BloodlineState s, BloodlineEvent e, BloodlineContext c) {
        return switch (e) {
            case RITE_OF_IMMORTALITY -> immortality(s, c);
            case DIED_WITH_ORIGINAL_BLOOD -> diedWithOriginalBlood(s, c);
            case DRANK_DOPPELGANGER_BLOOD -> doppelgangerBlood(s, c);
            case DRANK_OTHER_BLOOD -> s.inTransition() && !s.unstable()
                    ? Outcome.of(s.toBuilder().unstable(true).build()) : unchanged(s);
            case TRANSITION_TIMEOUT -> s.inTransition()
                    ? Outcome.of(clearTransition(s.toBuilder()).build(), Action.KILL_PLAYER) : unchanged(s);
            case UNSTABLE_DEATH -> s.inTransition() ? Outcome.of(clearTransition(s.toBuilder()).build()) : unchanged(s);
            case JOINED_VAMPIRE -> joinedVampire(s, c);
            case RITE_OF_THE_MOON -> riteOfTheMoon(s, c);
            case RITE_OF_LINEAGE_AWAKEN -> awakenAsHybrid(s, c, false);
            case GENE_AWAKENED_AS_VAMPIRE -> awakenAsHybrid(s, c, true);
            case RITE_OF_CLEANSING_BLOOD -> cure(s, c, false);
            case RITE_OF_THE_SILVER_MOON -> cure(s, c, true);
            case DRANK_THE_CURE -> drankTheCure(s, c);
            case LEFT_BASE_FACTION -> leftBaseFaction(s, c);
            case ANCESTRAL_CALLING_WITCH -> ancestralCalling(s, c, Species.WITCH);
            case ANCESTRAL_CALLING_SIPHONER -> ancestralCalling(s, c, Species.SIPHONER);
            case DIED -> Outcome.of(s.toBuilder().siphonedPower(0).build());
            case RITE_OF_THE_FIVE -> riteOfTheFive(s, c);
            case CHOSEN_AS_POTENTIAL -> chosenAsPotential(s, c);
            case KILLED_PREY -> killedPrey(s, c);
            case KILLED_BY_PREY -> s.species() == Species.HUNTER_OF_THE_FIVE
                    ? Outcome.of(becomeNone(s, c).build()) : unchanged(s);
            case KILLED_MARKED_HUNTER -> isPrey(s, c) ? Outcome.of(s.toBuilder().hunterCurse(true).build()) : unchanged(s);
            case HUNTER_CURSE_LIFTED -> Outcome.of(s.toBuilder().hunterCurse(false).build());
        };
    }

    // ---- helpers ----

    private static Outcome unchanged(BloodlineState s) {
        return Outcome.of(s);
    }

    /** Switches species and stamps {@code turnedAt}; a no-op stamp when the species is the same. */
    private static BloodlineState.Builder to(BloodlineState s, Species species, BloodlineContext c) {
        BloodlineState.Builder b = s.toBuilder().species(species);
        if (species != s.species()) {
            b.turnedAt(species == Species.NONE ? 0 : c.gameTime());
        }
        if (!isHybridType(species)) {
            b.hybridCurseBound(false);
        }
        return b;
    }

    /** Back to no species: layer-specific data goes with it, and so does any transition in progress. */
    private static BloodlineState.Builder becomeNone(BloodlineState s, BloodlineContext c) {
        return clearTransition(to(s, Species.NONE, c).sire(null).witchLayer(false).markKills(0).lastVampireKill(0));
    }

    private static BloodlineState.Builder clearTransition(BloodlineState.Builder b) {
        return b.inTransition(false).unstable(false).transitionTicksLeft(0).pendingSire(null);
    }

    /** The Teen Wolf layer reset; the hidden gene is a human trait and stays (it must not be re-rolled). */
    private static WolfState clearedWolf(WolfState w) {
        return WolfState.NONE.toBuilder().geneRolled(w.geneRolled()).latentGene(w.latentGene()).build();
    }

    /** Leaving the werewolf side: no wolf rank and no half-done hybrid transition. */
    private static BloodlineState.Builder leaveWerewolf(BloodlineState s, BloodlineState.Builder b) {
        return clearTransition(b).wolf(clearedWolf(s.wolf()));
    }

    private static boolean isHybridType(Species sp) {
        return sp == Species.HYBRID || sp == Species.ORIGINAL_HYBRID || sp == Species.TRIBRID;
    }

    /** R81: vampires and werewolf types. Species wins over faction, so witches and siphoners are never prey. */
    private static boolean isPrey(BloodlineState s, BloodlineContext c) {
        return switch (s.species()) {
            case ORIGINAL, ORIGINAL_HYBRID, HYBRID, HERETIC, TRIBRID -> true;
            case NONE -> c.faction() == BaseFaction.VAMPIRE || c.faction() == BaseFaction.WEREWOLF;
            default -> false;
        };
    }

    // ---- Originals and the hybrid transition ----

    private static Outcome immortality(BloodlineState s, BloodlineContext c) {
        if (!c.humanLike()) {
            return Outcome.refuse(s, "not_human_like");
        }
        if (c.faction() != BaseFaction.NONE) {
            return Outcome.refuse(s, "has_faction");
        }
        if (s.species() != Species.NONE) {
            return Outcome.refuse(s, "has_species");
        }
        if (c.maxOriginals() > 0 && c.originalsInWorld() >= c.maxOriginals()) {
            return Outcome.refuse(s, "original_cap");
        }
        return Outcome.of(to(s, Species.ORIGINAL, c).build(), Action.JOIN_VAMPIRE);
    }

    private static Outcome diedWithOriginalBlood(BloodlineState s, BloodlineContext c) {
        if (c.faction() != BaseFaction.WEREWOLF || s.inTransition() || isHybridType(s.species())) {
            return unchanged(s); // R45: a second dose does nothing
        }
        if (s.species() == Species.SIPHONER) {
            return Outcome.refuse(s, "siphoner_hybrid");
        }
        return Outcome.of(s.toBuilder().inTransition(true).unstable(false).transitionTicksLeft(c.transitionTicks())
                .pendingSire(c.bloodSource()).build());
    }

    private static Outcome doppelgangerBlood(BloodlineState s, BloodlineContext c) {
        if (!s.inTransition() || s.unstable()) {
            return unchanged(s);
        }
        boolean witch = s.witchLayer() || s.species() == Species.WITCH;
        BloodlineState.Builder b = to(s, witch ? Species.TRIBRID : Species.HYBRID, c).sire(s.pendingSire());
        return Outcome.of(clearTransition(b).build());
    }

    private static Outcome joinedVampire(BloodlineState s, BloodlineContext c) {
        return switch (s.species()) {
            case WITCH -> Outcome.of(to(s, Species.NONE, c).witchLayer(false).build());
            case SIPHONER -> Outcome.of(to(s, Species.HERETIC, c).build());
            default -> unchanged(s);
        };
    }

    // ---- Werewolf gene and the Rite of the Moon ----

    private static Outcome riteOfTheMoon(BloodlineState s, BloodlineContext c) {
        boolean gene = s.wolf().latentGene();
        if (s.species() == Species.ORIGINAL) { // R35: an Original hybrid needs the gene
            return gene
                    ? Outcome.of(to(s, Species.ORIGINAL_HYBRID, c).hybridCurseBound(false).wolf(born(s.wolf())).build(),
                            Action.LEAVE_FACTION, Action.JOIN_WEREWOLF)
                    : Outcome.refuse(s, "no_gene");
        }
        if (isHybridType(s.species()) && s.hybridCurseBound()) {
            return Outcome.of(s.toBuilder().hybridCurseBound(false).build());
        }
        return Outcome.refuse(s, gene ? "not_original" : "no_gene");
    }

    /** R24 (gene awakened by a kill, needs the gene) and R12/R35 (the lineage rite gives it): a bound hybrid. */
    private static Outcome awakenAsHybrid(BloodlineState s, BloodlineContext c, boolean needsGene) {
        Species target;
        if (s.species() == Species.ORIGINAL) {
            target = Species.ORIGINAL_HYBRID;
        } else if (s.species() == Species.HERETIC) {
            return Outcome.refuse(s, "siphoner_hybrid");
        } else if (s.species() == Species.NONE && c.faction() == BaseFaction.VAMPIRE) {
            target = Species.HYBRID;
        } else {
            return unchanged(s); // humans are PackRules' business
        }
        if (needsGene && !s.wolf().latentGene()) {
            return Outcome.refuse(s, "no_gene");
        }
        // The lineage rite is what gives the gene.
        WolfState wolf = born(s.wolf().toBuilder().geneRolled(true).latentGene(true).build());
        return Outcome.of(to(s, target, c).hybridCurseBound(true).wolf(wolf).build(),
                Action.LEAVE_FACTION, Action.JOIN_WEREWOLF);
    }

    /** R90: a hybrid made by an awakening counts as born. An existing lineage is kept. */
    private static WolfState born(WolfState w) {
        return w.lineage() == WolfState.Lineage.NONE
                ? w.toBuilder().lineage(WolfState.Lineage.BORN).rank(WolfState.Rank.OMEGA).build() : w;
    }

    // ---- Cures ----

    private static boolean isOriginal(Species sp) {
        return sp == Species.ORIGINAL || sp == Species.ORIGINAL_HYBRID;
    }

    /** Cleansing Blood removes the vampire side (R22, R44, R71); the Silver Moon removes the werewolf side. */
    private static Outcome cure(BloodlineState s, BloodlineContext c, boolean silverMoon) {
        if (isOriginal(s.species())) {
            return Outcome.refuse(s, "original_cure"); // R34
        }
        return silverMoon ? silverMoon(s, c) : cleansingBlood(s, c);
    }

    /** Hybrid -> plain werewolf (NONE), tribrid -> witch, heretic -> siphoner; the sire bond goes with the layer. */
    private static Outcome cleansingBlood(BloodlineState s, BloodlineContext c) {
        return switch (s.species()) {
            case HYBRID -> Outcome.of(to(s, Species.NONE, c).sire(null).build());
            case TRIBRID -> Outcome.of(to(s, Species.WITCH, c).witchLayer(true).sire(null).build());
            case HERETIC -> Outcome.of(to(s, Species.SIPHONER, c).build(), Action.LEAVE_FACTION);
            case NONE, WITCH, SIPHONER -> c.faction() == BaseFaction.VAMPIRE
                    ? Outcome.of(s, Action.LEAVE_FACTION) : unchanged(s);
            default -> unchanged(s);
        };
    }

    /** Every success clears the wolf layer (the gene stays) and any transition. Hybrid-types become plain vampires. */
    private static Outcome silverMoon(BloodlineState s, BloodlineContext c) {
        return switch (s.species()) {
            case HYBRID -> Outcome.of(leaveWerewolf(s, to(s, Species.NONE, c)).build(), Action.LEAVE_FACTION, Action.JOIN_VAMPIRE);
            case TRIBRID -> Outcome.of(leaveWerewolf(s, to(s, Species.NONE, c).witchLayer(false)).build(), // R61
                    Action.LEAVE_FACTION, Action.JOIN_VAMPIRE);
            case NONE, WITCH, SIPHONER -> c.faction() == BaseFaction.WEREWOLF
                    ? Outcome.of(leaveWerewolf(s, s.toBuilder()).build(), Action.LEAVE_FACTION) : unchanged(s);
            default -> unchanged(s);
        };
    }

    private static Outcome drankTheCure(BloodlineState s, BloodlineContext c) {
        BloodlineState cured = becomeNone(s, c).hunterCurse(false).siphonedPower(0).thirst(20)
                .wolf(clearedWolf(s.wolf())).build();
        boolean leaves = c.faction() == BaseFaction.VAMPIRE || c.faction() == BaseFaction.WEREWOLF
                || switch (s.species()) {
                    case ORIGINAL, ORIGINAL_HYBRID, HYBRID, TRIBRID, HERETIC -> true;
                    default -> false;
                };
        return leaves ? Outcome.of(cured, Action.LEAVE_FACTION) : Outcome.of(cured);
    }

    /**
     * Any other way out of the base faction (command, Vampirism UI): the layer goes to match. {@code c.faction()}
     * is the faction being left; it is what tells a plain werewolf (or witch-werewolf) from a plain vampire.
     * A werewolf-based state also loses its wolf layer and any transition.
     */
    private static Outcome leftBaseFaction(BloodlineState s, BloodlineContext c) {
        return switch (s.species()) {
            case HERETIC -> Outcome.of(to(s, Species.SIPHONER, c).build());
            case TRIBRID -> Outcome.of(leaveWerewolf(s, to(s, Species.WITCH, c).witchLayer(true).sire(null)).build());
            case ORIGINAL, POTENTIAL, HUNTER_OF_THE_FIVE -> Outcome.of(becomeNone(s, c).build());
            case ORIGINAL_HYBRID, HYBRID -> Outcome.of(leaveWerewolf(s, becomeNone(s, c)).build());
            case NONE, WITCH, SIPHONER -> c.faction() == BaseFaction.WEREWOLF
                    ? Outcome.of(leaveWerewolf(s, s.toBuilder()).build()) : unchanged(s);
        };
    }

    // ---- Witches and siphoners ----

    private static Outcome ancestralCalling(BloodlineState s, BloodlineContext c, Species target) {
        if (!c.humanLike()) {
            return Outcome.refuse(s, "not_human_like");
        }
        if (c.faction() == BaseFaction.VAMPIRE) {
            return Outcome.refuse(s, "vampire_no_magic");
        }
        if (c.faction() == BaseFaction.HUNTER) {
            return Outcome.refuse(s, "has_faction");
        }
        if (s.species() != Species.NONE) {
            return Outcome.refuse(s, "has_species");
        }
        return Outcome.of(to(s, target, c).witchLayer(target == Species.WITCH).build());
    }

    // ---- Hunters of the Five ----

    private static Outcome riteOfTheFive(BloodlineState s, BloodlineContext c) {
        if (!c.humanLike()) {
            return Outcome.refuse(s, "not_human_like");
        }
        if (c.faction() != BaseFaction.HUNTER) {
            return Outcome.refuse(s, "not_hunter");
        }
        if (s.species() != Species.NONE && s.species() != Species.POTENTIAL) {
            return Outcome.refuse(s, "has_species");
        }
        if (c.fiveCount() >= c.maxFive()) {
            return Outcome.refuse(s, "five_full");
        }
        // The instinct timer (R84) starts now, otherwise a fresh mark would start out weakened.
        return Outcome.of(to(s, Species.HUNTER_OF_THE_FIVE, c).markKills(0).lastVampireKill(c.gameTime()).build());
    }

    private static Outcome chosenAsPotential(BloodlineState s, BloodlineContext c) {
        if (!c.humanLike()) {
            return Outcome.refuse(s, "not_human_like");
        }
        if (c.faction() != BaseFaction.HUNTER) {
            return Outcome.refuse(s, "not_hunter");
        }
        return s.species() == Species.NONE ? Outcome.of(to(s, Species.POTENTIAL, c).build()) : unchanged(s);
    }

    private static Outcome killedPrey(BloodlineState s, BloodlineContext c) {
        return switch (s.species()) {
            case HUNTER_OF_THE_FIVE -> Outcome.of(s.toBuilder().markKills(s.markKills() + 1).lastVampireKill(c.gameTime()).build());
            case POTENTIAL -> Outcome.of(to(s, Species.HUNTER_OF_THE_FIVE, c).markKills(1).lastVampireKill(c.gameTime()).build());
            default -> unchanged(s);
        };
    }
}
