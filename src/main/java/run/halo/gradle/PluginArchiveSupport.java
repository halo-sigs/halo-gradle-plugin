package run.halo.gradle;

import org.gradle.api.java.archives.Attributes;
import org.gradle.api.java.archives.Manifest;

public class PluginArchiveSupport {
    private static final String UNSPECIFIED_VERSION = "unspecified";

    private final String loaderMainClass;

    PluginArchiveSupport(String loaderMainClass) {
        this.loaderMainClass = loaderMainClass;
    }

    void configureManifest(Manifest manifest, String mainClass, String classes, String lib,
        String classPathIndex,
        String layersIndex, String jdkVersion, String implementationTitle,
        Object implementationVersion) {
        Attributes attributes = manifest.getAttributes();
        attributes.putIfAbsent("Main-Class", this.loaderMainClass);
        if (classPathIndex != null) {
            attributes.putIfAbsent("Spring-Boot-Classpath-Index", classPathIndex);
        }
        if (layersIndex != null) {
            attributes.putIfAbsent("Spring-Boot-Layers-Index", layersIndex);
        }
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
