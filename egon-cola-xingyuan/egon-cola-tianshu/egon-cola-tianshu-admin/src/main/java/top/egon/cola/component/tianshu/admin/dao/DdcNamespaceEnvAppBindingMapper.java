package top.egon.cola.component.tianshu.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.tianshu.admin.model.po.DdcNamespaceEnvAppBindingPO;

import java.util.List;
import java.util.Map;

@Mapper
@Validated
public interface DdcNamespaceEnvAppBindingMapper extends EgonColaMapper<DdcNamespaceEnvAppBindingPO> {
    DdcNamespaceEnvAppBindingPO selectActiveById(@Param("tenantId") Long tenantId, @Param("id") Long id);
    List<DdcNamespaceEnvAppBindingPO> selectActiveByIds(@Param("tenantId") Long tenantId,
            @Param("ids") List<Long> ids);
    DdcNamespaceEnvAppBindingPO selectActiveByPublicId(@Param("tenantId") Long tenantId,
            @Param("publicId") String publicId);
    long countByTuple(@Param("tenantId") Long tenantId, @Param("namespaceId") Long namespaceId,
            @Param("envCode") String envCode, @Param("appId") Long appId, @Param("excludeId") Long excludeId);
    long countByNamespaceId(@Param("tenantId") Long tenantId, @Param("namespaceId") Long namespaceId);
    long countByAppId(@Param("tenantId") Long tenantId, @Param("appId") Long appId);
    long countByEnvCode(@Param("tenantId") Long tenantId, @Param("envCode") String envCode);
    List<DdcNamespaceEnvAppBindingPO> selectByNamespaceAndEnv(@Param("tenantId") Long tenantId,
            @Param("namespaceId") Long namespaceId, @Param("envCode") String envCode,
            @Param("enabled") Boolean enabled);
    List<DdcNamespaceEnvAppBindingPO> selectJoinedList(@Param("tenantId") Long tenantId,
            @Param("bizCode") String bizCode, @Param("namespaceCode") String namespaceCode,
            @Param("envCode") String envCode, @Param("appCode") String appCode);
    List<DdcNamespaceEnvAppBindingPO> selectJoinedPage(@Param("tenantId") Long tenantId,
            @Param("bizCode") String bizCode, @Param("namespaceCode") String namespaceCode,
            @Param("envCode") String envCode, @Param("appCode") String appCode,
            @Param("offset") long offset, @Param("limit") int limit);
    long countJoinedPage(@Param("tenantId") Long tenantId, @Param("bizCode") String bizCode,
            @Param("namespaceCode") String namespaceCode, @Param("envCode") String envCode,
            @Param("appCode") String appCode);
    List<String> selectVisibleNamespaceCodes(@Param("tenantId") Long tenantId,
            @Param("bizCode") String bizCode, @Param("envCode") String envCode,
            @Param("appCode") String appCode);
    List<Map<String, Object>> selectVisiblePhysicalScopes(@Param("tenantId") Long tenantId,
            @Param("bizCode") String bizCode, @Param("namespaceCode") String namespaceCode);
}
