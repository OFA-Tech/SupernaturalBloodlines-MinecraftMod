package net.ofatech.supernaturalbloodlines.core.bloodline;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/** The six Originals who are not players. Each is the root of a line under a fixed UUID, so blood bottles and the line index need no entity. */
public enum NpcOriginal {
    MIKAEL, FINN, ELIJAH, KOL, REBEKAH, KLAUS;

    private final UUID uuid = UUID.nameUUIDFromBytes(("supernaturalbloodlines:npc_original:" + name().toLowerCase()).getBytes(StandardCharsets.UTF_8));

    public String id() {
        return name().toLowerCase();
    }

    public UUID uuid() {
        return uuid;
    }

    public String translationKey() {
        return "supernaturalbloodlines.original." + id();
    }

    public static Optional<NpcOriginal> byUuid(UUID id) {
        return Arrays.stream(values()).filter(o -> o.uuid.equals(id)).findFirst();
    }

    public static Optional<NpcOriginal> byId(String id) {
        return Arrays.stream(values()).filter(o -> o.id().equals(id)).findFirst();
    }
}
