package ${package}.infrastructure.repo.user.converter;

import ${package}.domain.entities.user.User;
import ${package}.domain.enums.UserStatus;
import ${package}.infrastructure.repo.user.po.UserPo;
import io.github.linpeilie.BaseMapper;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component("userPoConverter")
@RequiredArgsConstructor
public class UserPoConverter {
    @Qualifier("converter")
    private final Converter converter;

    public UserPo toPo(User user) {
        UserPo userPo = converter.convert(user, UserPo.class);
        return new UserPo(userPo.getId(), userPo.getName(), userPo.getEmail(), user.getStatus().name(), LocalDateTime.now());
    }

    public User toEntity(UserPo userPo, List<String> schoolClassIds) {
        return User.restore(
                userPo.getId(),
                userPo.getName(),
                userPo.getEmail(),
                UserStatus.valueOf(userPo.getStatus()),
                schoolClassIds);
    }

    @Mapper(componentModel = "spring")
    public interface UserMapper extends BaseMapper<User, UserPo> {
        @Override
        @Mapping(target = "createdAt", ignore = true)
        @Mapping(target = "status", ignore = true)
        UserPo convert(User user);
    }
}
