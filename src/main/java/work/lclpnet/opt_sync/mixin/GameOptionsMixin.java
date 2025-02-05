package work.lclpnet.opt_sync.mixin;

import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.lclpnet.opt_sync.OptSyncEntrypoint;
import work.lclpnet.opt_sync.lib.OptionSync;

@Mixin(GameOptions.class)
public class GameOptionsMixin {

    @Inject(
            method = "write",
            at = @At("TAIL")
    )
    public void mcOptSync$afterConfigSaved(CallbackInfo ci) {
        OptSyncEntrypoint.getOptionSync().ifPresent(OptionSync::pushOptions);
    }
}
