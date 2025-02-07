package work.lclpnet.opt_sync.lib;

import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collection;

public class OptionSync {

    private final OptionStorage storage;
    private final ModuleProvider moduleProvider;
    private final Logger logger;
    private volatile boolean enabled = true;

    public OptionSync(OptionStorage storage, ModuleProvider moduleProvider, Logger logger) {
        this.storage = storage;
        this.moduleProvider = moduleProvider;
        this.logger = logger;
    }

    public synchronized void pullOptions() {
        if (!enabled) return;

        logger.info("Pulling synced options...");

        if (initStorage()) return;

        var modules = modules();

        var checker = new ConflictChecker(storage, logger);

        if (!checker.check(modules)) {
            enabled = false;
            logger.warn("Conflicts detected, disabling options sync...");
            return;
        }

        for (Module module : modules) {
            try {
                storage.pull(module);
            } catch (IOException e) {
                logger.error("Failed to pull module {}", module);
            }
        }

        logger.info("Options are now up-to-date");
    }

    public synchronized void pushOptions() {
        if (!enabled) return;

        logger.info("Pushing synced options...");

        if (initStorage()) return;

        for (Module module : modules()) {
            try {
                storage.push(module);
            } catch (IOException e) {
                logger.error("Failed to push module {}", module);
            }
        }

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

    private Collection<Module> modules() {
        var modules = moduleProvider.modules();

        logger.debug("Found modules {}", modules);

        return modules;
    }
}
