package top.egon.cola.component.ddc.admin.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigCreateRequest;
import top.egon.cola.component.ddc.admin.model.dto.DdcConfigUpdateRequest;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigVO;
import top.egon.cola.component.ddc.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.ddc.admin.repository.DdcOperationLogRepository;
import top.egon.cola.component.ddc.admin.repository.DdcConfigVersionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(DdcConfigService.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:ddc_config_service_test?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.flyway.enabled=false",
        "egon.cola.component.ddc.enabled=false"
})
class DdcConfigServiceTest {

    @Autowired
    private DdcConfigService configService;

    @Autowired
    private DdcConfigVersionRepository versionRepository;

    @Autowired
    private DdcOperationLogRepository operationLogRepository;

    @Autowired
    private DdcConfigItemRepository configItemRepository;

    @Test
    void updateCreatesNewVersion() {
        DdcConfigCreateRequest create = new DdcConfigCreateRequest("demo", "dev", "default",
                "switch", "false", "false", "BOOLEAN", "switch");
        DdcConfigVO created = configService.create(create, "tester");

        DdcConfigUpdateRequest update = new DdcConfigUpdateRequest(created.getId(), "true",
                "enable switch", created.getCurrentVersion());
        DdcConfigVO updated = configService.update(update, "tester");

        assertThat(updated.getCurrentVersion()).isEqualTo(2L);
        assertThat(versionRepository.findByConfigIdOrderByVersionDesc(created.getId())).hasSize(2);
    }

    @Test
    void scopedUpsertAndDeleteUseExpectedVersionAndDeleteIsIdempotent() {
        DdcConfigCreateRequest request = new DdcConfigCreateRequest(
                "gateway",
                "dev",
                "runtime",
                "gateway.routes",
                "{}",
                null,
                "JSON",
                "routes"
        );
        DdcConfigVO created = configService.upsert(request, null, "gateway-admin");
        DdcConfigVO updated = configService.upsert(
                new DdcConfigCreateRequest(
                        "gateway",
                        "dev",
                        "runtime",
                        "gateway.routes",
                        "{\"enabled\":true}",
                        null,
                        "JSON",
                        "routes"
                ),
                created.getCurrentVersion(),
                "gateway-admin"
        );

        assertThat(updated.getCurrentVersion()).isEqualTo(2L);
        assertThatThrownBy(() -> configService.delete(
                "gateway",
                "dev",
                "runtime",
                "gateway.routes",
                1L,
                "gateway-admin",
                "release removed"
        )).hasMessageContaining("version");

        DdcConfigVO deleted = configService.delete(
                "gateway",
                "dev",
                "runtime",
                "gateway.routes",
                updated.getCurrentVersion(),
                "gateway-admin",
                "release removed"
        );
        DdcConfigVO repeated = configService.delete(
                "gateway",
                "dev",
                "runtime",
                "gateway.routes",
                deleted.getCurrentVersion(),
                "gateway-admin",
                "repeat"
        );

        assertThat(deleted.getDeleted()).isTrue();
        assertThat(repeated.getCurrentVersion()).isEqualTo(deleted.getCurrentVersion());
        assertThat(versionRepository.findByConfigIdOrderByVersionDesc(created.getId()))
                .hasSize(3);
        assertThat(operationLogRepository.findAll())
                .extracting(log -> log.getOperationType())
                .containsExactlyInAnyOrder("CREATE", "UPDATE", "DELETE");
    }

    @Test
    void pullReturnsPublishedVersionInsteadOfNewerDraft() {
        DdcConfigVO created = configService.create(new DdcConfigCreateRequest(
                "orders",
                "test",
                "default",
                "feature.rules",
                "{\"version\":1}",
                null,
                "JSON",
                "rules"
        ), "tester");
        configService.upsert(new DdcConfigCreateRequest(
                "orders",
                "test",
                "default",
                "feature.rules",
                "version: 2",
                null,
                "YAML",
                "rules"
        ), created.getCurrentVersion(), "tester");
        DdcConfigItemEntity item = configItemRepository.findById(created.getId())
                .orElseThrow();
        item.setPublishedVersion(1L);
        configItemRepository.saveAndFlush(item);

        assertThat(configService.pull("orders", "test", "default"))
                .singleElement()
                .satisfies(value -> {
                    assertThat(value.getConfigValue()).isEqualTo("{\"version\":1}");
                    assertThat(value.getValueType()).isEqualTo("JSON");
                    assertThat(value.getVersion()).isEqualTo(1L);
                });
        assertThat(configService.value(
                "orders", "test", "default", "feature.rules"
        )).satisfies(value -> {
            assertThat(value.getConfigValue()).isEqualTo("{\"version\":1}");
            assertThat(value.getVersion()).isEqualTo(1L);
        });
    }

    @Test
    void newDraftIsNotVisibleToRuntimeReadsBeforePublish() {
        configService.create(new DdcConfigCreateRequest(
                "orders",
                "test",
                "default",
                "new.draft",
                "draft",
                null,
                "STRING",
                "new draft"
        ), "tester");

        assertThat(configService.pull("orders", "test", "default")).isEmpty();
        assertThat(configService.value(
                "orders", "test", "default", "new.draft"
        )).isNull();
    }

    @Test
    void draftDeleteDoesNotHidePreviouslyPublishedValue() {
        DdcConfigVO created = configService.create(new DdcConfigCreateRequest(
                "orders",
                "test",
                "default",
                "stable.value",
                "published",
                null,
                "STRING",
                "stable value"
        ), "tester");
        DdcConfigItemEntity item = configItemRepository.findById(created.getId())
                .orElseThrow();
        item.setPublishedVersion(1L);
        configItemRepository.saveAndFlush(item);

        DdcConfigVO deleted = configService.delete(
                created.getId(), "tester", "draft delete"
        );

        assertThat(deleted.getDeleted()).isTrue();
        assertThat(configService.value(
                "orders", "test", "default", "stable.value"
        )).satisfies(value -> {
            assertThat(value.getConfigValue()).isEqualTo("published");
            assertThat(value.getVersion()).isEqualTo(1L);
        });
        assertThat(configService.pull("orders", "test", "default"))
                .singleElement()
                .satisfies(value -> assertThat(value.getConfigKey())
                        .isEqualTo("stable.value"));

        item = configItemRepository.findById(created.getId()).orElseThrow();
        item.setPublishedVersion(2L);
        configItemRepository.saveAndFlush(item);
        assertThat(configService.pull("orders", "test", "default")).isEmpty();
    }
}
