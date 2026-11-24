package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity;

import java.util.ArrayList;
import java.util.List;
import net.ofatech.supernaturalbloodlines.core.wheel.StoneMenus;

/** A stand-in for Infinity Stones: a list of powers the test sets, and a record of every call the wheel makes. */
final class FakeStoneApi implements StoneApi {
    final List<NodeInfo> nodes = new ArrayList<>();
    final List<String> calls = new ArrayList<>();
    String holding = "";
    long receivedAt = 10_000;
    boolean soulSupported = true;
    int soulMode;

    static final String[] STONES = {"mind", "power", "space", "reality", "soul", "time"};

    FakeStoneApi node(int stone, String id, boolean unlocked, boolean isSwitch, boolean isChannel, boolean needsAim, boolean isDual, NodeInfo.Levels levels) {
        nodes.add(new NodeInfo(stone, STONES[stone], id, "Name " + id, "Does " + id, unlocked, false, 0, isSwitch, isChannel, needsAim, isDual, levels, StoneMenus.Kind.NONE, false));
        return this;
    }

    FakeStoneApi menu(int stone, String id, boolean needsAim, boolean isDual, StoneMenus.Kind menu, boolean menuOnLeft) {
        nodes.add(new NodeInfo(stone, STONES[stone], id, "Name " + id, "Does " + id, true, false, 0, false, false, needsAim, isDual, null, menu, menuOnLeft));
        return this;
    }

    FakeStoneApi plain(int stone, String id, boolean unlocked) {
        return node(stone, id, unlocked, false, false, false, false, null);
    }

    @Override
    public List<NodeInfo> nodes() {
        return nodes;
    }

    @Override
    public String holding() {
        return holding;
    }

    @Override
    public long receivedAt() {
        return receivedAt;
    }

    @Override
    public void use(int stone, String node, int phase) {
        calls.add("use " + stone + " " + node + " " + phase);
    }

    @Override
    public void setLevel(int stone, String node, int delta) {
        calls.add("level " + stone + " " + node + " " + delta);
    }

    @Override
    public void openDestination(int stone, String node, boolean movesTarget) {
        calls.add("destination " + stone + " " + node + " " + movesTarget);
    }

    @Override
    public void openConstruct() {
        calls.add("construct");
    }

    @Override
    public void openQuickSlots() {
        calls.add("quick");
    }

    @Override
    public void requestChoiceMenu(int stone, String node) {
        calls.add("choice " + stone + " " + node);
    }

    @Override
    public void reload() {
        calls.add("reload");
    }

    @Override
    public boolean soulSupported() {
        return soulSupported;
    }

    @Override
    public int soulMode() {
        return soulMode;
    }

    @Override
    public void soulAction(int action) {
        calls.add("soul " + action);
    }
}
