package work.lclpnet.opt_sync.lib;

import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.semver4j.Semver;
import org.slf4j.Logger;
import work.lclpnet.opt_sync.lib.cfg.SyncConfig;
import work.lclpnet.opt_sync.lib.cfg.SyncEntry;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static work.lclpnet.opt_sync.lib.Constants.MOD_ID;

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
                .collect(Collectors.groupingBy(entry -> Pair.of(entry.getModule(), versionWithContext(entry.getVersion()))));

        return groupedByModule.entrySet().stream().flatMap(group -> {
            var moduleTuple = group.getKey();
            Path srcDir = entryDir(moduleTuple.first(), moduleTuple.second()).orElse(null);

            if (srcDir == null) {
                return Stream.empty();
            }

            if (!Files.isDirectory(srcDir)) {
                logger.info("Module {} doesn't exist yet, trying to find an older version...", baseDir.relativize(srcDir));

                // try to find older version of the module
                var olderSrc = findOlderSrc(moduleTuple.first(), moduleTuple.second());

                if (olderSrc.isEmpty()) {
                    logger.info("No older version was found for module {}, skipping", baseDir.relativize(srcDir));
                    return Stream.empty();
                }

                logger.info("Using closest older version: {} -> {}", baseDir.relativize(olderSrc.get()), baseDir.relativize(srcDir));

                srcDir = olderSrc.get();
            }

            Path finalSrcDir = srcDir;

            logger.debug("Pulling module {}", baseDir.relativize(finalSrcDir));

            if (logger.isDebugEnabled()) {
                logger.debug("Matching files: {}", group.getValue().stream().map(SyncEntry::asFileRef).toList());
            }

            List<PathMatcher> matchers = group.getValue().stream()
                    .map(SyncEntry::asFileRef)
                    .flatMap(fileRef -> fileRef.asPathMatcher(fs, logger).stream())
                    .toList();

            if (matchers.isEmpty()) return Stream.empty();

            try (var stream = Files.walk(finalSrcDir)) {
                R res = action.apply(stream
                        .filter(path -> !handled.contains(path))
                        .filter(Files::isRegularFile)
                        .filter(path -> {
                            Path rel = finalSrcDir.relativize(path);

                            return !ignore.test(rel) && matchers.stream().anyMatch(matcher -> matcher.matches(rel));
                        })
                        .filter(handled::add)
                        .map(path -> Pair.of(finalSrcDir, path)));

                return Stream.of(res);
            } catch (IOException e) {
                logger.error("Failed to walk file tree {}", finalSrcDir, e);
                return Stream.empty();
            }
        });
    }

    private Optional<Path> findOlderSrc(String module, String version) {
        var semver = Semver.parse(version);

        if (semver == null) {
            logger.info("'{}' is not a valid semantic version; can't compare against other versions", version);
            return Optional.empty();
        }

        Path moduleDir = moduleDir(module).orElse(null);

        if (moduleDir == null) {
            return Optional.empty();
        }

        try (var stream = Files.list(moduleDir)) {
            return stream.filter(Files::isDirectory)
                    .map(dir -> dir.getFileName().toString())
                    .filter(dir -> !dir.equals(version))
                    .map(Semver::parse)
                    .filter(Objects::nonNull)
                    .filter(semver::isGreaterThan)
                    .max(Comparator.naturalOrder())
                    .map(latestSmaller -> moduleDir.resolve(latestSmaller.toString()))
                    .filter(Files::isDirectory);  // make sure the conversion didn't drop any part of the dirname
        } catch (IOException e) {
            logger.error("Failed to list module versions: {}", moduleDir, e);
            return Optional.empty();
        }
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
        return entryDir(entry.getModule(), versionWithContext(entry.getVersion()));
    }

    private @NotNull Optional<Path> entryDir(String module, String version) {
        if (version.isBlank()) {
            logger.error("Version cannot be blank. Context version: {}", ctx.version().orElse("<none>"));
            return Optional.empty();
        }

        return moduleDir(module).map(dir -> dir.resolve(version));
    }

    private @NotNull Optional<Path> moduleDir(String module) {
        if (module.isBlank()) {
            logger.error("Module cannot be blank");
            return Optional.empty();
        }

        return Optional.of(baseDir.resolve(module));
    }

    private @NotNull String versionWithContext(@Nullable String version) {
        return Optional.ofNullable(version)
                .or(ctx::version)
                .orElse("unknown");
    }

    private static @NotNull Path cwd() {
        return Path.of("");
    }

    private static @NotNull Path targetFile(Path oldBase, Path newBase, Path file) {
        Path rel = oldBase.relativize(file);
        return newBase.resolve(rel);
    }

    public static @NotNull Path getDataDir() {
        Path appDataPath = OsUtil.getOsDataDir();

        return appDataPath.resolve(MOD_ID);
    }
}
