package top.egon.cola.archetype.source.light.infrastructure.user.repo.dao;

import top.egon.cola.archetype.source.light.infrastructure.user.repo.po.PermissionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.Collection;
import java.util.List;

/** MyBatis mapper for permissions. */
@Mapper
public interface PermissionDAO extends EgonColaMapper<PermissionPO> {
    List<PermissionPO> selectByCode(@Param("code") String code);

    List<PermissionPO> selectByCodeIn(@Param("codes") Collection<String> codes);
}
