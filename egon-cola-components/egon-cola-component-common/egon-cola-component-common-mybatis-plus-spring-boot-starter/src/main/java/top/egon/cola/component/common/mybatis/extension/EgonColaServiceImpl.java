package top.egon.cola.component.common.mybatis.extension;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import com.baomidou.mybatisplus.extension.kotlin.KtQueryChainWrapper;
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.ChainWrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationUtils;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Explicitly enhanced MyBatis-Plus 3.5.16 Service implementation.
 *
 * @param <M> concrete EgonColaMapper type
 * @param <T> concrete EgonModel type
 */
public class EgonColaServiceImpl<M extends EgonColaMapper<T>, T extends EgonModel<T>>
        extends ServiceImpl<M, T> implements EgonColaIService<T> {

    private final EgonColaModelValidationUtils modelValidationUtils;
    private final EgonColaTenantIdProvider tenantIdProvider;
    private final EgonColaMybatisPlusProperties properties;

    protected EgonColaServiceImpl(EgonColaModelValidationUtils modelValidationUtils,
                                   EgonColaTenantIdProvider tenantIdProvider,
                                   EgonColaMybatisPlusProperties properties) {
        this.modelValidationUtils = Objects.requireNonNull(modelValidationUtils,
                "modelValidationUtils must not be null");
        this.tenantIdProvider = Objects.requireNonNull(tenantIdProvider,
                "tenantIdProvider must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public boolean save(T entity) {
        requireTenantId();
        validateBusiness(entity, EgonColaModelValidationGroups.Operation.INSERT);
        return SqlHelper.retBool(baseMapper.insert(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<T> entityList, int batchSize) {
        Long snapshot = prepareBatch(entityList, batchSize, BatchOperation.INSERT);
        if (entityList.isEmpty()) {
            return false;
        }
        boolean result = super.saveBatch(entityList, checkedBatchSize(batchSize));
        verifyTenantSnapshot(snapshot);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateBatch(Collection<T> entityList, int batchSize) {
        Long snapshot = prepareBatch(entityList, batchSize, BatchOperation.UPSERT);
        if (entityList.isEmpty()) {
            return false;
        }
        boolean result = super.saveOrUpdateBatch(entityList, checkedBatchSize(batchSize));
        verifyTenantSnapshot(snapshot);
        return result;
    }

    @Override
    public boolean removeById(Serializable id) {
        requireTenantId();
        return SqlHelper.retBool(baseMapper.deleteById(requireSerializableId(id)));
    }

    @Override
    public boolean removeById(Serializable id, boolean useFill) {
        requireTenantId();
        return SqlHelper.retBool(baseMapper.deleteById(requireSerializableId(id), useFill));
    }

    @Override
    public boolean removeById(T entity) {
        requireTenantId();
        validateBusiness(entity, EgonColaModelValidationGroups.Operation.DELETE);
        requireEntityId(entity);
        return SqlHelper.retBool(baseMapper.deleteById(entity));
    }

    @Override
    public boolean removeByMap(Map<String, Object> columnMap) {
        requireTenantId();
        requireNonEmptyMap(columnMap, "columnMap");
        return SqlHelper.retBool(baseMapper.deleteByMap(columnMap));
    }

    @Override
    public boolean remove(Wrapper<T> queryWrapper) {
        requireTenantId();
        return SqlHelper.retBool(baseMapper.delete(requireWriteWrapper(queryWrapper)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(Collection<?> list) {
        requireTenantId();
        if (list == null || list.isEmpty()) {
            return false;
        }
        return SqlHelper.retBool(baseMapper.deleteByIds(list));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(Collection<?> list, boolean useFill) {
        requireTenantId();
        if (list == null || list.isEmpty()) {
            return false;
        }
        return SqlHelper.retBool(baseMapper.deleteByIds(list, useFill));
    }

    @Override
    public boolean updateById(T entity) {
        requireTenantId();
        validateBusiness(entity, EgonColaModelValidationGroups.Operation.UPDATE);
        requireEntityId(entity);
        return SqlHelper.retBool(baseMapper.updateById(entity));
    }

    @Override
    public boolean update(Wrapper<T> updateWrapper) {
        return update(null, updateWrapper);
    }

    @Override
    public boolean update(T entity, Wrapper<T> updateWrapper) {
        requireTenantId();
        Wrapper<T> checkedWrapper = requireWriteWrapper(updateWrapper);
        if (entity != null) {
            validateBusiness(entity, EgonColaModelValidationGroups.Operation.UPDATE);
        }
        return SqlHelper.retBool(baseMapper.update(entity, checkedWrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchById(Collection<T> entityList, int batchSize) {
        Long snapshot = prepareBatch(entityList, batchSize, BatchOperation.UPDATE);
        if (entityList.isEmpty()) {
            return false;
        }
        entityList.forEach(EgonColaServiceImpl::requireEntityId);
        boolean result = super.updateBatchById(entityList, checkedBatchSize(batchSize));
        verifyTenantSnapshot(snapshot);
        return result;
    }

    @Override
    public boolean saveOrUpdate(T entity) {
        requireTenantId();
        validateBusiness(entity, entity == null || entity.getId() == null
                ? EgonColaModelValidationGroups.Operation.INSERT
                : EgonColaModelValidationGroups.Operation.UPDATE);
        return super.saveOrUpdate(entity);
    }

    @Override
    public T getById(Serializable id) {
        requireTenantId();
        return baseMapper.selectById(requireSerializableId(id));
    }

    @Override
    public Optional<T> getOptById(Serializable id) {
        return Optional.ofNullable(getById(id));
    }

    @Override
    public List<T> listByIds(Collection<? extends Serializable> idList) {
        requireTenantId();
        if (idList == null || idList.isEmpty()) {
            return Collections.emptyList();
        }
        return baseMapper.selectByIds(idList);
    }

    @Override
    public List<T> listByMap(Map<String, Object> columnMap) {
        requireTenantId();
        return baseMapper.selectByMap(columnMap == null ? Collections.emptyMap() : columnMap);
    }

    @Override
    public T getOne(Wrapper<T> queryWrapper) {
        return getOne(queryWrapper, true);
    }

    @Override
    public Optional<T> getOneOpt(Wrapper<T> queryWrapper) {
        return getOneOpt(queryWrapper, true);
    }

    @Override
    public T getOne(Wrapper<T> queryWrapper, boolean throwEx) {
        requireTenantId();
        return super.getOne(normalizeQueryWrapper(queryWrapper), throwEx);
    }

    @Override
    public Optional<T> getOneOpt(Wrapper<T> queryWrapper, boolean throwEx) {
        requireTenantId();
        return super.getOneOpt(normalizeQueryWrapper(queryWrapper), throwEx);
    }

    @Override
    public Map<String, Object> getMap(Wrapper<T> queryWrapper) {
        requireTenantId();
        return super.getMap(normalizeQueryWrapper(queryWrapper));
    }

    @Override
    public <V> V getObj(Wrapper<T> queryWrapper, Function<? super Object, V> mapper) {
        requireTenantId();
        return super.getObj(normalizeQueryWrapper(queryWrapper), Objects.requireNonNull(mapper, "mapper must not be null"));
    }

    @Override
    public boolean exists(Wrapper<T> queryWrapper) {
        requireTenantId();
        return baseMapper.exists(normalizeQueryWrapper(queryWrapper));
    }

    @Override
    public long count() {
        return count(Wrappers.emptyWrapper());
    }

    @Override
    public long count(Wrapper<T> queryWrapper) {
        requireTenantId();
        return SqlHelper.retCount(baseMapper.selectCount(normalizeQueryWrapper(queryWrapper)));
    }

    @Override
    public List<T> list(Wrapper<T> queryWrapper) {
        requireTenantId();
        return baseMapper.selectList(normalizeQueryWrapper(queryWrapper));
    }

    @Override
    public List<T> list(IPage<T> page, Wrapper<T> queryWrapper) {
        requireTenantId();
        return baseMapper.selectList(requirePage(page), normalizeQueryWrapper(queryWrapper));
    }

    @Override
    public List<T> list() {
        return list(Wrappers.emptyWrapper());
    }

    @Override
    public List<T> list(IPage<T> page) {
        return list(page, Wrappers.emptyWrapper());
    }

    @Override
    public <E extends IPage<T>> E page(E page, Wrapper<T> queryWrapper) {
        requireTenantId();
        return baseMapper.selectPage(requirePage(page), normalizeQueryWrapper(queryWrapper));
    }

    @Override
    public <E extends IPage<T>> E page(E page) {
        return page(page, Wrappers.emptyWrapper());
    }

    @Override
    public List<Map<String, Object>> listMaps(Wrapper<T> queryWrapper) {
        requireTenantId();
        return baseMapper.selectMaps(normalizeQueryWrapper(queryWrapper));
    }

    @Override
    public List<Map<String, Object>> listMaps(IPage<? extends Map<String, Object>> page,
                                              Wrapper<T> queryWrapper) {
        requireTenantId();
        return baseMapper.selectMaps(requireMapPage(page), normalizeQueryWrapper(queryWrapper));
    }

    @Override
    public List<Map<String, Object>> listMaps() {
        return listMaps(Wrappers.emptyWrapper());
    }

    @Override
    public List<Map<String, Object>> listMaps(IPage<? extends Map<String, Object>> page) {
        return listMaps(page, Wrappers.emptyWrapper());
    }

    @Override
    public <E> List<E> listObjs() {
        requireTenantId();
        return baseMapper.selectObjs(Wrappers.emptyWrapper());
    }

    @Override
    public <V> List<V> listObjs(Function<? super Object, V> mapper) {
        return listObjs(Wrappers.emptyWrapper(), mapper);
    }

    @Override
    public <E> List<E> listObjs(Wrapper<T> queryWrapper) {
        requireTenantId();
        return baseMapper.selectObjs(normalizeQueryWrapper(queryWrapper));
    }

    @Override
    public <V> List<V> listObjs(Wrapper<T> queryWrapper, Function<? super Object, V> mapper) {
        requireTenantId();
        Objects.requireNonNull(mapper, "mapper must not be null");
        return baseMapper.selectObjs(normalizeQueryWrapper(queryWrapper)).stream()
                .filter(Objects::nonNull)
                .map(mapper)
                .collect(Collectors.toList());
    }

    @Override
    public <E extends IPage<Map<String, Object>>> E pageMaps(E page, Wrapper<T> queryWrapper) {
        requireTenantId();
        return baseMapper.selectMapsPage(requireMapPage(page), normalizeQueryWrapper(queryWrapper));
    }

    @Override
    public <E extends IPage<Map<String, Object>>> E pageMaps(E page) {
        return pageMaps(page, Wrappers.emptyWrapper());
    }

    @Override
    public M getBaseMapper() {
        return super.getBaseMapper();
    }

    @Override
    public Class<T> getEntityClass() {
        return super.getEntityClass();
    }

    @Override
    public QueryChainWrapper<T> query() {
        requireTenantId();
        return ChainWrappers.queryChain(baseMapper);
    }

    @Override
    public LambdaQueryChainWrapper<T> lambdaQuery() {
        requireTenantId();
        return ChainWrappers.lambdaQueryChain(baseMapper, getEntityClass());
    }

    @Override
    public LambdaQueryChainWrapper<T> lambdaQuery(T entity) {
        requireTenantId();
        return ChainWrappers.lambdaQueryChain(baseMapper, entity);
    }

    @Override
    public KtQueryChainWrapper<T> ktQuery() {
        requireTenantId();
        return ChainWrappers.ktQueryChain(baseMapper, getEntityClass());
    }

    @Override
    public KtUpdateChainWrapper<T> ktUpdate() {
        requireTenantId();
        return ChainWrappers.ktUpdateChain(baseMapper, getEntityClass());
    }

    @Override
    public UpdateChainWrapper<T> update() {
        requireTenantId();
        return ChainWrappers.updateChain(baseMapper);
    }

    @Override
    public LambdaUpdateChainWrapper<T> lambdaUpdate() {
        requireTenantId();
        return ChainWrappers.lambdaUpdateChain(baseMapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<T> entityList) {
        return saveBatch(entityList, properties.getBatch().getDefaultSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateBatch(Collection<T> entityList) {
        return saveOrUpdateBatch(entityList, properties.getBatch().getDefaultSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeBatchByIds(Collection<?> list) {
        return removeByIds(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchById(Collection<T> entityList) {
        return updateBatchById(entityList, properties.getBatch().getDefaultSize());
    }

    private Long prepareBatch(Collection<T> entityList, int batchSize, BatchOperation operation) {
        Long snapshot = requireTenantId();
        Objects.requireNonNull(entityList, "entityList must not be null");
        checkedBatchSize(batchSize);
        if (entityList.isEmpty()) {
            return snapshot;
        }
        for (T entity : entityList) {
            if (entity == null) {
                throw new IllegalArgumentException("entityList must not contain null");
            }
            EgonColaModelValidationGroups.Operation validationOperation = switch (operation) {
                case INSERT -> EgonColaModelValidationGroups.Operation.INSERT;
                case UPDATE -> EgonColaModelValidationGroups.Operation.UPDATE;
                case UPSERT -> entity.getId() == null
                        ? EgonColaModelValidationGroups.Operation.INSERT
                        : EgonColaModelValidationGroups.Operation.UPDATE;
            };
            validateBusiness(entity, validationOperation);
        }
        if (entityList.size() > properties.getBatch().getMaxCollectionSize()) {
            throw new IllegalArgumentException("BATCH_COLLECTION_SIZE_INVALID");
        }
        return snapshot;
    }

    private enum BatchOperation {
        INSERT,
        UPDATE,
        UPSERT
    }

    private int checkedBatchSize(int batchSize) {
        if (batchSize <= 0 || batchSize > properties.getBatch().getMaxChunkSize()) {
            throw new IllegalArgumentException("BATCH_SIZE_INVALID");
        }
        return batchSize;
    }

    private Long requireTenantId() {
        Long tenantId = tenantIdProvider.currentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TENANT_CONTEXT_MISSING");
        }
        return tenantId;
    }

    private void verifyTenantSnapshot(Long snapshot) {
        Long current = requireTenantId();
        if (!snapshot.equals(current)) {
            throw new IllegalStateException("TENANT_CONTEXT_MISMATCH");
        }
    }

    private void validateBusiness(T entity, EgonColaModelValidationGroups.Operation operation) {
        if (entity == null) {
            throw new IllegalArgumentException("entity must not be null");
        }
        modelValidationUtils.validateBusiness(entity, operation);
    }

    private static Serializable requireSerializableId(Serializable id) {
        return Objects.requireNonNull(id, "id must not be null");
    }

    private static void requireEntityId(EgonModel<?> entity) {
        if (entity.getId() == null) {
            throw new IllegalArgumentException("id must not be null");
        }
    }

    private static void requireNonEmptyMap(Map<?, ?> values, String name) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
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
        if (page.getSize() <= 0 || page.getSize() > properties.getPagination().getMaxPageSize()) {
            throw new IllegalArgumentException("PAGE_SIZE_INVALID");
        }
        return page;
    }

    private <E extends IPage<? extends Map<String, Object>>> E requireMapPage(E page) {
        Objects.requireNonNull(page, "page must not be null");
        if (page.getSize() <= 0 || page.getSize() > properties.getPagination().getMaxPageSize()) {
            throw new IllegalArgumentException("PAGE_SIZE_INVALID");
        }
        return page;
    }
}
