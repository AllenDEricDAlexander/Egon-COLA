package ${package}.infrastructure.repo.user.converter;

import ${package}.domain.entities.user.User;
import ${package}.infrastructure.repo.user.po.UserPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserDomainFactory.class)
public interface UserDomainMapper extends BaseMapper<UserPo, User> {
    @Override
    @Mapping(target = "schoolClassIds", ignore = true)
    User convert(UserPo userPo);
}
