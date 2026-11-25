package net.ofatech.supernaturalbloodlines;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** The project's own sources, for the tests that scan them. The tests run in build/minecraft-junit, so the root is the nearest parent with src/main/java. */
public final class SourceTree {
    private SourceTree() {
    }

    public static Path root() {
        Path base = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(base.resolve("src/main/java"))) {
            base = base.getParent();
        }
        return base;
    }

    /** Every .java file under {@code src/<set>/java}, as paths below the root with forward slashes. */
    public static List<String> javaFiles(String set) {
        Path dir = root().resolve("src/" + set + "/java");
        try (Stream<Path> files = Files.walk(dir)) {
            return files.filter(p -> p.toString().endsWith(".java")).map(p -> root().relativize(p).toString().replace('\\', '/')).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String read(String relative) {
        try {
            return Files.readString(root().resolve(relative));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
