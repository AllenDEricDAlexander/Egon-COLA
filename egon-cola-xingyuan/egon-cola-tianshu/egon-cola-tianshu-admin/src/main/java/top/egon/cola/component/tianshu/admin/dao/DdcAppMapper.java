package top.egon.cola.component.tianshu.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.tianshu.admin.model.po.DdcAppPO;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.io.Serializable;

@Mapper
@Validated
public interface DdcAppMapper extends EgonColaMapper<DdcAppPO> {
    @Override
    default DdcAppPO selectActiveById(Serializable id) {
        return selectActiveById(EgonColaTenantIdProvider.currentTenantId(), requireInternalId(id));
    }
    @Override
    default List<DdcAppPO> selectActiveByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Long> internalIds = new ArrayList<>(ids.size());
        ids.forEach(id -> internalIds.add(requireInternalId(id)));
        return selectActiveByIds(EgonColaTenantIdProvider.currentTenantId(), internalIds);
    }
    private static Long requireInternalId(Serializable id) {
        if (!(id instanceof Long value) || value <= 0) throw new IllegalArgumentException("ID_MUST_BE_POSITIVE_LONG");
        return value;
    }

    DdcAppPO selectActiveById(@Param("tenantId") Long tenantId, @Param("id") Long id);
    List<DdcAppPO> selectActiveByIds(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);
    DdcAppPO selectActiveByPublicId(@Param("tenantId") Long tenantId, @Param("publicId") String publicId);
    DdcAppPO selectActiveByBizAndAppCode(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("appCode") String appCode);
    DdcAppPO selectFirstByAppCode(@Param("tenantId") Long tenantId, @Param("appCode") String appCode);
    long countByAppCode(@Param("tenantId") Long tenantId, @Param("appCode") String appCode,
            @Param("excludeId") Long excludeId);
    long countByBizCode(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode);
    List<DdcAppPO> selectByBizCode(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode);
    List<DdcAppPO> selectText(@Param("tenantId") Long tenantId,
            @Param("appCodeKeyword") String appCodeKeyword, @Param("appNameKeyword") String appNameKeyword,
            @Param("bizCode") String bizCode, @Param("enabled") Boolean enabled);
    List<DdcAppPO> selectPage(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("namespaceCode") String namespaceCode, @Param("env") String env,
            @Param("keyword") String keyword, @Param("enabled") Boolean enabled,
            @Param("offset") long offset, @Param("limit") int limit);
    long countPage(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("namespaceCode") String namespaceCode, @Param("env") String env,
            @Param("keyword") String keyword, @Param("enabled") Boolean enabled);
}
