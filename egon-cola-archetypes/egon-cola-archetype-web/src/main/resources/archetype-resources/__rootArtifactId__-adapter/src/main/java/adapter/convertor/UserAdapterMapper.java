package ${package}.adapter.convertor;

import ${package}.domain.entities.user.User;
import ${package}.facade.dto.user.UserDTO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserAdapterMapper extends BaseMapper<User, UserDTO> {
    @Override
    @Mapping(target = "status", ignore = true)
    UserDTO convert(User user);
}
