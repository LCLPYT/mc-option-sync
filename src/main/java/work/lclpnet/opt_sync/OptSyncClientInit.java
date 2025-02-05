package work.lclpnet.opt_sync;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import work.lclpnet.opt_sync.lib.OptionSync;

public class OptSyncClientInit implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> pushOptions());
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> pushOptions());
	}

	private static void pushOptions() {
		OptSyncEntrypoint.getOptionSync().ifPresent(OptionSync::pushOptions);
	}
}