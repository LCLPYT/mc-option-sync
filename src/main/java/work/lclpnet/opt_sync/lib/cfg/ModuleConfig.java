package work.lclpnet.opt_sync.lib.cfg;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class ModuleConfig {

    private List<FilePattern> include = new ArrayList<>();
    private List<FilePattern> exclude = new ArrayList<>();
}
