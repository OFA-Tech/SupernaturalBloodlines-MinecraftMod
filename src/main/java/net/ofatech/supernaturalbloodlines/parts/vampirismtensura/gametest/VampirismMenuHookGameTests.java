package net.ofatech.supernaturalbloodlines.parts.vampirismtensura.gametest;

import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.ofatech.supernaturalbloodlines.SupernaturalBloodlines;
import com.mojang.authlib.GameProfile;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.world.MenuProvider;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Plan 23 Task 0 P8: Vampirism's menu packet, handled for a server player without any key press. Logs a {@code PLAN23 P8} line. */
@PrefixGameTestTemplate(false)
public final class VampirismMenuHookGameTests {
    private static final String NS = SupernaturalBloodlines.MODID;

    private VampirismMenuHookGameTests() {
    }

    /** The handler only reads the player and queues its work; this context runs the work at once. */
    private static IPayloadContext contextOf(ServerPlayer p) {
        return (IPayloadContext) Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(), new Class<?>[] {IPayloadContext.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "player":
                    return p;
                case "enqueueWork":
                    if (args[0] instanceof Runnable r) {
                        r.run();
                        return CompletableFuture.completedFuture(null);
                    }
                    return CompletableFuture.completedFuture(((Supplier<?>) args[0]).get());
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                case "toString":
                    return "plan23-context";
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        });
    }

    private static final String HANDLER = "de.teamlapen.vampirism.server.ServerPayloadHandler";
    private static final String PAYLOAD = "de.teamlapen.vampirism.network.ServerboundSimpleInputEvent";

    /** Vampirism's own server handler, called the way its payload registration calls it (only the API is on our compile path, so by reflection). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String menuAfterPacket(MenuSpy p) throws ReflectiveOperationException {
        Class<?> payloadClass = Class.forName(PAYLOAD);
        Class<? extends Enum> eventClass = (Class<? extends Enum>) Class.forName(PAYLOAD + "$Event");
        Object payload = payloadClass.getConstructor(eventClass).newInstance(Enum.valueOf(eventClass, "VAMPIRISM_MENU"));
        Class<?> handlerClass = Class.forName(HANDLER);
        Object handler = handlerClass.getMethod("getInstance").invoke(null);
        handlerClass.getMethod("handleSimpleInputEvent", payloadClass, IPayloadContext.class).invoke(handler, payload, contextOf(p));
        return p.opened == null ? "unchanged" : p.opened;
    }

    /** A FakePlayer opens no menu; this one records the menu the server asked it to open, the way a real player's connection would show it. */
    private static final class MenuSpy extends FakePlayer {
        private String opened;

        private MenuSpy(GameTestHelper helper) {
            super(helper.getLevel(), new GameProfile(UUID.randomUUID(), "test-menu"));
        }

        @Override
        public OptionalInt openMenu(MenuProvider provider) {
            AbstractContainerMenu menu = provider.createMenu(1, getInventory(), this);
            opened = menu == null ? "null menu" : menu.getClass().getName();
            return OptionalInt.empty();
        }
    }

    @GameTest(template = "empty", templateNamespace = NS)
    public static void theVampirismMenuPacketOpensTheMenuWithoutAKey(GameTestHelper helper) throws ReflectiveOperationException {
        MenuSpy vampire = new MenuSpy(helper);
        MenuSpy human = new MenuSpy(helper);
        helper.assertTrue(VampirismAPI.factionPlayerHandler(vampire).setFactionAndLevel(VReference.VAMPIRE_FACTION, 1), "setup: could not join");
        String vampireMenu = menuAfterPacket(vampire);
        String humanMenu = menuAfterPacket(human);
        SupernaturalBloodlines.LOGGER.info("PLAN23 P8 handler={} vampire={} noFaction={}", HANDLER, vampireMenu, humanMenu);
        helper.assertTrue(!"unchanged".equals(vampireMenu), "the menu packet opened nothing for a vampire");
        // finding: the server answers the packet only for a player in a faction (getCurrentFactionPlayer is empty otherwise)
        helper.assertTrue("unchanged".equals(humanMenu), "the menu packet opened " + humanMenu + " for a player in no faction");
        helper.succeed();
    }
}
