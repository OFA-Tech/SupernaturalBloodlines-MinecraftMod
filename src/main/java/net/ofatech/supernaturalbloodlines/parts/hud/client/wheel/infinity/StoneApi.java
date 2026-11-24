package net.ofatech.supernaturalbloodlines.parts.hud.client.wheel.infinity;

import java.util.List;

/**
 * What the wheel asks of Infinity Stones. The reflective {@link ReflectiveStoneApi} answers it from his classes; the tests answer it with a fake. Every call is on the client
 * thread and never throws: what cannot be read is empty, false or a no-op.
 */
public interface StoneApi {
    /** The powers of the stones the player carries, in his order, as his server last sent them (locked ones too, with {@code unlocked} false). */
    List<NodeInfo> nodes();

    /** The mode id ({@code stone.node}) of the channel in progress, or an empty string. */
    String holding();

    /** When the state was received (ms since the epoch), 0 when never. */
    long receivedAt();

    /** His {@code WheelPayloads.Use}: phase 0 use, 1 stop a channel, 2 the second option. */
    void use(int stone, String node, int phase);

    /** His {@code WheelPayloads.Setting}: change a power's level by {@code delta} (+1 or -1). */
    void setLevel(int stone, String node, int delta);

    /** His {@code DestinationScreen} for a power, opened over whatever screen is up (the caller has left the wheel): its confirm sends his own payloads. */
    void openDestination(int stone, String node, boolean movesTarget);

    /** His {@code ConstructScreen} (Will Construct's menu). */
    void openConstruct();

    /** His {@code QuickSlotsScreen} (Will Construct's quick slots). */
    void openQuickSlots();

    /** His {@code ExtraClientApi.requestMenu}: asks his server for the power's choice menu, which it answers by opening his screen. */
    void requestChoiceMenu(int stone, String node);

    /** The language or the resources were reloaded: the cached names and descriptions are read again. */
    void reload();

    /** Whether his possession classes were found, so the soul control entry and the covered key can work. */
    boolean soulSupported();

    /** His {@code SoulNet.state.mode()}: 0 nothing possessed, 1 spectate, 2 control, 3 mob. */
    int soulMode();

    /** His {@code SoulNet.Action(action, 0)} sent to his server, exactly what his key sends. */
    void soulAction(int action);
}
