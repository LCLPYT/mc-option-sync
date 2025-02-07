package work.lclpnet.opt_sync.lib;

import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import work.lclpnet.opt_sync.lib.cfg.SyncConfig;
import work.lclpnet.opt_sync.lib.cfg.SyncEntry;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class FileEntryMatcher {

    private final Path base;
    private final List<Pair<SyncEntry, PathMatcher>> matchers;
    private final Predicate<Path> ignore;

    public FileEntryMatcher(Path base, List<Pair<SyncEntry, PathMatcher>> matchers, Predicate<Path> ignore) {
        this.base = base;
        this.matchers = matchers;
        this.ignore = ignore;
    }

    public Optional<SyncEntry> entryOf(Path path) {
        Path rel = base.relativize(path);

        if (ignore.test(rel)) {
            return Optional.empty();
        }

        return matchers.stream()
                .filter(pair -> pair.second().matches(rel))
                .map(Pair::first)
                .findFirst();
    }

    public static FileEntryMatcher of(Path base, SyncConfig config, FileSystem fs, Logger logger) {
        var matchers = config.getSync().stream()
                .flatMap(entry -> entry.asFileRef().asPathMatcher(fs, logger)
                        .map(matcher -> Pair.of(entry, matcher))
                        .stream())
                .toList();

        return new FileEntryMatcher(base, matchers, ignoreChecker(config, fs, logger));
    }

    public static @NotNull Predicate<Path> ignoreChecker(SyncConfig config, FileSystem fs, Logger logger) {
        var matchers = config.getIgnore().stream()
                .flatMap(ref -> ref.asPathMatcher(fs, logger).stream())
                .toList();

        return path -> matchers.stream().anyMatch(m -> m.matches(path));
    }
}
