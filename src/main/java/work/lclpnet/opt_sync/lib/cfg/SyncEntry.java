package work.lclpnet.opt_sync.lib.cfg;

import com.electronwill.nightconfig.core.serde.annotations.SerdeSkipDeserializingIf;
import com.electronwill.nightconfig.core.serde.annotations.SerdeSkipSerializingIf;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Getter @Setter
public class SyncEntry {

    private String file;
    private String module = "common";

    @SerdeSkipSerializingIf(SerdeSkipSerializingIf.SkipSerIf.IS_NULL)
    @SerdeSkipDeserializingIf(SerdeSkipDeserializingIf.SkipDeIf.IS_MISSING)
    private @Nullable String version = null;

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
