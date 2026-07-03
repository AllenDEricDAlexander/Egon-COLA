package ${package}.adapter.convertor;

import ${package}.domain.entities.user.User;
import ${package}.facade.dto.user.UserDTO;
import io.github.linpeilie.BaseMapper;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("userAdapterConverter")
@RequiredArgsConstructor
public class UserAdapterConverter {
    @Qualifier("converter")
    private final Converter converter;

    public UserDTO toDto(User user) {
        UserDTO dto = converter.convert(user, UserDTO.class);
        dto.setStatus(user.getStatus().name());
        return dto;
    }

    @Mapper(componentModel = "spring")
    public interface UserMapper extends BaseMapper<User, UserDTO> {
        @Override
        @Mapping(target = "status", ignore = true)
        UserDTO convert(User user);
    }
}
