package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.core.wheel.WheelModel.Open;
import org.junit.jupiter.api.Test;

class WheelModelTest {
    private record E(PowerRef ref, PowerGroup group) implements WheelModel.Entry {
    }

    private static E e(PowerSource s, String id, PowerGroup g) {
        return new E(new PowerRef(s, ResourceLocation.parse(id)), g);
    }

    private static List<E> sample() {
        return List.of(e(PowerSource.TENSURA, "tensura:a", PowerGroup.SKILLS), e(PowerSource.TENSURA, "tensura:b", PowerGroup.MAGIC), e(PowerSource.TENSURA, "tensura:c", PowerGroup.SKILLS),
                e(PowerSource.VAMPIRISM, "vampirism:rage", PowerGroup.MAIN), e(PowerSource.GREAT_SAGE, "greatsage:scan", PowerGroup.MAIN));
    }

    @Test
    void categoriesComeFromTheCatalogueAndGreatSage() {
        WheelModel<E> m = new WheelModel<>(sample(), s -> true);
        assertEquals(List.of(PowerSource.VAMPIRISM, PowerSource.TENSURA, PowerSource.GREAT_SAGE), m.mods());
    }

    @Test
    void emptyCategoryNeverShows() {
        assertTrue(new WheelModel<E>(List.of(), s -> true).mods().isEmpty());
        assertEquals(List.of(PowerSource.VAMPIRISM), new WheelModel<>(sample(), s -> s == PowerSource.VAMPIRISM).mods(), "an uninstalled mod's powers do not show");
    }

    @Test
    void middleRingOnlyForTensuraWithMoreThanOneGroup() {
        WheelModel<E> m = new WheelModel<>(sample(), s -> true);
        assertEquals(List.of(PowerGroup.SKILLS, PowerGroup.MAGIC), m.groups(PowerSource.TENSURA));
        assertTrue(m.groups(PowerSource.VAMPIRISM).isEmpty());
        List<E> onlySkills = List.of(e(PowerSource.TENSURA, "tensura:a", PowerGroup.SKILLS), e(PowerSource.TENSURA, "tensura:c", PowerGroup.SKILLS));
        assertTrue(new WheelModel<>(onlySkills, s -> true).groups(PowerSource.TENSURA).isEmpty());
        assertEquals(2, new WheelModel<>(onlySkills, s -> true).powers(PowerSource.TENSURA, PowerGroup.SKILLS).size(), "with no middle ring all of Tensura's are one list");
    }

    @Test
    void hoveringAModChoosesTheGroupsAndTheFirstGroupIsOpen() {
        WheelModel<E> m = new WheelModel<>(sample(), s -> true);
        Open tensura = m.openMod(1); // mods: Vampirism, Tensura, Great Sage
        assertEquals(new Open(1, 0, 0), tensura);
        assertEquals(List.of("tensura:a", "tensura:c"), m.powers(tensura).stream().map(x -> x.ref().id().toString()).toList(), "the first group, Skills, is open");
        assertEquals(List.of("tensura:b"), m.powers(m.openGroup(tensura, 1)).stream().map(x -> x.ref().id().toString()).toList());
        assertEquals(List.of("vampirism:rage"), m.powers(m.openMod(0)).stream().map(x -> x.ref().id().toString()).toList());
    }

    @Test
    void groupHiddenWhenEmpty() {
        List<E> noSpells = List.of(e(PowerSource.TENSURA, "tensura:a", PowerGroup.SKILLS), e(PowerSource.TENSURA, "tensura:z", PowerGroup.BATTLEWILL));
        assertEquals(List.of(PowerGroup.SKILLS, PowerGroup.BATTLEWILL), new WheelModel<>(noSpells, s -> true).groups(PowerSource.TENSURA));
    }

    @Test
    void aMadeUpPathFallsBackToTheFirst() {
        WheelModel<E> m = new WheelModel<>(sample(), s -> true);
        assertEquals(new Open(2, 0, 0), m.fit(new Open(9, 5, 3)));
        assertEquals(new Open(1, 1, 0), m.fit(new Open(1, 1, 7)), "a page past the end falls to the last");
        assertEquals(Open.FIRST, new WheelModel<E>(List.of(), s -> true).fit(new Open(3, 3, 3)));
        assertTrue(new WheelModel<E>(List.of(), s -> true).powers(Open.FIRST).isEmpty());
        List<E> many = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            many.add(e(PowerSource.VAMPIRISM, "vampirism:p" + i, PowerGroup.MAIN));
        }
        assertEquals(new Open(0, 0, 2), new WheelModel<>(many, s -> true).fit(new Open(0, 0, 9)), "30 powers are 3 pages");
    }
}
