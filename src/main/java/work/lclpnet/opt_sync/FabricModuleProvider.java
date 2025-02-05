package work.lclpnet.opt_sync;

import net.fabricmc.loader.api.FabricLoader;
import org.semver4j.Semver;
import work.lclpnet.opt_sync.lib.Module;
import work.lclpnet.opt_sync.lib.ModuleProvider;
import work.lclpnet.opt_sync.lib.cfg.FilePattern;
import work.lclpnet.opt_sync.lib.cfg.ModuleConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

public class FabricModuleProvider implements ModuleProvider {

    @Override
    public Collection<Module> modules() {
        var modules = new ArrayList<Module>(1);

        FabricLoader.getInstance().getModContainer("minecraft")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .map(Semver::parse)
                .map(version -> new MinecraftModule(Path.of("minecraft"), Path.of(""), version))
                .ifPresent(modules::add);

        return modules;
    }

    private record MinecraftModule(Path modulePath, Path filePath, Semver version) implements Module {

        @Override
        public ModuleConfig createConfig() {
            ModuleConfig cfg = Module.super.createConfig();

            var include = cfg.getInclude();
            include.add(new FilePattern("options.txt"));
            include.add(new FilePattern("servers.dat"));
            include.add(new FilePattern("config/**/*.{json,toml,properties,cfg,json5}"));

            return cfg;
        }
    }
}
