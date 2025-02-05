package work.lclpnet.opt_sync.lib;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.opt_sync.lib.cfg.ModuleConfig;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static work.lclpnet.opt_sync.OptSyncEntrypoint.MOD_ID;

public class OptionStorage {

    private final @NotNull Path baseDir;
    private final Logger logger;

    public OptionStorage(@NotNull Path baseDir, Logger logger) {
        this.baseDir = baseDir;
        this.logger = logger;
    }

    @Blocking
    public void init() throws IOException {
        if (Files.exists(baseDir)) return;

        Files.createDirectories(baseDir);
    }

    @Blocking
    public void pull(Module module) throws IOException {
        Path moduleDir = baseDir.resolve(module.modulePath());
        Path dir = moduleDir.resolve(module.version().toString());

        if (Files.exists(dir)) {
            pullDir(module, dir);
        } else {
            logger.debug("No data to pull for module {}", module);
        }
    }

    private @NotNull ModuleConfig readModuleConfig(Module module) {
        Path moduleDir = baseDir.resolve(module.modulePath());
        Path moduleCfgDir = moduleDir.resolve("module.toml");

        ModuleConfig cfg = module.createConfig();

        try (var manager = new ConfigManager<>(moduleCfgDir, cfg)) {
            manager.load();

            return manager.config();
        } catch (Throwable t) {
            logger.error("Failed to load module config", t);
            return cfg;
        }
    }

    @Blocking
    private void pullDir(Module module, Path dir) throws IOException {
        ModuleConfig config = readModuleConfig(module);
        FileMatcher matcher = FileMatcher.of(config, logger);

        try (var fileTree = Files.walk(dir)) {
            fileTree.filter(matcher)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            pullFile(module, dir, file);
                        } catch (Exception e) {
                            logger.error("Failed to pull file '{}' from module '{}'", dir.relativize(file), module);
                        }
                    });
        }
    }

    private void pullFile(Module module, Path root, Path source) throws IOException {
        Path rel = root.relativize(source);
        Path target = module.filePath().resolve(rel);

        if (source.toAbsolutePath().equals(target.toAbsolutePath())) {
            throw new IllegalStateException("Source and target files are the same: " + source.toAbsolutePath());
        }

        Path dir = target.getParent();

        if (dir != null && !Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        logger.debug("Pulling {} -> {}", source, target);

        try (var srcChannel = FileChannel.open(source, StandardOpenOption.READ);
             var dstChannel = FileChannel.open(target, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
             FileLock ignored = srcChannel.lock(0, Long.MAX_VALUE, true);
             FileLock ignored1 = dstChannel.lock()) {

            srcChannel.transferTo(0, srcChannel.size(), dstChannel);
        }
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
