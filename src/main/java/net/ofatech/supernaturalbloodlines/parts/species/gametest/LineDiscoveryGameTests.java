package net.ofatech.supernaturalbloodlines.parts.species.gametest;

import static net.ofatech.supernaturalbloodlines.parts.species.gametest.DoppelgangerRiteGameTests.taglock;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.cleanUp;
import static net.ofatech.supernaturalbloodlines.parts.species.gametest.SirelineCommandGameTests.species;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.place;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.remove;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.start;
import static net.ofatech.supernaturalbloodlines.parts.vampirismenchanted.gametest.RiteTestSupport.type;

import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Genes;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;
import net.ofatech.supernaturalbloodlines.gametest.TestPlayers;
import net.ofatech.supernaturalbloodlines.parts.species.SpeciesConfig;
import net.ofatech.supernaturalbloodlines.parts.species.line.LineIndex;
import net.ofatech.supernaturalbloodlines.parts.species.line.Lines;
import net.ofatech.supernaturalbloodlines.parts.species.rite.RevelationRiteFactory;
import net.ofatech.supernaturalbloodlines.parts.species.sireline.SireRecorder;

/** Discovering the Original of your line (R123): the Rite of Revelation reveals it, per player, and {@code original_visibility = always} shows it without. */
@PrefixGameTestTemplate(false)
public final class LineDiscoveryGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private LineDiscoveryGameTests() {
    }

    private static boolean skip(GameTestHelper helper) {
        if (type(helper, "rite_of_revelation") == null) {
            helper.succeed(); // gated on the species part
            return true;
        }
        return false;
    }

    private static LineTestPlayer member(GameTestHelper helper, ServerPlayer original) {
        LineTestPlayer p = LineTestPlayer.create(helper);
        species(p, Species.HYBRID);
        SireRecorder.record(original, p);
        return p;
    }

    private static void done(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            LineIndex.get(helper.getLevel().getServer()).remove(p.getUUID());
            cleanUp(p);
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void revelationRevealsTheOriginalOfTheLine(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        ServerPlayer o = TestPlayers.createMortal(helper);
        species(o, Species.ORIGINAL);
        LineTestPlayer subject = member(helper, o);
        place(helper, subject);
        try {
            helper.assertTrue(Lines.visibleRoot(subject).isEmpty(), "control: the Original was visible before the rite");
            start(helper, "rite_of_revelation", subject, List.of(taglock(subject.getUUID())));
            helper.assertTrue(Lines.visibleRoot(subject).filter(o.getUUID()::equals).isPresent(), "the Original is still hidden");
            helper.assertTrue(subject.heard().contains("[" + RevelationRiteFactory.LINE + "]" + o.getGameProfile().getName()),
                    "the player was not told the Original's name: " + subject.heard());
        } finally {
            remove(helper, subject);
            done(helper, o, subject);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void revelationIsPerPlayerNotTheirSired(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        ServerPlayer o = TestPlayers.createMortal(helper);
        species(o, Species.ORIGINAL);
        LineTestPlayer subject = member(helper, o);
        LineTestPlayer sired = LineTestPlayer.create(helper);
        species(sired, Species.HYBRID);
        SireRecorder.record(subject, sired);
        place(helper, subject);
        try {
            start(helper, "rite_of_revelation", subject, List.of(taglock(subject.getUUID())));
            helper.assertTrue(Lines.visibleRoot(subject).isPresent(), "control: the subject did not discover");
            helper.assertTrue(Lines.rootOf(helper.getLevel().getServer(), sired.getUUID()).isPresent(), "control: their sired has no line");
            helper.assertTrue(Lines.visibleRoot(sired).isEmpty(), "the discovery was shared with their sired");
        } finally {
            remove(helper, subject);
            done(helper, o, subject, sired);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void revelationOnALinelessPlayerSaysNoLine(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        LineTestPlayer subject = LineTestPlayer.create(helper);
        species(subject, Species.HYBRID);
        place(helper, subject);
        try {
            start(helper, "rite_of_revelation", subject, List.of(taglock(subject.getUUID())));
            helper.assertTrue(subject.heard().contains("[" + RevelationRiteFactory.NO_LINE + "]"), "they were not told they have no line: " + subject.heard());
            helper.assertTrue(BloodlineAccess.get(subject).knownLine() == null, "something was discovered");
        } finally {
            remove(helper, subject);
            done(helper, subject);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void revelationStillRevealsGenes(GameTestHelper helper) {
        if (skip(helper)) {
            return;
        }
        ServerPlayer o = TestPlayers.createMortal(helper);
        species(o, Species.ORIGINAL);
        LineTestPlayer subject = member(helper, o);
        place(helper, subject);
        try {
            BloodlineAccess.set(subject, BloodlineAccess.get(subject).toBuilder().doppelgangerGene(true).doppelgangerRolled(true).build());
            start(helper, "rite_of_revelation", subject, List.of(taglock(subject.getUUID())));
            helper.assertTrue(BloodlineAccess.get(subject).revealedGenes().contains(Genes.DOPPELGANGER), "the gene was not revealed");
            helper.assertTrue(Lines.visibleRoot(subject).isPresent(), "the Original was not revealed with it");
        } finally {
            remove(helper, subject);
            done(helper, o, subject);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void alwaysVisibilityShowsWithoutTheRite(GameTestHelper helper) {
        ServerPlayer o = TestPlayers.createMortal(helper);
        species(o, Species.ORIGINAL);
        LineTestPlayer subject = member(helper, o);
        try {
            helper.assertTrue(Lines.visibleRoot(subject).isEmpty(), "control: visible under the default");
            SpeciesConfig.overrideOriginalVisibility(true);
            helper.assertTrue(Lines.visibleRoot(subject).filter(o.getUUID()::equals).isPresent(), "hidden under original_visibility = always");
            helper.assertTrue(BloodlineAccess.get(subject).knownLine() == null, "visibility wrote a discovery");
        } finally {
            SpeciesConfig.overrideOriginalVisibility(null);
            done(helper, o, subject);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void discoveryIsLostWhenTheRootChanges(GameTestHelper helper) {
        ServerPlayer o = TestPlayers.createMortal(helper);
        species(o, Species.ORIGINAL);
        LineTestPlayer subject = member(helper, o);
        try {
            Lines.reveal(subject);
            helper.assertTrue(Lines.visibleRoot(subject).isPresent(), "control: the discovery did not work");
            Lines.freeTarget(helper.getLevel().getServer(), subject.getUUID(), o.getUUID(), LineIndex.Kind.BROKEN_LINE);
            UUID now = Lines.rootOf(helper.getLevel().getServer(), subject.getUUID()).orElseThrow();
            helper.assertTrue(now.equals(subject.getUUID()), "control: the freed player is not their own root");
            helper.assertTrue(Lines.visibleRoot(subject).isEmpty(), "an old discovery showed the new root");
        } finally {
            done(helper, o, subject);
        }
        helper.succeed();
    }
}
