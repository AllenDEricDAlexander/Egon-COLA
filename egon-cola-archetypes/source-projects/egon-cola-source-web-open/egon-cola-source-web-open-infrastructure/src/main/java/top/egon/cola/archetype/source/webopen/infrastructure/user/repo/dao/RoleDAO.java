package top.egon.cola.archetype.source.webopen.infrastructure.user.repo.dao;

import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.po.RolePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

@Mapper
public interface RoleDAO extends EgonColaMapper<RolePO> {
    RolePO selectByCode(@Param("code") String code);
}
