package top.egon.cola.archetype.source.serviceopen.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.boot.context.properties.bind.Binder;
import top.egon.cola.archetype.source.serviceopen.infrastructure.config.datasource.ShardingDataSourceProperties;
import static org.assertj.core.api.Assertions.assertThat;

class EvaluationDataSourceModeTest {
    @Test
    void bothModesBindPostgresqlDdlTargetsWithoutStartingAnApplication() throws Exception {
        for (String mode : java.util.List.of("sharding", "sharding-readwrite")) {
            var environment = new StandardEnvironment();
            var properties = new YamlPropertySourceLoader().load(mode, new ClassPathResource("datasource/" + mode + ".yml"));
            properties.forEach(environment.getPropertySources()::addFirst);
            var targets = Binder.get(environment).bind("app." + mode + ".ddl", ShardingDataSourceProperties.ShardingDdlProperties.class).orElseThrow(() -> new IllegalStateException("DDL targets missing"));
            assertThat(targets.targets()).hasSize(3).allSatisfy(target -> {
                assertThat(target.schema()).isEqualTo("public");
                assertThat(target.manifest()).isEqualTo("classpath:db/egon-mp/repository-manifest.json");
                assertThat(target.dataSourceName()).doesNotContain("replica");
            });
        }
    }
}
