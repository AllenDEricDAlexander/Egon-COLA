package top.egon.cola.component.tianshu.admin.service.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.config.DdcAdminProperties;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigItemBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigVersionBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcOperationLogBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcConfigCreateRequest;
import top.egon.cola.component.tianshu.admin.model.dto.DdcConfigQueryRequest;
import top.egon.cola.component.tianshu.admin.model.dto.DdcConfigRollbackRequest;
import top.egon.cola.component.tianshu.admin.model.dto.DdcConfigUpdateRequest;
import top.egon.cola.component.tianshu.admin.model.enums.ChangeType;
import top.egon.cola.component.tianshu.admin.model.vo.DdcConfigVO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcConfigVersionVO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcOperationLogRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcConfigItemConverterImpl;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcConfigVersionConverterImpl;
import top.egon.cola.component.tianshu.model.config.DdcConfigValue;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DdcConfigServiceTest {

    @BeforeAll
    static void initializeIdGenerator() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

    private DdcConfigItemRepository itemRepository;
    private DdcConfigVersionRepository versionRepository;
    private DdcOperationLogRepository operationLogRepository;
    private DdcNamespaceEnvAppBindingRepository bindingRepository;
    private DdcPublishTaskRepository publishTaskRepository;
    private DdcConfigService service;

    @BeforeEach
    void setUp() {
        itemRepository = mock(DdcConfigItemRepository.class);
        versionRepository = mock(DdcConfigVersionRepository.class);
        operationLogRepository = mock(DdcOperationLogRepository.class);
        bindingRepository = mock(DdcNamespaceEnvAppBindingRepository.class);
        publishTaskRepository = mock(DdcPublishTaskRepository.class);
        ObjectProvider<DdcAdminProperties> properties =
                new DefaultListableBeanFactory().getBeanProvider(DdcAdminProperties.class);
        service = new DdcConfigService(itemRepository, versionRepository, new DdcConfigItemConverterImpl(),
                new DdcConfigVersionConverterImpl(), operationLogRepository, bindingRepository,
                publishTaskRepository, properties);
        when(itemRepository.insert(any(DdcConfigItemBO.class))).thenAnswer(invocation -> {
            DdcConfigItemBO item = invocation.getArgument(0);
            item.setId(101L).setTenantId(401L).setPublicId("config-public-1").setVersion(0L);
            return item;
        });
        when(itemRepository.update(any(DdcConfigItemBO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.insert(any(DdcConfigVersionBO.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsConfigAndVersionWithSeparatePublicAndInternalIds() {
        DdcConfigVO result = service.create(configRequest("infra", "prod", "gateway"), "tester");

        assertThat(result.getId()).isEqualTo("config-public-1");
        assertThat(result.getCurrentVersion()).isEqualTo(1L);
        assertThat(result.getDeleted()).isFalse();
        var version = org.mockito.ArgumentCaptor.forClass(DdcConfigVersionBO.class);
        verify(versionRepository).insert(version.capture());
        assertThat(version.getValue().getConfigItemId()).isEqualTo(101L);
        assertThat(version.getValue().getConfigId()).isEqualTo("config-public-1");
        assertThat(version.getValue().getConfigRevision()).isEqualTo(1L);
        assertThat(version.getValue().getChangeType()).isEqualTo(ChangeType.CREATE.name());
        var operation = org.mockito.ArgumentCaptor.forClass(DdcOperationLogBO.class);
        verify(operationLogRepository).insert(operation.capture());
        assertThat(operation.getValue().getBizCode()).isEqualTo("infra");
        assertThat(operation.getValue().getAppCode()).isEqualTo("gateway");
        assertThat(operation.getValue().getOperationType()).isEqualTo(ChangeType.CREATE.name());
    }

    @Test
    void upsertKeepsBusinessVersionAndClearsDefaultValueWithoutTouchingPublishedPointer() {
        DdcConfigItemBO existing = item(101L, "config-public-1", 3L, 2L, false, "old");
        existing.setDefaultValue("fallback");
        when(itemRepository.findByBizCodeAndEnvAndAppCode("infra", "prod", "gateway"))
                .thenReturn(List.of(existing));

        DdcConfigVO result = service.upsert(configRequest("infra", "prod", "gateway"), 3L, "tester");

        assertThat(result.getCurrentVersion()).isEqualTo(4L);
        assertThat(result.getContent()).contains("enabled: true");
        assertThat(existing.getDefaultValue()).isNull();
        assertThat(existing.getPublishedVersion()).isEqualTo(2L);
        var version = org.mockito.ArgumentCaptor.forClass(DdcConfigVersionBO.class);
        verify(versionRepository).insert(version.capture());
        assertThat(version.getValue().getConfigRevision()).isEqualTo(4L);
        assertThat(version.getValue().getOldContent()).isEqualTo("old");
    }

    @Test
    void createRefusesToReuseABusinessDeletedResourceUntilRollback() {
        when(itemRepository.findByBizCodeAndEnvAndAppCode("infra", "prod", "gateway"))
                .thenReturn(List.of(item(101L, "config-public-1", 4L, 2L, true, "old")));
        assertThatThrownBy(() -> service.upsert(configRequest("infra", "prod", "gateway"), 4L, "tester"))
                .isInstanceOf(DdcAdminException.class).hasMessage("deleted config resource cannot be reused");
        verify(itemRepository, never()).insert(any(DdcConfigItemBO.class));
        verify(itemRepository, never()).update(any(DdcConfigItemBO.class));
    }

    @Test
    void businessDeleteAndRollbackLeaveTechnicalDeletedAtUntouched() {
        DdcConfigItemBO deletedItem = item(101L, "config-public-1", 2L, 1L, false, "current");
        when(itemRepository.findByPublicId("config-public-1")).thenReturn(Optional.of(deletedItem));
        DdcConfigVO deleted = service.delete("config-public-1", "tester", "remove");
        assertThat(deleted.getDeleted()).isTrue();
        assertThat(deleted.getEnabled()).isFalse();
        assertThat(deletedItem.getDeletedAt()).isNull();
        assertThat(deletedItem.getCurrentVersion()).isEqualTo(3L);

        DdcConfigVersionBO target = version(101L, "config-public-1", 1L, "feature:\n  enabled: false\n",
                ChangeType.CREATE.name());
        when(versionRepository.findByConfigIdAndVersion("config-public-1", 1L)).thenReturn(Optional.of(target));
        DdcConfigVO restored = service.rollback(new DdcConfigRollbackRequest("config-public-1", 1L, "restore"),
                "tester");
        assertThat(restored.getDeleted()).isFalse();
        assertThat(restored.getEnabled()).isTrue();
        assertThat(restored.getContent()).isEqualTo("feature:\n  enabled: false\n");
        assertThat(deletedItem.getDeletedAt()).isNull();
        assertThat(deletedItem.getCurrentVersion()).isEqualTo(4L);
    }

    @Test
    void searchAndPagePreserveFiltersVisibilityAndPageEnvelope() {
        DdcConfigItemBO item = item(101L, "config-public-1", 2L, 2L, false, "runtime");
        when(itemRepository.search("infra", "team-a", "prod", "gateway", null, false)).thenReturn(List.of(item));
        when(bindingRepository.findVisibleNamespaceCodes("infra", "prod", "gateway")).thenReturn(List.of("team-a"));
        when(itemRepository.search(eq("infra"), eq("team-a"), eq("prod"), eq("gateway"), isNull(),
                eq(false), any(PageQuery.class))).thenReturn(DdcPageResult.<DdcConfigItemBO>builder()
                        .records(List.of(item)).total(12).pageNo(2).pageSize(10).build());
        DdcConfigQueryRequest query = new DdcConfigQueryRequest();
        query.setBizCode(" infra ");
        query.setNamespaceCode(" team-a ");
        query.setEnv(" prod ");
        query.setAppCode(" gateway ");
        query.setIncludeDeleted(false);

        DdcConfigVO listed = service.list(query).getFirst();
        DdcPageResult<DdcConfigVO> page = service.page(query, new PageQuery(2, 10));

        assertThat(listed.getId()).isEqualTo("config-public-1");
        assertThat(listed.getVisibleNamespaces()).containsExactly("team-a");
        assertThat(page.getTotal()).isEqualTo(12);
        assertThat(page.getPageNo()).isEqualTo(2);
        assertThat(page.getRecords().getFirst().getId()).isEqualTo("config-public-1");
    }

    @Test
    void pullUsesPublishedBusinessRevisionRatherThanTechnicalVersion() {
        DdcConfigItemBO item = item(101L, "config-public-1", 8L, 5L, false, "draft");
        when(itemRepository.findByBizCodeAndEnvAndAppCode("infra", "prod", "gateway")).thenReturn(List.of(item));
        when(versionRepository.findByConfigItemIdAndRevision(101L, 5L)).thenReturn(Optional.of(
                version(101L, "config-public-1", 5L, "published", ChangeType.UPDATE.name())));

        List<DdcConfigValue> result = service.pull("infra", "prod", "gateway");

        assertThat(result).singleElement().satisfies(value -> {
            assertThat(value.getVersion()).isEqualTo(5L);
            assertThat(value.getContent()).isEqualTo("published");
        });
        verify(versionRepository).findByConfigItemIdAndRevision(101L, 5L);
        verifyNoInteractions(publishTaskRepository);
    }

    @Test
    void versionListAndPageProjectConfigRevisionToTheLegacyVersionField() {
        DdcConfigVersionBO version = version(101L, "config-public-1", 5L, "published", ChangeType.UPDATE.name());
        when(versionRepository.findByConfigIdOrderByVersionDesc("config-public-1")).thenReturn(List.of(version));
        when(versionRepository.findByConfigIdOrderByVersionDescIdDesc(eq("config-public-1"), any(PageQuery.class)))
                .thenReturn(DdcPageResult.<DdcConfigVersionBO>builder().records(List.of(version))
                        .total(1).pageNo(1).pageSize(10).build());

        DdcConfigVersionVO row = service.versions("config-public-1").getFirst();
        DdcPageResult<DdcConfigVersionVO> page = service.pageVersions("config-public-1", new PageQuery(1, 10));

        assertThat(row.getId()).isEqualTo(version.getPublicId());
        assertThat(row.getConfigId()).isEqualTo("config-public-1");
        assertThat(row.getVersion()).isEqualTo(5L);
        assertThat(page.getRecords().getFirst().getVersion()).isEqualTo(5L);
    }

    private DdcConfigCreateRequest configRequest(String biz, String env, String app) {
        return new DdcConfigCreateRequest(biz, env, app, null, "application.yml",
                "feature:\n  enabled: true\n", "YAML", "config test");
    }

    private DdcConfigItemBO item(Long id, String publicId, Long currentVersion, Long publishedVersion,
            boolean deleted, String content) {
        return DdcConfigItemBO.builder().id(id).tenantId(401L).publicId(publicId).bizCode("infra")
                .appCode("gateway").env("prod").namespace(null).resourceName("application.yml")
                .content(content).format("YAML").currentVersion(currentVersion).publishedVersion(publishedVersion)
                .enabled(!deleted).deleted(deleted).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .version(0L).build();
    }

    private DdcConfigVersionBO version(Long configItemId, String configPublicId, Long revision,
            String content, String changeType) {
        return DdcConfigVersionBO.builder().id(revision + 100L).publicId("version-" + revision)
                .configItemId(configItemId).configId(configPublicId).configRevision(revision)
                .resourceName("application.yml").newContent(content).format("YAML").changeType(changeType)
                .createdAt(LocalDateTime.now()).version(0L).build();
    }
}
