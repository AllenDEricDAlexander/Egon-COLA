package ${package}.infrastructure.repo.user.converter;

import ${package}.domain.entities.user.User;
import ${package}.infrastructure.repo.user.po.UserPo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component("userPoConverter")
@RequiredArgsConstructor
public class UserPoConverter {
    @Qualifier("userPoMapperImpl")
    private final UserPoMapper userPoMapper;

    @Qualifier("userDomainMapperImpl")
    private final UserDomainMapper userDomainMapper;

    public UserPo toPo(User user) {
        UserPo userPo = userPoMapper.convert(user);
        return new UserPo(userPo.getId(), userPo.getName(), userPo.getEmail(), user.getStatus().name(), LocalDateTime.now());
    }

    public User toEntity(UserPo userPo, List<String> schoolClassIds) {
        User user = userDomainMapper.convert(userPo);
        return User.restore(user.getId(), user.getName(), user.getEmail(), user.getStatus(), schoolClassIds);
    }
}
