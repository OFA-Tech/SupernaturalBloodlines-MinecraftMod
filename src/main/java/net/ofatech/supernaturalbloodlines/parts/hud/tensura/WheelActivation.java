package net.ofatech.supernaturalbloodlines.parts.hud.tensura;

import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.impl.TickingSkill;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The scope the wheel opens around a Tensura skill that is in no preset slot, so that Tensura's own "is it in the active preset" check ({@code NextPreset.matches}) says yes
 * for that one skill while we start it, and while it runs.
 * <ul>
 * <li>{@link #scope}: thread-local, restored in {@code finally}, nests. {@link #allows} is true only for the scoped skill (and the scoped mode, or any mode when the caller asks for none).</li>
 * <li>The hold bracket: Tensura's {@code getMaxHeldTime} is asked <em>while the skill ticks</em> (20 ticks when the skill is in no slot), long after our scope closed.
 * So the skills the wheel started are kept per player ({@link #holding}), and around that player's tick ({@code PlayerTickEvent.Pre} to {@code Post}, which contains
 * ManasCore's own living tick) a scope is open for each of them. An entry ends on {@link #stopHolding}, when the skill is neither held nor toggled any more, and on logout, death, dimension change.</li>
 * </ul>
 * Linked to Tensura and ManasCore: reached only from {@code NextPreset} (applied only with Tensura) and the Tensura use path.
 */
public final class WheelActivation {
    private record Scope(ManasSkill skill, int mode) {
    }

    private static final ThreadLocal<Deque<Scope>> SCOPES = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<List<Scope>> TICKING = ThreadLocal.withInitial(ArrayList::new);
    private static final Map<UUID, List<Scope>> HOLDING = new ConcurrentHashMap<>();
    private static boolean registered;

    private WheelActivation() {
    }

    public static <T> T scope(ManasSkill skill, int mode, Supplier<T> body) {
        Deque<Scope> stack = SCOPES.get();
        stack.push(new Scope(skill, mode));
        try {
            return body.get();
        } finally {
            stack.pop();
        }
    }

    /** True only inside a scope (or the player's tick bracket) for this very skill; {@code mode} must be the scoped one or {@link NextPreset#ANY}. */
    public static boolean allows(ManasSkill skill, int slot, int mode) {
        for (Scope s : SCOPES.get()) {
            if (matches(s, skill, mode)) {
                return true;
            }
        }
        for (Scope s : TICKING.get()) {
            if (matches(s, skill, mode)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(Scope s, ManasSkill skill, int mode) {
        return s.skill == skill && (mode == NextPreset.ANY || mode == s.mode);
    }

    /** The wheel started this skill for the player: keep a scope open around their tick while it runs. */
    public static void holding(ServerPlayer p, ManasSkill skill, int mode) {
        List<Scope> list = HOLDING.computeIfAbsent(p.getUUID(), k -> new java.util.concurrent.CopyOnWriteArrayList<>());
        Scope s = new Scope(skill, mode);
        if (!list.contains(s)) {
            list.add(s);
        }
    }

    public static void stopHolding(ServerPlayer p, ManasSkill skill, int mode) {
        List<Scope> list = HOLDING.get(p.getUUID());
        if (list != null) {
            list.remove(new Scope(skill, mode));
            if (list.isEmpty()) {
                HOLDING.remove(p.getUUID());
            }
        }
    }

    /** Logout, death, dimension change: nothing the wheel started is bracketed any more. */
    public static void stopAll(ServerPlayer p) {
        HOLDING.remove(p.getUUID());
    }

    public static boolean isHolding(ServerPlayer p, ManasSkill skill) {
        List<Scope> list = HOLDING.get(p.getUUID());
        if (list == null) {
            return false;
        }
        for (Scope s : list) {
            if (s.skill == skill) {
                return true;
            }
        }
        return false;
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(WheelActivation::onTickPre);
        NeoForge.EVENT_BUS.addListener(WheelActivation::onTickPost);
    }

    private static void onTickPre(PlayerTickEvent.Pre e) {
        List<Scope> ticking = TICKING.get();
        ticking.clear();
        if (!(e.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        List<Scope> list = HOLDING.get(p.getUUID());
        if (list == null) {
            return;
        }
        for (Scope s : list) {
            if (stillRunning(p, s)) {
                ticking.add(s);
            } else {
                list.remove(s);
            }
        }
        if (list.isEmpty()) {
            HOLDING.remove(p.getUUID());
        }
    }

    private static void onTickPost(PlayerTickEvent.Post e) {
        TICKING.get().clear();
    }

    private static boolean stillRunning(ServerPlayer p, Scope s) {
        if (TickingSkill.isTickingSkill(p, s.skill, s.mode)) {
            return true;
        }
        return SkillAPI.getSkillsFrom(p).getSkill(s.skill).map(ManasSkillInstance::isToggled).orElse(false);
    }
}
