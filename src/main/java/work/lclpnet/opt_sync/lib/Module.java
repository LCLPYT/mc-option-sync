package work.lclpnet.opt_sync.lib;

import org.semver4j.Semver;
import work.lclpnet.opt_sync.lib.cfg.ModuleConfig;

import java.nio.file.Path;

public interface Module {

    /**
     * The path to the module relative to the storage base directory, without the version.
     * @return A relative path to the module.
     */
    Path modulePath();

    /**
     * The path to the options files relative to the Minecraft directory.
     */
    Path filePath();

    /**
     * The version of the module.
     * Used to resolve the version specific directory of the option files to use.
     * @return A semantic version.
     */
    Semver version();

    default ModuleConfig createConfig() {
        return new ModuleConfig();
    }
}
