package work.lclpnet.opt_sync;

import net.fabricmc.loader.api.FabricLoader;
import work.lclpnet.opt_sync.lib.Constants;
import work.lclpnet.opt_sync.lib.SyncContext;

import java.nio.file.Path;
import java.util.Optional;

public class FabricSyncContext implements SyncContext {

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir().resolve(Constants.MOD_ID);
    }

    @Override
    public Optional<String> version() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString());
    }
}
