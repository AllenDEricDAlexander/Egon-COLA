package top.egon.cola.component.common.mybatis.business;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapts the static TenantID entry to MyBatis-Plus TenantLine SQL ASTs.
 */
public final class EgonColaTenantIdTenantLineHandler implements TenantLineHandler {

    private static final String TENANT_COLUMN = "tenant_id";

    private final Set<String> ignoredTables;

    public EgonColaTenantIdTenantLineHandler(EgonColaMybatisPlusProperties properties) {
        Objects.requireNonNull(properties, "properties must not be null");
        this.ignoredTables = properties.getTenantId().getIgnoredTables().stream()
                .filter(Objects::nonNull)
                .map(EgonColaTenantIdTenantLineHandler::normalizeTableName)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Expression getTenantId() {
        return new LongValue(EgonColaTenantIdProvider.currentTenantId());
    }

    @Override
    public String getTenantIdColumn() {
        return TENANT_COLUMN;
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return ignoredTables.contains(normalizeTableName(tableName));
    }

    private static String normalizeTableName(String tableName) {
        if (tableName == null) {
            return "";
        }
        String normalized = tableName.trim()
                .replace("`", "")
                .replace("\"", "");
        int separator = normalized.lastIndexOf('.');
        return (separator >= 0 ? normalized.substring(separator + 1) : normalized)
                .toLowerCase(Locale.ROOT);
    }
}
