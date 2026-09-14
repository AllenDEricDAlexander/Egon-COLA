package top.egon.cola.component.common.mybatis.schema;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.routing.EgonColaPhysicalTargetBO;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Idempotent PostgreSQL CREATE/ADD from MyBatis-Plus TableInfo. Never emits DROP.
 */
@Slf4j
@Component("egonColaTableInfoSchemaMaintainer")
@RequiredArgsConstructor
public class EgonColaTableInfoSchemaMaintainer {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Qualifier("egonColaClock")
    private final Clock clock;

    public List<EgonColaSchemaMaintainResult> maintain(EgonColaSchemaMaintainQuery query) {
        validationUtils.validate(query);
        if (query.dropExtraColumns()) {
            throw new EgonColaMybatisPlusConfigurationException("DROP_FORBIDDEN");
        }
        Instant appliedAt = clock.instant();
        List<EgonColaSchemaMaintainResult> results = new ArrayList<>();
        for (Class<?> model : scan(query.modelPackages())) {
            TableInfo tableInfo = tableInfo(model);
            if (missingTenantId(tableInfo)) {
                throw new EgonColaMybatisPlusConfigurationException("TENANT_ID_REQUIRED");
            }
            EgonColaRoutingProfileBO profile = query.profiles().get(tableInfo.getTableName());
            if (profile == null) {
                continue;
            }
            for (EgonColaPhysicalTargetBO node : nodes(profile)) {
                String sql = createTableSql(node, tableInfo);
                if (sql.toUpperCase(Locale.ROOT).contains("DROP")) {
                    throw new EgonColaMybatisPlusConfigurationException("DROP_FORBIDDEN");
                }
                results.add(new EgonColaSchemaMaintainResult(tableInfo.getTableName(), "CREATED", sql, appliedAt));
            }
        }
        return List.copyOf(results);
    }

    private static boolean missingTenantId(TableInfo tableInfo) {
        if ("tenant_id".equals(tableInfo.getKeyColumn())) {
            return false;
        }
        return tableInfo.getFieldList().stream().noneMatch(field -> "tenant_id".equals(field.getColumn()));
    }

    private static List<EgonColaPhysicalTargetBO> nodes(EgonColaRoutingProfileBO profile) {
        return profile.actualNodes().values().stream().flatMap(Collection::stream).toList();
    }

    private static String createTableSql(EgonColaPhysicalTargetBO node, TableInfo tableInfo) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS \"")
                .append(node.schema()).append("\".\"").append(node.table()).append("\" (");
        List<String> columns = new ArrayList<>();
        if (tableInfo.getKeyColumn() != null) {
            columns.add("\"" + tableInfo.getKeyColumn() + "\" BIGINT NOT NULL");
        }
        for (TableFieldInfo field : tableInfo.getFieldList()) {
            columns.add("\"" + field.getColumn() + "\" " + postgresType(field) + nullable(field));
        }
        sql.append(String.join(", ", columns)).append(')');
        return sql.toString();
    }

    private static String postgresType(TableFieldInfo field) {
        Class<?> type = field.getPropertyType();
        if (type == Long.class || type == long.class) {
            return "BIGINT";
        }
        if (type == Integer.class || type == int.class) {
            return "INTEGER";
        }
        if (java.time.Instant.class.isAssignableFrom(type) || java.time.LocalDateTime.class.isAssignableFrom(type)) {
            return "TIMESTAMPTZ";
        }
        return "TEXT";
    }

    private static String nullable(TableFieldInfo field) {
        return "tenant_id".equals(field.getColumn()) ? " NOT NULL" : "";
    }

    private static TableInfo tableInfo(Class<?> model) {
        TableInfo existing = TableInfoHelper.getTableInfo(model);
        if (existing != null) {
            return existing;
        }
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), model.getName());
        return TableInfoHelper.initTableInfo(assistant, model);
    }

    private static List<Class<?>> scan(List<String> modelPackages) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(TableName.class));
        Set<Class<?>> models = new LinkedHashSet<>();
        for (String basePackage : modelPackages) {
            for (BeanDefinition definition : scanner.findCandidateComponents(basePackage)) {
                try {
                    models.add(Class.forName(definition.getBeanClassName()));
                } catch (ClassNotFoundException failure) {
                    throw new EgonColaMybatisPlusConfigurationException("TENANT_ID_REQUIRED", failure);
                }
            }
        }
        return List.copyOf(models);
    }
}
