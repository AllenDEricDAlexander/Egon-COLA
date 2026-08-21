package top.egon.cola.component.common.mybatis.extension;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.UpdateChainWrapper;
import com.baomidou.mybatisplus.extension.kotlin.KtQueryChainWrapper;
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Explicit MyBatis-Plus 3.5.16 IService contract for EgonModel persistence.
 *
 * @param <T> concrete EgonModel type
 */
public interface EgonColaIService<T extends EgonModel<T>> extends IService<T> {

    @Override
    boolean save(T entity);

    @Override
    boolean saveBatch(Collection<T> entityList, int batchSize);

    @Override
    boolean saveOrUpdateBatch(Collection<T> entityList, int batchSize);

    @Override
    boolean removeById(Serializable id);

    @Override
    boolean removeById(Serializable id, boolean useFill);

    @Override
    boolean removeById(T entity);

    @Override
    boolean removeByMap(Map<String, Object> columnMap);

    @Override
    boolean remove(Wrapper<T> queryWrapper);

    @Override
    boolean removeByIds(Collection<?> list);

    @Override
    boolean removeByIds(Collection<?> list, boolean useFill);

    @Override
    boolean updateById(T entity);

    @Override
    boolean update(Wrapper<T> updateWrapper);

    @Override
    boolean update(T entity, Wrapper<T> updateWrapper);

    @Override
    boolean updateBatchById(Collection<T> entityList, int batchSize);

    @Override
    boolean saveOrUpdate(T entity);

    @Override
    T getById(Serializable id);

    @Override
    Optional<T> getOptById(Serializable id);

    @Override
    List<T> listByIds(Collection<? extends Serializable> idList);

    @Override
    List<T> listByMap(Map<String, Object> columnMap);

    @Override
    T getOne(Wrapper<T> queryWrapper);

    @Override
    Optional<T> getOneOpt(Wrapper<T> queryWrapper);

    @Override
    T getOne(Wrapper<T> queryWrapper, boolean throwEx);

    @Override
    Optional<T> getOneOpt(Wrapper<T> queryWrapper, boolean throwEx);

    @Override
    Map<String, Object> getMap(Wrapper<T> queryWrapper);

    @Override
    <V> V getObj(Wrapper<T> queryWrapper, Function<? super Object, V> mapper);

    @Override
    boolean exists(Wrapper<T> queryWrapper);

    @Override
    long count();

    @Override
    long count(Wrapper<T> queryWrapper);

    @Override
    List<T> list(Wrapper<T> queryWrapper);

    @Override
    List<T> list(IPage<T> page, Wrapper<T> queryWrapper);

    @Override
    List<T> list();

    @Override
    List<T> list(IPage<T> page);

    @Override
    <E extends IPage<T>> E page(E page, Wrapper<T> queryWrapper);

    @Override
    <E extends IPage<T>> E page(E page);

    @Override
    List<Map<String, Object>> listMaps(Wrapper<T> queryWrapper);

    @Override
    List<Map<String, Object>> listMaps(IPage<? extends Map<String, Object>> page, Wrapper<T> queryWrapper);

    @Override
    List<Map<String, Object>> listMaps();

    @Override
    List<Map<String, Object>> listMaps(IPage<? extends Map<String, Object>> page);

    @Override
    <E> List<E> listObjs();

    @Override
    <V> List<V> listObjs(Function<? super Object, V> mapper);

    @Override
    <E> List<E> listObjs(Wrapper<T> queryWrapper);

    @Override
    <V> List<V> listObjs(Wrapper<T> queryWrapper, Function<? super Object, V> mapper);

    @Override
    <E extends IPage<Map<String, Object>>> E pageMaps(E page, Wrapper<T> queryWrapper);

    @Override
    <E extends IPage<Map<String, Object>>> E pageMaps(E page);

    @Override
    BaseMapper<T> getBaseMapper();

    @Override
    Class<T> getEntityClass();

    @Override
    QueryChainWrapper<T> query();

    @Override
    LambdaQueryChainWrapper<T> lambdaQuery();

    @Override
    LambdaQueryChainWrapper<T> lambdaQuery(T entity);

    @Override
    KtQueryChainWrapper<T> ktQuery();

    @Override
    KtUpdateChainWrapper<T> ktUpdate();

    @Override
    UpdateChainWrapper<T> update();

    @Override
    LambdaUpdateChainWrapper<T> lambdaUpdate();

    @Override
    boolean saveBatch(Collection<T> entityList);

    @Override
    boolean saveOrUpdateBatch(Collection<T> entityList);

    @Override
    boolean removeBatchByIds(Collection<?> list);

    @Override
    boolean updateBatchById(Collection<T> entityList);
}
