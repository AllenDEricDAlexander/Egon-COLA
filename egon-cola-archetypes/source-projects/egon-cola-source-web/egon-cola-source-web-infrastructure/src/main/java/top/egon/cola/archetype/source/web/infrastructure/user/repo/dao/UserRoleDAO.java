package top.egon.cola.archetype.source.web.infrastructure.user.repo.dao;

import top.egon.cola.archetype.source.web.infrastructure.user.repo.po.UserRolePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface UserRoleDAO extends EgonColaMapper<UserRolePO> {
    List<UserRolePO> selectByUserId(@Param("userId") Long userId);

    List<UserRolePO> selectByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    long countByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
