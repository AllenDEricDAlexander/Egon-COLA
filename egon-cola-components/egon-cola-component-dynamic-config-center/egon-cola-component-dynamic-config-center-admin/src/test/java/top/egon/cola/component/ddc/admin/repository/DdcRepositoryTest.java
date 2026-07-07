package top.egon.cola.component.ddc.admin.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import top.egon.cola.component.common.util.IdUtils;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:ddc_repository_test?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.flyway.enabled=false"
})
class DdcRepositoryTest {

    @Autowired
    private DdcConfigItemRepository configItemRepository;

    @Test
    void savesAndFindsConfigItemByNaturalKey() {
        DdcConfigItemEntity entity = new DdcConfigItemEntity();
        entity.setId(IdUtils.simpleUuid());
        entity.setAppCode("demo");
        entity.setEnv("dev");
        entity.setNamespace("default");
        entity.setConfigKey("switch");
        entity.setConfigValue("true");
        entity.setValueType("BOOLEAN");
        entity.setCurrentVersion(1L);
        entity.setEnabled(true);
        entity.setDeleted(false);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        configItemRepository.save(entity);

        assertThat(configItemRepository.findByAppCodeAndEnvAndNamespaceAndConfigKey("demo", "dev", "default", "switch"))
                .isPresent();
    }
}
