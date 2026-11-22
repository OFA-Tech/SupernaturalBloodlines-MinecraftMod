package net.ofatech.supernaturalbloodlines.core.supernatural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.BuySkillPayload;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.SkillTablePayload;
import net.ofatech.supernaturalbloodlines.parts.supernatural.net.UnlockModSkillPayload;
import org.junit.jupiter.api.Test;

/** The skill table goes to the client as text and comes back as the same skills, trees and quests (as the ladders do). */
class SkillPayloadTest {
    private static SkillTable roundTrip(SkillTable server, java.util.function.Predicate<String> mods, java.util.function.Predicate<String> parts) {
        ByteBuf buf = Unpooled.buffer();
        SkillTablePayload.STREAM_CODEC.encode(buf, SkillTablePayload.of(server.sources(), mods, parts));
        SkillTable.Parsed client = SkillTablePayload.STREAM_CODEC.decode(buf).parseWithServerSets();
        assertEquals(List.of(), client.rejected());
        return client.table();
    }

    private static void same(SkillTable server, SkillTable client) {
        assertEquals(server.skills().stream().map(SkillSpec::id).sorted().toList(), client.skills().stream().map(SkillSpec::id).sorted().toList());
        server.skills().forEach(s -> assertEquals(s, client.skill(s.id()).orElseThrow(), s.id()));
        for (SupernaturalRace race : SupernaturalRace.values()) {
            assertEquals(server.tree(race), client.tree(race), "tree of " + race);
        }
        assertEquals(server.quests().stream().map(QuestSpec::id).sorted().toList(), client.quests().stream().map(QuestSpec::id).sorted().toList());
        server.quests().forEach(q -> assertEquals(q, client.quest(q.id()).orElseThrow(), q.id()));
    }

    @Test
    void theTableSurvivesTheWire() {
        SkillTable server = SkillDataTest.load(m -> true);
        same(server, roundTrip(server, m -> true, p -> true));
        assertEquals(1, server.quests().size());
    }

    @Test
    void theClientPrunesWithTheServersSetsNotItsOwn() {
        // the server has no Tensura and no species part: the client, whatever it has installed, gets the same pruned table
        SkillTable server = SkillDataTest.load(m -> !m.equals("tensura"), p -> !p.equals("species"));
        SkillTable client = roundTrip(server, m -> !m.equals("tensura"), p -> !p.equals("species"));
        same(server, client);
        assertEquals(40, client.skills().size());
        assertEquals(2, client.quest("Q-VAM-6").orElseThrow().tasks().size(), "the species task is still there");
    }

    @Test
    void thePurchaseAndUnlockPayloadsCarryJustAName() {
        ByteBuf buf = Unpooled.buffer();
        BuySkillPayload.STREAM_CODEC.encode(buf, new BuySkillPayload("S-VAM-1"));
        assertEquals("S-VAM-1", BuySkillPayload.STREAM_CODEC.decode(buf).skillId());
        UnlockModSkillPayload.STREAM_CODEC.encode(buf, new UnlockModSkillPayload("vampirism:blood_vision"));
        assertEquals("vampirism:blood_vision", UnlockModSkillPayload.STREAM_CODEC.decode(buf).skillId());
        assertTrue(buf.readableBytes() == 0);
    }
}