package net.ofatech.supernaturalbloodlines.core.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class LoadoutPowersTest {
    private static PowerRef r(PowerSource s, String id) {
        return new PowerRef(s, ResourceLocation.parse(id));
    }

    @Test
    void loadoutPageListsTheSameLoadoutPowersAsTheWheel() {
        List<PowerRef> all = List.of(r(PowerSource.BLOODLINE, "supernaturalbloodlines:bite"), r(PowerSource.TENSURA, "tensura:coercion"), r(PowerSource.VAMPIRISM, "vampirism:rage"),
                r(PowerSource.WEREWOLVES, "werewolves:howl"), r(PowerSource.INFINITY, "infinity:soul"), r(PowerSource.GREAT_SAGE, "greatsage:scan"));
        assertEquals(List.of(all.get(0), all.get(2), all.get(3)), WheelRules.loadoutPowers(all), "ours, Vampirism's and Werewolves' go in our Loadout; Tensura has its own presets and stones and Great Sage have no slot");
        assertEquals(List.of(), WheelRules.loadoutPowers(List.of()));
    }
}
