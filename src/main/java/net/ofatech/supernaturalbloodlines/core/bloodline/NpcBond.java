package net.ofatech.supernaturalbloodlines.core.bloodline;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;

/**
 * What makes a mob a member of a player's pack (R148, R149): whose it is, how it came ({@code source}), what it was told to do ({@code order}), whether
 * it goes hunting ({@code hunt}) and tidies chests ({@code sort}), the post it holds and when it joined. Saved with the entity, every field optional.
 * A Tensura subordinate keeps Tensura's own orders: for it {@code order}, {@code hunt}, {@code sort} and {@code post} mean nothing and are never used.
 */
public record NpcBond(UUID owner, Source source, Order order, boolean hunt, boolean sort, long post, long since) {
    public enum Source { BROTHER, TENSURA, SPIRIT }

    public enum Order {
        FOLLOW, GUARD, REST;

        /** The next word of a typed or clicked order; null for a word that is no order. */
        public static Order of(String word) {
            for (Order o : values()) {
                if (o.name().equalsIgnoreCase(word)) {
                    return o;
                }
            }
            return null;
        }
    }

    /** What an entity with no bond reads as: nobody's. */
    public static final UUID NOBODY = new UUID(0, 0);
    public static final NpcBond NONE = new NpcBond(NOBODY, Source.BROTHER, Order.FOLLOW, false, false, 0, 0);

    public static final Codec<NpcBond> CODEC = RecordCodecBuilder.create(i -> i.group(
            BloodlineCodecs.UUID_CODEC.lenientOptionalFieldOf("owner", NOBODY).forGetter(NpcBond::owner),
            BloodlineCodecs.enumByName(Source.class, Source.BROTHER).lenientOptionalFieldOf("source", Source.BROTHER).forGetter(NpcBond::source),
            BloodlineCodecs.enumByName(Order.class, Order.FOLLOW).lenientOptionalFieldOf("order", Order.FOLLOW).forGetter(NpcBond::order),
            Codec.BOOL.lenientOptionalFieldOf("hunt", false).forGetter(NpcBond::hunt),
            Codec.BOOL.lenientOptionalFieldOf("sort", false).forGetter(NpcBond::sort),
            Codec.LONG.lenientOptionalFieldOf("post", 0L).forGetter(NpcBond::post),
            Codec.LONG.lenientOptionalFieldOf("since", 0L).forGetter(NpcBond::since)
    ).apply(i, NpcBond::new));

    public static NpcBond brother(UUID owner, long since, long post) {
        return new NpcBond(owner, Source.BROTHER, Order.FOLLOW, false, false, post, since);
    }

    public static NpcBond tensura(UUID owner, long since) {
        return new NpcBond(owner, Source.TENSURA, Order.FOLLOW, false, false, 0, since);
    }

    public boolean bonded() {
        return !NOBODY.equals(owner);
    }

    public NpcBond withOrder(Order o, long newPost) {
        return new NpcBond(owner, source, o, hunt, sort, newPost, since);
    }

    public NpcBond withHunt(boolean on) {
        return new NpcBond(owner, source, order, on, sort, post, since);
    }

    public NpcBond withSort(boolean on) {
        return new NpcBond(owner, source, order, hunt, on, post, since);
    }
}
