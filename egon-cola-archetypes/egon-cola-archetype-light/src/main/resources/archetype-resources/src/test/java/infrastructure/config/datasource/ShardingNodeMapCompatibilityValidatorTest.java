package ${package}.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Properties;
import org.junit.jupiter.api.Test;

class ShardingNodeMapCompatibilityValidatorTest {

    private final ShardingNodeMapCompatibilityValidator validator =
            new ShardingNodeMapCompatibilityValidator();

    @Test
    void acceptsOnlyAppendOnlyNToTwoNExpansion() {
        ShardingNodeMap current = parse(1, 4,
                "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1");
        ShardingNodeMap candidate = parse(2, 8,
                "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1,"
                        + "4=shard_2:0,5=shard_2:1,6=shard_3:0,7=shard_3:1");

        assertThatCode(() -> validator.assertCanExpandTo(current, candidate))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsVersionCountAndPrefixChanges() {
        ShardingNodeMap current = parse(1, 4,
                "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1");
        ShardingNodeMap sameVersion = parse(1, 8,
                "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1,"
                        + "4=shard_2:0,5=shard_2:1,6=shard_3:0,7=shard_3:1");
        ShardingNodeMap notDoubled = parse(2, 4,
                "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1");
        ShardingNodeMap changedPrefix = parse(2, 8,
                "0=shard_0:1,1=shard_0:0,2=shard_1:0,3=shard_1:1,"
                        + "4=shard_2:0,5=shard_2:1,6=shard_3:0,7=shard_3:1");

        assertThatThrownBy(() -> validator.assertCanExpandTo(current, sameVersion))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version");
        assertThatThrownBy(() -> validator.assertCanExpandTo(current, notDoubled))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("N to 2N");
        assertThatThrownBy(() -> validator.assertCanExpandTo(current, changedPrefix))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("slot");
    }

    private static ShardingNodeMap parse(int version, int count, String nodes) {
        Properties properties = new Properties();
        properties.setProperty("mapping-version", Integer.toString(version));
        properties.setProperty("node-count", Integer.toString(count));
        properties.setProperty("node-map", nodes);
        return ShardingNodeMap.parse(properties);
    }
}
