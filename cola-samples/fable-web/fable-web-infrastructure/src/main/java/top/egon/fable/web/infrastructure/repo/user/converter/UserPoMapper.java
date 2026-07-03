package top.egon.fable.web.infrastructure.repo.user.converter;

import top.egon.fable.web.domain.entities.user.User;
import top.egon.fable.web.infrastructure.repo.user.po.UserPo;
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
