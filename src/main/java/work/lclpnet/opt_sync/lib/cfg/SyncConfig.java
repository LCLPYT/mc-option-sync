package work.lclpnet.opt_sync.lib.cfg;

import lombok.Getter;
import work.lclpnet.opt_sync.OptSyncEntrypoint;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SyncConfig {

    private final List<SyncEntry> sync = new ArrayList<>();
    private final List<FileRef> ignore = new ArrayList<>();

    public SyncConfig() {
        sync.add(new SyncEntry("options.txt"));
        sync.add(new SyncEntry("servers.dat"));
        sync.add(new SyncEntry("config/**/*.{json,toml,properties,cfg,json5}"));

        ignore.add(new FileRef("config/%s/*".formatted(OptSyncEntrypoint.MOD_ID)));
    }
}
