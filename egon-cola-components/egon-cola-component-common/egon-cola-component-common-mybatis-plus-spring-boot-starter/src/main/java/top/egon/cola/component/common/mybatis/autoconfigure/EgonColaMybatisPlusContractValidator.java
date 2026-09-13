package top.egon.cola.component.common.mybatis.autoconfigure;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.core.toolkit.reflect.GenericTypeUtils;
import com.baomidou.mybatisplus.extension.ddl.IDdl;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataChangeRecorderInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.IllegalSQLInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.handler.EgonColaMetaObjectHandler;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaDataChangeRecorderInnerInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaLocalWriteGuardInnerInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaModelValidationInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaOriginalSqlGuardInterceptor;
import top.egon.cola.component.common.mybatis.interceptor.EgonColaTenantIdGuardInnerInterceptor;
import top.egon.cola.component.common.mybatis.model.EgonColaIdentifierGenerator;
import top.egon.cola.component.common.mybatis.model.EgonModel;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Validates actual factory wiring and ORM metadata before the application accepts requests. */
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("deprecation")
public final class EgonColaMybatisPlusContractValidator implements SmartInitializingSingleton {

    @Qualifier("mybatisPlusInterceptor")
    private final ObjectProvider<MybatisPlusInterceptor> outerProvider;
    @Qualifier("egonColaMetaObjectHandler")
    private final ObjectProvider<MetaObjectHandler> handlerProvider;
    @Qualifier("egonColaModelValidationInterceptor")
    private final ObjectProvider<EgonColaModelValidationInterceptor> validationProvider;
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("beanFactory")
    private final ConfigurableListableBeanFactory beanFactory;
    @Qualifier("environment")
    private final Environment environment;
    @Qualifier("egonColaRoutingProfiles")
    private final Map<String, EgonColaRoutingProfileBO> profiles;

    @Override
    public void afterSingletonsInstantiated() {
        validateProperties();
        List<MybatisPlusInterceptor> outers = outerProvider.orderedStream().toList();
        if (outers.isEmpty()) { throw failure("MYBATIS_PLUS_OUTER_INTERCEPTOR_MISSING"); }
        outers.forEach(outer -> validateInnerChain(outer.getInterceptors()));
        EgonColaModelValidationInterceptor validation = validationProvider.getIfAvailable();
        if (validation == null) { throw failure("MODEL_VALIDATION_INTERCEPTOR_MISSING"); }
        List<MetaObjectHandler> handlers = handlerProvider.orderedStream().toList();
        if (handlers.isEmpty() || handlers.stream().anyMatch(handler -> !(handler instanceof EgonColaMetaObjectHandler))) {
            throw failure("META_OBJECT_HANDLER_CONTRACT_INVALID");
        }
        EgonColaIdentifierGenerator idGenerator = beanFactory.getBeanProvider(EgonColaIdentifierGenerator.class).getIfAvailable();
        if (idGenerator == null) { throw failure("IDENTIFIER_GENERATOR_CONTRACT_INVALID"); }
        if (properties.getDdl().isEnabled() && (beanFactory.getBeanNamesForType(IDdl.class, false, false).length > 0
                || beanFactory.getBeanNamesForType(com.baomidou.mybatisplus.autoconfigure.DdlApplicationRunner.class, false, false).length > 0)) {
            throw failure("DEFAULT_DDL_RUNNER_CONFLICT");
        }
        Set<Class<?>> persistedEnums = new HashSet<>();
        for (SqlSessionFactory factory : beanFactory.getBeanProvider(SqlSessionFactory.class).orderedStream().toList()) {
            var configuration = factory.getConfiguration();
            List<Interceptor> plugins = configuration.getInterceptors();
            if (plugins.isEmpty() || !(plugins.getLast() instanceof EgonColaOriginalSqlGuardInterceptor)
                    || plugins.stream().filter(EgonColaOriginalSqlGuardInterceptor.class::isInstance).count() != 1
                    || plugins.stream().noneMatch(plugin -> plugin == validation)) {
                throw failure("FACTORY_INTERCEPTOR_CONTRACT_INVALID");
            }
            List<MybatisPlusInterceptor> factoryOuters = plugins.stream().filter(MybatisPlusInterceptor.class::isInstance)
                    .map(MybatisPlusInterceptor.class::cast).toList();
            if (factoryOuters.size() != 1) { throw failure("FACTORY_INTERCEPTOR_CONTRACT_INVALID"); }
            validateInnerChain(factoryOuters.getFirst().getInterceptors());
            var global = GlobalConfigUtils.getGlobalConfig(configuration);
            if (global.getIdentifierGenerator() != idGenerator) { throw failure("FACTORY_IDENTIFIER_GENERATOR_INVALID"); }
            if (!handlers.contains(global.getMetaObjectHandler())) { throw failure("FACTORY_META_HANDLER_INVALID"); }
            if (configuration.getTypeHandlerRegistry().getTypeHandler(String.class).getClass() != org.apache.ibatis.type.StringTypeHandler.class) {
                throw failure("GLOBAL_STRING_TYPE_HANDLER_FORBIDDEN");
            }
            for (Class<?> mapper : configuration.getMapperRegistry().getMappers()) {
                if (!BaseMapper.class.isAssignableFrom(mapper)) { continue; }
                Class<?>[] arguments = GenericTypeUtils.resolveTypeArguments(mapper, BaseMapper.class);
                if (arguments == null || !EgonModel.class.isAssignableFrom(arguments[0])) { throw failure("EGON_MODEL_REQUIRED"); }
                Class<?> model = arguments[0];
                validateModel(model);
                TableInfo table = TableInfoHelper.getTableInfo(model);
                if (table == null || properties.getTenantId().ignores(normalizeTable(table.getTableName()))) {
                    throw failure("MODEL_TABLE_CANNOT_BE_IGNORED");
                }
                for (String method : List.of("selectActiveById", "selectActiveByIds", "deleteVersionedById")) {
                    String id = mapper.getName() + '.' + method;
                    if (!configuration.hasStatement(id, false)
                            || configuration.getMappedStatement(id, false).getResource() == null
                            || !configuration.getMappedStatement(id, false).getResource().contains(".xml")) {
                        throw failure("MAPPER_XML_CONTRACT_MISSING");
                    }
                }
                for (var field : table.getFieldList()) {
                    if (field.getPropertyType().isEnum()) {
                        validateEnumContract(field.getPropertyType(), false);
                        persistedEnums.add(field.getPropertyType());
                    }
                }
            }
        }
        validateExternalEnums(persistedEnums);
    }

    private void validateProperties() {
        boolean dev = environment.acceptsProfiles(Profiles.of("dev"));
        boolean prod = environment.acceptsProfiles(Profiles.of("prod"));
        if (dev && prod) { throw failure("ENVIRONMENT_PROFILE_CONFLICT"); }
        if (!dev && (properties.getDataChangeRecorder().isEnabled() || properties.getIllegalSql().isEnabled())) {
            throw failure("DEV_DIAGNOSTIC_PROFILE_REQUIRED");
        }
        if (!properties.getMetaFill().isEnabled() || !properties.getLocalWriteGuard().isEnabled()
                || !properties.getBlockAttack().isEnabled() || !properties.getOptimisticLocker().isEnabled()) {
            throw failure("MANDATORY_GUARD_DISABLED");
        }
        if (properties.getPagination().getMaxPageSize() > 500 || properties.getPagination().getMaxPageSize() < 1
                || properties.getPagination().isOverflow()) { throw failure("PAGINATION_POLICY_INVALID"); }
        var batch = properties.getBatch();
        if (batch.getDefaultSize() < 1 || batch.getDefaultSize() > batch.getMaxChunkSize()
                || batch.getMaxChunkSize() > 1000 || batch.getMaxChunkSize() > batch.getMaxCollectionSize()
                || batch.getMaxCollectionSize() > 10000) { throw failure("BATCH_POLICY_INVALID"); }
        for (Duration timeout : List.of(properties.getDdl().getLockTimeout(), properties.getDdl().getStatementTimeout(),
                properties.getDdl().getTopologyReadyTimeout())) {
            if (timeout.isNegative() || timeout.isZero() || timeout.compareTo(Duration.ofMillis(Integer.MAX_VALUE)) > 0) {
                throw failure("DDL_TIMEOUT_INVALID");
            }
        }
        properties.getDynamicTableName().getTables().forEach((table, actual) -> {
            if (!identifier(table) || !identifier(actual)) { throw failure("DYNAMIC_TABLE_IDENTIFIER_INVALID"); }
            if (profiles.containsKey(table) || profiles.values().stream().flatMap(profile -> profile.actualNodes().values().stream())
                    .flatMap(Collection::stream).anyMatch(target -> target.table().equals(actual))) {
                throw failure("DYNAMIC_SHARDING_TABLE_CONFLICT");
            }
        });
        properties.getLocalWriteGuard().getAllowedRootStatements().forEach((statement, column) -> {
            if (statement == null || !statement.matches("[a-zA-Z_$][a-zA-Z0-9_$]*(?:\\.[a-zA-Z_$][a-zA-Z0-9_$]*)+")
                    || !identifier(column) || Set.of("tenant_id", "deleted_at", "version", "create_time", "update_time").contains(column)) {
                throw failure("ROOT_STATEMENT_POLICY_INVALID");
            }
        });
        if (properties.getDataChangeRecorder().isEnabled()) {
            var logging = LoggingSystem.get(getClass().getClassLoader()).getLoggerConfiguration(EgonColaDataChangeRecorderInnerInterceptor.class.getName());
            if (logging == null || logging.getEffectiveLevel() != LogLevel.OFF) { throw failure("DATA_CHANGE_RAW_LOGGER_NOT_OFF"); }
        }
    }

    private void validateInnerChain(List<InnerInterceptor> chain) {
        List<Class<? extends InnerInterceptor>> order = new ArrayList<>(List.of(EgonColaTenantIdGuardInnerInterceptor.class,
                BlockAttackInnerInterceptor.class));
        if (properties.getDynamicTableName().isEnabled()) { order.add(DynamicTableNameInnerInterceptor.class); }
        order.add(TenantLineInnerInterceptor.class);
        order.add(OptimisticLockerInnerInterceptor.class);
        order.add(EgonColaLocalWriteGuardInnerInterceptor.class);
        if (properties.getDataChangeRecorder().isEnabled()) { order.add(EgonColaDataChangeRecorderInnerInterceptor.class); }
        if (properties.getIllegalSql().isEnabled()) { order.add(IllegalSQLInnerInterceptor.class); }
        if (properties.getPagination().isEnabled()) { order.add(PaginationInnerInterceptor.class); }
        int previous = -1;
        for (Class<? extends InnerInterceptor> type : order) {
            int found = -1;
            for (int index = 0; index < chain.size(); index++) {
                if (type.isInstance(chain.get(index))) {
                    if (found >= 0) { throw failure("MYBATIS_PLUS_INTERCEPTOR_ORDER_INVALID"); }
                    found = index;
                }
            }
            if (found <= previous) { throw failure("MYBATIS_PLUS_INTERCEPTOR_ORDER_INVALID"); }
            previous = found;
        }
        if (!properties.getDataChangeRecorder().isEnabled() && chain.stream().anyMatch(DataChangeRecorderInnerInterceptor.class::isInstance)
                || !properties.getIllegalSql().isEnabled() && chain.stream().anyMatch(IllegalSQLInnerInterceptor.class::isInstance)) {
            throw failure("DEV_DIAGNOSTIC_PROFILE_REQUIRED");
        }
    }

    private static void validateModel(Class<?> model) {
        try {
            Field id = EgonModel.class.getDeclaredField("id");
            Field deleted = EgonModel.class.getDeclaredField("deletedAt");
            Field version = EgonModel.class.getDeclaredField("version");
            if (id.getType() != Long.class || id.getAnnotation(TableId.class).type() != IdType.ASSIGN_ID
                    || deleted.getType() != LocalDateTime.class || !"null".equals(deleted.getAnnotation(TableLogic.class).value())
                    || version.getType() != Long.class || version.getAnnotation(Version.class) == null
                    || model.isAnnotationPresent(KeySequence.class)) { throw failure("MODEL_METADATA_INVALID"); }
            Set<String> names = Set.of("id", "tenantId", "createUserId", "createTime", "updateUserId", "updateTime", "deletedAt", "version");
            Set<String> columns = Set.of("id", "tenant_id", "create_user_id", "create_time", "update_user_id", "update_time", "deleted_at", "version");
            for (Class<?> current = model; current != EgonModel.class; current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    TableField mapping = field.getAnnotation(TableField.class);
                    if (names.contains(field.getName()) || field.isAnnotationPresent(TableId.class)
                            || field.isAnnotationPresent(TableLogic.class) || field.isAnnotationPresent(Version.class)
                            || mapping != null && mapping.exist() && columns.contains(mapping.value().replace("\"", "").toLowerCase(Locale.ROOT))) {
                        throw failure("MODEL_TECHNICAL_FIELD_SHADOWED");
                    }
                }
            }
            TableInfo table = TableInfoHelper.getTableInfo(model);
            if (table == null || !"id".equals(table.getKeyColumn()) || table.getIdType() != IdType.ASSIGN_ID
                    || !table.isWithLogicDelete() || !table.isWithVersion()) { throw failure("MODEL_METADATA_INVALID"); }
            Set<String> mapped = new HashSet<>();
            mapped.add(table.getKeyColumn());
            table.getFieldList().forEach(field -> mapped.add(field.getColumn()));
            if (!mapped.containsAll(columns)) { throw failure("MODEL_METADATA_INVALID"); }
            for (var field : table.getFieldList()) {
                if (!columns.contains(field.getColumn())) { continue; }
                FieldFill expected = Set.of("tenant_id", "update_user_id", "update_time").contains(field.getColumn())
                        ? FieldFill.INSERT_UPDATE : FieldFill.INSERT;
                if (field.getField().getDeclaringClass() != EgonModel.class || field.getFieldFill() != expected) {
                    throw failure("MODEL_METADATA_INVALID");
                }
                if (Set.of("tenant_id", "create_user_id", "create_time", "deleted_at").contains(field.getColumn())
                        && field.getUpdateStrategy() != FieldStrategy.NEVER) { throw failure("MODEL_METADATA_INVALID"); }
            }
            if (!"deleted_at".equals(table.getLogicDeleteFieldInfo().getColumn())
                    || !"null".equals(table.getLogicDeleteFieldInfo().getLogicNotDeleteValue())
                    || !"(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')".equals(table.getLogicDeleteFieldInfo().getLogicDeleteValue())
                    || !"version".equals(table.getVersionFieldInfo().getColumn())
                    || table.getVersionFieldInfo().getPropertyType() != Long.class) { throw failure("MODEL_METADATA_INVALID"); }
        } catch (NoSuchFieldException failure) {
            throw new EgonColaMybatisPlusConfigurationException("MODEL_METADATA_INVALID", failure);
        }
    }

    private static void validateEnumContract(Class<?> type, boolean external) {
        if (!type.isEnum()) { return; }
        List<Field> codes = Arrays.stream(type.getDeclaredFields()).filter(field -> field.isAnnotationPresent(EnumValue.class)).toList();
        if (codes.size() != 1) { throw failure("ENUM_VALUE_REQUIRED"); }
        Field code = codes.getFirst();
        code.setAccessible(true);
        List<AccessibleObject> json = new ArrayList<>();
        Arrays.stream(type.getDeclaredFields()).filter(field -> field.isAnnotationPresent(JsonValue.class)
                && field.getAnnotation(JsonValue.class).value()).forEach(json::add);
        Arrays.stream(type.getDeclaredMethods()).filter(method -> method.isAnnotationPresent(JsonValue.class)
                && method.getAnnotation(JsonValue.class).value()).forEach(json::add);
        if (external && json.size() != 1) { throw failure("ENUM_JSON_VALUE_REQUIRED"); }
        Set<Object> seen = new HashSet<>();
        try {
            for (Object constant : type.getEnumConstants()) {
                Object value = code.get(constant);
                if (value == null || !seen.add(value)) { throw failure("ENUM_CODE_INVALID"); }
                if (external) {
                    AccessibleObject member = json.getFirst();
                    member.setAccessible(true);
                    Object exported = member instanceof Field field ? field.get(constant) : ((Method) member).invoke(constant);
                    if (!Objects.equals(value, exported)) { throw failure("ENUM_JSON_CODE_MISMATCH"); }
                }
            }
        } catch (ReflectiveOperationException failure) {
            throw new EgonColaMybatisPlusConfigurationException("ENUM_CODE_INVALID", failure);
        }
    }

    private void validateExternalEnums(Set<Class<?>> persistedEnums) {
        if (persistedEnums.isEmpty()) { return; }
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Annotation> controller = (Class<? extends Annotation>) Class.forName("org.springframework.web.bind.annotation.RestController");
            Set<Type> visited = new HashSet<>();
            for (String name : beanFactory.getBeanDefinitionNames()) {
                Class<?> type = beanFactory.getType(name, false);
                if (type != null && AnnotatedElementUtils.hasAnnotation(type, controller)) {
                    for (Method method : type.getMethods()) {
                        visitExternal(method.getGenericReturnType(), persistedEnums, visited);
                        for (Type argument : method.getGenericParameterTypes()) { visitExternal(argument, persistedEnums, visited); }
                    }
                }
            }
        } catch (ClassNotFoundException absentWebStack) {
            // Storage-only hosts have no external Jackson/MVC DTO boundary to inspect.
        }
    }

    private static void visitExternal(Type type, Set<Class<?>> persistedEnums, Set<Type> visited) {
        if (!visited.add(type)) { return; }
        if (type instanceof ParameterizedType parameterized) {
            for (Type argument : parameterized.getActualTypeArguments()) { visitExternal(argument, persistedEnums, visited); }
            visitExternal(parameterized.getRawType(), persistedEnums, visited);
        } else if (type instanceof Class<?> candidate) {
            if (persistedEnums.contains(candidate)) { validateEnumContract(candidate, true); return; }
            if (candidate.isArray()) { visitExternal(candidate.getComponentType(), persistedEnums, visited); return; }
            if (candidate.isPrimitive() || candidate.isEnum() || candidate.getName().startsWith("java.")
                    || candidate.getName().startsWith("org.springframework.")) { return; }
            for (Field field : candidate.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) { visitExternal(field.getGenericType(), persistedEnums, visited); }
            }
        }
    }

    private static String normalizeTable(String table) {
        int dot = table.lastIndexOf('.');
        return table.substring(dot + 1).replace("\"", "").toLowerCase(Locale.ROOT);
    }

    private static boolean identifier(String value) { return value != null && value.matches("[a-z_][a-z0-9_]{0,62}"); }
    private static EgonColaMybatisPlusConfigurationException failure(String code) { return new EgonColaMybatisPlusConfigurationException(code); }
}
