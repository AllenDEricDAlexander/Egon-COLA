package top.egon.cola.component.common.mybatis.extension;

import com.baomidou.mybatisplus.core.batch.BatchMethod;
import com.baomidou.mybatisplus.core.batch.MybatisBatch;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.reflect.GenericTypeUtils;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import com.baomidou.mybatisplus.extension.kotlin.KtQueryChainWrapper;
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.handler.EgonColaMetaObjectHandler;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.io.Serializable;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Guarded MyBatis-Plus repository template. Batch calls require the command layer to own the Spring transaction.
 *
 * @param <M> concrete EgonColaMapper type
 * @param <T> concrete EgonModel type
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class EgonColaRepository<M extends EgonColaMapper<T>, T extends EgonModel<T>> extends CrudRepository<M, T> implements EgonColaIRepository<T> {

    protected abstract EgonColaModelValidationUtils getModelValidationUtils();

    protected abstract EgonColaTenantIdProvider getTenantIdProvider();

    protected abstract EgonColaMybatisPlusProperties getProperties();

    @Override
    public final boolean save(T entity) {
        requireTenantId();
        validateBusiness(entity, EgonColaModelValidationGroups.Operation.INSERT);
        boolean written = SqlHelper.retBool(getBaseMapper().insert(entity));
        return written;
    }

    @Override
    public final boolean saveBatch(Collection<T> entityList, int batchSize) {
        Long snapshot = prepareBatch(entityList, batchSize, BatchOperation.INSERT);
        if (entityList.isEmpty()) {
            return false;
        }
        verifyTenantSnapshot(snapshot);
        executeMybatisBatch(entityList, batchSize, new MybatisBatch.Method<T>(getMapperClass()).insert());
        verifyTenantSnapshot(snapshot);
        return true;
    }

    @Override
    public final boolean saveOrUpdateBatch(Collection<T> entityList, int batchSize) {
        Long tenant = prepareBatch(entityList, batchSize, BatchOperation.UPSERT);
        if (entityList.isEmpty()) {
            return false;
        }
        ConnectionHolder transaction = requireTransaction(getSqlSessionFactory());
        try {
            List<Long> ids = entityList.stream().map(EgonModel::getId).filter(Objects::nonNull).toList();
            Set<Long> existing = listByIds(ids).stream().map(EgonModel::getId).collect(Collectors.toSet());
            List<T> inserts = new ArrayList<>();
            List<T> updates = new ArrayList<>();
            for (T entity : entityList) {
                if (existing.contains(entity.getId())) {
                    validateBusiness(entity, EgonColaModelValidationGroups.Operation.UPDATE);
                    updates.add(entity);
                } else {
                    validateBusiness(entity, EgonColaModelValidationGroups.Operation.INSERT);
                    inserts.add(entity);
                }
            }
            saveBatch(inserts, batchSize);
            updateBatchById(updates, batchSize);
            verifyTenantSnapshot(tenant);
            return true;
        } catch (RuntimeException | Error failure) {
            transaction.setRollbackOnly();
            throw failure;
        }
    }

    @Override
    public final boolean removeById(Serializable id) {
        T snapshot = getById(requireSerializableId(id));
        return snapshot != null && removeById(snapshot);
    }

    @Override
    public final boolean removeById(Serializable id, boolean useFill) {
        requireDeleteFill(useFill);
        return removeById(id);
    }

    @Override
    public final boolean removeById(T entity) {
        requireTenantId();
        validateBusiness(entity, EgonColaModelValidationGroups.Operation.DELETE);
        requireEntityId(entity);
        boolean written = SqlHelper.retBool(getBaseMapper().deleteVersionedById(entity));
        return written;
    }

    @Override
    public final boolean removeByMap(Map<String, Object> columnMap) {
        throw new UnsupportedOperationException("UNSCOPED_WRITE_FORBIDDEN");
    }

    @Override
    public final boolean remove(Wrapper<T> queryWrapper) {
        throw new UnsupportedOperationException("UNSCOPED_WRITE_FORBIDDEN");
    }

    @Override
    public final boolean removeByIds(Collection<?> list) {
        requireTenantId();
        requireCollection(list);
        if (list.isEmpty()) {
            return false;
        }
        Map<Long, T> supplied = new LinkedHashMap<>();
        for (Object item : list) {
            if (item instanceof EgonModel<?> model) {
                T entity = getEntityClass().cast(model);
                validateBusiness(entity, EgonColaModelValidationGroups.Operation.DELETE);
                supplied.put((Long) requireSerializableId(entity.getId()), entity);
            } else if (item instanceof Serializable id) {
                supplied.putIfAbsent((Long) requireSerializableId(id), null);
            } else {
                throw new IllegalArgumentException("ID_MUST_BE_POSITIVE_LONG");
            }
        }
        ConnectionHolder transaction = requireTransaction(getSqlSessionFactory());
        try {
            List<T> selected = listByIds(supplied.keySet());
            if (selected.isEmpty()) {
                return false;
            }
            List<T> entities = selected.stream().map(row -> supplied.get(row.getId()) == null ? row : supplied.get(row.getId())).toList();
            BatchMethod<T> method = new BatchMethod<>(getMapperClass().getName() + ".deleteVersionedById", entity -> {
                Map<String, Object> parameter = new HashMap<>();
                parameter.put(Constants.ENTITY, entity);
                return parameter;
            });
            executeMybatisBatch(entities, requireProperties().getBatch().getDefaultSize(), method);
            return true;
        } catch (RuntimeException | Error failure) {
            transaction.setRollbackOnly();
            throw failure;
        }
    }

    @Override
    public final boolean removeByIds(Collection<?> list, boolean useFill) {
        requireDeleteFill(useFill);
        return removeByIds(list);
    }

    @Override
    public final boolean updateById(T entity) {
        requireTenantId();
        validateBusiness(entity, EgonColaModelValidationGroups.Operation.UPDATE);
        requireEntityId(entity);
        boolean written = SqlHelper.retBool(getBaseMapper().updateById(entity));
        return written;
    }

    @Override
    public final boolean update(Wrapper<T> updateWrapper) {
        throw new UnsupportedOperationException("UNSCOPED_WRITE_FORBIDDEN");
    }

    @Override
    public final boolean update(T entity, Wrapper<T> updateWrapper) {
        requireTenantId();
        Wrapper<T> checkedWrapper = requireWriteWrapper(updateWrapper);
        validateBusiness(entity, EgonColaModelValidationGroups.Operation.UPDATE);
        requireEntityId(entity);
        boolean written = SqlHelper.retBool(getBaseMapper().update(entity, checkedWrapper));
        return written;
    }

    @Override
    public final boolean updateBatchById(Collection<T> entityList, int batchSize) {
        Long snapshot = prepareBatch(entityList, batchSize, BatchOperation.UPDATE);
        if (entityList.isEmpty()) {
            return false;
        }
        verifyTenantSnapshot(snapshot);
        executeMybatisBatch(entityList, batchSize, new MybatisBatch.Method<T>(getMapperClass()).updateById());
        verifyTenantSnapshot(snapshot);
        return true;
    }

    @Override
    public final boolean saveOrUpdate(T entity) {
        requireTenantId();
        validateBusiness(entity, EgonColaModelValidationGroups.Operation.INSERT);
        return entity.getId() == null || getById(entity.getId()) == null ? save(entity) : updateById(entity);
    }

    @Override
    public final T getById(Serializable id) {
        requireTenantId();
        return validateLoaded(getBaseMapper().selectActiveById(requireSerializableId(id)));
    }

    @Override
    public final Optional<T> getOptById(Serializable id) {
        return Optional.ofNullable(getById(id));
    }

    @Override
    public final List<T> listByIds(Collection<? extends Serializable> idList) {
        requireTenantId();
        requireCollection(idList);
        if (idList.isEmpty()) {
            return List.of();
        }
        List<Serializable> ids = idList.stream().map(EgonColaRepository::requireSerializableId).distinct().toList();
        return validateLoadedList(getBaseMapper().selectActiveByIds(ids));
    }

    @Override
    public final List<T> listByMap(Map<String, Object> columnMap) {
        requireTenantId();
        return validateLoadedList(getBaseMapper().selectByMap(columnMap == null ? Collections.emptyMap() : columnMap));
    }

    @Override
    public final T getOne(Wrapper<T> queryWrapper) {
        return getOne(queryWrapper, true);
    }

    @Override
    public final Optional<T> getOneOpt(Wrapper<T> queryWrapper) {
        return getOneOpt(queryWrapper, true);
    }

    @Override
    public final T getOne(Wrapper<T> queryWrapper, boolean throwEx) {
        requireTenantId();
        return validateLoaded(super.getOne(normalizeQueryWrapper(queryWrapper), throwEx));
    }

    @Override
    public final Optional<T> getOneOpt(Wrapper<T> queryWrapper, boolean throwEx) {
        requireTenantId();
        return Optional.ofNullable(getOne(queryWrapper, throwEx));
    }

    @Override
    public final Map<String, Object> getMap(Wrapper<T> queryWrapper) {
        requireTenantId();
        return super.getMap(normalizeQueryWrapper(queryWrapper));
    }

    @Override
    public final <V> V getObj(Wrapper<T> queryWrapper, Function<? super Object, V> mapper) {
        requireTenantId();
        return super.getObj(normalizeQueryWrapper(queryWrapper), Objects.requireNonNull(mapper, "mapper must not be null"));
    }

    @Override
    public final boolean exists(Wrapper<T> queryWrapper) {
        requireTenantId();
        return getBaseMapper().exists(normalizeQueryWrapper(queryWrapper));
    }

    @Override
    public final long count() {
        return count(Wrappers.emptyWrapper());
    }

    @Override
    public final long count(Wrapper<T> queryWrapper) {
        requireTenantId();
        return SqlHelper.retCount(getBaseMapper().selectCount(normalizeQueryWrapper(queryWrapper)));
    }

    @Override
    public final List<T> list(Wrapper<T> queryWrapper) {
        requireTenantId();
        return validateLoadedList(getBaseMapper().selectList(normalizeQueryWrapper(queryWrapper)));
    }

    @Override
    public final List<T> list(IPage<T> page, Wrapper<T> queryWrapper) {
        requireTenantId();
        return validateLoadedList(getBaseMapper().selectList(requirePage(page), normalizeQueryWrapper(queryWrapper)));
    }

    @Override
    public final List<T> list() {
        return list(Wrappers.emptyWrapper());
    }

    @Override
    public final List<T> list(IPage<T> page) {
        return list(page, Wrappers.emptyWrapper());
    }

    @Override
    public final <E extends IPage<T>> E page(E page, Wrapper<T> queryWrapper) {
        requireTenantId();
        E result = getBaseMapper().selectPage(requirePage(page), normalizeQueryWrapper(queryWrapper));
        validateLoadedList(result.getRecords());
        return result;
    }

    @Override
    public final <E extends IPage<T>> E page(E page) {
        return page(page, Wrappers.emptyWrapper());
    }

    @Override
    public final List<Map<String, Object>> listMaps(Wrapper<T> queryWrapper) {
        requireTenantId();
        return getBaseMapper().selectMaps(normalizeQueryWrapper(queryWrapper));
    }

    @Override
    public final List<Map<String, Object>> listMaps(IPage<? extends Map<String, Object>> page, Wrapper<T> queryWrapper) {
        requireTenantId();
        return getBaseMapper().selectMaps(requireMapPage(page), normalizeQueryWrapper(queryWrapper));
    }

    @Override
    public final List<Map<String, Object>> listMaps() {
        return listMaps(Wrappers.emptyWrapper());
    }

    @Override
    public final List<Map<String, Object>> listMaps(IPage<? extends Map<String, Object>> page) {
        return listMaps(page, Wrappers.emptyWrapper());
    }

    @Override
    public final <E> List<E> listObjs() {
        requireTenantId();
        return getBaseMapper().selectObjs(Wrappers.emptyWrapper());
    }

    @Override
    public final <V> List<V> listObjs(Function<? super Object, V> mapper) {
        return listObjs(Wrappers.emptyWrapper(), mapper);
    }

    @Override
    public final <E> List<E> listObjs(Wrapper<T> queryWrapper) {
        requireTenantId();
        return getBaseMapper().selectObjs(normalizeQueryWrapper(queryWrapper));
    }

    @Override
    public final <V> List<V> listObjs(Wrapper<T> queryWrapper, Function<? super Object, V> mapper) {
        requireTenantId();
        Objects.requireNonNull(mapper, "mapper must not be null");
        return getBaseMapper().selectObjs(normalizeQueryWrapper(queryWrapper)).stream().filter(Objects::nonNull).map(mapper).collect(Collectors.toList());
    }

    @Override
    public final <E extends IPage<Map<String, Object>>> E pageMaps(E page, Wrapper<T> queryWrapper) {
        requireTenantId();
        return getBaseMapper().selectMapsPage(requireMapPage(page), normalizeQueryWrapper(queryWrapper));
    }

    @Override
    public final <E extends IPage<Map<String, Object>>> E pageMaps(E page) {
        return pageMaps(page, Wrappers.emptyWrapper());
    }

    @Override
    public abstract M getBaseMapper();

    @Override
    public final Class<T> getEntityClass() {
        @SuppressWarnings("unchecked") Class<T> type = (Class<T>) GenericTypeUtils.resolveTypeArguments(getClass(), EgonColaRepository.class)[1];
        return type;
    }

    @Override
    public final QueryChainWrapper<T> query() {
        throw new UnsupportedOperationException("QUERY_CHAIN_FORBIDDEN");
    }

    @Override
    public final LambdaQueryChainWrapper<T> lambdaQuery() {
        throw new UnsupportedOperationException("QUERY_CHAIN_FORBIDDEN");
    }

    @Override
    public final LambdaQueryChainWrapper<T> lambdaQuery(T entity) {
        throw new UnsupportedOperationException("QUERY_CHAIN_FORBIDDEN");
    }

    @Override
    public final KtQueryChainWrapper<T> ktQuery() {
        throw new UnsupportedOperationException("QUERY_CHAIN_FORBIDDEN");
    }

    @Override
    public final KtUpdateChainWrapper<T> ktUpdate() {
        throw new UnsupportedOperationException("UNSCOPED_WRITE_FORBIDDEN");
    }

    @Override
    public final UpdateChainWrapper<T> update() {
        throw new UnsupportedOperationException("UNSCOPED_WRITE_FORBIDDEN");
    }

    @Override
    public final LambdaUpdateChainWrapper<T> lambdaUpdate() {
        throw new UnsupportedOperationException("UNSCOPED_WRITE_FORBIDDEN");
    }

    @Override
    public final boolean saveBatch(Collection<T> entityList) {
        return saveBatch(entityList, requireProperties().getBatch().getDefaultSize());
    }

    @Override
    public final boolean saveOrUpdateBatch(Collection<T> entityList) {
        return saveOrUpdateBatch(entityList, requireProperties().getBatch().getDefaultSize());
    }

    @Override
    public final boolean removeBatchByIds(Collection<?> list) {
        return removeByIds(list);
    }

    @Override
    public final boolean updateBatchById(Collection<T> entityList) {
        return updateBatchById(entityList, requireProperties().getBatch().getDefaultSize());
    }

    private Long prepareBatch(Collection<T> entities, int batchSize, BatchOperation operation) {
        Long snapshot = requireTenantId();
        checkedBatchSize(batchSize);
        requireCollection(entities);
        Set<Long> ids = new HashSet<>();
        for (T entity : entities) {
            if (entity.getId() != null && !ids.add(entity.getId())) {
                throw new IllegalArgumentException("BATCH_DUPLICATE_ID");
            }
            validateBusiness(entity, operation == BatchOperation.UPDATE ? EgonColaModelValidationGroups.Operation.UPDATE : EgonColaModelValidationGroups.Operation.INSERT);
        }
        return snapshot;
    }

    protected final List<BatchResult> executeMybatisBatch(Collection<T> entities, int batchSize, BatchMethod<T> method) {
        requireCollection(entities);
        checkedBatchSize(batchSize);
        Objects.requireNonNull(method, "method");
        if (entities.isEmpty()) {
            return List.of();
        }
        SqlSessionFactory factory = getSqlSessionFactory();
        ConnectionHolder transaction = requireTransaction(factory);
        try {
            if (!method.getStatementId().startsWith(getMapperClass().getName() + ".")) {
                throw new IllegalArgumentException("BATCH_MAPPER_MISMATCH");
            }
            SqlCommandType command = factory.getConfiguration().getMappedStatement(method.getStatementId()).getSqlCommandType();
            if (command != SqlCommandType.INSERT && command != SqlCommandType.UPDATE && command != SqlCommandType.DELETE) {
                throw new IllegalArgumentException("BATCH_DML_REQUIRED");
            }
            Long tenant = requireTenantId();
            var handler = GlobalConfigUtils.getGlobalConfig(factory.getConfiguration()).getMetaObjectHandler();
            if (!(handler instanceof EgonColaMetaObjectHandler fill)) {
                throw new IllegalStateException("META_OBJECT_HANDLER_CONTRACT_INVALID");
            }
            String user = requireUserSnapshot(fill);
            Set<Long> ids = new HashSet<>();
            for (T entity : entities) {
                if (entity.getId() != null && !ids.add(entity.getId())) {
                    throw new IllegalArgumentException("BATCH_DUPLICATE_ID");
                }
                validateBusiness(entity, command == SqlCommandType.INSERT ? EgonColaModelValidationGroups.Operation.INSERT : method.getStatementId().endsWith(".deleteVersionedById") || command == SqlCommandType.DELETE ? EgonColaModelValidationGroups.Operation.DELETE : EgonColaModelValidationGroups.Operation.UPDATE);
            }
            List<BatchResult> results = new MybatisBatch<T>(factory, entities, batchSize).execute(method);
            int processed = 0;
            for (BatchResult result : results) {
                int[] counts = result.getUpdateCounts();
                if (counts == null || counts.length != result.getParameterObjects().size()) {
                    throw new IllegalStateException("BATCH_UPDATE_COUNT_UNKNOWN");
                }
                for (int count : counts) {
                    if (count != 1 && !(command == SqlCommandType.INSERT && count == Statement.SUCCESS_NO_INFO)) {
                        throw new IllegalStateException("BATCH_VERSION_CONFLICT_OR_WRITE_FAILED");
                    }
                    processed++;
                }
            }
            if (processed != entities.size()) {
                throw new IllegalStateException("BATCH_UPDATE_COUNT_MISMATCH");
            }
            verifyTenantSnapshot(tenant);
            if (!user.equals(requireUserSnapshot(fill))) {
                throw new IllegalStateException("USER_CONTEXT_MISMATCH");
            }
            for (T entity : entities) {
                if (!tenant.equals(entity.getTenantId()) || !user.equals(entity.getUpdateUserId())) {
                    throw new IllegalStateException("TENANT_OR_USER_CONTEXT_MISMATCH");
                }
            }
            return List.copyOf(results);
        } catch (RuntimeException | Error failure) {
            transaction.setRollbackOnly();
            throw failure;
        }
    }

    private static ConnectionHolder requireTransaction(SqlSessionFactory factory) {
        var environment = factory.getConfiguration().getEnvironment();
        Object resource = TransactionSynchronizationManager.getResource(environment.getDataSource());
        if (!(environment.getTransactionFactory() instanceof SpringManagedTransactionFactory) || !TransactionSynchronizationManager.isActualTransactionActive() || !TransactionSynchronizationManager.isSynchronizationActive() || !(resource instanceof ConnectionHolder holder)) {
            throw new IllegalStateException("BATCH_TRANSACTION_REQUIRED");
        }
        return holder;
    }

    private static String requireUserSnapshot(EgonColaMetaObjectHandler handler) {
        String user = handler.getUserIdProvider().currentUserId();
        if (user == null || user.isBlank()) {
            throw new IllegalStateException("USER_CONTEXT_MISSING");
        }
        return user;
    }

    private void requireCollection(Collection<?> values) {
        if (values == null) {
            throw new IllegalArgumentException("collection must not be null");
        }
        if (values.size() > requireProperties().getBatch().getMaxCollectionSize()) {
            throw new IllegalArgumentException("BATCH_COLLECTION_SIZE_INVALID");
        }
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("collection must not contain null");
        }
    }

    private T validateLoaded(T entity) {
        return entity == null ? null : requireModelValidationUtils().validate(entity, EgonColaModelValidationGroups.Operation.LOADED);
    }

    private List<T> validateLoadedList(List<T> entities) {
        entities.forEach(this::validateLoaded);
        return entities;
    }

    private static void requireDeleteFill(boolean useFill) {
        if (!useFill) {
            throw new IllegalArgumentException("DELETE_AUDIT_FILL_REQUIRED");
        }
    }

    private enum BatchOperation {
        INSERT, UPDATE, UPSERT
    }

    private int checkedBatchSize(int batchSize) {
        if (batchSize <= 0 || batchSize > requireProperties().getBatch().getMaxChunkSize()) {
            throw new IllegalArgumentException("BATCH_SIZE_INVALID");
        }
        return batchSize;
    }

    private Long requireTenantId() {
        Long tenantId = requireTenantIdProvider().currentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TENANT_CONTEXT_MISSING");
        }
        return tenantId;
    }

    private void verifyTenantSnapshot(Long snapshot) {
        Long current = requireTenantId();
        if (!snapshot.equals(current)) {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                Object resource = TransactionSynchronizationManager.getResource(getSqlSessionFactory().getConfiguration().getEnvironment().getDataSource());
                if (resource instanceof ConnectionHolder holder) {
                    holder.setRollbackOnly();
                }
            }
            throw new IllegalStateException("TENANT_CONTEXT_MISMATCH");
        }
    }

    private void validateBusiness(T entity, EgonColaModelValidationGroups.Operation operation) {
        if (entity == null) {
            throw new IllegalArgumentException("entity must not be null");
        }
        requireModelValidationUtils().validateBusiness(entity, operation);
    }

    private static Serializable requireSerializableId(Serializable id) {
        if (!(id instanceof Long value) || value <= 0) {
            throw new IllegalArgumentException("ID_MUST_BE_POSITIVE_LONG");
        }
        return id;
    }

    private static void requireEntityId(EgonModel<?> entity) {
        if (entity.getId() == null) {
            throw new IllegalArgumentException("id must not be null");
        }
    }

    private static <T> Wrapper<T> normalizeQueryWrapper(Wrapper<T> wrapper) {
        return wrapper == null ? Wrappers.emptyWrapper() : wrapper;
    }

    private static <T> Wrapper<T> requireWriteWrapper(Wrapper<T> wrapper) {
        if (wrapper == null || wrapper.isEmptyOfWhere()) {
            throw new IllegalArgumentException("BUSINESS_PREDICATE_REQUIRED");
        }
        return wrapper;
    }

    private <E extends IPage<T>> E requirePage(E page) {
        Objects.requireNonNull(page, "page must not be null");
        if (page.getSize() <= 0 || page.getSize() > requireProperties().getPagination().getMaxPageSize()) {
            throw new IllegalArgumentException("PAGE_SIZE_INVALID");
        }
        return page;
    }

    private <E extends IPage<? extends Map<String, Object>>> E requireMapPage(E page) {
        Objects.requireNonNull(page, "page must not be null");
        if (page.getSize() <= 0 || page.getSize() > requireProperties().getPagination().getMaxPageSize()) {
            throw new IllegalArgumentException("PAGE_SIZE_INVALID");
        }
        return page;
    }

    private EgonColaModelValidationUtils requireModelValidationUtils() {
        return Objects.requireNonNull(getModelValidationUtils(), "modelValidationUtils must not be null");
    }

    private EgonColaTenantIdProvider requireTenantIdProvider() {
        return Objects.requireNonNull(getTenantIdProvider(), "tenantIdProvider must not be null");
    }

    private EgonColaMybatisPlusProperties requireProperties() {
        return Objects.requireNonNull(getProperties(), "properties must not be null");
    }
}
