package top.egon.cola.component.common.mybatis.extension;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Common Mapper type that only narrows the model self-type.
 *
 * @param <T> concrete EgonModel type
 */
public interface EgonColaMapper<T extends EgonModel<T>> extends BaseMapper<T> {
    T selectActiveById(@Param("id") Serializable id);

    List<T> selectActiveByIds(@Param("ids") Collection<? extends Serializable> ids);

    int deleteVersionedById(@Param("et") T entity);
}
