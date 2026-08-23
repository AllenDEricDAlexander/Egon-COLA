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
    private final UserAdapterConvertor convertor;

    @PostMapping
    public UserDetailVO assignRole(@PathVariable String userId, @Valid @RequestBody AssignRoleRequest request) {
        RequestContext context = RequestContextHolder.currentOrAnonymous();
        return convertor.toUserDetail(roleManage.assignRole(new AssignRoleCommand(
                parseId(userId),
                request.roleCode(),
                context.operatorId(),
                context.requestId())));
    }

    private static long parseId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("userId must be a positive decimal Long", exception);
        }
    }
}
