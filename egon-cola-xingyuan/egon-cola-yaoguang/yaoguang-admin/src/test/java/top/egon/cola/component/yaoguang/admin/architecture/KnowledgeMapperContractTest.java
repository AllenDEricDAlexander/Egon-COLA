package top.egon.cola.component.yaoguang.admin.architecture;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeMapperContractTest {
    @Test
    void allLocalMapperFilesParseAndEveryMappedPropertyBelongsToItsCarrier() throws Exception {
        var configuration = new MybatisConfiguration();
        Path root = Path.of("src/main/resources/mybatis/mapper");
        try (var files = Files.walk(root)) {
            var mappers = files.filter(path -> path.toString().endsWith(".xml")).sorted().toList();
            assertThat(mappers).hasSize(9);
            for (Path mapper : mappers) {
                assertThat(Files.readString(mapper)).doesNotContain("gateway_group", "gateway_release", "release_id");
                try (var input = Files.newInputStream(mapper)) {
                    new XMLMapperBuilder(input, configuration, mapper.toString(), configuration.getSqlFragments()).parse();
                }
            }
        }
        for (var result : configuration.getResultMaps()) {
            if (Map.class.isAssignableFrom(result.getType())) { continue; }
            var carrier = MetaClass.forClass(result.getType(), new DefaultReflectorFactory());
            for (var field : result.getResultMappings()) {
                if (field.getProperty() != null) {
                    assertThat(carrier.hasSetter(field.getProperty()))
                            .as("%s must expose mapped property %s", result.getType().getName(), field.getProperty())
                            .isTrue();
                }
            }
        }
    }
}
