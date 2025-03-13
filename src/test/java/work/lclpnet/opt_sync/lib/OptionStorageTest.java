package work.lclpnet.opt_sync.lib;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import work.lclpnet.opt_sync.lib.cfg.FileRef;
import work.lclpnet.opt_sync.lib.cfg.SyncConfig;
import work.lclpnet.opt_sync.lib.cfg.SyncEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static java.nio.file.Files.exists;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionStorageTest {

    private static final Logger logger = LoggerFactory.getLogger(OptionStorageTest.class);
    private OptionStorage storage;
    private Path local, remote;
    private String version;

    @BeforeEach
    void setup() throws IOException {
        Path dir = Files.createTempDirectory("mc-option-sync-test");
        local = dir.resolve("local");
        remote = dir.resolve("remote");

        logger.info("Test directory is {}", dir);

        Files.createDirectories(local);

        version = "1.0.0";

        TestSyncContext ctx = new TestSyncContext(dir.resolve("config"), version);

        storage = new OptionStorage(local, remote, ctx, logger);
    }

    @Test
    void push_new_all() throws IOException {
        copyResourcesTo(local);
        storage.init();

        SyncConfig cfg = emptyCfg();
        cfg.getSync().add(new SyncEntry("*"));
        cfg.getSync().add(new SyncEntry("**/*"));

        storage.push(cfg);

        assertTrue(exists(moduleDir().resolve("foo.txt")));
        assertTrue(exists(moduleDir().resolve("dir").resolve("test.md")));
    }

    @Test
    void push_new_ignore() throws IOException {
        copyResourcesTo(local);
        storage.init();

        SyncConfig cfg = emptyCfg();
        cfg.getSync().add(new SyncEntry("*"));
        cfg.getSync().add(new SyncEntry("**/*"));
        cfg.getIgnore().add(new FileRef("foo.txt"));

        storage.push(cfg);

        assertFalse(exists(moduleDir().resolve("foo.txt")));
        assertTrue(exists(moduleDir().resolve("dir").resolve("test.md")));
    }

    @Test
    void pull_new_all() throws IOException {
        copyResourcesTo(moduleDir());
        storage.init();

        SyncConfig cfg = emptyCfg();
        cfg.getSync().add(new SyncEntry("*"));
        cfg.getSync().add(new SyncEntry("**/*"));

        storage.pull(cfg);

        assertTrue(exists(local.resolve("foo.txt")));
        assertTrue(exists(local.resolve("dir").resolve("test.md")));
    }

    private @NotNull Path moduleDir() {
        return remote.resolve("common").resolve(version);
    }

    private @NotNull SyncConfig emptyCfg() {
        var cfg = new SyncConfig();

        cfg.getSync().clear();
        cfg.getIgnore().clear();

        return cfg;
    }

    private void copyResourcesTo(Path dir) throws IOException {
        Path src = Path.of("src/test/resources");

        try (var files = Files.walk(src)) {
            files
                    .filter(Files::isRegularFile)
                    .forEach(source -> {
                        Path rel = src.relativize(source);
                        Path target = dir.resolve(rel);

                        try {
                            Files.createDirectories(target.getParent());
                            Files.copy(source, target);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    record TestSyncContext(Path configDir, @Nullable String ver) implements SyncContext {

        @Override
        public Optional<String> version() {
            return Optional.ofNullable(ver);
        }
    }
}