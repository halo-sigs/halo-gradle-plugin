package run.halo.gradle.utils;

import org.junit.jupiter.api.Test;

class VersionUtilsTest {

    @Test
    void latestVersionBySemverRange() {
        String[] version = new String[] {"2.5.0-rc.1",
            "2.5.0-rc.2",
            "2.5.1",
            "2.5.2",
            "latest",
            "sha-01306b0",
            "sha-0794644",
            "sha-0c18fb3",
            "sha-15e1012",
            "sha-173350d",
            "sha-18c0ced"};
        String s = VersionUtils.latestVersionBySemverRange(">=2.5.0", version);
        Assert.isTrue(s.equals("2.5.2"), "not equal");

        s = VersionUtils.latestVersionBySemverRange(">=2.5.0 & <2.5.2", version);
        Assert.isTrue(s.equals("2.5.1"), "not equal");
    }
}