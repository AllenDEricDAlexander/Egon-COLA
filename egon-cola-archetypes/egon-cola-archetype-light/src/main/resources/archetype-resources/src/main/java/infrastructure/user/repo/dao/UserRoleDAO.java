package ${package}.infrastructure.user.repo.dao;

import ${package}.infrastructure.user.repo.po.UserRolePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.List;

/** MyBatis mapper for user-role links. */
@Mapper
public interface UserRoleDAO extends EgonColaMapper<UserRolePO> {
    List<UserRolePO> selectByUserId(@Param("userId") Long userId);
}
