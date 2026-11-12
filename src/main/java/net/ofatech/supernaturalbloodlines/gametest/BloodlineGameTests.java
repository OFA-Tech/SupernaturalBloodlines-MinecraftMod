package net.ofatech.supernaturalbloodlines.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.attachment.ModAttachments;
import net.ofatech.supernaturalbloodlines.core.bloodline.BloodlineState;
import net.ofatech.supernaturalbloodlines.core.bloodline.Species;

@GameTestHolder(SupernaturalBloodlines.MODID)
@PrefixGameTestTemplate(false)
public final class BloodlineGameTests {
    private BloodlineGameTests() {
    }

    @GameTest(template = "empty")
    public static void bloodlineSurvivesSaveLoad(GameTestHelper helper) {
        ServerPlayer saved = helper.makeMockServerPlayerInLevel();
        saved.setData(ModAttachments.BLOODLINE, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());

        CompoundTag nbt = new CompoundTag();
        saved.saveWithoutId(nbt);

        ServerPlayer loaded = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(BloodlineAccess.get(loaded).species() == Species.NONE, "fresh player should start as NONE");
        loaded.load(nbt);
        helper.assertTrue(BloodlineAccess.get(loaded).species() == Species.WITCH,
                "species after reload was " + BloodlineAccess.get(loaded).species());
        helper.succeed();
    }
}
