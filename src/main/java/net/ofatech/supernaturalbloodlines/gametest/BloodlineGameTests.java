package net.ofatech.supernaturalbloodlines.gametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.ofatech.supernaturalbloodlines.core.Part;
import net.ofatech.supernaturalbloodlines.core.StalePartModifiers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.server.MinecraftServer;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.BloodlineAccess;
import net.ofatech.supernaturalbloodlines.core.EnabledParts;
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
        ServerPlayer saved = TestPlayers.create(helper);
        saved.setData(ModAttachments.BLOODLINE, BloodlineState.NONE.toBuilder().species(Species.WITCH).build());

        CompoundTag nbt = new CompoundTag();
        saved.saveWithoutId(nbt);

        ServerPlayer loaded = TestPlayers.create(helper);
        helper.assertTrue(BloodlineAccess.get(loaded).species() == Species.NONE, "fresh player should start as NONE");
        loaded.load(nbt);
        helper.assertTrue(BloodlineAccess.get(loaded).species() == Species.WITCH,
                "species after reload was " + BloodlineAccess.get(loaded).species());
        helper.succeed();
    }

    // a part that is switched off never runs, so core strips the saved modifiers it owns on login
    @GameTest(template = "empty")
    public static void disabledPartsLoseTheirSavedModifiers(GameTestHelper helper) {
        ResourceLocation original = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "original");
        ResourceLocation foreign = ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "something_else");
        ServerPlayer p = TestPlayers.create(helper);
        double health = p.getMaxHealth();
        double attack = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        p.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier(original, 4.0, AttributeModifier.Operation.ADD_VALUE));
        p.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier(original, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        p.getAttribute(Attributes.ARMOR).addPermanentModifier(new AttributeModifier(foreign, 1.0, AttributeModifier.Operation.ADD_VALUE));
        p.setHealth(p.getMaxHealth());

        StalePartModifiers.strip(p, EnumSet.of(Part.SPECIES));
        helper.assertTrue(p.getAttribute(Attributes.MAX_HEALTH).hasModifier(original), "an enabled part lost its modifier");

        StalePartModifiers.strip(p, EnumSet.noneOf(Part.class));
        helper.assertTrue(!p.getAttribute(Attributes.MAX_HEALTH).hasModifier(original), "the health modifier of a disabled part stayed");
        helper.assertTrue(!p.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(original), "the attack modifier of a disabled part stayed");
        helper.assertTrue(p.getMaxHealth() == health && p.getAttributeValue(Attributes.ATTACK_DAMAGE) == attack, "values did not return to the base");
        helper.assertTrue(p.getHealth() <= p.getMaxHealth(), "health is above the maximum: " + p.getHealth());
        helper.assertTrue(p.getAttribute(Attributes.ARMOR).hasModifier(foreign), "a modifier no part owns was removed");
        helper.succeed();
    }

    // our pickaxe tag entry names a block that only exists when a part is on; a required entry would
    // make vanilla drop the whole merged tag
    @GameTest(template = "empty")
    public static void pickaxeTagSurvivesPartOnlyEntries(GameTestHelper helper) {
        helper.assertTrue(Blocks.STONE.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE),
                "minecraft:stone is no longer in #minecraft:mineable/pickaxe");
        helper.succeed();
    }

    // part data carries supernaturalbloodlines:part_enabled: with the switch off (or a mod missing) none of it may load,
    // with it on it must (run with -PpartsOff=... and -PnoLinkedMods to see the off side)
    @GameTest(template = "empty")
    public static void partDataFollowsSwitch(GameTestHelper helper) {
        RegistryAccess access = helper.getLevel().registryAccess();
        // explicit ids: each part owns exactly these rites (a prefix or a namespace count would mix the parts up)
        checkPartRites(helper, access, "enchantedTensura", Set.of("purification", "purification_holy_water", "curse_magicule_drain", "curse_skill_seal", "magicule_offering"));
        checkPartRites(helper, access, "vampirismEnchanted", Set.of("cleansing_blood", "blood_offering", "silver_moon"));
        // Tensura's registry has no namespace prefix and our files sit in its data namespace
        checkPartData(helper, access, "enchantedTensura", "entity_existence", "tensura", "supernaturalbloodlines_enchanted_");
        checkPartData(helper, access, "vampirismTensura", "entity_existence", "tensura", "supernaturalbloodlines_vampirism_");
        checkPartData(helper, access, "species", "damage_type", "supernaturalbloodlines", "failed_transition");
        helper.succeed();
    }

    private static void checkPartRites(GameTestHelper helper, RegistryAccess access, String part, Set<String> ids) {
        long count = access.registries().filter(e -> e.key().location().getPath().equals("circle_magic/rite"))
                .mapToLong(e -> e.value().keySet().stream()
                        .filter(id -> id.getNamespace().equals(SupernaturalBloodlines.MODID) && ids.contains(id.getPath())).count()).sum();
        boolean on = EnabledParts.isEnabled(part);
        helper.assertTrue(on == (count > 0), "part " + part + " enabled=" + on + " but " + count + " of its rites are in the registry");
    }

    private static void checkPartData(GameTestHelper helper, RegistryAccess access, String part, String registryPath, String entryNs,
                                      String entryPrefix) {
        long count = access.registries().filter(e -> e.key().location().getPath().equals(registryPath))
                .mapToLong(e -> e.value().keySet().stream()
                        .filter(id -> id.getNamespace().equals(entryNs) && id.getPath().startsWith(entryPrefix)).count()).sum();
        boolean on = EnabledParts.isEnabled(part);
        helper.assertTrue(on == (count > 0), "part " + part + " enabled=" + on + " but " + count + " '" + entryPrefix + "' entries in " + registryPath);
    }
    private static CommandSourceStack asOp(ServerPlayer p) {
        return p.createCommandSourceStack().withPermission(4);
    }

    @GameTest(template = "empty")
    public static void debugSetSurvivesSaveLoad(GameTestHelper helper) throws CommandSyntaxException {
        ServerPlayer player = TestPlayers.create(helper);
        MinecraftServer server = helper.getLevel().getServer();

        server.getCommands().performPrefixedCommand(asOp(player), "supernaturalbloodlines debug set @s WITCH");
        helper.assertTrue(BloodlineAccess.get(player).species() == Species.WITCH,
                "species after debug set was " + BloodlineAccess.get(player).species());

        // the same command line in lower case, through the "/" form a player would type
        // performPrefixedCommand logs a throwing command and carries on, so the rest use the dispatcher, which rethrows
        server.getCommands().getDispatcher().execute("supernaturalbloodlines debug set @s hunter_of_the_five", asOp(player));
        helper.assertTrue(BloodlineAccess.get(player).species() == Species.HUNTER_OF_THE_FIVE,
                "species after lower-case debug set was " + BloodlineAccess.get(player).species());
        server.getCommands().getDispatcher().execute("supernaturalbloodlines debug set @s WITCH", asOp(player));

        CompoundTag nbt = new CompoundTag();
        player.saveWithoutId(nbt);
        ServerPlayer reloaded = TestPlayers.create(helper);
        reloaded.load(nbt);
        helper.assertTrue(BloodlineAccess.get(reloaded).species() == Species.WITCH,
                "species after reload was " + BloodlineAccess.get(reloaded).species());

        helper.assertTrue(server.getCommands().getDispatcher().execute("supernaturalbloodlines debug get @s", asOp(reloaded)) == 1,
                "debug get did not succeed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void debugBadInputGivesErrorNotCrash(GameTestHelper helper) throws CommandSyntaxException {
        ServerPlayer player = TestPlayers.create(helper);
        MinecraftServer server = helper.getLevel().getServer();
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        CommandSourceStack op = asOp(player);

        for (String bad : new String[] {"debug set @s NOT_A_SPECIES", "debug event @s NOT_AN_EVENT"}) {
            try {
                dispatcher.execute("supernaturalbloodlines " + bad, op);
                helper.fail("expected a command error for: " + bad);
            } catch (CommandSyntaxException expected) {
                // a normal command error, shown to the player
            }
            server.getCommands().performPrefixedCommand(op, "supernaturalbloodlines " + bad);
        }
        helper.assertTrue(BloodlineAccess.get(player).species() == Species.NONE,
                "bad input must not change the state, species was " + BloodlineAccess.get(player).species());

        // permission 2 is required
        try {
            dispatcher.execute("supernaturalbloodlines debug set @s WITCH", player.createCommandSourceStack().withPermission(1));
            helper.fail("permission 1 must not run the debug command");
        } catch (CommandSyntaxException expected) {
            // not allowed
        }
        helper.assertTrue(BloodlineAccess.get(player).species() == Species.NONE, "permission 1 changed the state");

        // a real event, through the command
        dispatcher.execute("supernaturalbloodlines debug event @s RITE_OF_IMMORTALITY", op);
        helper.assertTrue(BloodlineAccess.get(player).species() == Species.ORIGINAL,
                "species after RITE_OF_IMMORTALITY was " + BloodlineAccess.get(player).species());

        // a refusal (the default context has no hunter faction) returns 0 and leaves the state alone
        helper.assertTrue(dispatcher.execute("supernaturalbloodlines debug event @s RITE_OF_THE_FIVE", op) == 0,
                "a refused event should return 0");
        helper.assertTrue(BloodlineAccess.get(player).species() == Species.ORIGINAL,
                "a refused event changed the species to " + BloodlineAccess.get(player).species());
        helper.succeed();
    }
}
