package work.lclpnet.opt_sync.lib.cfg;

import org.slf4j.Logger;

import java.nio.file.FileSystem;
import java.nio.file.PathMatcher;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;

public record FilePattern(String pattern) {

    public Optional<PathMatcher> asPathMatcher(FileSystem fs, Logger logger) {
        try {
            return Optional.of(fs.getPathMatcher("glob:" + pattern));
        } catch (PatternSyntaxException e) {
            logger.error("Invalid pattern: '{}'", pattern, e);
            return Optional.empty();
        }
    }
}
