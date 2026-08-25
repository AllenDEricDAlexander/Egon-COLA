package top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.controller.RoleResourceGrantController;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleResourceGrantControllerTest {

    @Test
    void exposesOnlyResourceTreeAndAtomicReplaceUnderIamRoles() {
        RequestMapping mapping = RoleResourceGrantController.class
                .getAnnotation(RequestMapping.class);
        assertEquals("/api/rbac3/v1/iam/roles/{roleId}/resources",
                mapping.value()[0]);

        Method tree = Arrays.stream(RoleResourceGrantController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(GetMapping.class))
                .findFirst()
                .orElseThrow();
        Method replace = Arrays.stream(RoleResourceGrantController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PutMapping.class))
                .findFirst()
                .orElseThrow();
        assertTrue(tree.getReturnType().getName().endsWith("ResultRecord"));
        assertTrue(replace.getReturnType().getName().endsWith("ResultRecord"));
        assertEquals("system:role-resource:read",
                tree.getAnnotation(RequiresPermission.class).value());
        assertEquals("system:role-resource:manage",
                replace.getAnnotation(RequiresPermission.class).value());
    }
}
