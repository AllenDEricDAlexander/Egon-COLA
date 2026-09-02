package top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.dao;

import top.egon.cola.archetype.source.lightopen.infrastructure.user.repo.po.UserPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.List;

/** MyBatis mapper for users. */
@Mapper
public interface UserDAO extends EgonColaMapper<UserPO> {
    List<UserPO> selectByExternalId(@Param("externalId") String externalId);
}
