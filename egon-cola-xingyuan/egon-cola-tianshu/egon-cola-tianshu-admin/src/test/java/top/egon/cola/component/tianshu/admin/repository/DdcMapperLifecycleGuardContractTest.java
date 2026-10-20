package top.egon.cola.component.tianshu.admin.repository;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaTenantIdGuardInnerInterceptor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcMapperLifecycleGuardContractTest {

    @Test
    void namedActiveReadsSatisfyTheActualGuardForEveryJoinedTable() throws Exception {
        Configuration configuration = new Configuration();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("tenantId", 1001L);
        parameters.put("id", 1L);
        parameters.put("resourceId", 1L);
        parameters.put("configId", 1L);
        parameters.put("includeDeleted", false);
        parameters.put("updatedAt", LocalDateTime.of(2026, 10, 18, 0, 0));
        parameters.put("offset", 0L);
        parameters.put("limit", 20L);
        for (var resource : new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mybatis/mapper/tianshu/*.xml")) {
            String xml;
            try (var input = resource.getInputStream()) {
                xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            var collections = Pattern.compile("collection=\"([^\"]+)\"").matcher(xml);
            while (collections.find()) {
                String key = collections.group(1);
                if (!key.contains(".")) {
                    parameters.putIfAbsent(key, key.toLowerCase().contains("status")
                            ? List.of("PENDING", "PUBLISHING") : List.of(1L, 2L));
                }
            }
            try (var input = resource.getInputStream()) {
                new XMLMapperBuilder(input, configuration, resource.toString(), configuration.getSqlFragments()).parse();
            }
        }
        EgonColaMybatisPlusProperties properties = new EgonColaMybatisPlusProperties();
        var source = new org.springframework.beans.factory.config.YamlPropertiesFactoryBean();
        source.setResources(new org.springframework.core.io.ClassPathResource("egon-tianshu-persistence.yml"));
        Map<String, Object> configured = new HashMap<>();
        source.getObject().forEach((key, value) -> configured.put(key.toString(), value));
        new org.springframework.boot.context.properties.bind.Binder(
                new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(configured))
                .bind(EgonColaMybatisPlusProperties.PREFIX + ".lifecycle",
                        org.springframework.boot.context.properties.bind.Bindable.ofInstance(properties.getLifecycle()));
        var guard = new EgonColaTenantIdGuardInnerInterceptor(() -> "system:test", properties);
        new org.springframework.context.annotation.CommonAnnotationBeanPostProcessor()
                .postProcessBeforeInitialization(guard, "sqlGuard");
        var previous = MDC.getCopyOfContextMap();
        List<String> failures = new ArrayList<>();
        int checked = 0;
        var parser = new org.apache.shardingsphere.infra.parser.ShardingSphereSQLParserEngine(
                org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader.getService(
                        org.apache.shardingsphere.database.connector.core.type.DatabaseType.class, "PostgreSQL"),
                new org.apache.shardingsphere.sql.parser.engine.api.CacheOption(16, 256),
                new org.apache.shardingsphere.sql.parser.engine.api.CacheOption(16, 256));
        EgonColaTenantIdProvider.initialize("tenantId");
        MDC.put("tenantId", "1001");
        try {
            for (String name : configuration.getMappedStatementNames().stream()
                    .filter(value -> value.startsWith("top.egon.cola.component.tianshu.admin.dao.")).sorted().toList()) {
                var statement = configuration.getMappedStatement(name, false);
                if (statement.getSqlCommandType() != SqlCommandType.SELECT) { continue; }
                checked++;
                try {
                    guard.beforeQuery(null, statement, parameters, RowBounds.DEFAULT, null, statement.getBoundSql(parameters));
                    parser.parse(statement.getBoundSql(parameters).getSql(), false);
                } catch (RuntimeException failure) {
                    Throwable cause = failure;
                    while (cause.getCause() != null) { cause = cause.getCause(); }
                    failures.add(name + ": " + cause.getMessage());
                }
            }
            assertThat(checked).isGreaterThan(100);
            assertThat(failures).isEmpty();
            var maxRun = configuration.getMappedStatement(
                    "top.egon.cola.component.tianshu.admin.dao.DdcAiOptimizationRunMapper.selectMaxRunNo");
            Map<String, Object> unbounded = new HashMap<>(parameters);
            unbounded.put("resourceId", null);
            assertThatThrownBy(() -> guard.beforeQuery(null, maxRun, unbounded, RowBounds.DEFAULT, null,
                    maxRun.getBoundSql(unbounded))).hasMessage("LIFECYCLE_KEY_REQUIRED");
            Map<String, Object> foreignTenant = new HashMap<>(parameters);
            foreignTenant.put("tenantId", 1002L);
            assertThatThrownBy(() -> guard.beforeQuery(null, maxRun, foreignTenant, RowBounds.DEFAULT, null,
                    maxRun.getBoundSql(foreignTenant))).hasMessage("TENANT_CONTEXT_MISMATCH");
        } finally {
            if (previous == null) { MDC.clear(); } else { MDC.setContextMap(previous); }
        }
    }
}
