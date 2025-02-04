package work.lclpnet.opt_sync;

import net.fabricmc.loader.api.FabricLoader;
import org.semver4j.Semver;
import work.lclpnet.opt_sync.lib.Module;
import work.lclpnet.opt_sync.lib.ModuleProvider;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

public class FabricModuleProvider implements ModuleProvider {

    @Override
    public Collection<Module> modules() {
        record FabricModule(Path modulePath, Path filePath, Semver version) implements Module {}

        var modules = new ArrayList<Module>(1);

        FabricLoader.getInstance().getModContainer("minecraft")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .map(Semver::parse)
                .map(version -> new FabricModule(Path.of("minecraft"), Path.of(""), version))
                .ifPresent(modules::add);

        return modules;
    }
}
