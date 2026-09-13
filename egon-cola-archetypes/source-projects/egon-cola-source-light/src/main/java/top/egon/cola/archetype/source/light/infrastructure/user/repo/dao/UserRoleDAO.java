package top.egon.cola.archetype.source.light.infrastructure.user.repo.dao;

import top.egon.cola.archetype.source.light.infrastructure.user.repo.po.UserRolePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.List;

/** MyBatis mapper for user-role links. */
@Mapper
/** Shared active reads and versioned deletion are bound by this DAO XML. */
public interface UserRoleDAO extends EgonColaMapper<UserRolePO> {
    List<UserRolePO> selectByUserId(@Param("userId") Long userId);
}
