package work.lclpnet.opt_sync.lib;

import java.nio.file.Path;
import java.util.Optional;

public interface SyncContext {

    Path configDir();

    Optional<String> version();
}
