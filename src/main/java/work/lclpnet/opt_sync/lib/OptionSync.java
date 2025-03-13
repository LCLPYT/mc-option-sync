package work.lclpnet.opt_sync.lib;

import com.electronwill.nightconfig.core.file.GenericBuilder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import work.lclpnet.kibu.config.ConfigManager;
import work.lclpnet.opt_sync.lib.cfg.SyncConfig;

import java.io.IOException;
import java.nio.file.Path;

public class OptionSync {

    private final OptionStorage storage;
    private final SyncContext ctx;
    private final ConflictHandler conflictHandler;
    private final Logger logger;
    private volatile @Nullable SyncConfig config = null;
    private volatile boolean enabled = true;

    public OptionSync(OptionStorage storage, SyncContext ctx, ConflictHandler conflictHandler, Logger logger) {
        this.storage = storage;
        this.ctx = ctx;
        this.conflictHandler = conflictHandler;
        this.logger = logger;
    }

    public synchronized void init() {
        if (config != null) return;

        Path configFile = ctx.configDir().resolve("config.toml");
        var config = new SyncConfig();

        try (var cfgManager = new ConfigManager<>(configFile, config, GenericBuilder::sync)) {
            cfgManager.load();
        } catch (Throwable t) {
            logger.error("Failed to load config {}", configFile, t);
            return;
        }

        this.config = config;
    }

    public synchronized void pullOptions() {
        if (!enabled || config == null) return;

        logger.info("Pulling synced options...");

        if (initStorage()) return;

        var checker = new ConflictChecker(storage, conflictHandler, logger);

        if (checker.check(config)) {
            enabled = false;
            logger.warn("Conflicts detected, disabling options sync...");
            return;
        }

        storage.pull(config);

        logger.info("Options are now up-to-date");
    }

    public synchronized void pushOptions() {
        if (!enabled || config == null) return;

        logger.info("Pushing synced options...");

        if (initStorage()) return;

        storage.push(config);

        logger.info("Pushed options successfully");
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
