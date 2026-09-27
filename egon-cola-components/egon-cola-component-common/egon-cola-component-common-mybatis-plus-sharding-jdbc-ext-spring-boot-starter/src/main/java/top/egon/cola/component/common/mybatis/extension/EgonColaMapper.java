package top.egon.cola.component.common.mybatis.extension;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.common.mybatis.model.EgonModel;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Common Mapper type that only narrows the model self-type.
 *
 * @param <T> concrete EgonModel type
 */
@Validated
public interface EgonColaMapper<T extends EgonModel<T>> extends BaseMapper<T> {
    @Validated(EgonColaModelValidationGroups.Query.class)
    T selectActiveById(@Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class) Serializable id);

    @Validated(EgonColaModelValidationGroups.Query.class)
    List<T> selectActiveByIds(@Param("ids") @NotNull(groups = EgonColaModelValidationGroups.Query.class) Collection<? extends Serializable> ids);

    @Validated(EgonColaModelValidationGroups.Delete.class)
    int deleteVersionedById(@Param("et") @Valid @NotNull(groups = EgonColaModelValidationGroups.Delete.class) T entity);
}
