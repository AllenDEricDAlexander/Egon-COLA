package top.egon.cola.component.tianshu.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.tianshu.admin.model.po.DdcAppPO;

import java.util.List;

@Mapper
@Validated
public interface DdcAppMapper extends EgonColaMapper<DdcAppPO> {
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
