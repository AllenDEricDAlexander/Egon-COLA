package ${package}.infrastructure.repo.user.converter;

import ${package}.domain.entities.user.User;
import ${package}.infrastructure.repo.user.po.UserPo;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserPoMapper extends BaseMapper<User, UserPo> {
    @Override
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    UserPo convert(User user);
}
