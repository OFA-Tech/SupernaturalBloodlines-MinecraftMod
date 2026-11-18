package net.ofatech.supernaturalbloodlines.shared.hub;

import java.util.Optional;
import java.util.UUID;

/** The person a profile hands to the Admin page ("Open in Admin"): set once, taken once by the page when it next draws. Pure. */
public final class AdminHandoff {
    public record Person(UUID id, String name) {
    }

    private static volatile Person pending;

    private AdminHandoff() {}

    public static void offer(UUID id, String name) {
        pending = new Person(id, name);
    }

    /** The person offered since the last take, once. */
    public static Optional<Person> take() {
        Person p = pending;
        pending = null;
        return Optional.ofNullable(p);
    }
}

