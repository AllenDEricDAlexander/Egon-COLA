package top.egon.cola.component.tianshu.admin.dao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.tianshu.admin.model.po.DdcEnvPO;

import java.util.List;

@Mapper
@Validated
public interface DdcEnvMapper extends EgonColaMapper<DdcEnvPO> {
    DdcEnvPO selectActiveById(@Param("tenantId") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long tenantId,
            @Param("id") @NotNull(groups = EgonColaModelValidationGroups.Query.class)
            @Positive(groups = EgonColaModelValidationGroups.Query.class) Long id);
    List<DdcEnvPO> selectActiveByIds(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);
    DdcEnvPO selectActiveByEnvCode(@Param("tenantId") Long tenantId, @Param("envCode") String envCode);
    long countByEnvCode(@Param("tenantId") Long tenantId, @Param("envCode") String envCode,
            @Param("excludeId") Long excludeId);
    List<DdcEnvPO> selectAllActive(@Param("tenantId") Long tenantId);
    List<DdcEnvPO> selectByText(@Param("tenantId") Long tenantId, @Param("envCodeKeyword") String envCodeKeyword,
            @Param("descriptionKeyword") String descriptionKeyword);
    List<DdcEnvPO> selectPage(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("namespaceCode") String namespaceCode, @Param("keyword") String keyword,
            @Param("offset") long offset, @Param("limit") int limit);
    long countPage(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("namespaceCode") String namespaceCode, @Param("keyword") String keyword);
}
