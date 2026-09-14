package top.egon.cola.component.common.mybatis.contract;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EgonColaShardingDependencyContractTest {

    @Test
    void artifact_id_is_sharding_jdbc_ext_spring_boot_starter() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));
        assertThat(pom).contains(
                "<artifactId>egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter</artifactId>");
        assertThat(pom).contains("<artifactId>shardingsphere-jdbc</artifactId>");
        assertThat(pom).doesNotContain("<artifactId>shardingsphere-transaction-xa-core</artifactId>");
    }
}
