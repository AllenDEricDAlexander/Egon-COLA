package top.egon.cola.archetype.source.lightopen.infrastructure.user.dao;

import top.egon.cola.archetype.source.lightopen.infrastructure.user.po.PermissionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.Collection;
import java.util.List;

/** MyBatis mapper for permissions. */
@Mapper
/** Shared active reads and versioned deletion are bound by this DAO XML. */
public interface PermissionDAO extends EgonColaMapper<PermissionPO> {
    List<PermissionPO> selectByCode(@Param("code") String code);

    List<PermissionPO> selectByCodeIn(@Param("codes") Collection<String> codes);
}
