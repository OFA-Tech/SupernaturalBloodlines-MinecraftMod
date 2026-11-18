package net.ofatech.supernaturalbloodlines.shared.hub;

/**
 * Where the reply of a hub command may be drawn. An OP's debug actions (the {@code D} rows, {@code B2} and {@code B7}) answer with the server's own dump of a
 * player's state: that belongs to the Admin page and to the chat, and no player page draws it. Every other command's reply shows wherever it was run.
 */
public final class ReplyScope {
    private ReplyScope() {}

    public static boolean isDebug(String rowId) {
        return rowId.startsWith("D") || rowId.equals("B2") || rowId.equals("B7");
    }

    public static boolean shownOn(String rowId, HubPage page) {
        return page == HubPage.ADMIN || !isDebug(rowId);
    }
}

