# gradle-plugin-halo
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
    id "io.github.guqing.plugin-development" version "0.0.1-SNAPSHOT"
}
```
## what this plugin does
- 项目 `build` 时会自动生成 `build/classes/java/main/META-INF/plugin-components.idx` 文件
- 提供了一个 gradle task group 名为 `halo server`
  - defaultThemeInstall 的 task 执行时会自动安装默认主题
  - serverInstall 的 task 执行时会下载 halo-2.0.0-SNAPSHOT.jar 到项目目录的 `workplace`
  - haloServer 的 task 执行时会启动 workplace 中的 halo.jar

当运行 `gradle haloServer` 时会自动执行下载默认主题、下载 halo.jar 、启用当前正在开发的插件等初始化操作，此时可以在浏览器中访问 `http://localhost:8090` 查看效果。
系统默认启动时的用户名和密码为 `admin` 和 `123456`。

由于 `halo-admin` 还没有打包到 `halo-2.0.0-SNAPSHOT.jar` 中，所以如需管理后台则需要自行启动。
