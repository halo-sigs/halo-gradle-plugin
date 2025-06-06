# halo-gradle-plugin

This is a Gradle plugin for building Halo plugins, written in Java.

> For Chinese users, you can refer to [README_zh.md](./README_zh.md)

## How to Use

Since this is currently a Snapshot version, you need to add the following configuration to the `settings.gradle` file of your project:

```groovy
pluginManagement {
    repositories {
        maven { url 'https://s01.oss.sonatype.org/content/repositories/snapshots' }
        gradlePluginPortal()
    }
}
```

Then, add the following configuration to the `build.gradle` file of your project:

```groovy
plugins {
    // ...
    // Add this Gradle plugin dependency
    id "run.halo.plugin.devtools" version "0.0.9"
}
```

## Tasks

This plugin provides two tasks: `haloServer` and `watch`. To use these tasks, you need to have a Docker environment.

For Windows and Mac users, you can directly install Docker Desktop. For Linux users, you can refer to the [official Docker documentation](https://docs.docker.com/engine/install/) to install Docker.

After starting the Docker service, you can use the `haloServer` and `watch` tasks.

These tasks will mount Halo's working directory to the `workplace` directory of the plugin project to ensure that data is not lost when restarting tasks.

If you want to modify Halo's configuration, you can create a `config` directory under the `workplace` directory and add an `application.yaml` file. Then, add Halo's configuration in this file to override Halo's default configuration, for example:

```yaml
# workplace/config/application.yaml
logging:
    level:
        run.halo.app: DEBUG
```

The default configuration used by Halo is as follows:

```groovy
halo {
    version = '2.9.1'
    superAdminUsername = 'admin'
    superAdminPassword = 'admin'
    externalUrl = 'http://localhost:8090'
    docker {
        // For Windows, the default is npipe:////./pipe/docker_engine
        url = 'unix:///var/run/docker.sock'
        apiVersion = '1.42'
    }
}
```

If needed, you can modify this configuration in `build.gradle`.

### haloServer Task

Usage:

```shell
./gradlew haloServer
```

This task is used to start the Halo service and automatically load the Halo plugin project using this Gradle plugin in development mode into the Halo service. However, after modifying the plugin, you need to stop this task and then re-execute it for the changes to take effect. Alternatively, you can use Halo's API to restart the plugin, which allows you to reload the plugin without stopping the `haloServer` task:

```shell
./gradlew clean build -x test
# Replace {your-username} and {your-password} with Halo's username and password, and {your-plugin-name} with the plugin name
curl -u {your-username}:{your-password} -X PUT http://localhost:8090/apis/api.console.halo.run/v1alpha1/plugins/{your-plugin-name}/reload
```

### watch Task

Usage:

```shell
./gradlew watch
```

This task monitors changes in the Halo plugin project and automatically reloads them into the Halo service. By default, it only monitors file changes in the `src/main/java` and `src/main/resources` directories. If you need to monitor other directories, you can add the following configuration to the `build.gradle` file of your project:

```groovy
haloPlugin {
    watchDomains {
        // consoleSource is a custom name and can be arbitrary
        consoleSource {
            // Monitor file changes in the console/src/ directory
            files files('console/src/')
        }
        // ... You can add more
    }
}
```

### Generating API Client

#### What is an API Client

An API client is a tool or library designed to simplify communication between front-end applications and back-end servers, especially when using RESTful APIs or GraphQL APIs. It provides a concise and type-safe way to call server-side APIs and handle requests and responses.

In a TypeScript environment, using an API client has the following advantages:

- **Automated HTTP Requests**: The API client encapsulates the details of HTTP requests, such as building URLs, setting request headers, and handling query parameters. Developers only need to call the functions provided by the client to send requests.
- **Type Safety**: By combining TypeScript type definitions generated from OpenAPI specifications, the API client ensures that the data types of requests and responses are validated at compile time. This helps reduce runtime errors and improves code readability and maintainability.
- **Unified Error Handling**: The API client can provide a unified error-handling mechanism, such as automatic retries and error logging, so developers don't need to write repetitive error-handling logic for each API call.
- **Improved Development Efficiency**: By using an API client, developers can focus on implementing business logic without worrying about the underlying HTTP details. This not only improves development efficiency but also reduces code redundancy.

#### How to Generate an API Client

This plugin provides a `generateApiClient` task to generate an API client for the plugin project. The generation rules are based on the OpenAPI specification to automatically generate client code.

To generate API client code, the plugin project needs to declare custom APIs in the documentation, for example:

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

Or define custom models in the plugin. Automatically generated CRUD APIs for custom models are already supported.

Here are the detailed steps to configure and use `generateApiClient`:

##### Configuring `generateApiClient`

In the `build.gradle` file, use the `haloPlugin` block to configure settings related to OpenAPI documentation generation and API client generation:

```groovy
haloPlugin {
    openApi {
        // outputDir = file("$rootDir/api-docs/openapi/v3_0") // Specify the output directory for OpenAPI documentation. By default, it outputs to the build directory. It is not recommended to modify this unless you need to commit it to the code repository.
        groupingRules {
            // Define API grouping rules to group APIs in the plugin project and generate API client code only for this group.
            // A group named extensionApis is defined. The task will access the API docs through /v3/api-docs/extensionApis and generate API client code.
            // The name extensionApis can be replaced with another name, but it must match the name in groupedApiMappings.
            extensionApis {
                // Display name for the group. To avoid name conflicts, it is recommended to replace {your-plugin-name} with the plugin name.
                displayName = 'Extension API for {your-plugin-name}'
                // API rules for the group, used to match APIs in the plugin project and assign them to this group. It is an Ant-style path matching rule and can have multiple entries.
                pathsToMatch = ['/apis/staticpage.halo.run/v1alpha1/**']
            }
        }
        groupedApiMappings = [
            // This is a fixed format. Copy it as is unless the name extensionApis in groupingRules is changed.
            '/v3/api-docs/extensionApis': 'extensionApis.json'
        ]
        generator {
             // Specify the output directory for API client code, such as console or ui
            outputDir = file("${projectDir}/console/src/api/generated")

            // Customization options. The following are default configurations and do not need to be added to build.gradle.
            additionalProperties = [
                useES6: true,
                useSingleRequestParameter: true,
                withSeparateModelsAndApi: true,
                apiPackage: "api",
                modelPackage: "models"
            ]
            // Type mappings, used to map types in OpenAPI to TypeScript types. The following are default configurations and do not need to be added to build.gradle.
            typeMappings = [
                set: "Array"
            ]
        }
    }
}
```

##### Executing `generateApiClient`

Run the following command in the project directory to generate API client code in the specified directory:

```shell
./gradlew generateApiClient
```

Then, create an `index.ts` file in the `openApi.generator.outputDir` directory and create instances. For example, for the Moments plugin:

```typescript
// console/src/api/index.ts
// First, import axiosInstance for requests
import { axiosInstance } from "@halo-dev/api-client";
// Import the tag names specified when declaring the API doc, such as CommentV1alpha1Console defined above
import {
  ConsoleApiMomentHaloRunV1alpha1MomentApi,
  MomentV1alpha1Api,
  UcApiMomentHaloRunV1alpha1MomentApi,
} from "./generated";

// MomentV1alpha1Api is the API tag generated for custom models. Here, a momentsCoreApiClient instance is created.
const momentsCoreApiClient = {
  moment: new MomentV1alpha1Api(undefined, "", axiosInstance),
};

// ConsoleApiMomentHaloRunV1alpha1MomentApi is the API tag for console-side APIs. Here, a momentsConsoleApiClient instance is created for console-side calls.
const momentsConsoleApiClient = {
  moment: new ConsoleApiMomentHaloRunV1alpha1MomentApi(
    undefined,
    "",
    axiosInstance
  ),
};

// For APIs called in the personal center, a separate momentsUcApiClient instance is created.
const momentsUcApiClient = {
  moment: new UcApiMomentHaloRunV1alpha1MomentApi(undefined, "", axiosInstance),
};
// Export instances
export { momentsConsoleApiClient, momentsCoreApiClient, momentsUcApiClient };
```

Usage example:

```typescript
import { momentsConsoleApiClient } from "@/api";

// Query tags for moments
const { data } = await momentsConsoleApiClient.moment.listTags({
    name: props.keyword?.value,
});
```

> [!NOTE]
> It will first execute the `generateOpenApiDocs` task to access `/v3/api-docs/extensionApis` based on the configuration, retrieve the OpenAPI documentation, and save the schema file to the `openApi.outputDir` directory. Then, the `generateApiClient` task will generate API client code in the `openApi.generator.outputDir` directory based on the schema file.

> [!WARNING]
> Executing the `generateApiClient` task will first delete all files in the `openApi.generator.outputDir` directory. Therefore, it is recommended to set the output directory for the API client to a separate directory to avoid accidentally deleting other files.

### generateRoleTemplates Task

In Halo plugin development, permission management is a critical issue, especially when configuring [role templates](https://docs.halo.run/developer-guide/plugin/security/rbac#%E8%A7%92%E8%89%B2%E6%A8%A1%E6%9D%BF). The `rules` section of roles often confuses developers. Specifically, distinguishing between concepts like resource, apiGroup, and verb is a common pain point.

The `generateRoleTemplates` task simplifies this process. It can retrieve OpenAPI docs JSON files based on the configuration in [Configuring Generate API Client](#configuring-generateapiclient) and automatically generate Halo's Role YAML files. This allows developers to focus on their business logic instead of dealing with complex role `rules` configurations.

In the generated `roleTemplate.yaml` file, the `rules` section is automatically generated based on API resources and request methods in the OpenAPI docs, covering possible operations. However, in actual production environments, roles are usually divided into different permission levels based on specific needs, such as:

- **View-only Role Templates**: Typically include only read permissions for resources, such as `get`, `list`, and `watch`.
- **Management Role Templates**: May include create, modify, and delete permissions, such as `create`, `update`, and `delete`.

> The `watch` verb is for WebSocket APIs and will not appear as `watch` in `roleTemplates.yaml`. Instead, it will appear as `list`. Developers need to adjust this based on actual scenarios.

Therefore, the generated YAML file is just a basic template covering all possible operations. Developers need to adjust these `rules` based on their actual needs. For example, for scenarios that only require viewing resources, developers can remove `modify` and `delete` operations from the YAML and retain read permissions. For scenarios that require managing resources, developers can retain `create`, `update`, and `delete` permissions. Developers can also adjust role template dependencies and aggregation relationships based on actual needs.

This way, developers can use the generated YAML file as a foundation to quickly customize permission configurations for different scenarios without having to write complex rules from scratch, reducing the likelihood of errors.

#### How to Use

In the `build.gradle` file, use the `haloPlugin` block to configure settings related to OpenAPI documentation generation and Role template generation:

```groovy
haloPlugin {
    openApi {
        // Refer to the configuration in generateApiClient
    }
}
```

Run the following command in the project directory to generate the `roleTemplates.yaml` file in the `workplace` directory:

```shell
./gradlew generateRoleTemplates
```

## Debug

If you want to debug a Halo plugin project, you can use IntelliJ IDEA's Debug mode to run the `haloServer` or `watch` task. You will see information similar to the following at the beginning of the logs:

```shell
Listening for transport dt_socket at address: 50781 Attach debugger
```

Then click `Attach debugger` to open the debug panel.

## Using Snapshot Versions

Clone this project locally, then execute:

```shell
./gradlew clean build -x test && ./gradlew publishToMavenLocal
```

This will publish the plugin to the local Maven repository. Then, add the following configuration to the `build.gradle` file of your Halo plugin project:

```groovy
plugins {
    //...
    id "run.halo.plugin.devtools" version "0.0.10-SNAPSHOT"
}
```

Next, modify the configuration in `settings.gradle`:

```groovy
pluginManagement {
    repositories {
        mavenLocal()
        // ...
        gradlePluginPortal()
    }
}
```

After completing the above steps, you can use this plugin.
