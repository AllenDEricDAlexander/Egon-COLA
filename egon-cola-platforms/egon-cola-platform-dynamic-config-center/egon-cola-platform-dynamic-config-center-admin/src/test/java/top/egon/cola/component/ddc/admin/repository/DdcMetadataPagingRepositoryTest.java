package top.egon.cola.component.ddc.admin.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcBizEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcEnvEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcNamespaceEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcNamespaceEnvAppBindingEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:ddc_metadata_paging_test?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.flyway.enabled=false"
})
class DdcMetadataPagingRepositoryTest {

    @Autowired
    private DdcBizRepository bizRepository;

    @Autowired
    private DdcNamespaceRepository namespaceRepository;

    @Autowired
    private DdcEnvRepository envRepository;

    @Autowired
    private DdcAppRepository appRepository;

    @Autowired
    private DdcNamespaceEnvAppBindingRepository bindingRepository;

    @Test
    void pagesOnlyMetadataVisibleThroughEnabledScopeBindings() {
        bizRepository.save(biz("biz-infra", "infra"));
        bizRepository.save(biz("biz-retail", "retail"));

        namespaceRepository.save(namespace("namespace-infra", "infra", "default", true));
        namespaceRepository.save(namespace("namespace-retail", "retail", "default", true));
        namespaceRepository.save(namespace("namespace-disabled", "infra", "disabled", false));

        envRepository.save(env("env-dev", "dev", 10));
        envRepository.save(env("env-prod", "prod", 20));

        appRepository.save(app("app-gateway", "infra", "gateway"));
        appRepository.save(app("app-worker", "infra", "worker"));
        appRepository.save(app("app-checkout", "retail", "checkout"));

        bindingRepository.save(binding("binding-gateway", "namespace-infra", "prod", "app-gateway", true));
        bindingRepository.save(binding("binding-worker", "namespace-infra", "dev", "app-worker", false));
        bindingRepository.save(binding("binding-checkout", "namespace-retail", "prod", "app-checkout", true));
        bindingRepository.save(binding("binding-disabled", "namespace-disabled", "dev", "app-gateway", true));

        Page<DdcEnvEntity> envPage = envRepository.search(
                "infra",
                "default",
                "pro",
                PageRequest.of(0, 10, Sort.by("sortOrder").ascending()
                        .and(Sort.by("envCode").ascending())
                        .and(Sort.by("id").ascending())));
        Page<DdcAppEntity> appPage = appRepository.search(
                "infra",
                "default",
                "prod",
                "gate",
                PageRequest.of(0, 10, Sort.by("bizCode", "appCode", "id").ascending()));
        Page<DdcEnvEntity> disabledNamespacePage = envRepository.search(
                "infra",
                "disabled",
                null,
                PageRequest.of(0, 10));

        assertThat(envPage.getTotalElements()).isEqualTo(1);
        assertThat(envPage.getContent())
                .extracting(DdcEnvEntity::getEnvCode)
                .containsExactly("prod");
        assertThat(appPage.getTotalElements()).isEqualTo(1);
        assertThat(appPage.getContent())
                .extracting(DdcAppEntity::getAppCode)
                .containsExactly("gateway");
        assertThat(disabledNamespacePage).isEmpty();
    }

    private DdcBizEntity biz(String id, String bizCode) {
        DdcBizEntity entity = new DdcBizEntity();
        entity.setId(id);
        entity.setBizCode(bizCode);
        entity.setBizName(bizCode);
        entity.setEnabled(true);
        return entity;
    }

    private DdcNamespaceEntity namespace(
            String id,
            String bizCode,
            String namespaceCode,
            boolean enabled) {
        DdcNamespaceEntity entity = new DdcNamespaceEntity();
        entity.setId(id);
        entity.setBizCode(bizCode);
        entity.setNamespaceCode(namespaceCode);
        entity.setNamespace(namespaceCode);
        entity.setEnabled(enabled);
        return entity;
    }

    private DdcEnvEntity env(String id, String envCode, int sortOrder) {
        DdcEnvEntity entity = new DdcEnvEntity();
        entity.setId(id);
        entity.setEnvCode(envCode);
        entity.setSortOrder(sortOrder);
        entity.setEnabled(true);
        return entity;
    }

    private DdcAppEntity app(String id, String bizCode, String appCode) {
        DdcAppEntity entity = new DdcAppEntity();
        entity.setId(id);
        entity.setBizCode(bizCode);
        entity.setAppCode(appCode);
        entity.setAppName(appCode);
        entity.setEnabled(true);
        return entity;
    }

    private DdcNamespaceEnvAppBindingEntity binding(
            String id,
            String namespaceId,
            String envCode,
            String appId,
            boolean enabled) {
        DdcNamespaceEnvAppBindingEntity entity = new DdcNamespaceEnvAppBindingEntity();
        entity.setId(id);
        entity.setNamespaceId(namespaceId);
        entity.setEnvCode(envCode);
        entity.setAppId(appId);
        entity.setEnabled(enabled);
        return entity;
    }
}
