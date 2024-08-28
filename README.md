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
    id "run.halo.plugin.devtools" version "0.0.9"
}
```

## 任务

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
    version = '2.9.1'
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

### haloServer 任务

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

### 生成 API client

#### 什么是 API client

API client 是一种工具或库，旨在简化前端应用程序与后端服务器之间的通信，尤其是在使用 RESTful API 或 GraphQL API 的情况下。
它提供了一种简洁且类型安全的方式来调用服务器端的 API，并处理请求和响应。

在 TypeScript 环境中，使用 API client 有以下几个优点：

- 自动化 HTTP 请求：API 客户端封装了 HTTP 请求的细节，如构建 URL、设置请求头、处理查询参数等。开发者只需调用客户端提供的函数即可发送请求。

- 类型安全：通过结合 OpenAPI 等规范生成的 TypeScript 类型定义，API 客户端可以确保请求和响应的数据类型在编译时就能得到验证。这可以帮助减少运行时的错误，并提高代码的可读性和可维护性。

- 统一的错误处理：API 客户端可以提供统一的错误处理机制，比如自动重试、错误日志记录等，这样开发者无需在每个 API 调用中重复编写相同的错误处理逻辑。

- 提高开发效率：通过使用 API 客户端，开发者可以专注于业务逻辑的实现，而不用关心底层的 HTTP 细节。这不仅提高了开发效率，还减少了代码冗余。

#### 如何生成 API client

本插件提供了一个 `generateApiClient` 任务，用于为插件项目生成 API client，生成规则基于 OpenAPI 规范来自动生成客户端代码。

能生成 API 客户端代码的前提是插件项目中需要对自定义的 API 进行文档声明如：

```java
final var tag = "CommentV1alpha1Console";
return SpringdocRouteBuilder.route()
    .GET("comments", this::listComments, builder -> {
            builder.operationId("ListComments")
                .description("List comments.")
                .tag(tag)
                .response(responseBuilder()
                    .implementation(ListResult.generateGenericClass(ListedComment.class))
                );
            CommentQuery.buildParameters(builder);
        }
    )
    .build();
```

或者是在插件中定义了自定义模型，自定义模型自动生成的 CRUD APIs 是已经支持的。

以下是如何配置和使用 `generateApiClient` 的详细步骤：

##### 配置 `generateApiClient`

在 build.gradle 文件中，使用 haloPlugin 块来配置 OpenAPI 文档生成和 API 客户端生成的相关设置：

```groovy
haloPlugin {
    openApi {
        // outputDir = file("$rootDir/api-docs/openapi/v3_0") // 指定 OpenAPI 文档的输出目录默认输出到 build 目录下，不建议修改，除非需要提交到代码仓库
        groupingRules {
            // 定义 API 分组规则，用于为插件项目中的 APIs 分组然后只对此分组生成 API 客户端代码
            // 定义了一个名为 extensionApis 的分组，task 会通过 /v3/api-docs/extensionApis 访问到 api docs 然后生成 API 客户端代码
            // extensionApis 名称可以替换为其他名称，但需要与 groupedApiMappings 中的名称一致
            extensionApis {
                // 分组显示名称，避免与其他分组重名建议替换 {your-plugin-name} 为插件名称
                displayName = 'Extension API for {your-plugin-name}'
                // 分组的 API 规则用于匹配插件项目中的 API 将其划分到此分组，它是一个 Ant 风格的路径匹配规则可以写多个
                pathsToMatch = ['/apis/staticpage.halo.run/v1alpha1/**']
            }
        }
        groupedApiMappings = [
            // 这里为固定写法，照搬即可，除非是 groupingRules 中 extensionApis 的名字修改了
            '/v3/api-docs/extensionApis': 'extensionApis.json'
        ]
        generator {
             // 指定 API 客户端代码的输出目录如 console 或 ui
            outputDir = file("${projectDir}/console/src/api/generated")

            // 定制生成，以下是默认配置可以不需要添加到 build.gradle 中
            additionalProperties = [
                useES6: true,
                useSingleRequestParameter: true,
                withSeparateModelsAndApi: true,
                apiPackage: "api",
                modelPackage: "models"
            ]
            // 类型映射，用于将 OpenAPI 中的类型映射到 TypeScript 中的类型，以下是默认配置可以不需要添加到 build.gradle 中
            typeMappings = [
                set: "Array"
            ]
        }
    }
}
```

##### 执行 `generateApiClient`

在项目目录中执行以下命令即可生成 API 客户端代码到指定目录：

```shell
./gradlew generateApiClient
```

然后在 `openApi.generator.outputDir` 目录创建一个 `index.ts` 文件并创建实例，以瞬间插件为例

```typescript
// console/src/api/index.ts
// 先引入 axiosInstance 用于请求
import { axiosInstance } from "@halo-dev/api-client";
// 这里导入的是声明 API doc 时指定的 tag 名称，如上文中定义的 CommentV1alpha1Console
import {
  ConsoleApiMomentHaloRunV1alpha1MomentApi,
  MomentV1alpha1Api,
  UcApiMomentHaloRunV1alpha1MomentApi,
} from "./generated";

// MomentV1alpha1Api 是自定义模型生成的 API tag 这里创建了一个 momentsCoreApiClient 实例
const momentsCoreApiClient = {
  moment: new MomentV1alpha1Api(undefined, "", axiosInstance),
};

// ConsoleApiMomentHaloRunV1alpha1MomentApi 是用于在 console 端调用的 APIs 的 tag，这里创建了一个 momentsConsoleApiClient 实例用于在 console 端调用
const momentsConsoleApiClient = {
  moment: new ConsoleApiMomentHaloRunV1alpha1MomentApi(
    undefined,
    "",
    axiosInstance
  ),
};

// 用于在个人中心调用的 APIs，单独创建一个 momentsUcApiClient 实例
const momentsUcApiClient = {
  moment: new UcApiMomentHaloRunV1alpha1MomentApi(undefined, "", axiosInstance),
};
// 导出实例
export { momentsConsoleApiClient, momentsCoreApiClient, momentsUcApiClient };
```

使用定义的实例:

```typescript
import { momentsConsoleApiClient } from "@/api";

// 查询瞬间的标签
const { data } = await momentsConsoleApiClient.moment.listTags({
    name: props.keyword?.value,
});
```

> [!NOTE]
> 它会先执行 `generateOpenApiDocs` 任务根据配置访问 `/v3/api-docs/extensionApis` 获取 OpenAPI 文档，
> 并将 OpenAPI 的 Schema 文件保存到 `openApi.outputDir` 目录下，然后再由 `generateApiClient` 任务根据 Schema 文件生成 API 客户端代码到 `openApi.generator.outputDir` 目录下。

> [!WARNING]
> 执行 `generateApiClient` 任务时会先删除 `openApi.generator.outputDir` 下的所有文件，因此建议将 API client 的输出目录设置为一个独立的目录，以避免误删其他文件。

## Debug

如果你想要调试 Halo 插件项目，可以使用 IntelliJ IDEA 的 Debug 模式运行 `haloServer` 或 `watch` 任务，而后会在日志开头看到类似如下信息：

```shell
Listening for transport dt_socket at address: 50781 Attach debugger
```

然后点击 `Attach debugger` 即可开启调试面板。

## 使用 Snapshot 版本

克隆本项目到本地，然后执行

```shell
./gradlew clean build -x test && ./gradlew publishToMavenLocal
```

即可将插件发布到本地 Maven 仓库，然后在 Halo 插件项目的 `build.gradle` 中添加如下配置：

```groovy
plugins {
    //...
    id "run.halo.plugin.devtools" version "0.0.10-SNAPSHOT"
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
