package work.lclpnet.opt_sync.lib;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class OsUtil {

    public static @NotNull Path getOsDataDir() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");

        if (os.contains("win")) {
            // Windows: %APPDATA%\mc-option-sync (default ~\AppData\Roaming\mc-option-sync)
            String appData = System.getenv("APPDATA");

            if (appData == null || appData.isEmpty()) {
                return Path.of(userHome, "AppData", "Roaming");
            }

            return Path.of(appData);
        }

        if (os.contains("mac")) {
            // macOS: ~/Library/Application Support/mc-option-sync
            return Path.of(userHome, "Library", "Application Support");
        }

        // Linux: $XDG_DATA_HOME/mc-option-sync (default ~/.local/share/mc-option-sync)
        // https://specifications.freedesktop.org/basedir-spec/latest/
        String xdgDataHome = System.getenv("XDG_DATA_HOME");

        if (xdgDataHome == null || xdgDataHome.isEmpty()) {
            return Path.of(userHome, ".local", "share");
        }

        return Path.of(xdgDataHome);
    }
}
