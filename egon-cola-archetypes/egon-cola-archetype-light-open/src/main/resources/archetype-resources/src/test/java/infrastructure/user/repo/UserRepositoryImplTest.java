package ${package}.infrastructure.user.repo;

import ${package}.domain.user.aggregates.RolePermissionAggregate;
import ${package}.domain.user.aggregates.UserAggregate;
import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.PermissionStatus;
import ${package}.domain.user.enums.RoleStatus;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.repos.PermissionRepository;
import ${package}.domain.user.repos.RoleRepository;
import ${package}.domain.user.repos.UserRepository;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.RoleCode;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.user.repo.converter.PermissionPOConverter;
import ${package}.infrastructure.user.repo.converter.RolePOConverter;
import ${package}.infrastructure.user.repo.converter.UserPOConverter;
import ${package}.infrastructure.user.repo.impl.PermissionRepositoryImpl;
import ${package}.infrastructure.user.repo.impl.RoleRepositoryImpl;
import ${package}.infrastructure.user.repo.impl.UserRepositoryImpl;
import ${package}.infrastructure.user.repo.mapper.PermissionMapper;
import ${package}.infrastructure.user.repo.mapper.RoleMapper;
import ${package}.infrastructure.user.repo.mapper.RolePermissionMapper;
import ${package}.infrastructure.user.repo.mapper.UserMapper;
import ${package}.infrastructure.user.repo.mapper.UserRoleMapper;
import ${package}.infrastructure.user.repo.po.PermissionPO;
import ${package}.infrastructure.user.repo.po.RolePO;
import ${package}.infrastructure.user.repo.po.RolePermissionPO;
import ${package}.infrastructure.user.repo.po.UserPO;
import ${package}.infrastructure.user.repo.po.UserRolePO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {
    private static final long USER_ID = 1001L;
    private static final long OTHER_USER_ID = 1004L;

    @Mock UserMapper userMapper;
    @Mock RoleMapper roleMapper;
    @Mock PermissionMapper permissionMapper;
    @Mock UserRoleMapper userRoleMapper;
    @Mock RolePermissionMapper rolePermissionMapper;
    @Mock UserPOConverter userConverter;
    @Mock RolePOConverter roleConverter;
    @Mock PermissionPOConverter permissionConverter;

    @InjectMocks UserRepositoryImpl userRepositoryImpl;
    @InjectMocks RoleRepositoryImpl roleRepositoryImpl;
    @InjectMocks PermissionRepositoryImpl permissionRepositoryImpl;

    @Test
    void persists_user_role_and_permission_aggregates_through_mappers() {
        User user = user(USER_ID, "mario@example.com");
        Role role = new Role(new RoleCode("teacher"), "Teacher", RoleStatus.ACTIVE);
        Permission permission = new Permission(
                new PermissionCode("course:read"), "Read courses", PermissionStatus.ACTIVE);
        UserPO userPO = new UserPO(USER_ID, "ext-" + USER_ID, "Mario",
                "mario@example.com", "ACTIVE", Instant.now());
        RolePO rolePO = new RolePO("teacher", "Teacher", "ACTIVE", Instant.now());
        PermissionPO permissionPO = new PermissionPO(
                "course:read", "Read courses", "ACTIVE", Instant.now());

        when(userConverter.toPO(user)).thenReturn(userPO);
        when(userConverter.toDomain(userPO)).thenReturn(user);
        when(roleConverter.toPO(role)).thenReturn(rolePO);
        when(roleConverter.toDomain(rolePO)).thenReturn(role);
        when(permissionConverter.toPO(permission)).thenReturn(permissionPO);
        when(permissionConverter.toDomain(permissionPO)).thenReturn(permission);
        when(userMapper.selectById(USER_ID)).thenReturn(null);
        when(roleMapper.selectById("teacher")).thenReturn(null);
        when(permissionMapper.selectById("course:read")).thenReturn(null);
        when(userMapper.insert(userPO)).thenReturn(1);
        when(roleMapper.insert(rolePO)).thenReturn(1);
        when(permissionMapper.insert(permissionPO)).thenReturn(1);
        when(userRoleMapper.insertRelation(any(UserRolePO.class))).thenReturn(1);
        when(rolePermissionMapper.insertRelation(any(RolePermissionPO.class))).thenReturn(1);
        when(userRoleMapper.findByUserId(USER_ID)).thenReturn(List.of(
                new UserRolePO(USER_ID, "teacher", Instant.now())));
        when(rolePermissionMapper.findByRoleCodes(anyCollection())).thenReturn(List.of(
                new RolePermissionPO("teacher", "course:read", Instant.now())));
        when(permissionMapper.findByCodesOrderByCode(anyCollection())).thenReturn(List.of(permissionPO));

        UserRepository userRepository = userRepositoryImpl;
        RoleRepository roleRepository = roleRepositoryImpl;
        PermissionRepository permissionRepository = permissionRepositoryImpl;

        assertEquals("Mario", userRepository.save(user).name());
        assertEquals("Teacher", roleRepository.save(role).name());
        assertEquals("Read courses", permissionRepository.save(permission).name());

        UserAggregate userAggregate = new UserAggregate(user);
        userAggregate.assign(role);
        userRepository.saveRoles(userAggregate);
        RolePermissionAggregate roleAggregate = new RolePermissionAggregate(role);
        roleAggregate.grant(permission);
        roleRepository.savePermissions(roleAggregate);

        assertEquals("course:read", permissionRepository.findByUserId(new UserId(USER_ID))
                .getFirst().code().value());
        verify(userRoleMapper).insertRelation(any(UserRolePO.class));
        verify(rolePermissionMapper).insertRelation(any(RolePermissionPO.class));
    }

    @Test
    void rejects_duplicate_email_from_mapper() {
        User user = user(OTHER_USER_ID, "mario@example.com");
        UserPO userPO = new UserPO(
                OTHER_USER_ID, "ext-" + OTHER_USER_ID, "Mario",
                "mario@example.com", "ACTIVE", Instant.now());
        when(userConverter.toPO(user)).thenReturn(userPO);
        when(userMapper.selectById(OTHER_USER_ID)).thenReturn(null);
        when(userMapper.insert(userPO)).thenThrow(new DuplicateKeyException("uk_users_email"));

        assertThrows(DuplicateKeyException.class, () -> userRepositoryImpl.save(user));
    }

    private User user(long id, String email) {
        return new User(new UserId(id), "ext-" + id, "Mario", email, UserStatus.ACTIVE);
    }
}
