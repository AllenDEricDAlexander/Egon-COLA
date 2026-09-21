package top.egon.cola.archetype.source.lightopen.adapter.user.controller;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.archetype.source.lightopen.adapter.filter.RequestContext;
import top.egon.cola.archetype.source.lightopen.adapter.filter.RequestContextHolder;
import top.egon.cola.archetype.source.lightopen.adapter.user.pojo.convertor.UserAdapterConvertor;
import top.egon.cola.archetype.source.lightopen.adapter.user.pojo.dto.AssignRoleRequest;
import top.egon.cola.archetype.source.lightopen.adapter.user.pojo.vo.UserDetailVO;
import top.egon.cola.archetype.source.lightopen.application.user.pojo.command.AssignRoleCommand;
import top.egon.cola.archetype.source.lightopen.application.user.manage.RoleManage;
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
@Slf4j
public class RoleController {
    private final RoleManage roleManage;
    private final UserAdapterConvertor convertor;

    @PostMapping
    public UserDetailVO assignRole(@PathVariable String userId, @Valid @RequestBody AssignRoleRequest request) {
        RequestContext context = RequestContextHolder.currentOrAnonymous();
        return convertor.toTarget(roleManage.assignRole(new AssignRoleCommand(
                Long.valueOf(userId),
                request.roleCode(),
                context.operatorId(),
                context.requestId())));
    }
}
