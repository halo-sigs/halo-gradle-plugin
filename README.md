# halo-gradle-plugin
This is a Gradle plugin for building Halo plugins, written in Java.

## how to use
由于目前还是 Snapshot 版本，需要在项目的 `settings.gradle` 中添加如下配置：
```groovy
pluginManagement {
    repositories {
        maven { url 'https://s01.oss.sonatype.org/content/repositories/snapshots' }
        gradlePluginPortal()
    }
}
```
然后在项目的 `build.gradle` 中添加如下配置：
```groovy
plugins {
    // ...
    // 添加此 gradle 插件依赖
    id "run.halo.plugin.devtools" version "0.0.2-SNAPSHOT"
}
```

