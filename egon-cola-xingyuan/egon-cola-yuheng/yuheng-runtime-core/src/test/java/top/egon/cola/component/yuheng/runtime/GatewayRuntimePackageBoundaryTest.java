package top.egon.cola.component.yuheng.runtime;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayRuntimePackageBoundaryTest {

    private static final Path SOURCE_ROOT = Path.of(
            "src/main/java/top/egon/cola/component/yuheng/runtime"
    );

    @Test
    void containsOnlySharedCapabilities() throws IOException {
        assertTrue(Files.isDirectory(SOURCE_ROOT));
        Set<String> allowed = Set.of(
                "config", "provider", "security", "traffic", "transport",
                "observability", "http", "rpc", "operation", "rule"
        );
        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertTrue(allowed.contains(SOURCE_ROOT.relativize(file).getName(0).toString()),
                        () -> "Unexpected runtime capability: " + file);
                String source = Files.readString(file);
                assertFalse(source.contains("@SpringBootApplication"), file.toString());
                assertFalse(source.contains("import top.egon.cola.component.yuheng.engine."), file.toString());
                assertFalse(source.contains("import top.egon.cola.component.yuheng.mcp."), file.toString());
                assertFalse(source.contains("import top.egon.cola.component.yuheng.admin."), file.toString());
            }
        }
    }

    @Test
    void ownsReusableListenersAndOutboundCallsWithoutApiIngress() {
        for (String shared : List.of(
                "http/service/GatewayHttpListener.java",
                "http/adapter/HttpUpstreamAdapter.java",
                "rpc/adapter/RpcProviderChannelCache.java",
                "rpc/adapter/ProtobufDescriptorRegistry.java",
                "operation/service/EngineGatewayOperationInvoker.java"
        )) {
            assertTrue(Files.isRegularFile(SOURCE_ROOT.resolve(shared)), shared);
        }
        for (String ingress : List.of(
                "http/service/GatewayHttpServer.java",
                "http/service/DefaultGatewayHttpDataPlaneHandler.java",
                "http/websocket/service/GatewayWebSocketProxy.java",
                "rpc/service/RpcGatewayServer.java",
                "rpc/service/RpcGatewayForwarder.java"
        )) {
            assertFalse(Files.exists(SOURCE_ROOT.resolve(ingress)), ingress);
        }
    }

    @Test
    void doesNotDependOnExecutableModules() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));
        for (String artifact : List.of(
                "yuheng-biz-gateway", "yuheng-mcp-gateway",
                "yuheng-mcp-core", "yuheng-admin",
                "spring-boot-maven-plugin"
        )) {
            assertFalse(pom.contains("<artifactId>" + artifact + "</artifactId>"), artifact);
        }
    }
}
