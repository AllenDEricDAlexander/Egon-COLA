package ${package}.adapter.user.controller;

import ${package}.adapter.filter.RequestContext;
import ${package}.adapter.filter.RequestContextHolder;
import ${package}.adapter.user.convertor.UserAdapterConvertor;
import ${package}.adapter.user.dto.GrantPermissionRequest;
import ${package}.adapter.user.vo.PermissionTreeVO;
import ${package}.application.user.command.GrantPermissionCommand;
import ${package}.application.user.manage.PermissionManage;
import ${package}.application.user.query.GetUserPermissionsQuery;
import ${package}.application.user.result.PermissionResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PermissionController {
    private final PermissionManage permissionManage;

    @PostMapping("/roles/{roleCode}/permissions")
    public PermissionResult grantPermission(
            @PathVariable String roleCode,
            @Valid @RequestBody GrantPermissionRequest request) {
        RequestContext context = RequestContextHolder.currentOrAnonymous();
        return permissionManage.grantPermission(new GrantPermissionCommand(
                roleCode,
                request.permissionCode(),
                context.operatorId(),
                context.requestId()));
    }

    @GetMapping("/users/{userId}/permissions")
    public List<PermissionTreeVO> getUserPermissions(@PathVariable String userId) {
        return UserAdapterConvertor.toPermissionTree(
                permissionManage.getByUser(new GetUserPermissionsQuery(userId)));
    }
}
