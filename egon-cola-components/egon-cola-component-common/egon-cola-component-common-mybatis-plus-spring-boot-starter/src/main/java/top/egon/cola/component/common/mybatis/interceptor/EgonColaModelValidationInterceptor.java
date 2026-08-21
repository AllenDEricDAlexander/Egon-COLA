package top.egon.cola.component.common.mybatis.interceptor;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.sql.Statement;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * One MyBatis boundary for repository Model parameter and result validation.
 * MP fills IDs/audit fields before ParameterHandler.setParameters, so the
 * persisted group is evaluated only after authoritative fill has completed.
 */
@Intercepts({
        @Signature(type = ParameterHandler.class, method = "setParameters",
                args = {java.sql.PreparedStatement.class}),
        @Signature(type = ResultSetHandler.class, method = "handleResultSets",
                args = {Statement.class})
})
public final class EgonColaModelValidationInterceptor implements Interceptor {

    private final EgonColaModelValidationUtils modelValidationUtils;

    public EgonColaModelValidationInterceptor(EgonColaModelValidationUtils modelValidationUtils) {
        this.modelValidationUtils = Objects.requireNonNull(modelValidationUtils,
                "modelValidationUtils must not be null");
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object target = unwrap(invocation.getTarget());
        if (target instanceof ParameterHandler parameterHandler
                && "setParameters".equals(invocation.getMethod().getName())) {
            MappedStatement mappedStatement = mappedStatement(target);
            EgonColaModelValidationGroups.Operation operation = operation(mappedStatement);
            visit(parameterHandler.getParameterObject(), operation,
                    Collections.newSetFromMap(new IdentityHashMap<>()));
            return invocation.proceed();
        }
        if (target instanceof ResultSetHandler
                && "handleResultSets".equals(invocation.getMethod().getName())) {
            Object result = invocation.proceed();
            visit(result, EgonColaModelValidationGroups.Operation.LOADED,
                    Collections.newSetFromMap(new IdentityHashMap<>()));
            return result;
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof ParameterHandler || target instanceof ResultSetHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    private static Object unwrap(Object target) {
        return com.baomidou.mybatisplus.core.toolkit.PluginUtils.realTarget(target);
    }

    private static MappedStatement mappedStatement(Object target) {
        try {
            MetaObject metaObject = SystemMetaObject.forObject(target);
            Object value = metaObject.getValue("mappedStatement");
            return value instanceof MappedStatement statement ? statement : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void visit(Object value,
                       EgonColaModelValidationGroups.Operation operation,
                       Set<Object> visited) {
        if (value == null || !visited.add(value)) {
            return;
        }
        if (value instanceof EgonModel<?> model) {
            if (operation == EgonColaModelValidationGroups.Operation.DELETE
                    && isIdentifierOnly(model)) {
                return;
            }
            validateModel(model, operation);
            return;
        }
        if (value instanceof IPage<?> page) {
            visit(page.getRecords(), operation, visited);
            return;
        }
        if (value instanceof Optional<?> optional) {
            optional.ifPresent(candidate -> visit(candidate, operation, visited));
            return;
        }
        if (value instanceof Map<?, ?> map) {
            map.values().forEach(candidate -> visit(candidate, operation, visited));
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(candidate -> visit(candidate, operation, visited));
            return;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                visit(Array.get(value, index), operation, visited);
            }
            return;
        }
        Method getEntity = findGetEntity(value.getClass());
        if (getEntity != null) {
            try {
                getEntity.setAccessible(true);
                visit(getEntity.invoke(value), operation, visited);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("MODEL_PARAMETER_GRAPH_UNREADABLE", exception);
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void validateModel(EgonModel<?> model,
                               EgonColaModelValidationGroups.Operation operation) {
        modelValidationUtils.validate((EgonModel) model, operation);
    }

    private static Method findGetEntity(Class<?> type) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredMethod("getEntity");
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static boolean isIdentifierOnly(EgonModel<?> model) {
        return model.getCreateUserId() == null
                && model.getCreateTime() == null
                && model.getIsDeleted() == null;
    }

    private static EgonColaModelValidationGroups.Operation operation(SqlCommandType commandType) {
        return switch (commandType) {
            case INSERT -> EgonColaModelValidationGroups.Operation.INSERT;
            case UPDATE -> EgonColaModelValidationGroups.Operation.UPDATE;
            case DELETE -> EgonColaModelValidationGroups.Operation.DELETE;
            case SELECT, UNKNOWN, FLUSH -> EgonColaModelValidationGroups.Operation.QUERY;
        };
    }

    private static EgonColaModelValidationGroups.Operation operation(MappedStatement mappedStatement) {
        if (mappedStatement == null) {
            return EgonColaModelValidationGroups.Operation.QUERY;
        }
        String id = mappedStatement.getId();
        if (id != null) {
            String normalized = id.toLowerCase(java.util.Locale.ROOT);
            if (normalized.contains("deletebyid") || normalized.endsWith(".delete")) {
                return EgonColaModelValidationGroups.Operation.DELETE;
            }
        }
        return operation(mappedStatement.getSqlCommandType());
    }
}
