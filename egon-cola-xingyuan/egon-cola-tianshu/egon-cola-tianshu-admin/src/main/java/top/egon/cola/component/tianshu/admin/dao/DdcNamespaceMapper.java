package top.egon.cola.component.tianshu.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.tianshu.admin.model.po.DdcNamespacePO;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.io.Serializable;

@Mapper
@Validated
public interface DdcNamespaceMapper extends EgonColaMapper<DdcNamespacePO> {
    @Override
    default DdcNamespacePO selectActiveById(Serializable id) {
        return selectActiveById(EgonColaTenantIdProvider.currentTenantId(), requireInternalId(id));
    }
    @Override
    default List<DdcNamespacePO> selectActiveByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Long> internalIds = new ArrayList<>(ids.size());
        ids.forEach(id -> internalIds.add(requireInternalId(id)));
        return selectActiveByIds(EgonColaTenantIdProvider.currentTenantId(), internalIds);
    }
    private static Long requireInternalId(Serializable id) {
        if (!(id instanceof Long value) || value <= 0) throw new IllegalArgumentException("ID_MUST_BE_POSITIVE_LONG");
        return value;
    }

    DdcNamespacePO selectActiveById(@Param("tenantId") Long tenantId, @Param("id") Long id);
    List<DdcNamespacePO> selectActiveByIds(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);
    DdcNamespacePO selectActiveByPublicId(@Param("tenantId") Long tenantId, @Param("publicId") String publicId);
    DdcNamespacePO selectActiveByBizAndCode(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("namespaceCode") String namespaceCode);
    long countByBizCode(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode);
    long countByNamespace(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("namespace") String namespace, @Param("excludeId") Long excludeId);
    long countByNamespaceCode(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("namespaceCode") String namespaceCode, @Param("excludeId") Long excludeId);
    List<DdcNamespacePO> selectByBizCode(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode);
    List<DdcNamespacePO> selectList(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("namespaceKeyword") String namespaceKeyword, @Param("namespaceCodeKeyword") String namespaceCodeKeyword);
    List<DdcNamespacePO> selectPage(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("keyword") String keyword, @Param("offset") long offset, @Param("limit") int limit);
    long countPage(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("keyword") String keyword);
}
