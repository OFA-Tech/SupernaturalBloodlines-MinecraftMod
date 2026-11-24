package net.ofatech.supernaturalbloodlines.core.ward;

/** Dropping a ward asks twice (R310): the first press arms it, a second press on the same ward runs it, a press on another ward moves the arming. */
public final class DropConfirm {
    public record Result(boolean run, String armed) {}

    private DropConfirm() {
    }

    public static Result press(String armed, String ward) {
        return !ward.isEmpty() && ward.equals(armed) ? new Result(true, "") : new Result(false, ward);
    }
}
