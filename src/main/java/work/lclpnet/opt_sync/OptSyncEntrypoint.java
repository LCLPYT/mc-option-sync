package work.lclpnet.opt_sync;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.opt_sync.lib.OptionStorage;
import work.lclpnet.opt_sync.lib.OptionSync;

public class OptSyncEntrypoint implements PreLaunchEntrypoint {

	public static final String MOD_ID = "mc-option-sync";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onPreLaunch() {
		var storage = new OptionStorage(OptionStorage.getDataDir(), LOGGER);
		var moduleProvider = new FabricModuleProvider();

		new OptionSync(storage, moduleProvider, LOGGER).bootstrap();
    }
}