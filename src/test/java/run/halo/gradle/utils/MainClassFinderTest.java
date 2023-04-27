package run.halo.gradle.utils;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class MainClassFinderTest {

    @Test
    void findSingleMainClass() throws IOException {
        String singleMainClass = MainClassFinder.findSingleMainClass(
            new File("/Users/guqing/Development/halo-sigs/plugin-links/build/classes"),
            "run.halo.app.plugin.BasePlugin");
        System.out.println(singleMainClass);
    }
}