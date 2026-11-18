package net.ofatech.supernaturalbloodlines.shared.hub;

import java.util.ArrayList;
import java.util.List;

/** Where the nav draws a gap or a heading: where the group changes along the visible pages. Pure; every theme reads the same answer. */
public final class NavGroups {
    /** A group heading sits over the page at index {@code tab}. */
    public record Heading(int tab, HubPage.Group group) {
    }

    private NavGroups() {}

    /** Per page, how many gap units sit before it: 0 inside a group, 1 where the group changes, 2 before Admin (apart from the rest). */
    public static int[] gaps(List<HubPage> pages) {
        int[] out = new int[pages.size()];
        for (int i = 1; i < pages.size(); i++) {
            HubPage.Group g = pages.get(i).group();
            if (g != pages.get(i - 1).group()) {
                out[i] = g == HubPage.Group.OPERATOR ? 2 : 1;
            }
        }
        return out;
    }

    /** One heading per group of the visible pages, over its first page; Home has none. */
    public static List<Heading> headings(List<HubPage> pages) {
        List<Heading> out = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            HubPage.Group g = pages.get(i).group();
            if (g != HubPage.Group.NONE && (i == 0 || g != pages.get(i - 1).group())) {
                out.add(new Heading(i, g));
            }
        }
        return out;
    }

    public static int[] noGaps(int tabs) {
        return new int[tabs];
    }

    public static int total(int[] gaps) {
        int n = 0;
        for (int g : gaps) {
            n += g;
        }
        return n;
    }
}

