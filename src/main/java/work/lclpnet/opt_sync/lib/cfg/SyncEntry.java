package work.lclpnet.opt_sync.lib.cfg;

import lombok.Getter;

@Getter
public class SyncEntry {

    private final String file;
    private final String module = "common";
    private final String version = null;

    @SuppressWarnings("unused")
    private SyncEntry() {
        this("");
    }

    public SyncEntry(String file) {
        this.file = file;
    }

    public FileRef asFileRef() {
        return new FileRef(file);
    }
}
