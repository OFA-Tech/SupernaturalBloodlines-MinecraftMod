package net.ofatech.supernaturalbloodlines.core.ward;

import java.util.List;

/** Pages of a list the hub shows a few at a time (R310): every entry is on exactly one page, and a page number that is too big wraps around. */
public final class Paging {
    private Paging() {
    }

    public static int pages(int total, int size) {
        return Math.max(1, (total + size - 1) / size);
    }

    public static int wrap(int page, int total, int size) {
        return Math.floorMod(page, pages(total, size));
    }

    public static <T> List<T> slice(List<T> all, int page, int size) {
        int from = wrap(page, all.size(), size) * size;
        return all.subList(Math.min(from, all.size()), Math.min(from + size, all.size()));
    }
}
