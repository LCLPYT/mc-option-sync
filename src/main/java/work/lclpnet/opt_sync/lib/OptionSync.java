package work.lclpnet.opt_sync.lib;

import org.slf4j.Logger;

import java.io.IOException;

public class OptionSync {

    private final OptionStorage storage;
    private final Logger logger;

    public OptionSync(OptionStorage storage, Logger logger) {
        this.storage = storage;
        this.logger = logger;
    }

    public void bootstrap() {
        var storage = OptionStorage.get();

        try {
            storage.init();
        } catch (IOException e) {
            logger.error("Failed to initialize option storage; options cannot be synced", e);
            return;
        }

        logger.info("Checking for synced options to pull...");
    }
}
