package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.cleanUp;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.species;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.LineRules;
import net.ofatech.supernaturalbloodlines.core.bloodline.NpcOriginal;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.Lines;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SirelineIndex;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubSocial;

/** What the hub is told about the player's line (R120 to R127): the five states, and never a root the player has not discovered. */
@PrefixGameTestTemplate(false)
public final class LineHubGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;
    private static final UUID KLAUS = NpcOriginal.KLAUS.uuid();

    private LineHubGameTests() {
    }

    private static LineTestPlayer player(GameTestHelper helper, Species s) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        species(p, s);
        return p;
    }

    private static LineIndex lines(GameTestHelper helper) {
        return LineIndex.get(helper.getLevel().getServer());
    }

    private static void done(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            lines(helper).remove(p.getUUID());
        }
        cleanUp(ps);
    }

    private static HubSocial.Origin origin(ServerPlayer p) {
        return HubSocial.of(p).origin();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void snapshotCarriesTheOrigin(GameTestHelper helper) {
        LineTestPlayer o = player(helper, Species.ORIGINAL);
        LineTestPlayer a = player(helper, Species.HYBRID);
        try {
            SireRecorder.record(o, a);
            Lines.reveal(a);
            HubSocial.Origin origin = origin(a);
            helper.assertTrue(origin.state() == LineRules.State.IN_LINE, "state is " + origin.state());
            helper.assertTrue(origin.root().equals(Optional.of(o.getGameProfile().getName())), "root is " + origin.root());
            helper.assertTrue(origin.freedKind() == HubSocial.FreedKind.NONE && origin.freedFrom().isEmpty(), "a member looks freed: " + origin);
        } finally {
            done(helper, o, a);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void snapshotNeverLeaksAHiddenRoot(GameTestHelper helper) {
        LineTestPlayer a = player(helper, Species.HYBRID);
        SpeciesConfig.overrideOriginalVisibility(false);
        try {
            lines(helper).setRoot(a.getUUID(), KLAUS, true);
            HubSocial.Social social = HubSocial.of(a);
            helper.assertTrue(social.origin().state() == LineRules.State.IN_LINE, "control: state is " + social.origin().state());
            helper.assertTrue(social.origin().root().isEmpty(), "an undiscovered Original was sent: " + social.origin().root());
            String everything = social.toString().toLowerCase();
            helper.assertTrue(!everything.contains("klaus") && !everything.contains(KLAUS.toString()), "the hidden Original is in the snapshot: " + everything);

            // freed from the same Original: the break is known, the name is not
            lines(helper).free(a.getUUID(), KLAUS, LineIndex.Kind.BROKEN_LINE);
            HubSocial.Social freed = HubSocial.of(a);
            helper.assertTrue(freed.origin().state() == LineRules.State.FREE && freed.origin().freedFrom().isEmpty(), "freed: " + freed.origin());
            helper.assertTrue(!freed.toString().toLowerCase().contains("klaus") && !freed.toString().contains(KLAUS.toString()), "the hidden Original is in the freed snapshot");
        } finally {
            SpeciesConfig.overrideOriginalVisibility(null);
            done(helper, a);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void snapshotOfAFreePlayer(GameTestHelper helper) {
        LineTestPlayer o = player(helper, Species.ORIGINAL);
        LineTestPlayer a = player(helper, Species.HYBRID);
        try {
            SireRecorder.record(o, a);
            lines(helper).free(a.getUUID(), o.getUUID(), LineIndex.Kind.BROKEN_LINE);
            HubSocial.Origin before = origin(a);
            helper.assertTrue(before.state() == LineRules.State.FREE && before.freedKind() == HubSocial.FreedKind.BROKEN_LINE, "freed: " + before);
            helper.assertTrue(before.freedFrom().isEmpty(), "the Original they were freed from showed before they knew: " + before.freedFrom());
            Lines.learn(a, o.getUUID());
            helper.assertTrue(origin(a).freedFrom().equals(Optional.of(o.getGameProfile().getName())), "freedFrom is " + origin(a).freedFrom());
            lines(helper).free(a.getUUID(), o.getUUID(), LineIndex.Kind.IMPRISONED);
            helper.assertTrue(origin(a).freedKind() == HubSocial.FreedKind.IMPRISONED, "the kind was not carried");
        } finally {
            done(helper, o, a);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void snapshotOfAnNpcRootUsesTheNpcPrefix(GameTestHelper helper) {
        LineTestPlayer a = player(helper, Species.HYBRID);
        try {
            lines(helper).setRoot(a.getUUID(), KLAUS, true);
            Lines.reveal(a);
            helper.assertTrue(origin(a).root().equals(Optional.of("npc:klaus")), "root is " + origin(a).root());
        } finally {
            done(helper, a);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void snapshotMarksNpcAncestorsHidden(GameTestHelper helper) {
        LineTestPlayer c = player(helper, Species.HYBRID);
        UUID s = UUID.randomUUID();
        UUID p = UUID.randomUUID();
        UUID creature = UUID.randomUUID();
        try {
            SirelineIndex index = SirelineIndex.get(helper.getLevel());
            index.add(creature, p, "minecraft:player", "Pat");
            index.add(p, s, "minecraft:player", "Sam");
            index.add(UUID.randomUUID(), creature, "minecraft:zombie", "Zed");
            BloodlineAccess.set(c, BloodlineAccess.get(c).toBuilder().sire(s).build());
            HubSocial.Social social = HubSocial.of(c);
            helper.assertTrue(social.ancestors().size() >= 2 && social.origin().ancestorHidden().size() == social.ancestors().size(),
                    "ancestors " + social.ancestors() + " hidden " + social.origin().ancestorHidden());
            helper.assertTrue(!social.origin().ancestorHidden().get(0) && social.ancestors().get(0).equals("Pat"), "a player ancestor was hidden: " + social);
            helper.assertTrue(social.origin().ancestorHidden().get(1) && social.ancestors().get(1).isEmpty(), "a creature ancestor was shown: " + social);
        } finally {
            BloodlineAccess.set(c, BloodlineAccess.get(c).toBuilder().sire(null).build());
            SirelineIndex.get(helper.getLevel()).remove(s);
            SirelineIndex.get(helper.getLevel()).remove(p);
            SirelineIndex.get(helper.getLevel()).remove(creature);
            done(helper, c);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void snapshotStatesTable(GameTestHelper helper) {
        LineTestPlayer original = player(helper, Species.ORIGINAL);
        LineTestPlayer human = player(helper, Species.NONE);
        LineTestPlayer unknown = player(helper, Species.HYBRID);
        LineTestPlayer inLine = player(helper, Species.HYBRID);
        try {
            SireRecorder.record(original, inLine);
            helper.assertTrue(origin(original).state() == LineRules.State.ORIGINAL, "an Original is " + origin(original).state());
            helper.assertTrue(origin(original).root().isEmpty(), "an Original has a root above them");
            helper.assertTrue(origin(human).state() == LineRules.State.NOT_VAMPIRE, "a human is " + origin(human).state());
            helper.assertTrue(origin(unknown).state() == LineRules.State.SIRE_UNKNOWN, "a vampire with no sire is " + origin(unknown).state());
            helper.assertTrue(origin(inLine).state() == LineRules.State.IN_LINE, "a member is " + origin(inLine).state());
            helper.assertTrue(List.of(origin(original), origin(human), origin(unknown)).stream().allMatch(o -> o.freedKind() == HubSocial.FreedKind.NONE),
                    "a state carries a freed kind");
        } finally {
            done(helper, original, human, unknown, inLine);
        }
        helper.succeed();
    }

    /** A witch is supernatural but not a vampire (no line, no sire to be unknown). */
    @GameTest(template = "empty", templateNamespace = NS)
    public static void aWitchIsNotAVampireForTheBloodlinePage(GameTestHelper helper) {
        LineTestPlayer witch = player(helper, Species.WITCH);
        try {
            helper.assertTrue(origin(witch).state() == LineRules.State.NOT_VAMPIRE, "a witch was given the state " + origin(witch).state());
        } finally {
            done(helper, witch);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void aCreatureSireIsHiddenLikeACreatureAncestor(GameTestHelper helper) {
        LineTestPlayer c = player(helper, Species.HYBRID);
        UUID zombieSire = UUID.randomUUID();
        UUID npcSire = NpcOriginal.KLAUS.uuid();
        try {
            SirelineIndex index = SirelineIndex.get(helper.getLevel());
            index.add(UUID.randomUUID(), zombieSire, "minecraft:zombie", "Zed");
            BloodlineAccess.set(c, BloodlineAccess.get(c).toBuilder().sire(zombieSire).build());
            helper.assertTrue(HubSocial.of(c).sire().equals(Optional.of("")), "a creature sire was named: " + HubSocial.of(c).sire());
            helper.assertTrue(!HubSocial.of(c).toString().contains("Zed"), "the creature's name is in the snapshot");
            BloodlineAccess.set(c, BloodlineAccess.get(c).toBuilder().sire(npcSire).build());
            helper.assertTrue(HubSocial.of(c).sire().equals(Optional.of("")), "an NPC Original sire was named: " + HubSocial.of(c).sire());
        } finally {
            BloodlineAccess.set(c, BloodlineAccess.get(c).toBuilder().sire(null).build());
            SirelineIndex.get(helper.getLevel()).remove(zombieSire);
            done(helper, c);
        }
        helper.succeed();
    }
}
