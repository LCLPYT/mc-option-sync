package work.lclpnet.opt_sync.lib.cfg;

import com.electronwill.nightconfig.core.serde.annotations.SerdeSkipDeserializingIf;
import com.electronwill.nightconfig.core.serde.annotations.SerdeSkipSerializingIf;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public class SyncEntry {

    private final String file;
    private final String module = "common";

    @SerdeSkipSerializingIf(SerdeSkipSerializingIf.SkipSerIf.IS_NULL)
    @SerdeSkipDeserializingIf(SerdeSkipDeserializingIf.SkipDeIf.IS_MISSING)
    private final @Nullable String version = null;

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
