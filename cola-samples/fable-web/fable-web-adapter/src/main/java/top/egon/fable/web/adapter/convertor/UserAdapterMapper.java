package top.egon.fable.web.adapter.convertor;

import top.egon.fable.web.domain.entities.user.User;
import top.egon.fable.web.facade.dto.user.UserDTO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserAdapterMapper extends BaseMapper<User, UserDTO> {
    @Override
    @Mapping(target = "status", ignore = true)
    UserDTO convert(User user);
}
