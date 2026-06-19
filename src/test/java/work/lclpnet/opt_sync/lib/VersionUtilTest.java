package work.lclpnet.opt_sync.lib;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionUtilTest {

    @Test
    public void flexibleVersion_flexible_padded() {
        String version = VersionUtil.getFlexibleSemver("26.2");

        assertEquals("26.2.0", version);
    }

    @Test
    public void flexibleVersion_complete_keptAsIs() {
        String version = VersionUtil.getFlexibleSemver("26.1.2");

        assertEquals("26.1.2", version);
    }

    @Test
    public void flexibleVersion_flexibleWithIdentifier_padded() {
        String version = VersionUtil.getFlexibleSemver("26.2-beta.1");

        assertEquals("26.2.0-beta.1", version);
    }

    @Test
    public void flexibleVersion_flexibleWithMeta_padded() {
        String version = VersionUtil.getFlexibleSemver("26.2+sha123");

        assertEquals("26.2.0+sha123", version);
    }

    @Test
    public void flexibleVersion_flexibleWithIdentifierAndMeta_padded() {
        String version = VersionUtil.getFlexibleSemver("26.2-beta.2+sha123");

        assertEquals("26.2.0-beta.2+sha123", version);
    }

    @Test
    public void flexibleVersion_completeWithIdentifier_keptAsIs() {
        String version = VersionUtil.getFlexibleSemver("26.1.2-beta.1");

        assertEquals("26.1.2-beta.1", version);
    }

    @Test
    public void flexibleVersion_completeWithMeta_keptAsIs() {
        String version = VersionUtil.getFlexibleSemver("26.1.2+sha56");

        assertEquals("26.1.2+sha56", version);
    }

    @Test
    public void flexibleVersion_completeWithIdentifierAndMeta_keptAsIs() {
        String version = VersionUtil.getFlexibleSemver("26.1.2-beta.3+sha56");

        assertEquals("26.1.2-beta.3+sha56", version);
    }
}