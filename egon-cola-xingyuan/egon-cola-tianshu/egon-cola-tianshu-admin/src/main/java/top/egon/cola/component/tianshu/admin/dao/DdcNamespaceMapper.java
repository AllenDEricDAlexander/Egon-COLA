package top.egon.cola.component.tianshu.admin.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;
import top.egon.cola.component.tianshu.admin.model.po.DdcNamespacePO;

import java.util.List;

@Mapper
@Validated
public interface DdcNamespaceMapper extends EgonColaMapper<DdcNamespacePO> {
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
