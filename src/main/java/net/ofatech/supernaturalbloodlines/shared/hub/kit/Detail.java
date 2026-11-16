package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * What a page says about its selection (a task, a skill, a pack member): the one contract every theme draws in its own place, the
 * Grimoire as the page opposite, Blood Moon as the floating bar, Tensura as the info pane. The page fills it; the theme places and
 * styles it; an action is a server-validated request the page already knows how to send. At most one action is the primary one.
 */
public record Detail(Component title, @Nullable Component subtitle, @Nullable SourceTag source, int pips, int pipsFilled, List<Component> body,
                     List<Field> fields, @Nullable Component reward, List<Action> actions) {
    public record Field(Component label, Component value) {
    }

    public record Action(String id, Component label, boolean primary, boolean enabled, Runnable run) {
    }

    public Detail {
        if (actions.stream().filter(Action::primary).count() > 1) {
            throw new IllegalArgumentException("a detail has one primary action");
        }
        pips = Math.max(0, pips);
        pipsFilled = Math.max(0, Math.min(pips, pipsFilled));
        body = List.copyOf(body);
        fields = List.copyOf(fields);
        actions = List.copyOf(actions);
    }

    public static Builder of(Component title) {
        return new Builder(title);
    }

    public static final class Builder {
        private final Component title;
        private Component subtitle;
        private SourceTag source;
        private int pips;
        private int filled;
        private final List<Component> body = new ArrayList<>();
        private final List<Field> fields = new ArrayList<>();
        private Component reward;
        private final List<Action> actions = new ArrayList<>();

        private Builder(Component title) {
            this.title = title;
        }

        public Builder subtitle(Component c) {
            subtitle = c;
            return this;
        }

        public Builder source(SourceTag s) {
            source = s;
            return this;
        }

        public Builder pips(int filled, int total) {
            this.filled = filled;
            pips = total;
            return this;
        }

        public Builder body(Component c) {
            body.add(c);
            return this;
        }

        public Builder field(Component label, Component value) {
            fields.add(new Field(label, value));
            return this;
        }

        public Builder reward(Component c) {
            reward = c;
            return this;
        }

        public Builder action(String id, Component label, boolean primary, boolean enabled, Runnable run) {
            actions.add(new Action(id, label, primary, enabled, run));
            return this;
        }

        public Detail build() {
            return new Detail(title, subtitle, source, pips, filled, body, fields, reward, actions);
        }
    }
}
