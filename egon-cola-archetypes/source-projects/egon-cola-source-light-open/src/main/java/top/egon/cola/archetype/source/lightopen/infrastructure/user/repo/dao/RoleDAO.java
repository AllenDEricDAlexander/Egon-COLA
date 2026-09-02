package top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.dao;

import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.po.RolePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.List;

/** MyBatis mapper for roles. */
@Mapper
public interface RoleDAO extends EgonColaMapper<RolePO> {
    List<RolePO> selectByCode(@Param("code") String code);
}
