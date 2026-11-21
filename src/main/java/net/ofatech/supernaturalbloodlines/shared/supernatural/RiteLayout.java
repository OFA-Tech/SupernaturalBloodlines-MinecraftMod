package net.ofatech.supernaturalbloodlines.shared.supernatural;

/** Where our rites are in Enchanted's book. Pure. */
public final class RiteLayout {
    private RiteLayout() {
    }

    /** The entry of one of our rites in Enchanted's book ({@code entries/supernaturalbloodlines/rites/<path>}); the book test pins this to the generated files. */
    public static String bookEntry(String riteId) {
        return "supernaturalbloodlines/rites/" + riteId.substring(riteId.indexOf(':') + 1);
    }
}
