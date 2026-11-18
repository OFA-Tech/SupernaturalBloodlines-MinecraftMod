package net.ofatech.supernaturalbloodlines.parts.supernatural.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import java.lang.reflect.RecordComponent;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.network.FriendlyByteBuf;
import net.ofatech.supernaturalbloodlines.shared.supernatural.HubRequests;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Fact;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileService;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.ProfileView;
import net.ofatech.supernaturalbloodlines.shared.supernatural.profile.Relation;
import org.junit.jupiter.api.Test;

class ProfilePayloadTest {
    private static ProfilePayload built(Relation relation, java.util.Set<Fact> discovered) {
        UUID subject = UUID.randomUUID();
        var facts = ProfileView.build(relation, discovered, f -> Optional.of(List.of("value of " + f)));
        return ProfilePayload.of(new ProfileService.Profile(subject, relation == Relation.OP_VIEW, facts));
    }

    @Test
    void aStrangersPayloadHasNoFields() {
        ProfilePayload p = built(Relation.STRANGER, EnumSet.noneOf(Fact.class));
        assertTrue(p.facts().isEmpty());
        assertFalse(p.opView());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ProfilePayload.STREAM_CODEC.encode(buf, p);
        assertEquals(p, ProfilePayload.STREAM_CODEC.decode(buf));
        assertEquals(19, buf.writerIndex(), "a UUID, the OP flag, the NPC flag and an empty map are all that crosses the wire");
    }

    @Test
    void thePayloadNeverNamesTheRelationOrWhatWasLeftOut() {
        List<String> components = Stream.of(ProfilePayload.class.getRecordComponents()).map(RecordComponent::getName).toList();
        assertEquals(List.of("subject", "opView", "npc", "facts"), components, "only whether it is no player is said about the subject, never the relation");
    }

    @Test
    void aPayloadWithFactsSurvivesTheWire() {
        ProfilePayload p = built(Relation.SAME_PACK, EnumSet.of(Fact.GENES));
        assertTrue(p.facts().containsKey(Fact.GENES) && !p.facts().containsKey(Fact.LINE));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ProfilePayload.STREAM_CODEC.encode(buf, p);
        assertEquals(p, ProfilePayload.STREAM_CODEC.decode(buf));
    }

    @Test
    void requestsSurviveTheWireAndAreCapped() {
        HubRequests.Request call = new HubRequests.Request(HubRequests.Kind.SIRE_CALL, UUID.randomUUID(), "Klaus", "ab12cd34", 1200);
        HubRequests.Request compel = new HubRequests.Request(HubRequests.Kind.COMPULSION, HubRequests.NOBODY, "", "0123abcd", 600);
        HubRequests.Request duel = new HubRequests.Request(HubRequests.Kind.CHALLENGE, UUID.randomUUID(), "Hayley", "", 2400);
        HubRequestsPayload p = new HubRequestsPayload(List.of(call, compel, duel));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        HubRequestsPayload.STREAM_CODEC.encode(buf, p);
        assertEquals(p, HubRequestsPayload.STREAM_CODEC.decode(buf));
        assertEquals(HubRequestsPayload.MAX, new HubRequestsPayload(java.util.Collections.nCopies(HubRequestsPayload.MAX + 5, call)).requests().size());
    }
}

