package ${package}.infrastructure.user.repo;

import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.vos.RoleCode;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.user.repo.converter.UserPOConverter;
import ${package}.infrastructure.user.repo.impl.UserRepositoryImpl;
import ${package}.infrastructure.user.repo.mapper.RoleMapper;
import ${package}.infrastructure.user.repo.mapper.UserMapper;
import ${package}.infrastructure.user.repo.mapper.UserRoleMapper;
import ${package}.infrastructure.user.repo.po.RolePO;
import ${package}.infrastructure.user.repo.po.UserPO;
import ${package}.infrastructure.user.repo.po.UserRolePO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.id.generator.LongIdGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserRepositoryImplTest {

    @Test
    void savesAndRestoresUserThroughMappersAndRelationMapper() {
        UserMapper userMapper = mock(UserMapper.class);
        UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        RolePO role = new RolePO(2001L, "STUDENT", "Student", "ACTIVE", LocalDateTime.now());
        UserPO row = new UserPO(
                1001L, "Mario", "mario@example.com", "ACTIVE", LocalDateTime.now());
        when(userMapper.selectById(1001L)).thenReturn(null, row);
        when(userMapper.insert(any(UserPO.class))).thenReturn(1);
        when(roleMapper.selectByCode("STUDENT")).thenReturn(role);
        when(roleMapper.selectById(2001L)).thenReturn(role);
        when(userRoleMapper.countByUserIdAndRoleId(1001L, 2001L)).thenReturn(0L);
        when(userRoleMapper.insert(any(UserRolePO.class))).thenReturn(1);
        when(userRoleMapper.selectByUserId(1001L)).thenReturn(
                List.of(new UserRolePO(9001L, 1001L, 2001L, LocalDateTime.now())));

        UserRepositoryImpl repository = new UserRepositoryImpl(
                userMapper,
                userRoleMapper,
                roleMapper,
                new UserPOConverter(),
                (LongIdGenerator) () -> 9001L);

        User saved = repository.save(new User(
                new UserId(1001L), "Mario", "mario@example.com", UserStatus.ACTIVE,
                List.of(new RoleCode("STUDENT"))));

        assertThat(saved.roleCodes()).containsExactly(new RoleCode("STUDENT"));
        assertThat(repository.findById(saved.id())).get()
                .extracting(User::email, User::status)
                .containsExactly("mario@example.com", UserStatus.ACTIVE);
        assertThat(repository.existsByEmail("mario@example.com")).isFalse();
        verify(userMapper).insert(any(UserPO.class));
        verify(userRoleMapper).insert(any(UserRolePO.class));
    }
}
