package net.ofatech.supernaturalbloodlines.parts.supernatural.client.hub.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.ofatech.supernaturalbloodlines.core.group.GroupView;
import net.ofatech.supernaturalbloodlines.core.group.clan.ClanView;
import net.ofatech.supernaturalbloodlines.core.group.clan.ContestRules;

/**
 * What the clan cards say and offer, as data: lines (a lang key and its arguments) and buttons (id, row, lang key, enabled). No fonts, no drawing, so the
 * unit tests can walk every state. Counts only: nothing here can name anybody outside the viewer's clan.
 */
public final class ClanPlan {
    static final String KEY = PageKit.HUB + "clan.";

    public enum Tone { TITLE, INK, MUTED, WARN }

    public record Line(String key, List<Object> args, Tone tone) {
    }

    /** {@code label} and {@code tip} are lang keys; {@code why} is the lang key of the reason a disabled button gives, or empty. */
    public record Button(String id, String row, String label, String tip, boolean enabled, boolean primary, String why, List<Object> tipArgs) {
    }

    public record Plan(List<Line> lines, List<Button> buttons) {
        public Plan {
            lines = List.copyOf(lines);
            buttons = List.copyOf(buttons);
        }
    }

    private ClanPlan() {
    }

    private static Line line(Tone tone, String key, Object... args) {
        return new Line(KEY + key, List.of(args), tone);
    }

    /** {@code key}, or {@code key.one} when the count (the last argument) is 1: "1 day", "30 days". */
    private static Line counted(Tone tone, String key, int count, Object... args) {
        return line(tone, count == 1 ? key + ".one" : key, args);
    }

    private static String rankKey(byte viewerRank) {
        return switch (viewerRank) {
            case GroupView.LEADER -> "rank.head";
            case GroupView.DAY_WALKER -> "rank.day_walker";
            default -> "rank.night_walker";
        };
    }

    /** A wait in a unit: "45 s", "12 min", "5 h" (the countdown of an answer window is {@link #clock}). */
    public static String wait(int seconds) {
        return net.ofatech.supernaturalbloodlines.core.group.clan.ClanTime.span(seconds * 1000L);
    }

    public static String clock(int seconds) {
        return seconds >= 3600 ? (seconds + 3599) / 3600 + " h" : seconds / 60 + ":" + String.format("%02d", seconds % 60);
    }

    /** The header lines: who the viewer is in the clan and what comes next. */
    static List<Line> header(ClanView v, byte viewerRank) {
        List<Line> out = new ArrayList<>();
        out.add(line(Tone.TITLE, "title"));
        out.add(v.daysInClan() == 1 ? line(Tone.INK, "you.one", new Key(KEY + rankKey(viewerRank))) : line(Tone.INK, "you", new Key(KEY + rankKey(viewerRank)), v.daysInClan()));
        if (viewerRank == GroupView.MEMBER) {
            out.add(v.autoPromoteDaysLeft() >= 0 ? counted(Tone.MUTED, "to_day_walker", v.autoPromoteDaysLeft(), v.autoPromoteDaysLeft()) : line(Tone.MUTED, "head_decides"));
        }
        return out;
    }

    /** A lang key passed as an argument: the page translates it first. */
    public record Key(String key) {
    }

    static Line charterState(ClanView.Charter c) {
        return switch (c.state()) {
            case ClanView.CHARTER_HELD -> c.mine() ? line(Tone.INK, "charter.held_you")
                    : c.holderName().isEmpty() ? line(Tone.INK, "charter.held_mate") : line(Tone.INK, "charter.held", c.holderName());
            case ClanView.CHARTER_STORED -> line(Tone.MUTED, "charter.stored");
            case ClanView.CHARTER_DROPPED -> line(Tone.WARN, "charter.dropped");
            case ClanView.CHARTER_LOST -> line(Tone.WARN, "charter.lost");
            case ClanView.CHARTER_INERT -> line(Tone.MUTED, "charter.inert");
            default -> line(Tone.MUTED, "charter.none");
        };
    }

    public static Plan of(ClanView v, byte viewerRank) {
        List<Line> lines = new ArrayList<>(header(v, viewerRank));
        List<Button> buttons = new ArrayList<>();
        boolean head = viewerRank == GroupView.LEADER;
        ClanView.Charter c = v.charter();
        ClanView.Contest k = v.contest();

        lines.add(line(Tone.TITLE, "charter.title"));
        lines.add(charterState(c));
        if (head && c.state() == ClanView.CHARTER_NONE) {
            buttons.add(new Button("clan:charter_forge", "C16", "charter_forge", "charter_forge.what", c.forgeable(), true, c.forgeable() ? "" : "charter_forge.few", List.of()));
        }
        if (head && c.state() == ClanView.CHARTER_LOST) {
            boolean ready = c.reforgeSecondsLeft() <= 0;
            if (!ready) {
                lines.add(line(Tone.MUTED, "charter.reforge_wait", wait(c.reforgeSecondsLeft())));
            }
            buttons.add(new Button("clan:charter_reforge", "C17", "charter_reforge", "charter_reforge.what", ready, true, ready ? "" : "charter_reforge.wait", List.of()));
        }

        if (c.state() != ClanView.CHARTER_NONE && c.state() != ClanView.CHARTER_INERT) {
            if (k.phase() == ClanView.CONTEST_PENDING) {
                lines.add(k.challengerName().isEmpty() ? line(Tone.INK, "contest.pending_anon") : line(Tone.INK, "contest.pending", k.challengerName()));
                lines.add(line(Tone.MUTED, "contest.pending_left", clock(k.secondsLeft())));
                if (head && k.iMustAccept()) {
                    buttons.add(new Button("pack:duel_accept", "C12", "accept", "accept.what", true, true, "", List.of()));
                }
            } else if (k.phase() == ClanView.CONTEST_RUNNING) {
                lines.add(line(Tone.INK, "contest.running"));
            } else if (viewerRank == GroupView.DAY_WALKER) {
                String why = k.iMayChallenge() ? "" : ContestRules.Refusal.fromWire(k.challengeWhy()).map(r -> "why." + r.id()).orElse("why.unknown");
                buttons.add(new Button("pack:challenge", "C11", "challenge", "challenge.what", k.iMayChallenge(), true, why, List.of()));
                Optional<ContestRules.Refusal> refusal = k.iMayChallenge() ? Optional.empty() : ContestRules.Refusal.fromWire(k.challengeWhy());
                if (refusal.filter(r -> r == ContestRules.Refusal.COOLDOWN).isPresent()) {
                    lines.add(counted(Tone.MUTED, "contest.cooldown", k.waitDaysLeft(), k.waitDaysLeft()));
                } else if (refusal.filter(r -> r == ContestRules.Refusal.TOO_NEW).isPresent() && k.waitDaysLeft() > 0) {
                    lines.add(counted(Tone.MUTED, "contest.too_new", k.waitDaysLeft(), k.waitDaysLeft()));
                }
            } else if (k.waitDaysLeft() > 0) {
                lines.add(counted(Tone.MUTED, "contest.cooldown", k.waitDaysLeft(), k.waitDaysLeft()));
            }
        }

        if (v.feedOn() && v.feedPercent() > 0) {
            lines.add(line(Tone.MUTED, "feed", v.feedPercent()));
        }
        lines.add(v.bonusPercent() > 0 ? line(Tone.MUTED, "line", v.bonusPercent()) : line(Tone.MUTED, "line_none"));
        return new Plan(lines, buttons);
    }
}
