package run.halo.gradle;

import org.gradle.api.java.archives.Attributes;
import org.gradle.api.java.archives.Manifest;

public class PluginArchiveSupport {

    private static final String UNSPECIFIED_VERSION = "unspecified";

    private final String pluginMainClass;

    PluginArchiveSupport(String pluginMainClass) {
        this.pluginMainClass = pluginMainClass;
    }

    void configureManifest(Manifest manifest, String lib,
        String jdkVersion, String implementationTitle,
        Object implementationVersion) {
        Attributes attributes = manifest.getAttributes();
        attributes.putIfAbsent("Plugin-Main-Class", pluginMainClass);
        attributes.putIfAbsent("Class-Path", lib);
        attributes.putIfAbsent("Build-Jdk-Spec", jdkVersion);
        attributes.putIfAbsent("Implementation-Title", implementationTitle);
        if (implementationVersion != null) {
            String versionString = implementationVersion.toString();
            if (!UNSPECIFIED_VERSION.equals(versionString)) {
                attributes.putIfAbsent("Implementation-Version", versionString);
            }
        }
    }
}
