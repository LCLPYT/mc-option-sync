package work.lclpnet.opt_sync.lib;

import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import work.lclpnet.opt_sync.lib.cfg.SyncConfig;
import work.lclpnet.opt_sync.lib.cfg.SyncEntry;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static work.lclpnet.opt_sync.OptSyncEntrypoint.MOD_ID;

public class OptionStorage {

    private final Path baseDir;
    private final SyncContext ctx;
    private final Logger logger;

    public OptionStorage(Path baseDir, SyncContext ctx, Logger logger) {
        this.baseDir = baseDir;
        this.ctx = ctx;
        this.logger = logger;
    }

    @Blocking
    public void init() throws IOException {
        if (Files.exists(baseDir)) return;

        Files.createDirectories(baseDir);
    }

    @Blocking
    public void pull(SyncConfig config) {
        Path dstDir = cwd();

        boolean success = pull(config, partial -> partial.allMatch(pair -> {
            Path srcDir = pair.first();
            Path file = pair.second();

            return handleCopy(srcDir, dstDir, file);
        })).allMatch(Boolean::booleanValue);

        if (!success) {
            logger.warn("Some files could not be pulled");
        }
    }

    @Blocking
    public boolean anyPullConflicts(SyncConfig config, Path dir) {
        return pull(config, partial -> partial.anyMatch(pair -> {
            Path moduleDir = pair.first();
            Path file = pair.second();

            return Files.exists(targetFile(moduleDir, dir, file));
        })).anyMatch(Boolean::booleanValue);
    }

    @Blocking
    public void push(SyncConfig config) {
        Path srcDir = cwd();

        FileEntryMatcher matcher = FileEntryMatcher.of(srcDir, config, FileSystems.getDefault(), logger);

        try (var stream = Files.walk(srcDir)) {
            stream.filter(Files::isRegularFile)
                    .flatMap(path -> matcher.entryOf(path)
                            .map(entry -> Pair.of(path, entry))
                            .stream())
                    .forEach(pair -> {
                        SyncEntry entry = pair.second();
                        Path dstDir = entryDir(entry).orElse(null);

                        if (dstDir == null) return;

                        Path file = pair.first();

                        handleCopy(srcDir, dstDir, file);
                    });
        } catch (IOException e) {
            logger.error("Failed to walk file tree {}", srcDir, e);
        }
    }

    private <R> Stream<R> pull(SyncConfig config, Function<Stream<Pair<Path, Path>>, R> action) {
        FileSystem fs = FileSystems.getDefault();
        var ignore = FileEntryMatcher.ignoreChecker(config, fs, logger);

        Set<Path> handled = new HashSet<>();

        var groupedByModule = config.getSync().stream()
                .collect(Collectors.groupingBy(entry -> Pair.of(entry.getModule(), entry.getVersion())));

        return groupedByModule.entrySet().stream().flatMap(group -> {
            var moduleTuple = group.getKey();
            Path srcDir = entryDir(moduleTuple.first(), moduleTuple.second()).orElse(null);

            if (srcDir == null || !Files.isDirectory(srcDir)) return Stream.empty();

            logger.debug("Pulling module {}", baseDir.relativize(srcDir));

            if (logger.isDebugEnabled()) {
                logger.debug("Matching files: {}", group.getValue().stream().map(SyncEntry::asFileRef).toList());
            }

            List<PathMatcher> matchers = group.getValue().stream()
                    .map(SyncEntry::asFileRef)
                    .flatMap(fileRef -> fileRef.asPathMatcher(fs, logger).stream())
                    .toList();

            if (matchers.isEmpty()) return Stream.empty();

            try (var stream = Files.walk(srcDir)) {
                R res = action.apply(stream
                        .filter(path -> !handled.contains(path))
                        .filter(Files::isRegularFile)
                        .filter(path -> {
                            Path rel = srcDir.relativize(path);

                            return !ignore.test(rel) && matchers.stream().anyMatch(matcher -> matcher.matches(rel));
                        })
                        .filter(handled::add)
                        .map(path -> Pair.of(srcDir, path)));

                return Stream.of(res);
            } catch (IOException e) {
                logger.error("Failed to walk file tree {}", srcDir, e);
                return Stream.empty();
            }
        });
    }

    private boolean handleCopy(Path srcDir, Path dstDir, Path file) {
        try {
            copyFile(srcDir, dstDir, file);
            return true;
        } catch (Exception e) {
            logger.error("Failed to sync file '{}' from '{}' into '{}'", dstDir.relativize(file), srcDir, dstDir);
            return false;
        }
    }

    private void copyFile(Path srcDir, Path dstDir, Path file) throws IOException {
        Path target = targetFile(srcDir, dstDir, file);

        if (file.toAbsolutePath().equals(target.toAbsolutePath())) {
            throw new IllegalStateException("Source and target files are the same: " + file.toAbsolutePath());
        }

        Path dir = target.getParent();

        if (dir != null && !Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        logger.debug("Copying {} -> {}", file, target);

        try (var srcChannel = FileChannel.open(file, StandardOpenOption.READ);
             var dstChannel = FileChannel.open(target, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
             FileLock ignored = srcChannel.lock(0, Long.MAX_VALUE, true);
             FileLock ignored1 = dstChannel.lock()) {

            srcChannel.transferTo(0, srcChannel.size(), dstChannel);
        }
    }

    private @NotNull Optional<Path> entryDir(SyncEntry entry) {
        return entryDir(entry.getModule(), entry.getVersion());
    }

    private @NotNull Optional<Path> entryDir(String module, @Nullable String version) {
        version = Optional.ofNullable(version)
                .or(ctx::version)
                .orElse("unknown");

        if (module.isBlank()) {
            logger.error("Module cannot be blank");
            return Optional.empty();
        }

        if (version.isBlank()) {
            logger.error("Version cannot be blank. Context version: {}", ctx.version().orElse("<none>"));
            return Optional.empty();
        }

        return Optional.of(baseDir.resolve(module).resolve(version));
    }

    private static @NotNull Path cwd() {
        return Path.of("");
    }

    private static @NotNull Path targetFile(Path oldBase, Path newBase, Path file) {
        Path rel = oldBase.relativize(file);
        return newBase.resolve(rel);
    }

    public static @NotNull Path getDataDir() {
        Path appDataPath = getOsDataDir();

        return appDataPath.resolve(MOD_ID);
    }

    private static @NotNull Path getOsDataDir() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");

        if (os.contains("win")) {
            // Windows: %APPDATA%\mc-option-sync (default ~\AppData\Roaming\mc-option-sync)
            String appData = System.getenv("APPDATA");

            if (appData == null || appData.isEmpty()) {
                return Path.of(userHome, "AppData", "Roaming");
            }

            return Path.of(appData);
        }

        if (os.contains("mac")) {
            // macOS: ~/Library/Application Support/mc-option-sync
            return Path.of(userHome, "Library", "Application Support");
        }

        // Linux: $XDG_DATA_HOME/mc-option-sync (default ~/.local/share/mc-option-sync)
        // https://specifications.freedesktop.org/basedir-spec/latest/
        String xdgDataHome = System.getenv("XDG_DATA_HOME");

        if (xdgDataHome == null || xdgDataHome.isEmpty()) {
            return Path.of(userHome, ".local", "share");
        }

        return Path.of(xdgDataHome);
    }
}
