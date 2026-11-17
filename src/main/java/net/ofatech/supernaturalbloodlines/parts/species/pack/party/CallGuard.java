package net.ofatech.supernaturalbloodlines.parts.species.pack.party;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import net.ofatech.supernaturalbloodlines.core.bloodline.FailureBreaker;

/**
 * Calls into the party mod, one at a time: a call that throws is counted, a call that answers (even with null) clears the count,
 * and {@code limit} failures in a row turn it off for good, telling {@code onTrip} once. Once it is off no call is made.
 */
final class CallGuard {
    private final FailureBreaker breaker;
    private final Consumer<Throwable> onTrip;

    CallGuard(int limit, Consumer<Throwable> onTrip) {
        this.breaker = new FailureBreaker(limit);
        this.onTrip = onTrip;
    }

    boolean off() {
        return breaker.tripped();
    }

    /** @return the answer, or empty when the call failed or the guard is off (a null answer is an answer: {@code Optional.of(null)} is not made, see {@link Answer}) */
    <T> Answer<T> call(Callable<T> c) {
        if (breaker.tripped()) {
            return Answer.none();
        }
        try {
            T result = c.call();
            breaker.record(true);
            return Answer.of(result);
        } catch (Exception | LinkageError e) {
            if (breaker.record(false)) {
                onTrip.accept(e);
            }
            return Answer.none();
        }
    }

    /** What a call gave back: nothing (it failed), or a value that may itself be null. */
    record Answer<T>(boolean ok, T value) {
        static <T> Answer<T> none() {
            return new Answer<>(false, null);
        }

        static <T> Answer<T> of(T value) {
            return new Answer<>(true, value);
        }

        Optional<T> toOptional() {
            return Optional.ofNullable(value);
        }
    }
}
