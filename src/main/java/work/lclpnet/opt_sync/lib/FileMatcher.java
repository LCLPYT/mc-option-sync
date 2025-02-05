package work.lclpnet.opt_sync.lib;

import org.slf4j.Logger;
import work.lclpnet.opt_sync.lib.cfg.ModuleConfig;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.function.Predicate;

public class FileMatcher implements Predicate<Path> {

    private final List<PathMatcher> includes, excludes;

    public FileMatcher(List<PathMatcher> includes, List<PathMatcher> excludes) {
        this.includes = includes;
        this.excludes = excludes;
    }

    @Override
    public boolean test(Path path) {
        return includes.stream().anyMatch(m -> m.matches(path))
                && excludes.stream().noneMatch(m -> m.matches(path));
    }

    public static FileMatcher of(ModuleConfig config, Logger logger) {
        FileSystem fs = FileSystems.getDefault();

        var includes = config.getInclude().stream()
                .flatMap( pattern -> pattern.asPathMatcher(fs, logger).stream())
                .toList();

        var excludes = config.getExclude().stream()
                .flatMap( pattern -> pattern.asPathMatcher(fs, logger).stream())
                .toList();

        return new FileMatcher(includes, excludes);
    }
}
