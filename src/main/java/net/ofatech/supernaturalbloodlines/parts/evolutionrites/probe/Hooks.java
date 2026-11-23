package net.ofatech.supernaturalbloodlines.parts.evolutionrites.probe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Looks up what an addon's class has by reflection, so the part has no compile dependency on any addon. Never throws: a hook that is not there comes
 * back as the name of what is missing, which is what the probes and the hook GameTest print.
 */
public final class Hooks {
    private Hooks() {
    }

    public record Hit(Optional<MethodHandle> handle, Optional<String> missing) {
        public boolean found() {
            return handle.isPresent();
        }
    }

    public record FieldHit(Optional<Object> value, Optional<String> missing) {
        public boolean found() {
            return missing.isEmpty();
        }
    }

    private static Class<?> load(String className) throws ClassNotFoundException {
        return Class.forName(className, false, Hooks.class.getClassLoader());
    }

    private static String simple(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    public static Hit method(String className, String name, Class<?>... params) {
        try {
            Class<?> c = load(className);
            Method m = c.getMethod(name, params);
            return new Hit(Optional.of(MethodHandles.publicLookup().unreflect(m)), Optional.empty());
        } catch (ClassNotFoundException | LinkageError e) {
            return new Hit(Optional.empty(), Optional.of(className));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            String sig = Arrays.stream(params).map(Class::getSimpleName).collect(Collectors.joining(","));
            return new Hit(Optional.empty(), Optional.of(simple(className) + "#" + name + "(" + sig + ")"));
        }
    }

    /** A public static field's current value. */
    public static FieldHit staticField(String className, String field) {
        try {
            Field f = load(className).getField(field);
            if (!Modifier.isStatic(f.getModifiers())) {
                return new FieldHit(Optional.empty(), Optional.of(simple(className) + "#" + field + " (not static)"));
            }
            return new FieldHit(Optional.ofNullable(f.get(null)), Optional.empty());
        } catch (ClassNotFoundException | LinkageError e) {
            return new FieldHit(Optional.empty(), Optional.of(className));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return new FieldHit(Optional.empty(), Optional.of(simple(className) + "#" + field));
        }
    }
}
