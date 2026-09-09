package top.egon.cola.component.yuheng.starter;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.contract.reporting.GatewayDefinitionSourceTypeEnum;
import top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Locks the base starter boundary to the RPC descriptor source.
 *
 * <p>中文：在删除旧 HTTP compiler 前先锁定 starter 只保留 RPC 描述符事实源。
 */
class GatewayStarterRpcOnlyContractTest {

    @Test
    void reportUsesTheTypedRpcDescriptorSourceVocabulary() throws Exception {
        var sourceType = Arrays.stream(
                        GatewayInterfaceDefinitionReport.InterfaceGroup.class
                                .getRecordComponents())
                .filter(component -> "sourceType".equals(component.getName()))
                .findFirst()
                .orElseThrow();
        assertThat(sourceType.getType())
                .isEqualTo(GatewayDefinitionSourceTypeEnum.class);

        assertThat(JsonMapper.builder().build()
                .writeValueAsString(GatewayDefinitionSourceTypeEnum.RPC_DESCRIPTOR))
                .isEqualTo("\"RPC_DESCRIPTOR\"");
    }

    @Test
    void rejectsUnknownProtocolsAndMismatchedSources() {
        assertThatThrownBy(() -> new GatewayInterfaceDefinitionReport.InterfaceGroup(
                "orders",
                "Orders",
                null,
                GatewayDefinitionSourceTypeEnum.RPC_DESCRIPTOR,
                null,
                "HTTP",
                Map.of(),
                List.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTP interface groups");

        assertThatThrownBy(() -> new GatewayInterfaceDefinitionReport.InterfaceGroup(
                "orders",
                "Orders",
                null,
                GatewayDefinitionSourceTypeEnum.OPENAPI31,
                null,
                "RPC",
                Map.of(),
                List.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RPC interface groups");

        assertThatThrownBy(() -> new GatewayInterfaceDefinitionReport.InterfaceGroup(
                "orders",
                "Orders",
                null,
                GatewayDefinitionSourceTypeEnum.MANUAL,
                null,
                "GRAPHQL",
                Map.of(),
                List.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported interface group protocol");
    }

    @Test
    void starterProductionSourcesContainNoHttpCompilerOrWebStack() throws IOException {
        Path starter = locateStarterModule();
        String source = readJavaSources(starter.resolve("src/main/java"));
        assertThat(source)
                .doesNotContain("top.egon.cola.component.yuheng.starter.annotation.EgonHttpService")
                .doesNotContain("top.egon.cola.component.yuheng.starter.annotation.GatewayRequestLocation")
                .doesNotContain("top.egon.cola.component.yuheng.starter.annotation.GatewayRequestSchemaField")
                .doesNotContain("top.egon.cola.component.yuheng.starter.annotation.GatewayResponseSchema")
                .doesNotContain("top.egon.cola.component.yuheng.starter.annotation.GatewaySchemaField")
                .doesNotContain("top.egon.cola.component.yuheng.starter.annotation.GatewaySchemaRequired")
                .doesNotContain("top.egon.cola.component.yuheng.starter.annotation.GatewaySchemaShape")
                .doesNotContain("top.egon.cola.component.yuheng.starter.annotation.GatewaySchemaType")
                .doesNotContain("MvcGatewayDefinitionContributor")
                .doesNotContain("WebFluxGatewayDefinitionContributor")
                .doesNotContain("GatewayJavaSchemaMapper")
                .doesNotContain("GatewayHttpOperationMapper");

        String pom;
        try {
            pom = Files.readString(starter.resolve("pom.xml"));
        } catch (IOException failure) {
            throw new IOException("cannot read starter POM: " + starter, failure);
        }
        assertThat(pom)
                .doesNotContain("spring-boot-starter-web")
                .doesNotContain("spring-boot-starter-webflux")
                .doesNotContain("spring-webmvc")
                .doesNotContain("spring-webflux")
                .doesNotContain("springdoc");
    }

    private Path locateStarterModule() {
        Path current = Path.of(System.getProperty("user.dir"));
        Path module = current.resolve(
                "egon-cola-xingyuan/egon-cola-yuheng/"
                        + "yuheng-starter");
        if (Files.isDirectory(module)) {
            return module;
        }
        return current;
    }

    private String readJavaSources(Path sourceRoot) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            StringBuilder source = new StringBuilder();
            paths.filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            source.append(Files.readString(path));
                        } catch (IOException failure) {
                            throw new SourceReadFailure(failure);
                        }
                    });
            return source.toString();
        } catch (SourceReadFailure failure) {
            throw failure.ioException;
        }
    }

    private static final class SourceReadFailure extends RuntimeException {

        private final IOException ioException;

        private SourceReadFailure(IOException ioException) {
            this.ioException = ioException;
        }
    }
}
