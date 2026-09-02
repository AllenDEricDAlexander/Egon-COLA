package top.egon.cola.archetype.source.light.adapter.user.controller;

import top.egon.cola.archetype.source.light.adapter.filter.RequestContext;
import top.egon.cola.archetype.source.light.adapter.filter.RequestContextHolder;
import top.egon.cola.archetype.source.light.adapter.user.convertor.UserAdapterConvertor;
import top.egon.cola.archetype.source.light.adapter.user.dto.AssignRoleRequest;
import top.egon.cola.archetype.source.light.adapter.user.vo.UserDetailVO;
import top.egon.cola.archetype.source.light.application.user.command.AssignRoleCommand;
import top.egon.cola.archetype.source.light.application.user.manage.RoleManage;
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
                Long.valueOf(userId),
                request.roleCode(),
                context.operatorId(),
                context.requestId())));
    }
}
