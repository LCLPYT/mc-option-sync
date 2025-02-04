package work.lclpnet.opt_sync.lib;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static work.lclpnet.opt_sync.OptSyncEntrypoint.MOD_ID;

public class OptionStorage {

    private final @NotNull Path dir;

    public OptionStorage(@NotNull Path dir) {
        this.dir = dir;
    }

    @Blocking
    public void init() throws IOException {
        if (Files.exists(dir)) return;

        Files.createDirectories(dir);
    }

    public static OptionStorage get() {
        return Holder.instance;
    }

    private static @NotNull Path getDataDir() {
        Path appDataPath = getOsDataDir();

        return appDataPath.resolve(MOD_ID);
    }

    private static @NotNull Path getOsDataDir() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Windows: %APPDATA%\mc-option-sync (default ~\AppData\Roaming\mc-option-sync)
            String appData = System.getenv("APPDATA");

            if (appData == null || appData.isEmpty()) {
                return Path.of(System.getProperty("user.home"), "AppData", "Roaming");
            }

            return Path.of(appData);
        }

        if (os.contains("mac")) {
            // macOS: ~/Library/Application Support/mc-option-sync
            return Path.of(System.getProperty("user.home"), "Library", "Application Support");
        }

        // Linux: $XDG_DATA_HOME/mc-option-sync (default ~/.local/share/mc-option-sync)
        // https://specifications.freedesktop.org/basedir-spec/latest/
        String xdgDataHome = System.getenv("XDG_DATA_HOME");

        if (xdgDataHome == null || xdgDataHome.isEmpty()) {
            return Path.of(System.getProperty("user.home"), ".local", "share");
        }

        return Path.of(xdgDataHome);
    }

    private static class Holder {
        private static final OptionStorage instance = new OptionStorage(getDataDir());
    }
}
