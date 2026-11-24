package net.ofatech.supernaturalbloodlines.core.group.circle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Who pays what for a linked rite: the members' share of the altar bill (power) and of the rite's EP, split equally with the remainder going to the first
 * members (the anchor first). A member the share would hurt too much, or who did not consent to that much, or whose EP would fall under the floor, is dropped
 * and the rest recomputed; fewer than two left is a solo cast.
 */
public final class CirclePlan {
    public enum DropReason { TOO_HEAVY, NEEDS_CONSENT, EP_FLOOR }

    public record Member(UUID id, int meter, int cap, Consent consent, double baseMaxEp, boolean anchor) {
    }

    public record Share(int power, double ep, ChannelMeter.Zone zone, int overdraw) {
    }

    public record Dropped(UUID id, DropReason reason) {
    }

    public record Result(int altarPays, Map<UUID, Share> shares, List<Dropped> dropped, int passes) {
    }

    private CirclePlan() {
    }

    /** {@code total} shared out among {@code n}: floor each, one more to each of the first {@code total % n}. */
    public static long[] split(long total, int n) {
        long[] out = new long[Math.max(n, 0)];
        if (n <= 0) {
            return out;
        }
        long each = total / n;
        long extra = total - each * n;
        for (int i = 0; i < n; i++) {
            out[i] = each + (i < extra ? 1 : 0);
        }
        return out;
    }

    public static Result plan(int altarBill, double epTotal, List<Member> ordered, double memberShare, int epKeepPercent) {
        List<Member> left = new ArrayList<>(ordered);
        List<Dropped> dropped = new ArrayList<>();
        int passes = 0;
        while (true) {
            passes++;
            if (left.size() < 2) {
                return new Result(altarBill, Map.of(), List.copyOf(dropped), passes);
            }
            int n = left.size();
            int pool = (int) Math.floor(Math.max(0, altarBill) * Math.max(0, memberShare) + 1e-9);
            pool = Math.min(pool, Math.max(0, altarBill));
            long[] power = split(pool, n);
            double wholeEp = Math.floor(Math.max(0, epTotal));
            long[] ep = split((long) wholeEp, n);
            double fraction = Math.max(0, epTotal) - wholeEp;
            Dropped worst = null;
            Map<UUID, Share> shares = new LinkedHashMap<>();
            for (int rank = 0; rank < 3 && worst == null; rank++) {
                for (int i = 0; i < n && worst == null; i++) {
                    Member m = left.get(i);
                    int overdraw = (int) power[i] - m.meter();
                    ChannelMeter.Zone zone = ChannelMeter.zone(overdraw, m.cap());
                    double epShare = ep[i] + (i == 0 ? fraction : 0);
                    DropReason reason = switch (rank) {
                        case 0 -> zone == ChannelMeter.Zone.REFUSED ? DropReason.TOO_HEAVY : null;
                        case 1 -> askedMoreThanAllowed(zone, m.consent()) ? DropReason.NEEDS_CONSENT : null;
                        default -> epShare > 0 && m.baseMaxEp() - epShare < m.baseMaxEp() * epKeepPercent / 100.0 ? DropReason.EP_FLOOR : null;
                    };
                    if (reason != null) {
                        worst = new Dropped(m.id(), reason);
                    }
                }
            }
            if (worst != null) {
                dropped.add(worst);
                UUID gone = worst.id();
                // the one who casts is the circle: without them it is a solo cast, and nobody else pays for a rite they do not hold
                if (left.stream().anyMatch(m -> m.anchor() && m.id().equals(gone))) {
                    return new Result(altarBill, Map.of(), List.copyOf(dropped), passes);
                }
                left.removeIf(m -> m.id().equals(gone));
                continue;
            }
            for (int i = 0; i < n; i++) {
                Member m = left.get(i);
                int overdraw = (int) power[i] - m.meter();
                shares.put(m.id(), new Share((int) power[i], ep[i] + (i == 0 ? fraction : 0), ChannelMeter.zone(overdraw, m.cap()), Math.max(0, overdraw)));
            }
            return new Result(altarBill - pool, shares, List.copyOf(dropped), passes);
        }
    }

    private static boolean askedMoreThanAllowed(ChannelMeter.Zone zone, Consent consent) {
        return switch (zone) {
            case NOSEBLEED -> consent == Consent.NONE;
            case DAMAGE -> consent != Consent.DAMAGE;
            default -> false;
        };
    }
}