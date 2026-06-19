package work.lclpnet.opt_sync.lib;

public class VersionUtil {

    public static String getFlexibleSemver(String version) {
        String cleaned = version.trim();

        if (cleaned.matches("^(v?\\d+\\.\\d+)(?=[\\-+]|$).*")) {
            return cleaned.replaceFirst("^(v?\\d+\\.\\d+)", "$1.0");
        }

        return cleaned;
    }
}
