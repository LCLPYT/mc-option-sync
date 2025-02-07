package work.lclpnet.opt_sync.lib;

import org.jetbrains.annotations.Blocking;
import org.slf4j.Logger;
import work.lclpnet.opt_sync.gui.ConflictsGui;
import work.lclpnet.opt_sync.lib.cfg.SyncConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConflictChecker {

    private final OptionStorage storage;
    private final Logger logger;

    public ConflictChecker(OptionStorage storage, Logger logger) {
        this.storage = storage;
        this.logger = logger;
    }

    @Blocking
    public boolean check(SyncConfig config) {
        var index = Path.of(".mc-option-sync");

        if (Files.exists(index)) return false;

        // this is the first startup where no changes were pulled yet
        // check if there are any files that would be overwritten by a pull

        Path cwd = Path.of("");

        if (!storage.anyPullConflicts(config, cwd)) {
            createIndex(index);
            return false;
        }

        if (!shouldOverwrite()) {
            return true;
        }

        createIndex(index);

        return false;
    }

    private void createIndex(Path index) {
        try {
            Files.createFile(index);
        } catch (IOException e) {
            logger.error("Failed to create index file {}", index, e);
        }
    }

    private boolean shouldOverwrite() {
        logger.info("File conflicts detected on the files to sync. Manual action required...");

        boolean shouldOverwrite;

        try {
            shouldOverwrite = new ConflictsGui(logger).awaitResponse();
        } catch (Exception e) {
            logger.error("Failed to get user response via the gui (selecting keep)", e);
            return false;
        }

        if (shouldOverwrite) {
            logger.info("The user requested to overwrite conflicting files of this Minecraft instance");
        } else {
            logger.info("The user requested to keep conflicting files");
        }

        return shouldOverwrite;
    }
}
