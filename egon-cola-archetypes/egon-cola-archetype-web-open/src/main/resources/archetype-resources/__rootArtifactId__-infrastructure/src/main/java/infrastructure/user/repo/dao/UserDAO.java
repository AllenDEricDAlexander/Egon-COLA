package ${package}.infrastructure.user.repo.dao;

import ${package}.infrastructure.user.repo.po.UserPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

@Mapper
public interface UserDAO extends EgonColaMapper<UserPO> {
    long countByEmail(@Param("email") String email);
}
