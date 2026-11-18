package net.ofatech.supernaturalbloodlines.shared.supernatural;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/**
 * A person's name in a chat message as a link to their Profile in the hub: a click runs the client-only command {@value #COMMAND}, which opens the hub
 * on that person (the server never sees it; a client without the hub shows the name as it always did). Only the id and the name the message already shows.
 */
public final class ProfileLink {
    public static final String COMMAND = "sbprofile";

    private ProfileLink() {}

    public static String command(UUID id, String name) {
        return "/" + COMMAND + " " + id + " " + name;
    }

    public static Component name(String name, UUID id) {
        return Component.literal(name).withStyle(s -> s.withUnderlined(true).withColor(ChatFormatting.WHITE)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command(id, name)))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("supernaturalbloodlines.hub.profile.link"))));
    }
}

