package top.egon.cola.component.tianshu.admin.repository;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaLocalWriteGuardInnerInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaTenantIdGuardInnerInterceptor;
import top.egon.cola.component.common.mybatis.routing.EgonColaPhysicalTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRouteResult;
import top.egon.cola.component.common.mybatis.routing.EgonColaTwoLevelRouteStrategy;
import top.egon.cola.component.tianshu.admin.dao.DdcInstanceMapper;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcInstanceConverterImpl;
import top.egon.cola.component.tianshu.admin.model.po.DdcInstancePO;
import top.egon.cola.component.tianshu.admin.repository.impl.DdcInstanceRepositoryImpl;

import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DdcInstancePersistenceContractTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 18, 12, 0);
    private static final Instant AUDIT_TIME = NOW.toInstant(ZoneOffset.UTC);
    private final DdcInstanceMapper mapper = mock(DdcInstanceMapper.class);
    private final DdcInstanceRepositoryImpl repository = new DdcInstanceRepositoryImpl(
            mapper, new DdcInstanceConverterImpl(), new EgonColaMybatisPlusProperties(), null);

    @BeforeEach
    void bindContext() {
        EgonColaTenantIdProvider.initialize("tenantId");
        MDC.put("tenantId", "42");
        MDC.put("userId", "system:test");
    }

    @AfterEach
    void clearContext() {
        MDC.clear();
    }

    @Test
    void offliningUsesTheObservedRowVersionAndOriginalLeaseCondition() {
        DdcInstancePO row = DdcInstancePO.builder().id(7L).version(3L).build();
        when(mapper.selectByInstanceId(42L, "instance")).thenReturn(row);
        when(mapper.markOfflineIfLeaseMatches(42L, 7L, 3L, "instance", "lease", "OFFLINE",
                NOW, AUDIT_TIME, "system:test")).thenReturn(0);

        assertThat(repository.markOfflineIfLeaseMatches("instance", "lease", "OFFLINE", NOW)).isZero();
        verify(mapper).markOfflineIfLeaseMatches(42L, 7L, 3L, "instance", "lease", "OFFLINE",
                NOW, AUDIT_TIME, "system:test");
        assertThat(repository.markOfflineIfLeaseMatches("missing", "lease", "OFFLINE", NOW)).isZero();
    }

    @Test
    void revocationSkipsOtherResourcesAndNewerAdmissionsAndCountsOnlyAppliedVersions() {
        DdcInstancePO first = DdcInstancePO.builder().id(7L).version(3L)
                .resourceServerId("resource").resourceVersion(4L).build();
        DdcInstancePO stale = DdcInstancePO.builder().id(8L).version(5L)
                .resourceServerId("resource").resourceVersion(4L).build();
        DdcInstancePO newer = DdcInstancePO.builder().id(9L).version(6L)
                .resourceServerId("resource").resourceVersion(6L).build();
        DdcInstancePO other = DdcInstancePO.builder().id(10L).version(7L)
                .resourceServerId("other").resourceVersion(4L).build();
        when(mapper.selectByScopeAndStatus(42L, "biz", "it", "app", "ONLINE"))
                .thenReturn(List.of(first, stale, newer, other));
        when(mapper.markResourceAdmissionOffline(42L, 7L, 3L, "resource", "biz", "it", "app",
                5L, NOW, AUDIT_TIME, "system:test")).thenReturn(1);

        assertThat(repository.markResourceAdmissionOfflineAt("resource", "biz", "it", "app", 5L, NOW))
                .isEqualTo(1);
        verify(mapper).markResourceAdmissionOffline(42L, 8L, 5L, "resource", "biz", "it", "app",
                5L, NOW, AUDIT_TIME, "system:test");
        verify(mapper, never()).markResourceAdmissionOffline(eq(42L), eq(9L), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any());
        verify(mapper, never()).markResourceAdmissionOffline(eq(42L), eq(10L), any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any());
    }

    @Test
    void actualXmlMutationsPassWriteGuardsAndRejectMissingIdOrVersion() throws Throwable {
        Configuration configuration = new Configuration();
        String resource = "mybatis/mapper/tianshu/DdcInstanceMapper.xml";
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        var properties = new EgonColaMybatisPlusProperties();
        var tenantGuard = new EgonColaTenantIdGuardInnerInterceptor(() -> "system:test", properties);
        var finalGuard = new EgonColaLocalWriteGuardInnerInterceptor(query -> new EgonColaRouteResult(
                List.of(new EgonColaPhysicalTargetBO("primary", "public", query.logicalTable())), "a".repeat(64)),
                tenantGuard, mock(EgonColaTwoLevelRouteStrategy.class), Map.of());
        var originalGuard = new EgonColaOriginalSqlGuardInterceptor(properties);
        Map<String, Object> parameters = new HashMap<>(Map.of("tenantId", 42L, "id", 7L,
                "expectedVersion", 3L, "instanceId", "instance", "leaseId", "lease", "status", "OFFLINE",
                "updatedAt", NOW, "updateTime", AUDIT_TIME, "actor", "system:test"));
        parameters.putAll(Map.of("resourceServerId", "resource", "bizCode", "biz", "env", "it",
                "appCode", "app", "resourceVersion", 5L));
        Executor executor = mock(Executor.class);
        for (String method : List.of("markOfflineIfLeaseMatches", "markResourceAdmissionOffline")) {
            var statement = configuration.getMappedStatement(DdcInstanceMapper.class.getName() + "." + method);
            var invocation = new Invocation(executor, Executor.class.getMethod("update",
                    org.apache.ibatis.mapping.MappedStatement.class, Object.class), new Object[]{statement, parameters});
            originalGuard.intercept(invocation);
            var handler = configuration.newStatementHandler(executor, statement, parameters, RowBounds.DEFAULT, null,
                    statement.getBoundSql(parameters));
            tenantGuard.beforePrepare(handler, mock(Connection.class), null);
            finalGuard.beforePrepare(handler, mock(Connection.class), null);
            parameters.put("id", null);
            assertThatThrownBy(() -> originalGuard.intercept(invocation)).hasMessage("BUSINESS_PREDICATE_REQUIRED");
            parameters.put("id", 7L);
            parameters.put("expectedVersion", null);
            var missingVersion = configuration.newStatementHandler(executor, statement, parameters,
                    RowBounds.DEFAULT, null, statement.getBoundSql(parameters));
            assertThatThrownBy(() -> finalGuard.beforePrepare(missingVersion, mock(Connection.class), null))
                    .hasMessage("EXPECTED_VERSION_REQUIRED");
            parameters.put("expectedVersion", 3L);
        }
    }
}
