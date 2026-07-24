#set( $symbol_dollar = '$' )
package ${package}.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.env.MockEnvironment;

class ShardingYamlLoaderTest {

    @Test
    void shouldResolveSpringPlaceholdersAndPreserveInlineExpressions() {
        String yaml = """
                rules:
                  - !SHARDING
                    actualDataNodes: shard_$->{0..1}.sample_$->{0..1}
                    node-map: ${symbol_dollar}{app.sharding.routing.node-map}
                props:
                  sql-show: ${symbol_dollar}{SHARDING_SQL_SHOW:false}
                """;
        ResourceLoader resourceLoader =
                resourceLoader(new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8)));
        MockEnvironment environment = new MockEnvironment()
                .withProperty(
                        "app.sharding.routing.node-map",
                        "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1");

        byte[] loaded = new ShardingYamlLoader(resourceLoader, environment)
                .load("classpath:rules.yml");

        assertThat(new String(loaded, StandardCharsets.UTF_8))
                .contains("shard_$->{0..1}.sample_$->{0..1}")
                .contains("node-map: 0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1")
                .contains("sql-show: false");
    }

    @Test
    void shouldCompareShardingRuleSuffixAsExactUtf8Text() {
        ShardingYamlLoader loader = new ShardingYamlLoader(
                resourceLoader(new ByteArrayResource(new byte[0])),
                new MockEnvironment());
        byte[] primary = "databaseName: sample\nrules:\n  - !SHARDING\n    value: one\n"
                .getBytes(StandardCharsets.UTF_8);
        byte[] readwrite = """
                databaseName: sample
                rules:
                  - !READWRITE_SPLITTING
                    value: rw
                  - !SHARDING
                    value: one
                """.getBytes(StandardCharsets.UTF_8);

        assertThat(loader.shardingRuleSuffix(primary))
                .isEqualTo(loader.shardingRuleSuffix(readwrite));
    }

    @Test
    void shouldFailWhenResourceDoesNotExist() {
        ShardingYamlLoader loader = new ShardingYamlLoader(
                resourceLoader(new ByteArrayResource(new byte[0]) {
                    @Override
                    public boolean exists() {
                        return false;
                    }
                }),
                new MockEnvironment());

        assertThatThrownBy(() -> loader.load("classpath:missing.yml"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("classpath:missing.yml");
    }

    private static ResourceLoader resourceLoader(Resource resource) {
        return new DefaultResourceLoader() {
            @Override
            public Resource getResource(String location) {
                return resource;
            }
        };
    }
}
