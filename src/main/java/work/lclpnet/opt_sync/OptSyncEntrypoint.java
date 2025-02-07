package work.lclpnet.opt_sync;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.opt_sync.lib.OptionStorage;
import work.lclpnet.opt_sync.lib.OptionSync;

import java.util.Optional;

public class OptSyncEntrypoint implements PreLaunchEntrypoint {

	public static final String MOD_ID = "mc-option-sync";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static @Nullable OptionSync optionSync = null;

	@Override
	public void onPreLaunch() {
		if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return;

		var ctx = new FabricSyncContext();
		var storage = new OptionStorage(OptionStorage.getDataDir(), ctx, LOGGER);
		var sync = new OptionSync(storage, ctx, LOGGER);

		sync.init();
		sync.pullOptions();

		optionSync = sync;
    }

	public static Optional<OptionSync> getOptionSync() {
		return Optional.ofNullable(optionSync);
	}
}