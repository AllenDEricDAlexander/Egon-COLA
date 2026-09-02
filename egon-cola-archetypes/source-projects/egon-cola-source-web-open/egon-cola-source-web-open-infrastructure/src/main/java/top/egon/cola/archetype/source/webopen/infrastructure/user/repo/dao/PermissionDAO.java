package top.egon.cola.archetype.source.webopen.infrastructure.user.repo.dao;

import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.po.PermissionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface PermissionDAO extends EgonColaMapper<PermissionPO> {
    PermissionPO selectByCode(@Param("code") String code);

    List<PermissionPO> selectPermissionsByIds(@Param("ids") Collection<Long> ids);
}
