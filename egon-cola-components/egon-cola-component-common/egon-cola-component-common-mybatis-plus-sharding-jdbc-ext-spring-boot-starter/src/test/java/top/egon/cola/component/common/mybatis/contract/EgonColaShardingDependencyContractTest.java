package top.egon.cola.component.common.mybatis.contract;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EgonColaShardingDependencyContractTest {

    @Test
    void artifact_id_is_sharding_jdbc_ext_spring_boot_starter() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));
        assertThat(pom).contains(
                "<artifactId>egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter</artifactId>");
        assertThat(pom).contains("<artifactId>shardingsphere-jdbc</artifactId>");
        assertThat(pom).contains("<artifactId>shardingsphere-parser-sql-engine-postgresql</artifactId>");
        assertThat(pom).doesNotContain("<artifactId>shardingsphere-parser-sql-engine-mysql</artifactId>");
        assertThat(pom).doesNotContain("<artifactId>shardingsphere-transaction-xa-core</artifactId>");
        String parent = Files.readString(Path.of("..", "pom.xml"));
        assertThat(parent).doesNotContain(
                "<module>egon-cola-component-common-mybatis-plus-spring-boot-starter</module>");
        String bom = Files.readString(Path.of("..", "..", "egon-cola-components-bom", "pom.xml"));
        assertThat(bom).doesNotContain(
                "<artifactId>egon-cola-component-common-mybatis-plus-spring-boot-starter</artifactId>");
    }

    @Test
    void readme_documents_strategy_native_and_xa() throws Exception {
        for (String readme : List.of("README.md", "README.zh-CN.md")) {
            String text = Files.readString(Path.of(readme));
            assertThat(text).contains("COMPLEX_TENANT_THEN_BUSINESS");
            assertThat(text).contains("order_id");
            assertThat(text).contains("config-style: NATIVE");
            assertThat(text).contains("native-rules-resource");
            assertThat(text).contains("!SHARDING");
            assertThat(text).contains("transaction-default-type: XA");
            assertThat(text).contains("shardingsphere-jdbc");
        }
    }
}
