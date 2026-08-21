package top.egon.cola.component.common.mybatis.extension;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.egon.cola.component.common.mybatis.model.EgonModel;

/**
 * Common Mapper type that only narrows the model self-type.
 *
 * @param <T> concrete EgonModel type
 */
public interface EgonColaMapper<T extends EgonModel<T>> extends BaseMapper<T> {
}
