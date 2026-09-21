package top.egon.cola.archetype.source.web.infrastructure.user.dao;

import top.egon.cola.archetype.source.web.infrastructure.user.po.RolePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

@Mapper
public interface RoleDAO extends EgonColaMapper<RolePO> {
    RolePO selectByCode(@Param("code") String code);
}
