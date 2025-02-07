package work.lclpnet.opt_sync.lib;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.opt_sync.lib.cfg.SyncConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class OptionSync {

    private final OptionStorage storage;
    private final SyncContext ctx;
    private final Logger logger;
    private volatile @Nullable SyncConfig config = null;
    private volatile boolean enabled = true;

    public OptionSync(OptionStorage storage, SyncContext ctx, Logger logger) {
        this.storage = storage;
        this.ctx = ctx;
        this.logger = logger;
    }

    public synchronized void init() {
        if (config != null) return;

        Path configFile = ctx.configDir().resolve("config.toml");
        var config = new SyncConfig();

        try (var cfgManager = new ConfigManager<>(configFile, config)) {
            cfgManager.load();
        }

        this.config = config;
    }

    public synchronized void pullOptions() {
        if (!enabled) return;

        SyncConfig cfg = requireConfig();

        logger.info("Pulling synced options...");

        if (initStorage()) return;

        var checker = new ConflictChecker(storage, logger);

        if (checker.check(cfg)) {
            enabled = false;
            logger.warn("Conflicts detected, disabling options sync...");
            return;
        }

        storage.pull(cfg);

        logger.info("Options are now up-to-date");
    }

    public synchronized void pushOptions() {
        if (!enabled) return;

        SyncConfig cfg = requireConfig();

        logger.info("Pushing synced options...");

        if (initStorage()) return;

        storage.push(cfg);

        logger.info("Pushed options successfully");
    }

    private @NotNull SyncConfig requireConfig() {
        return Objects.requireNonNull(config, "OptionSync not initialized");
    }

    private boolean initStorage() {
        try {
            storage.init();
        } catch (IOException e) {
            logger.error("Failed to initialize option storage; options cannot be synced", e);
            return true;
        }

        return false;
    }
}
