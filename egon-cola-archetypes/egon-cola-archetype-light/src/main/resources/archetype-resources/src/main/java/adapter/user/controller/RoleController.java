package ${package}.adapter.user.controller;

import ${package}.adapter.filter.RequestContext;
import ${package}.adapter.filter.RequestContextHolder;
import ${package}.adapter.user.convertor.UserAdapterConvertor;
import ${package}.adapter.user.dto.AssignRoleRequest;
import ${package}.adapter.user.vo.UserDetailVO;
import ${package}.application.user.command.AssignRoleCommand;
import ${package}.application.user.manage.RoleManage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleManage roleManage;

    @PostMapping
    public UserDetailVO assignRole(@PathVariable String userId, @Valid @RequestBody AssignRoleRequest request) {
        RequestContext context = RequestContextHolder.currentOrAnonymous();
        return UserAdapterConvertor.toUserDetail(roleManage.assignRole(new AssignRoleCommand(
                userId,
                request.roleCode(),
                context.operatorId(),
                context.requestId())));
    }
}
