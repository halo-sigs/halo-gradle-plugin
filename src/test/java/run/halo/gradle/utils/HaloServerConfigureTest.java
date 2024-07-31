package run.halo.gradle.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link HaloServerConfigure}.
 *
 * @author guqing
 * @since 0.0.10
 */
class HaloServerConfigureTest {

    @Test
    void mergeUserDefinedTest() throws JSONException, JsonProcessingException {
        var configure = HaloServerConfigure.builder()
            .workDir("/root/.halo2")
            .fixedPluginPath("/fake-project")
            .build();
        var applicationJson = configure.getServerConfig();
        JSONAssert.assertEquals("""
            {
                "server": {
                    "port": 8090
                },
                "spring": {
                    "thymeleaf": {
                        "cache": false
                    },
                    "web": {
                        "resources": {
                            "cache": {
                                "cachecontrol": {
                                    "no-cache": true
                                },
                                "use-last-modified": false
                            }
                        }
                    }
                },
                "halo": {
                    "external-url": "http://localhost:8090",
                    "plugin": {
                        "runtime-mode": "development",
                        "fixed-plugin-path": [
                            "/fake-project"
                        ]
                    },
                    "work-dir": "/root/.halo2"
                },
                "logging": {
                    "level": {
                        "run.halo.app": "DEBUG",
                        "org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler": "DEBUG"
                    }
                },
                "springdoc": {
                    "cache": {
                        "disabled": true
                    },
                    "api-docs": {
                        "enabled": true,
                        "version": "OPENAPI_3_0"
                    },
                    "swagger-ui": {
                        "enabled": true
                    },
                    "show-actuator": true
                },
                "management": {
                    "endpoints": {
                        "web": {
                            "exposure": {
                                "include": "*"
                            }
                        }
                    }
                }
            }
            """, applicationJson.toPrettyString(), true);

        var userDefined = YamlUtils.mapper.readValue("""
            server:
              port: 8080
            logging:
              level:
                run.halo.app: INFO
            halo:
              external-url: http://localhost:8080
              work-dir: /root/.halo-dev2
            """, JsonNode.class);
        var newApplicationJsonString = configure.mergeWithUserConfigAsJson(userDefined);
        JSONAssert.assertEquals("""
            {
                "server": {
                    "port": 8080
                },
                "spring": {
                    "thymeleaf": {
                        "cache": false
                    },
                    "web": {
                        "resources": {
                            "cache": {
                                "cachecontrol": {
                                    "no-cache": true
                                },
                                "use-last-modified": false
                            }
                        }
                    }
                },
                "halo": {
                    "external-url": "http://localhost:8080",
                    "plugin": {
                        "runtime-mode": "development",
                        "fixed-plugin-path": [
                            "/fake-project"
                        ]
                    },
                    "work-dir": "/root/.halo-dev2"
                },
                "logging": {
                    "level": {
                        "run.halo.app": "INFO",
                        "org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler": "DEBUG"
                    }
                },
                "springdoc": {
                    "cache": {
                        "disabled": true
                    },
                    "api-docs": {
                        "enabled": true,
                        "version": "OPENAPI_3_0"
                    },
                    "swagger-ui": {
                        "enabled": true
                    },
                    "show-actuator": true
                },
                "management": {
                    "endpoints": {
                        "web": {
                            "exposure": {
                                "include": "*"
                            }
                        }
                    }
                }
            }
            """, newApplicationJsonString, true);
    }
}
