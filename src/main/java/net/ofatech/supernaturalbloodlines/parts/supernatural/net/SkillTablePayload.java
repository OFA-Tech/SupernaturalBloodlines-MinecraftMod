package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import net.ofatech.supernaturalbloodlines.core.supernatural.SkillTable;

/**
 * Server to client, the same way as {@link LadderTablePayload}: the skill, tree and quest files the server accepted, as JSON text, sent
 * on login and after every data reload, with the mods and parts the server had when it pruned them. The client parses with the
 * server's sets ({@link SkillTable#clientCopy()}), so it shows the same skills and quests the server counts. The hub reads it.
 */
public record SkillTablePayload(Map<String, String> skills, Map<String, String> trees, Map<String, String> quests, List<String> mods,
                                List<String> parts) implements CustomPacketPayload {
    public static final Type<SkillTablePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SupernaturalBloodlines.MODID, "skill_table"));

    /** A skill TENSURA kind is hidden without Tensura without naming the mod in its file. */
    private static final String IMPLIED_MOD = "tensura";

    private static final Gson GSON = new Gson();

    private static final StreamCodec<ByteBuf, List<String>> NAMES = ByteBufCodecs.stringUtf8(64).apply(ByteBufCodecs.list(256));

    private static final StreamCodec<ByteBuf, Map<String, String>> FILES = ByteBufCodecs.<ByteBuf, String, String, Map<String, String>>map(HashMap::new,
            ByteBufCodecs.stringUtf8(64), ByteBufCodecs.stringUtf8(1 << 16));

    public static final StreamCodec<ByteBuf, SkillTablePayload> STREAM_CODEC = StreamCodec.composite(
            FILES, SkillTablePayload::skills,
            FILES, SkillTablePayload::trees,
            FILES, SkillTablePayload::quests,
            NAMES, SkillTablePayload::mods,
            NAMES, SkillTablePayload::parts,
            SkillTablePayload::new);

    /** @param modLoaded the server's answer for a mod id; @param partEnabled the server's answer for a part config key */
    public static SkillTablePayload of(SkillTable.Sources sources, Predicate<String> modLoaded, Predicate<String> partEnabled) {
        Set<String> mods = new TreeSet<>(Set.of(IMPLIED_MOD));
        Set<String> parts = new TreeSet<>();
        sources.quests().values().forEach(q -> names(q, mods, parts));
        mods.removeIf(m -> !modLoaded.test(m));
        parts.removeIf(p -> !partEnabled.test(p));
        return new SkillTablePayload(text(sources.skills()), text(sources.trees()), text(sources.quests()), List.copyOf(mods), List.copyOf(parts));
    }

    private static Map<String, String> text(Map<String, JsonElement> files) {
        Map<String, String> out = new HashMap<>();
        files.forEach((name, json) -> out.put(name, GSON.toJson(json)));
        return out;
    }

    /** Every mod id and part key a quest names (task {@code mod}, {@code part}). */
    private static void names(JsonElement e, Set<String> mods, Set<String> parts) {
        if (e.isJsonArray()) {
            e.getAsJsonArray().forEach(x -> names(x, mods, parts));
        } else if (e.isJsonObject()) {
            e.getAsJsonObject().entrySet().forEach(en -> {
                JsonElement v = en.getValue();
                switch (en.getKey()) {
                    case "mod" -> mods.add(v.isJsonPrimitive() ? v.getAsString() : "");
                    case "part" -> parts.add(v.isJsonPrimitive() ? v.getAsString() : "");
                    default -> names(v, mods, parts);
                }
            });
        }
    }

    /** The table as the server pruned it: its mods and parts decide, whatever this client has installed. */
    public SkillTable.Parsed parseWithServerSets() {
        return SkillTable.parse(parsed(skills), parsed(trees), parsed(quests), mods::contains, parts::contains);
    }

    /** A file that is not JSON is dropped, never a crash. */
    private static Map<String, JsonElement> parsed(Map<String, String> files) {
        Map<String, JsonElement> out = new HashMap<>();
        files.forEach((name, text) -> {
            try {
                out.put(name, JsonParser.parseString(text));
            } catch (RuntimeException e) {
                SupernaturalBloodlines.LOGGER.warn("Skill file {} from the server is not JSON, skipped", name);
            }
        });
        return out;
    }

    @Override
    public Type<SkillTablePayload> type() {
        return TYPE;
    }
}