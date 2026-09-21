package top.egon.cola.archetype.source.light.adapter.user.controller;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.archetype.source.light.adapter.filter.RequestContext;
import top.egon.cola.archetype.source.light.adapter.filter.RequestContextHolder;
import top.egon.cola.archetype.source.light.adapter.user.pojo.convertor.UserAdapterConvertor;
import top.egon.cola.archetype.source.light.adapter.user.pojo.dto.GrantPermissionRequest;
import top.egon.cola.archetype.source.light.adapter.user.pojo.vo.PermissionTreeVO;
import top.egon.cola.archetype.source.light.application.user.pojo.command.GrantPermissionCommand;
import top.egon.cola.archetype.source.light.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.light.application.user.pojo.query.GetUserPermissionsQuery;
import top.egon.cola.archetype.source.light.application.user.pojo.result.PermissionResult;
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
@Slf4j
public class PermissionController {
    private final PermissionManage permissionManage;
    private final UserAdapterConvertor convertor;

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
        return convertor.toPermissionTree(
                permissionManage.getByUser(new GetUserPermissionsQuery(Long.valueOf(userId))));
    }
}
