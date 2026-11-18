package net.ofatech.supernaturalbloodlines.shared.hub;

/**
 * Where the reply of a hub command may be drawn. An OP's debug actions (every {@code ADMIN_ACTION} row of {@code AdminCommands}) answer with the server's own dump of a
 * player's state: that belongs to the Admin page and to the chat, and no player page draws it. Every other command's reply shows wherever it was run.
 */
public final class ReplyScope {
    private ReplyScope() {}

    public static boolean isDebug(String rowId) {
        return net.ofatech.supernaturalbloodlines.shared.admin.AdminCommands.byId(rowId).map(r -> r.ui() == net.ofatech.supernaturalbloodlines.shared.admin.AdminCommands.Ui.ADMIN_ACTION).orElse(false);
    }

    public static boolean shownOn(String rowId, HubPage page) {
        return page == HubPage.ADMIN || !isDebug(rowId);
    }
}

