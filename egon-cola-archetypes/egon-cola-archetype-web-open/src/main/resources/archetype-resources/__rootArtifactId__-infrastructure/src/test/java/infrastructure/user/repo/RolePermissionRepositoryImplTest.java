package ${package}.infrastructure.user.repo;

import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.enums.PermissionStatus;
import ${package}.domain.user.enums.PermissionType;
import ${package}.domain.user.enums.RoleStatus;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.domain.user.vos.RoleCode;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.user.repo.converter.PermissionPOConverter;
import ${package}.infrastructure.user.repo.converter.RolePOConverter;
import ${package}.infrastructure.user.repo.impl.PermissionRepositoryImpl;
import ${package}.infrastructure.user.repo.impl.RoleRepositoryImpl;
import ${package}.infrastructure.user.repo.mapper.PermissionMapper;
import ${package}.infrastructure.user.repo.mapper.RoleMapper;
import ${package}.infrastructure.user.repo.mapper.RolePermissionMapper;
import ${package}.infrastructure.user.repo.mapper.UserRoleMapper;
import ${package}.infrastructure.user.repo.po.PermissionPO;
import ${package}.infrastructure.user.repo.po.RolePO;
import ${package}.infrastructure.user.repo.po.RolePermissionPO;
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

class RolePermissionRepositoryImplTest {

    @Test
    void persistsRolePermissionRelationWithMapper() {
        RoleMapper roleMapper = mock(RoleMapper.class);
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        RolePermissionMapper relationMapper = mock(RolePermissionMapper.class);
        PermissionPO permission = new PermissionPO(
                3001L, "CLASS_READ", "Read class", "API", "ACTIVE", LocalDateTime.now());
        RolePO roleRow = new RolePO(2001L, "STUDENT", "Student", "ACTIVE", LocalDateTime.now());
        when(roleMapper.selectById(2001L)).thenReturn(null, roleRow);
        when(roleMapper.insert(any(RolePO.class))).thenReturn(1);
        when(permissionMapper.selectByCode("CLASS_READ")).thenReturn(permission);
        when(permissionMapper.selectPermissionsByIds(List.of(3001L))).thenReturn(List.of(permission));
        when(relationMapper.countByRoleIdAndPermissionId(2001L, 3001L)).thenReturn(0L);
        when(relationMapper.insert(any(RolePermissionPO.class))).thenReturn(1);
        when(relationMapper.selectByRoleId(2001L)).thenReturn(
                List.of(new RolePermissionPO(9001L, 2001L, 3001L, LocalDateTime.now())));

        RoleRepositoryImpl repository = new RoleRepositoryImpl(
                roleMapper,
                permissionMapper,
                relationMapper,
                new RolePOConverter(),
                (LongIdGenerator) () -> 9001L);

        Role saved = repository.save(new Role(
                2001L, new RoleCode("STUDENT"), "Student", RoleStatus.ACTIVE,
                List.of(new PermissionCode("CLASS_READ"))));

        assertThat(saved.permissionCodes()).containsExactly(new PermissionCode("CLASS_READ"));
        verify(relationMapper).insert(any(RolePermissionPO.class));
    }

    @Test
    void resolvesPermissionsFromUserRoleAndRolePermissionMappers() {
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        UserRoleMapper userRoleMapper = mock(UserRoleMapper.class);
        RolePermissionMapper relationMapper = mock(RolePermissionMapper.class);
        PermissionPO permission = new PermissionPO(
                3001L, "CLASS_READ", "Read class", "API", "ACTIVE", LocalDateTime.now());
        when(userRoleMapper.selectByUserId(1001L)).thenReturn(
                List.of(new UserRolePO(9001L, 1001L, 2001L, LocalDateTime.now())));
        when(relationMapper.selectByRoleIds(List.of(2001L))).thenReturn(
                List.of(new RolePermissionPO(9002L, 2001L, 3001L, LocalDateTime.now())));
        when(permissionMapper.selectPermissionsByIds(List.of(3001L))).thenReturn(List.of(permission));

        PermissionRepositoryImpl repository = new PermissionRepositoryImpl(
                permissionMapper,
                userRoleMapper,
                relationMapper,
                new PermissionPOConverter());

        assertThat(repository.findByUserId(new UserId(1001L)))
                .extracting(Permission::code)
                .containsExactly(new PermissionCode("CLASS_READ"));
    }
}
