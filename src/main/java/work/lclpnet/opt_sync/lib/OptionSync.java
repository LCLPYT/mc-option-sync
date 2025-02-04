package work.lclpnet.opt_sync.lib;

import org.slf4j.Logger;

import java.io.IOException;

public class OptionSync {

    private final OptionStorage storage;
    private final ModuleProvider moduleProvider;
    private final Logger logger;

    public OptionSync(OptionStorage storage, ModuleProvider moduleProvider, Logger logger) {
        this.storage = storage;
        this.moduleProvider = moduleProvider;
        this.logger = logger;
    }

    public void bootstrap() {
        try {
            storage.init();
        } catch (IOException e) {
            logger.error("Failed to initialize option storage; options cannot be synced", e);
            return;
        }

        var modules = moduleProvider.modules();

        logger.debug("Found modules {}", modules);
        logger.info("Pulling synced options...");

        for (Module module : modules) {
            try {
                storage.pull(module);
            } catch (IOException e) {
                logger.error("Failed to pull module {}", module);
            }
        }

        logger.info("Options are now up-to-date");
    }
}
