package net.ofatech.supernaturalbloodlines.shared.hub.kit;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * What a page says about its selection (a task, a skill, a pack member): the one contract every theme draws in its own place, the
 * Grimoire as the page opposite, Blood Moon as the floating bar, Tensura as the info pane. The page fills it; the theme places and
 * styles it; an action is a server-validated request the page already knows how to send. At most one action is the primary one.
 *
 * @param kicker a small line over the title ("To become")
 * @param icon   a sprite beside the title
 * @param rows   task rows: a label, its source tag and progress (done of total)
 */
public record Detail(@Nullable Component kicker, Component title, @Nullable Component subtitle, @Nullable Icons icon, @Nullable SourceTag source, int pips,
                     int pipsFilled, List<Component> body, List<Row> rows, List<Field> fields, @Nullable Component reward, List<Action> actions) {
    public record Field(Component label, Component value) {
    }

    public record Row(Component label, @Nullable SourceTag source, long done, long total) {
        public boolean complete() {
            return total > 0 && done >= total;
        }
    }

    /** Asked before the action runs; the safe button is always "Stay" (Esc and Enter alone cancel). */
    public record Confirm(Component title, Component body) {
    }

    /**
     * @param confirm        when set the action asks first (the screen routes it through its confirm dialog)
     * @param disabledReason what to tell the player when the action cannot be used now (its tooltip)
     */
    public record Action(String id, Component label, boolean primary, boolean enabled, @Nullable Component disabledReason, @Nullable Confirm confirm,
                         Runnable run) {
    }

    public Detail {
        if (actions.stream().filter(Action::primary).count() > 1) {
            throw new IllegalArgumentException("a detail has one primary action");
        }
        pips = Math.max(0, pips);
        pipsFilled = Math.max(0, Math.min(pips, pipsFilled));
        body = List.copyOf(body);
        rows = List.copyOf(rows);
        fields = List.copyOf(fields);
        actions = List.copyOf(actions);
    }

    public static Builder of(Component title) {
        return new Builder(title);
    }

    public static final class Builder {
        private final Component title;
        private Component kicker;
        private Component subtitle;
        private Icons icon;
        private SourceTag source;
        private int pips;
        private int filled;
        private final List<Component> body = new ArrayList<>();
        private final List<Row> rows = new ArrayList<>();
        private final List<Field> fields = new ArrayList<>();
        private Component reward;
        private final List<Action> actions = new ArrayList<>();

        private Builder(Component title) {
            this.title = title;
        }

        public Builder kicker(Component c) {
            kicker = c;
            return this;
        }

        public Builder subtitle(Component c) {
            subtitle = c;
            return this;
        }

        public Builder icon(Icons i) {
            icon = i;
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

        public Builder row(Component label, SourceTag source, long done, long total) {
            rows.add(new Row(label, source, done, total));
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
            actions.add(new Action(id, label, primary, enabled, null, null, run));
            return this;
        }

        public Builder action(Action a) {
            actions.add(a);
            return this;
        }

        public Detail build() {
            return new Detail(kicker, title, subtitle, icon, source, pips, filled, body, rows, fields, reward, actions);
        }
    }
}
