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
    id "run.halo.plugin.devtools" version "0.0.3"
}
```

### 任务
本插件提供了 haloServer 和 watch 两个任务，使用 haloServer 和 watch 这两个任务的前提条件是需要具有 Docker 环境。

对于 Windows 和 Mac 用户，可以直接安装 Docker Desktop，对于 Linux 用户，可以参考 [Docker 官方文档](https://docs.docker.com/engine/install/) 安装 Docker。

然后启动 Docker 服务，即可使用 haloServer 和 watch 任务。

这两个任务会将 Halo 的工作目录挂在到插件项目的 `workplace` 目录下，以确保重启任务时不会丢失数据。

如果你想要修改 Halo 的配置，可以在 `workplace` 目录下创建一个 `config` 目录并添加一个 `application.yaml` 文件，然后在此文件中添加 Halo 的配置以覆盖 Halo 的 `default` 配置, 如：
```yaml
# workplace/config/application.yaml
logging:
  level:
    run.halo.app: DEBUG 
```

Halo 使用的缺省配置如下：
```groovy
halo {
    version = '2.5.2'
    superAdminUsername = 'admin'
    superAdminPassword = 'admin'
    externalUrl = 'http://localhost:8090'
    docker {
        // windows 默认为 npipe:////./pipe/docker_engine
        url = 'unix:///var/run/docker.sock'
        apiVersion = '1.42'
    }
}
```
如需修改，你可以在 `build.gradle` 配置。

#### haloServer 任务
使用方式：
```shell
./gradlew haloServer
```
此任务用于启动 Halo 服务并自动将使用此 Gradle 插件的 Halo 插件项目以开发模式加载到 Halo 服务中。
但当修改插件后，需要先停止此任务，然后再重新执行此任务才能生效。
或者使用 Halo 提供的重启插件的 API, 这可以在不停止 haloServer 任务的情况下重新加载插件：
```shell
./gradlew clean build -x test
# 替换 {your-username} 和 {your-password} 为 Halo 的用户名和密码, {your-plugin-name} 为插件的名称
curl -u {your-username}:{your-password} -X PUT http://localhost:8090/apis/api.console.halo.run/v1alpha1/plugins/{your-plugin-name}/reload
```
#### watch 任务
使用方式：
```shell
./gradlew watch
```
此任务用于监视 Halo 插件项目的变化并自动重新加载到 Halo 服务中。
默认只监听 `src/main/java` 和 `src/main/resources` 目录下的文件变化，如果需要监听其他目录，可以在项目的 `build.gradle` 中添加如下配置：
```groovy
haloPlugin {
    watchDomains {
        // consoleSource 为自定义的名称，可以随意取
        consoleSource {
            // 监听 console/src/ 目录下的文件变化
            files files('console/src/')
        }
        // ... 可以添加多个
    }
}
```
### 使用 Snapshot 版本
克隆本项目到本地，然后执行
```shell
./gradlew clean build -x test && ./gradlew publishToMavenLocal
```
即可将插件发布到本地 Maven 仓库，然后在 Halo 插件项目的 `build.gradle` 中添加如下配置：
```groovy
plugins {
    //... 
    id "run.halo.plugin.devtools" version "0.0.4-SNAPSHOT"
}
```
再修改 `settings.gradle` 中的配置：
```groovy
pluginManagement {
    repositories {
        mavenLocal()
        // ...
        gradlePluginPortal()
    }
}
```
完成上述步骤后，即可使用本插件。
