package net.ofatech.supernaturalbloodlines.parts.speciestensura.gametest;

import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.RaceAPI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.speciestensura.HumanLikeLine;

/**
 * Plan 23 Task 0 P6 and P12: what Tensura's human-like tag and evolution graph really hold on the running jars, and the registered race ids of
 * the addons. Each proof logs a {@code PLAN23 Pn} line. A difference between the filtered closure and {@link HumanLikeLine#EVOLVED} is a finding.
 */
@PrefixGameTestTemplate(false)
public final class HumanLikeProbeGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final Set<String> VAMPIRE_LINE = Set.of("tensura:vampire", "tensura:vampire_overcomer", "tensura:vampire_lord", "tensura:divine_vampire");
    private static final Set<String> BASE = Set.of("tensura:human", "tensura:dwarf", "tensura:elf", "tensura:merfolk", "tensura:beastfolk");
    /**
     * Finding with the pack's addons: Ascension's kitsune line evolves out of a human-like line, so a dynamic closure takes it in. Owner decision
     * 2026-11-27: the kitsune line stays human-like by default (an admin can remove it per world, Task 19).
     */
    private static final Set<String> KITSUNE = Set.of("ascension:kitsune", "ascension:three_tail_fox", "ascension:six_tail_fox", "ascension:nine_tail_fox", "ascension:divine_kitsune");
    private static final Set<String> HORIZONS = Set.of("tensurahorizons:ascendant_elf", "tensurahorizons:runeforged_dwarf", "tensurahorizons:tide_sovereign");

    private static Set<String> registered(Set<String> ids) {
        Set<String> out = new TreeSet<>();
        for (String id : ids) {
            if (RaceAPI.getRaceRegistry().get(ResourceLocation.parse(id)) != null) {
                out.add(id);
            }
        }
        return out;
    }

    private HumanLikeProbeGameTests() {
    }

    private static void note(String proof, String text) {
        SupernaturalBloodlines.LOGGER.info("PLAN23 {} {}", proof, text);
    }

    private static TagKey<ManasRace> tag(String path) {
        return TagKey.create(RaceAPI.getRaceRegistryKey(), ResourceLocation.fromNamespaceAndPath("tensura", path));
    }

    private static Set<String> tagged(TagKey<ManasRace> tag) {
        Set<String> ids = new TreeSet<>();
        for (ResourceLocation id : RaceAPI.getRaceRegistry().getIds()) {
            if (RaceAPI.getRaceRegistry().get(id).createDefaultInstance().is(tag)) {
                ids.add(id.toString());
            }
        }
        return ids;
    }

    private static List<String> next(String id, LivingEntity entity) {
        ManasRace race = RaceAPI.getRaceRegistry().get(ResourceLocation.parse(id));
        return race.getNextEvolutions(race.createDefaultInstance(), entity).stream().map(r -> Objects.requireNonNull(RaceAPI.getRaceRegistry().getId(r)).toString()).sorted().toList();
    }

    /** Every race reachable from the tag by next evolutions (the tag included), read with the given entity. */
    private static Set<String> closure(Set<String> start, Function<String, List<String>> nextOf, List<String> thrown) {
        Set<String> seen = new TreeSet<>(start);
        Deque<String> todo = new ArrayDeque<>(start);
        while (!todo.isEmpty()) {
            String id = todo.pop();
            List<String> following;
            try {
                following = nextOf.apply(id);
            } catch (RuntimeException e) {
                thrown.add(id + ": " + e);
                continue;
            }
            for (String n : following) {
                if (seen.add(n)) {
                    todo.push(n);
                }
            }
        }
        return seen;
    }

    private static Set<String> humanLikeTag() {
        Set<String> ids = tagged(tag("human_like"));
        ids.removeIf(id -> id.startsWith(NS + ":")); // our apex races are tagged by us
        return ids;
    }

    /**
     * The closure minus undead, daemon and the vampire line. Finding of this proof: the plan's filter also removed {@code #tensura:no_blood}, but that tag
     * holds {@code #tensura:spiritual}, and the saint and divine tiers of the five lines are spiritual, so it threw out ten races of today's list.
     */
    private static Set<String> filtered(Set<String> closure) {
        Set<String> out = new TreeSet<>(closure);
        out.removeAll(tagged(tag("undead")));
        out.removeAll(tagged(tag("daemon")));
        out.removeAll(VAMPIRE_LINE);
        out.removeIf(id -> id.startsWith(NS + ":"));
        return out;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theNoBloodTagWouldDropTheSaintAndDivineTiers(GameTestHelper helper) {
        Set<String> closure = filtered(closure(humanLikeTag(), id -> next(id, null), new ArrayList<>()));
        Set<String> noBlood = new TreeSet<>(closure);
        noBlood.retainAll(tagged(tag("no_blood")));
        Set<String> spiritual = new TreeSet<>(closure);
        spiritual.retainAll(tagged(tag("spiritual")));
        note("P6", "human-like races in #no_blood=" + noBlood + " in #spiritual=" + spiritual);
        Set<String> expected = new TreeSet<>(Set.of("tensura:human_saint", "tensura:divine_human", "tensura:dwarf_saint", "tensura:divine_dwarf", "tensura:elf_saint",
                "tensura:divine_elf", "tensura:merfolk_saint", "tensura:divine_fish", "tensura:spirit_beast", "tensura:divine_beast"));
        expected.addAll(registered(HORIZONS));
        expected.addAll(registered(Set.of("ascension:six_tail_fox", "ascension:nine_tail_fox")));
        helper.assertTrue(noBlood.equals(expected), "the no_blood members of the line are " + noBlood);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theTagHasTheFiveBaseRaces(GameTestHelper helper) {
        Set<String> tag = humanLikeTag();
        note("P6", "human_like tag (without ours): " + tag);
        helper.assertTrue(tag.equals(BASE), "the human_like tag is " + tag);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theEvolutionGraphIsReadableWithNoEntityAndWithAFakePlayer(GameTestHelper helper) {
        ServerPlayer fake = TestPlayers.create(helper);
        List<String> thrownNull = new ArrayList<>();
        List<String> thrownFake = new ArrayList<>();
        Set<String> withNull = closure(humanLikeTag(), id -> next(id, null), thrownNull);
        Set<String> withFake = closure(humanLikeTag(), id -> next(id, fake), thrownFake);
        List<String> differ = new ArrayList<>();
        for (String id : withNull) {
            try {
                if (!next(id, null).equals(next(id, fake))) {
                    differ.add(id + " null=" + next(id, null) + " player=" + next(id, fake));
                }
            } catch (RuntimeException e) {
                differ.add(id + " threw " + e);
            }
        }
        note("P6", "closure null-entity=" + withNull.size() + " fake-player=" + withFake.size() + " thrownNull=" + thrownNull + " thrownFake=" + thrownFake + " differ=" + differ);
        helper.assertTrue(withNull.equals(withFake), "the closure depends on the entity: " + withNull + " vs " + withFake);
        helper.assertTrue(thrownNull.isEmpty() && thrownFake.isEmpty(), "a race threw: " + thrownNull + " " + thrownFake);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aNaiveClosureReachesTheVampireLine(GameTestHelper helper) {
        Set<String> closure = closure(humanLikeTag(), id -> next(id, null), new ArrayList<>());
        Set<String> reached = new TreeSet<>(VAMPIRE_LINE);
        reached.retainAll(closure);
        note("P6", "naive closure " + closure);
        helper.assertTrue(reached.equals(VAMPIRE_LINE), "the naive closure reaches only " + reached + " of the vampire line");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theFilteredClosureEqualsTheHardCodedEvolvedList(GameTestHelper helper) {
        Set<String> evolved = filtered(closure(humanLikeTag(), id -> next(id, null), new ArrayList<>()));
        evolved.removeAll(BASE);
        Set<String> onlyInClosure = new TreeSet<>(evolved);
        onlyInClosure.removeAll(HumanLikeLine.EVOLVED);
        Set<String> onlyInList = new TreeSet<>(HumanLikeLine.EVOLVED);
        onlyInList.removeAll(evolved);
        Set<String> listNotRegistered = new TreeSet<>();
        for (String id : onlyInList) {
            if (RaceAPI.getRaceRegistry().get(ResourceLocation.parse(id)) == null) {
                listNotRegistered.add(id);
            }
        }
        note("P6", "filtered closure " + evolved + " onlyInClosure=" + onlyInClosure + " onlyInList=" + onlyInList + " (not registered here: " + listNotRegistered + ")");
        // the list also names Horizons races, which a run without Horizons cannot reach: only a registered race that is missing counts
        Set<String> missing = new TreeSet<>(onlyInList);
        missing.removeAll(listNotRegistered);
        helper.assertTrue(onlyInClosure.equals(registered(KITSUNE)) && missing.isEmpty(), "closure and list differ: onlyInClosure=" + onlyInClosure + " missing=" + missing);
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theSetIsReadableOnTheRunningServer(GameTestHelper helper) {
        int races = RaceAPI.getRaceRegistry().getIds().size();
        Set<String> tag = humanLikeTag();
        note("P6", "registry races=" + races + " human_like=" + tag.size() + " (read during the game tests, after server start)");
        helper.assertTrue(races > 0 && !tag.isEmpty(), "registry or tag empty on a running server");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void addonRacesAgainstTheClosure(GameTestHelper helper) {
        Set<String> closure = filtered(closure(humanLikeTag(), id -> next(id, null), new ArrayList<>()));
        Set<String> addonHumanLike = new TreeSet<>();
        Set<String> addons = new TreeSet<>();
        for (ResourceLocation id : RaceAPI.getRaceRegistry().getIds()) {
            String ns = id.getNamespace();
            if (!ns.equals("tensura") && !ns.equals(NS)) {
                addons.add(id.toString());
                if (closure.contains(id.toString())) {
                    addonHumanLike.add(id.toString());
                }
            }
        }
        note("P6", "addon races=" + addons.size() + " in the filtered closure=" + addonHumanLike);
        Set<String> horizons = HORIZONS;
        for (String h : horizons) {
            if (addons.contains(h)) {
                helper.assertTrue(closure.contains(h), h + " extends a human-like line but is not in the closure");
            }
        }
        Set<String> others = new TreeSet<>(addonHumanLike);
        others.removeAll(horizons);
        helper.assertTrue(others.equals(registered(KITSUNE)), "the addon races in the human-like closure other than Horizons' are " + others);
        helper.succeed();
    }

    /** The ids Task 13's race table is built from (P12): Mysticism's wolves and Ascension's Bloodfiends, read from the registry, not from lang names. */
    static final Set<String> WOLVES = Set.of("direwolf", "divine_wolf",
            "antumbra_spirit_wolf", "gale_spirit_wolf", "glacier_spirit_wolf", "magic_spirit_wolf", "molten_spirit_wolf", "mountain_spirit_wolf",
            "ocean_spirit_wolf", "penumbra_spirit_wolf", "planet_spirit_wolf", "storm_spirit_wolf", "string_spirit_wolf", "volcanic_spirit_wolf",
            "black_fang", "blue_fang", "brown_fang", "green_fang", "purple_fang", "red_fang", "light_fang", "darkness_fang",
            "mystical_black_fang", "mystical_blue_fang", "mystical_brown_fang", "mystical_green_fang", "mystical_purple_fang", "mystical_red_fang",
            "mystical_light_fang", "mystical_darkness_fang",
            "star_wolf", "tempest_star_wolf", "frost_wolf", "nivalis_frost_wolf", "scorch_wolf", "blazing_scorch_wolf");
    static final Set<String> BLOODFIENDS = Set.of("fledgling_bloodfiend", "kindred_bloodfiend", "blood_noble", "elder_bloodfiend", "progenitor_bloodfiend");

    private static Set<String> idsIn(String ns) {
        Set<String> ids = new TreeSet<>();
        for (ResourceLocation id : RaceAPI.getRaceRegistry().getIds()) {
            if (id.getNamespace().equals(ns)) {
                ids.add(id.getPath());
            }
        }
        return ids;
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theAddonRaceIdsTheTableUsesAreRegistered(GameTestHelper helper) {
        Set<String> mysticism = idsIn("mysticism");
        Set<String> ascension = idsIn("ascension");
        for (String ns : List.of("mysticism", "ascension", "tensurahorizons")) {
            Set<String> ids = idsIn(ns);
            note("P12", ns + " (" + ids.size() + "): " + String.join(",", ids));
        }
        if (mysticism.isEmpty() && ascension.isEmpty()) {
            note("P12", "SKIPPED: Mysticism and Ascension are not loaded (run with -PaddonJars)");
            helper.succeed();
            return;
        }
        if (!mysticism.isEmpty()) {
            Set<String> missing = new TreeSet<>(WOLVES);
            missing.removeAll(mysticism);
            helper.assertTrue(missing.isEmpty(), "wolf ids the table uses are not registered: " + missing);
            Set<String> wolfLike = new TreeSet<>();
            mysticism.stream().filter(id -> id.endsWith("_wolf") || id.endsWith("_fang") || id.equals("direwolf")).forEach(wolfLike::add);
            wolfLike.removeAll(WOLVES);
            helper.assertTrue(wolfLike.equals(Set.of("guitar_wolf", "amped_guitar_wolf")), "wolf-named ids outside the table: " + wolfLike);
            helper.assertTrue(mysticism.size() == 186, "Mysticism 2.2.1 registers 186 races, this one " + mysticism.size());
        }
        if (!ascension.isEmpty()) {
            Set<String> missing = new TreeSet<>(BLOODFIENDS);
            missing.removeAll(ascension);
            helper.assertTrue(missing.isEmpty(), "Bloodfiend ids the table uses are not registered: " + missing);
            helper.assertTrue(ascension.size() == 69, "Ascension 3.1.0 registers 69 races, this one " + ascension.size());
        }
        helper.succeed();
    }
}
